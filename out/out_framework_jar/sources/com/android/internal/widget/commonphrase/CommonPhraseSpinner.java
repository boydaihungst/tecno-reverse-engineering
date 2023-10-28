package com.android.internal.widget.commonphrase;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.text.Layout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.transsion.internal.R;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes4.dex */
public class CommonPhraseSpinner {
    private static int MAX_DISPLAY_PHRASES = 3;
    private PhraseAdapter mAdapter;
    private final TextView mAnchorView;
    private OnClickListener mClickListener;
    private FrameLayout mContentView;
    private Context mContext;
    private ListView mDropDownList;
    private int mMarginHorizontal;
    private int mMarginVertical;
    private PopupWindow mPopup;
    private List<String> mPhrasesList = new ArrayList();
    private int mXOffset = 0;
    private int mYOffset = 0;
    private final Point mCoordsOnWindow = new Point();
    private Rect mDisplayRect = new Rect();
    private Rect mVisibleDisplayRect = new Rect();
    private Point mScreenSize = new Point();

    /* loaded from: classes4.dex */
    public interface OnClickListener {
        void onItemClick(String str);

        void onTileClick();
    }

    public CommonPhraseSpinner(TextView anchorView, Context context) {
        this.mContext = context;
        this.mAnchorView = anchorView;
        this.mContext = context;
        buildDropDown();
        this.mMarginHorizontal = anchorView.getResources().getDimensionPixelSize(R.dimen.os_common_phrase_spinner_horizontal_margin);
        this.mMarginVertical = anchorView.getResources().getDimensionPixelSize(R.dimen.os_common_phrase_spinner_vertical_margin);
        if (anchorView.getDisplay() != null) {
            anchorView.getDisplay().getRealSize(this.mScreenSize);
        }
    }

    public void addPhrases(List<String> phrases) {
        this.mPhrasesList.clear();
        this.mPhrasesList.addAll(phrases);
    }

