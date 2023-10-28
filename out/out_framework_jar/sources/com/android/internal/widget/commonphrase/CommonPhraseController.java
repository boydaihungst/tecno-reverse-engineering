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
import com.android.internal.widget.commonphrase.CommonPhraseSpinner;
/* loaded from: classes4.dex */
public class CommonPhraseController {
    private static final String ACTION_ADD_PHRASE = "android.settings.ADD_COMMON_PHRASE";
    private static final String ACTION_PHRASE_SETTINGS = "android.settings.COMMON_PHRASE_SETTINGS";
    private static final String NEW_ACTION_ADD_PHRASE = "android.settings.NEW_ADD_COMMON_PHRASE";
    private static final String NEW_ACTION_PHRASE_SETTINGS = "android.settings.NEW_COMMON_PHRASE_SETTINGS";
    private static final String TAG = "CommonPhraseController";
    Context mContext;
    CommonPhraseSpinner mPhraseSpinner;
    TextView mTargetView;
    int mContentXOffset = 0;
    int mContentYOffset = 0;
    private Rect mWindowRect = new Rect();
    private CommonPhraseSpinner.OnClickListener mOnSpinnerClickListener = new CommonPhraseSpinner.OnClickListener() { // from class: com.android.internal.widget.commonphrase.CommonPhraseController.1
        @Override // com.android.internal.widget.commonphrase.CommonPhraseSpinner.OnClickListener
        public void onTileClick() {
            Intent intent = new Intent();
            intent.setAction(CommonPhraseController.NEW_ACTION_PHRASE_SETTINGS);
            intent.setFlags(268435456);
            try {
                CommonPhraseController.this.mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(CommonPhraseController.TAG, "onTileClick: start activity filed, action =" + intent.getAction());
            }
            CommonPhraseController.this.mPhraseSpinner.dismiss();
        }

        @Override // com.android.internal.widget.commonphrase.CommonPhraseSpinner.OnClickListener
        public void onItemClick(String str) {
            Editable editor = CommonPhraseController.this.mTargetView.getEditableText();
            if (editor != null) {
                editor.insert(CommonPhraseController.this.mTargetView.getSelectionEnd(), str);
            }
            CommonPhraseController.this.mPhraseSpinner.dismiss();
        }
    };
    private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.android.internal.widget.commonphrase.CommonPhraseController$$ExternalSyntheticLambda0
        @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
        public final void onGlobalLayout() {
            CommonPhraseController.this.m7185x517a54b1();
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-internal-widget-commonphrase-CommonPhraseController  reason: not valid java name */
    public /* synthetic */ void m7185x517a54b1() {
        Rect tempRect = new Rect();
        this.mTargetView.getWindowVisibleDisplayFrame(tempRect);
        if (!tempRect.equals(this.mWindowRect)) {
            this.mWindowRect = tempRect;
            this.mPhraseSpinner.dismiss();
        }
    }

    public CommonPhraseController(TextView targetView, Context context) {
        this.mTargetView = targetView;
        this.mContext = context;
        CommonPhraseSpinner commonPhraseSpinner = new CommonPhraseSpinner(targetView, context);
        this.mPhraseSpinner = commonPhraseSpinner;
        commonPhraseSpinner.setClickListener(this.mOnSpinnerClickListener);
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
        this.mPhraseSpinner.addPhrases(CommonPhraseRepository.queryWords(this.mContext));
        this.mPhraseSpinner.refreshOffset(this.mContentXOffset, this.mContentYOffset);
        this.mPhraseSpinner.show();
    }

    public void refreshOffset(int x, int y) {
        this.mContentXOffset = x;
        this.mContentYOffset = y;
        Rect tempRect = new Rect();
        this.mTargetView.getWindowVisibleDisplayFrame(tempRect);
        if (!tempRect.equals(this.mWindowRect)) {
            this.mWindowRect = tempRect;
            if (this.mPhraseSpinner.isShowing()) {
                this.mPhraseSpinner.dismiss();
                this.mPhraseSpinner.refreshOffset(this.mContentXOffset, this.mContentYOffset);
                this.mPhraseSpinner.show();
            }
        }
    }

    public void onFocusChanged(boolean focused) {
        if (!focused) {
            this.mPhraseSpinner.dismiss();
        }
    }

    public void onVisibilityChanged(int visibility) {
        if (visibility != 0) {
            this.mPhraseSpinner.dismiss();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        this.mPhraseSpinner.dismiss();
    }

    public void onDetachedFromWindow() {
        this.mPhraseSpinner.setClickListener(null);
        this.mTargetView.getViewTreeObserver().removeOnGlobalLayoutListener(this.mOnGlobalLayoutListener);
        this.mPhraseSpinner.dismiss();
    }

    public boolean existPhrase() {
        return CommonPhraseRepository.queryWords(this.mContext).size() != 0;
    }
}
