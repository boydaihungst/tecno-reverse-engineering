package com.android.internal.widget.commonphrase;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.transsion.internal.R;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes4.dex */
public class CommonPhraseDialog {
    private static final int MAX_DISPLAY_PHRASES = 3;
    private PhraseAdapter mAdapter;
    private final TextView mAnchorView;
    private OnClickListener mClickListener;
    private FrameLayout mContentView;
    private Context mContext;
    private Dialog mDialog;
    private ListView mDropDownList;
    private Rect mVisibleDisplayRect = new Rect();
    private List<String> mPhrasesList = new ArrayList();
    private Point mScreenSize = new Point();
    private final BroadcastReceiver mHomeKeyEventReceiver = new BroadcastReceiver() { // from class: com.android.internal.widget.commonphrase.CommonPhraseDialog.1
        final String SYSTEM_REASON = "reason";
        final String SYSTEM_HOME_KEY = "homekey";

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra("reason");
                if (TextUtils.equals(reason, "homekey")) {
                    CommonPhraseDialog.this.dismiss();
                }
            }
        }
    };

    /* loaded from: classes4.dex */
    public interface OnClickListener {
        void onItemClick(String str);

        void onTileClick();
    }

    public CommonPhraseDialog(TextView anchorView, Context context) {
        this.mContext = context;
        this.mAnchorView = anchorView;
        this.mContext = context;
        buildDropDown();
        anchorView.getDisplay().getRealSize(this.mScreenSize);
    }

    public void addPhrases(List<String> phrases) {
        this.mPhrasesList.clear();
        this.mPhrasesList.addAll(phrases);
    }

    private void buildDropDown() {
        this.mAdapter = new PhraseAdapter();
        Dialog dialog = new Dialog(this.mContext);
        this.mDialog = dialog;
        dialog.setCanceledOnTouchOutside(true);
        this.mContentView = new FrameLayout(this.mContext);
        ViewGroup dropDownView = (ViewGroup) LayoutInflater.from(this.mContext).inflate(R.layout.os_common_phrase_list, this.mContentView);
        ListView listView = (ListView) dropDownView.findViewById(R.id.lv_phrase);
        this.mDropDownList = listView;
        listView.setAdapter((ListAdapter) this.mAdapter);
        this.mDropDownList.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.android.internal.widget.commonphrase.CommonPhraseDialog$$ExternalSyntheticLambda0
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                CommonPhraseDialog.this.m7188xf1ff0ffc(adapterView, view, i, j);
            }
        });
        dropDownView.findViewById(R.id.ll_phrase_title).setBackground(this.mContext.getDrawable(R.drawable.os_common_phrase_title_background));
        dropDownView.findViewById(R.id.ll_phrase_title).setOnClickListener(new View.OnClickListener() { // from class: com.android.internal.widget.commonphrase.CommonPhraseDialog$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                CommonPhraseDialog.this.m7189xac74b07d(view);
            }
        });
        dropDownView.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() { // from class: com.android.internal.widget.commonphrase.CommonPhraseDialog$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                CommonPhraseDialog.this.m7190x66ea50fe(view);
            }
        });
        dropDownView.setClipToOutline(true);
        this.mDialog.setContentView(this.mContentView);
        this.mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        invokeMethod(this.mDialog.getWindow().getAttributes(), "setOSFullDialog");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$buildDropDown$0$com-android-internal-widget-commonphrase-CommonPhraseDialog  reason: not valid java name */
    public /* synthetic */ void m7188xf1ff0ffc(AdapterView parent, View view, int position, long id) {
        if (this.mClickListener != null && position < this.mPhrasesList.size()) {
            this.mClickListener.onItemClick(this.mPhrasesList.get(position));
            this.mDialog.dismiss();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$buildDropDown$1$com-android-internal-widget-commonphrase-CommonPhraseDialog  reason: not valid java name */
    public /* synthetic */ void m7189xac74b07d(View view) {
        if (this.mClickListener != null) {
            this.mDialog.dismiss();
            this.mClickListener.onTileClick();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$buildDropDown$2$com-android-internal-widget-commonphrase-CommonPhraseDialog  reason: not valid java name */
    public /* synthetic */ void m7190x66ea50fe(View view) {
        this.mDialog.dismiss();
    }

    public void show() {
        this.mContext.registerReceiver(this.mHomeKeyEventReceiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        refreshDialogSize();
        this.mDialog.show();
    }

    private void refreshDialogSize() {
        int count = this.mAdapter.getCount();
        int screenWidth = Math.min(this.mScreenSize.x, this.mScreenSize.y);
        int width = Math.min(this.mVisibleDisplayRect.right - this.mVisibleDisplayRect.left, screenWidth);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, Integer.MIN_VALUE);
        View itemView = this.mAdapter.getView(0, null, this.mDropDownList);
        itemView.measure(widthSpec, 0);
        int height = 0 + itemView.getMeasuredHeight();
        int height2 = height + this.mDropDownList.getDividerHeight();
        if (count <= 3) {
            LinearLayout.LayoutParams ListParams = new LinearLayout.LayoutParams(-1, -2);
            this.mDropDownList.setLayoutParams(ListParams);
        } else {
            LinearLayout.LayoutParams ListParams2 = new LinearLayout.LayoutParams(-1, height2 * 3);
            this.mDropDownList.setLayoutParams(ListParams2);
        }
        this.mAdapter.notifyDataSetChanged();
    }

    private void invokeMethod(WindowManager.LayoutParams layoutParams, String methodName) {
        try {
            Class clazz = layoutParams.getClass();
            Method method = clazz.getDeclaredMethod(methodName, Boolean.TYPE);
            method.setAccessible(true);
            method.invoke(layoutParams, true);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
        }
    }

    public void dismiss() {
        if (isShowing()) {
            this.mContext.unregisterReceiver(this.mHomeKeyEventReceiver);
            this.mDialog.dismiss();
        }
    }

    public boolean isShowing() {
        return this.mDialog.isShowing();
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
            return CommonPhraseDialog.this.mPhrasesList.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int position) {
            return CommonPhraseDialog.this.mPhrasesList.get(position);
        }

        @Override // android.widget.Adapter
        public long getItemId(int position) {
            return position;
        }

        @Override // android.widget.Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CommonPhraseDialog.this.mContext).inflate(R.layout.os_common_phrase_item, parent, false);
            }
            ((TextView) convertView.findViewById(R.id.tv_common_phrase)).setText(getItem(position).toString());
            return convertView;
        }
    }
}