    private void buildDropDown() {
        this.mAdapter = new PhraseAdapter();
        PopupWindow popupWindow = new PopupWindow(this.mContext);
        this.mPopup = popupWindow;
        popupWindow.setClippingEnabled(false);
        this.mPopup.setWindowLayoutType(1005);
        this.mPopup.setAnimationStyle(0);
        this.mContentView = new FrameLayout(this.mContext);
        ViewGroup dropDownView = (ViewGroup) LayoutInflater.from(this.mContext).inflate(R.layout.os_common_phrase_list, this.mContentView);
        ListView listView = (ListView) dropDownView.findViewById(R.id.lv_phrase);
        this.mDropDownList = listView;
        listView.setAdapter((ListAdapter) this.mAdapter);
        this.mDropDownList.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.android.internal.widget.commonphrase.CommonPhraseSpinner$$ExternalSyntheticLambda0
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                CommonPhraseSpinner.this.m7194x73c3f5e5(adapterView, view, i, j);
            }
        });
        dropDownView.setFocusable(true);
        dropDownView.setFocusableInTouchMode(true);
        dropDownView.findViewById(R.id.ll_phrase_title).setBackground(this.mContext.getDrawable(R.drawable.os_common_phrase_title_background));
        dropDownView.findViewById(R.id.ll_phrase_title).setOnClickListener(new View.OnClickListener() { // from class: com.android.internal.widget.commonphrase.CommonPhraseSpinner$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                CommonPhraseSpinner.this.m7195x8026584(view);
            }
        });
        this.mPopup.setOutsideTouchable(true);
        this.mPopup.setBackgroundDrawable(new ColorDrawable(0));
        dropDownView.setClipToOutline(true);
        this.mPopup.setContentView(this.mContentView);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$buildDropDown$0$com-android-internal-widget-commonphrase-CommonPhraseSpinner  reason: not valid java name */
    public /* synthetic */ void m7194x73c3f5e5(AdapterView parent, View view, int position, long id) {
        if (this.mClickListener != null && position < this.mPhrasesList.size()) {
            this.mClickListener.onItemClick(this.mPhrasesList.get(position));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$buildDropDown$1$com-android-internal-widget-commonphrase-CommonPhraseSpinner  reason: not valid java name */
    public /* synthetic */ void m7195x8026584(View view) {
        OnClickListener onClickListener = this.mClickListener;
        if (onClickListener != null) {
            onClickListener.onTileClick();
        }
    }

    public void refreshOffset(int x, int y) {
        this.mXOffset = x;
        this.mYOffset = y;
    }

    private void refreshWindowSize() {
        this.mAdapter.notifyDataSetChanged();
        int height = 0;
        int count = this.mAdapter.getCount();
        int screenWidth = Math.min(this.mScreenSize.x, this.mScreenSize.y);
        int width = Math.min(this.mVisibleDisplayRect.right - this.mVisibleDisplayRect.left, screenWidth);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, Integer.MIN_VALUE);
        for (int i = 0; i < MAX_DISPLAY_PHRASES; i++) {
            if (i < count) {
                View itemView = this.mAdapter.getView(i, null, this.mDropDownList);
                itemView.measure(widthSpec, 0);
                itemView.getParent();
                height = height + itemView.getMeasuredHeight() + this.mDropDownList.getDividerHeight();
            }
        }
        LinearLayout.LayoutParams ListParams = new LinearLayout.LayoutParams(-1, height - this.mDropDownList.getDividerHeight());
        this.mDropDownList.setLayoutParams(ListParams);
        this.mContentView.measure(widthSpec, 0);
        this.mPopup.setHeight(this.mContentView.getMeasuredHeight());
        this.mPopup.setWidth(width);
    }

    public void show() {
        refreshRootWindowDisplayFrame();
        refreshWindowSize();
        repositionWindow();
        this.mPopup.showAtLocation(this.mAnchorView, 0, this.mCoordsOnWindow.x, this.mCoordsOnWindow.y);
    }

    public void refreshRootWindowDisplayFrame() {
        this.mAnchorView.getRootView().getWindowVisibleDisplayFrame(this.mVisibleDisplayRect);
        this.mAnchorView.getRootView().getWindowDisplayFrame(this.mDisplayRect);
    }

    private Rect getCursorRect() {
        Layout layout = this.mAnchorView.getLayout();
        int line = layout.getLineForOffset(this.mAnchorView.getSelectionStart());
        int primaryHorizontal = clampHorizontalPosition(layout.getPrimaryHorizontal(this.mAnchorView.getSelectionStart()));
        Rect selectionBounds = new Rect();
        selectionBounds.set(primaryHorizontal, layout.getLineTop(line), primaryHorizontal, layout.getLineBottom(line));
        selectionBounds.set((int) Math.floor(selectionBounds.left + this.mXOffset + 1), (int) Math.floor(selectionBounds.top + this.mYOffset), (int) Math.ceil(selectionBounds.right + this.mXOffset + 1), (int) Math.ceil(selectionBounds.bottom + this.mYOffset));
        return selectionBounds;
    }

    private void repositionWindow() {
        int y;
        Rect contentRectOnScreen = getCursorRect();
        int[] rootViewPositionOnScreen = new int[2];
        this.mAnchorView.getRootView().getLocationOnScreen(rootViewPositionOnScreen);
        int[] viewPositionOnScreen = new int[2];
        this.mAnchorView.getLocationOnScreen(viewPositionOnScreen);
        Rect viewRectOnScreen = new Rect();
        this.mAnchorView.getGlobalVisibleRect(viewRectOnScreen);
        viewRectOnScreen.offset(rootViewPositionOnScreen[0], rootViewPositionOnScreen[1]);
        ViewParent parent = this.mAnchorView.getParent();
        if (parent instanceof ViewGroup) {
            parent.getChildVisibleRect(this.mAnchorView, contentRectOnScreen, null);
            contentRectOnScreen.offset(rootViewPositionOnScreen[0], rootViewPositionOnScreen[1]);
        } else {
            contentRectOnScreen.offset(viewPositionOnScreen[0], viewPositionOnScreen[1]);
        }
        contentRectOnScreen.set(Math.max(contentRectOnScreen.left, viewRectOnScreen.left), Math.max(contentRectOnScreen.top, viewRectOnScreen.top), Math.min(contentRectOnScreen.right, viewRectOnScreen.right), Math.min(contentRectOnScreen.bottom, viewRectOnScreen.bottom));
        int x = Math.min(contentRectOnScreen.centerX() - (this.mPopup.getWidth() / 2), this.mVisibleDisplayRect.right - this.mPopup.getWidth());
        int x2 = Math.max(x, this.mVisibleDisplayRect.left);
        int availableHeightAboveContent = contentRectOnScreen.top - this.mVisibleDisplayRect.top;
        int availableHeightBelowContent = this.mVisibleDisplayRect.bottom - contentRectOnScreen.bottom;
        int i = this.mDisplayRect.bottom - contentRectOnScreen.bottom;
        int maxHeigntAboveContent = contentRectOnScreen.top - this.mDisplayRect.top;
        int spinnerHeight = this.mPopup.getHeight();
        int spinnerHeightWithSignalVerticalMargin = spinnerHeight - this.mMarginVertical;
        if (availableHeightAboveContent >= spinnerHeightWithSignalVerticalMargin) {
            y = contentRectOnScreen.top - spinnerHeight;
        } else if (availableHeightBelowContent >= spinnerHeightWithSignalVerticalMargin) {
            y = contentRectOnScreen.bottom;
        } else if (maxHeigntAboveContent >= spinnerHeightWithSignalVerticalMargin) {
            y = contentRectOnScreen.top - spinnerHeight;
        } else {
            y = contentRectOnScreen.bottom;
        }
        int[] rootViewPositionOnWindow = new int[2];
        this.mAnchorView.getRootView().getLocationInWindow(rootViewPositionOnWindow);
        int windowLeftOnScreen = rootViewPositionOnScreen[0] - rootViewPositionOnWindow[0];
        int windowTopOnScreen = rootViewPositionOnScreen[1] - rootViewPositionOnWindow[1];
        this.mCoordsOnWindow.set(Math.max(0, x2 - windowLeftOnScreen), Math.max(0, y - windowTopOnScreen));
    }

    private int clampHorizontalPosition(float horizontal) {
        float horizontal2 = Math.max(0.5f, horizontal - 0.5f);
        int scrollX = this.mAnchorView.getScrollX();
        float horizontalDiff = horizontal2 - scrollX;
        int viewClippedWidth = (this.mAnchorView.getWidth() - this.mAnchorView.getCompoundPaddingLeft()) - this.mAnchorView.getCompoundPaddingRight();
        if (horizontalDiff >= viewClippedWidth - 1.0f) {
            int left = viewClippedWidth + scrollX;
            return left;
        } else if (Math.abs(horizontalDiff) <= 1.0f || (TextUtils.isEmpty(this.mAnchorView.getText()) && horizontal2 <= 1.0f)) {
            return scrollX;
        } else {
            int left2 = (int) horizontal2;
            return left2;
        }
    }

    public void dismiss() {
        this.mPopup.dismiss();
    }

    public boolean isShowing() {
        return this.mPopup.isShowing();
    }

    public void setClickListener(OnClickListener clickListener) {
        this.mClickListener = clickListener;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public class PhraseAdapter extends BaseAdapter {
        private PhraseAdapter() {
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return CommonPhraseSpinner.this.mPhrasesList.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int position) {
            return CommonPhraseSpinner.this.mPhrasesList.get(position);
        }

        @Override // android.widget.Adapter
        public long getItemId(int position) {
            return position;
        }

        @Override // android.widget.Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CommonPhraseSpinner.this.mContext).inflate(R.layout.os_common_phrase_item, parent, false);
            }
            ((TextView) convertView.findViewById(R.id.tv_common_phrase)).setText(getItem(position).toString());
            return convertView;
        }
    }
}
