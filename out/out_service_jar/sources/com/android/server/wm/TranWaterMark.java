package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
/* loaded from: classes2.dex */
public class TranWaterMark {
    private static final int REFRESH_VIEW = 1001;
    private static final String TAG = "TranssionWaterMark";
    private static final String TITLE = "TranssionWaterMark";
    private IntentFilter filter;
    private TranWaterMask tranWaterMask;
    private WindowManager windowManager;
    public static final boolean TRAN_DEMOPHONE_SIM_SUPPORT = "1".equals(SystemProperties.get("ro.tran_demophone_sim_support"));
    public static final boolean TECNO_DEMO_PHONE_SUPPORT = "1".equals(SystemProperties.get("sys.telephony.kom.enable"));
    public static final boolean INFINIX_DEMO_PHONE_SUPPORT = "1".equals(SystemProperties.get("ro.kom_telephony_support"));
    public static final boolean INTERNAL_PHONE_SUPPORT = "1".equals(SystemProperties.get("ro.tran_test_version"));
    public static final boolean FANS_SUPPORT = "1".equals(SystemProperties.get("persist.sys.fans.support"));
    private int mLastDW = 0;
    private int mLastDH = 0;
    private boolean aodShowing = false;
    private boolean keyguardShowing = false;
    private boolean needWait = false;
    private boolean isShowing = false;
    private Handler handler = new Handler() { // from class: com.android.server.wm.TranWaterMark.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            TranWaterMark.this.updateView();
        }
    };
    private BroadcastReceiver mDreamReceiver = new BroadcastReceiver() { // from class: com.android.server.wm.TranWaterMark.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.DREAMING_STARTED".equals(intent.getAction())) {
                TranWaterMark.this.aodShowing = true;
            } else if ("android.intent.action.DREAMING_STOPPED".equals(intent.getAction())) {
                TranWaterMark.this.aodShowing = false;
            }
            TranWaterMark.this.needUpdate();
        }
    };
    private WindowManager.LayoutParams params = BuildTranWaterMaskLayoutParams();

    public TranWaterMark(Context context) {
        this.windowManager = (WindowManager) context.getSystemService("window");
        this.tranWaterMask = new TranWaterMask(this, context);
        IntentFilter intentFilter = new IntentFilter();
        this.filter = intentFilter;
        intentFilter.addAction("android.intent.action.DREAMING_STARTED");
        this.filter.addAction("android.intent.action.DREAMING_STOPPED");
        context.registerReceiver(this.mDreamReceiver, this.filter);
    }

    public void positionSurface(int dw, int dh) {
        if (this.mLastDW != dw || this.mLastDH != dh) {
            this.mLastDW = dw;
            this.mLastDH = dh;
        }
    }

    public void updateView() {
        boolean needShow;
        WindowManager.LayoutParams layoutParams;
        TranWaterMask tranWaterMask = this.tranWaterMask;
        if (tranWaterMask == null) {
            return;
        }
        if (this.keyguardShowing) {
            needShow = true;
        } else if (FANS_SUPPORT) {
            needShow = true;
        } else {
            needShow = false;
        }
        tranWaterMask.postInvalidate();
        boolean z = this.isShowing;
        if (z != needShow) {
            if (needShow) {
                if (!z && (layoutParams = this.params) != null) {
                    this.windowManager.addView(this.tranWaterMask, layoutParams);
                    this.isShowing = true;
                }
            } else if (z) {
                this.windowManager.removeViewImmediate(this.tranWaterMask);
                this.isShowing = false;
            }
        }
        this.needWait = false;
    }

    public void needUpdate() {
        if (!this.needWait) {
            this.needWait = true;
            this.handler.removeMessages(1001);
            this.handler.sendEmptyMessageDelayed(1001, 100L);
        }
    }

    public void setKeyguardStatus(boolean keyguardShowing) {
        this.keyguardShowing = keyguardShowing;
        needUpdate();
    }

    private WindowManager.LayoutParams BuildTranWaterMaskLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = 2015;
        layoutParams.flags = 296;
        layoutParams.format = 1;
        layoutParams.setTitle("TranssionWaterMark");
        return layoutParams;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class TranWaterMask extends View {
        private static final String FANS_TEXT_COLOR = "#8B5A00";
        private static final float FANS_TITLE_MAGIN = 5.0f;
        private static final float FANS_TITLE_SIZE = 14.0f;
        private static final float NORMAL_TITLE_SIZE = 20.0f;
        private static final float PAINT_WIDTH = 1.0f;
        private static final String TITLE_DEMO_PHONE = "demo phone";
        private static final String TITLE_FANS = "Confidential";
        private static final String TITLE_INTERNAL_TEST = "internal test";
        private static final float TITLE_MAGIN = 50.0f;
        private Paint mTextPaint;
        private Resources resources;

        public TranWaterMask(TranWaterMark tranWaterMark, Context context) {
            this(tranWaterMark, context, null);
        }

        public TranWaterMask(TranWaterMark tranWaterMark, Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public TranWaterMask(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            this.resources = context.getResources();
            this.mTextPaint = new Paint(1);
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            this.mTextPaint.setTextSize(sp2px(NORMAL_TITLE_SIZE));
            this.mTextPaint.setStrokeWidth(sp2px(1.0f));
            if (TranWaterMark.this.keyguardShowing && !TranWaterMark.this.aodShowing) {
                if (!TranWaterMark.TRAN_DEMOPHONE_SIM_SUPPORT && (TranWaterMark.INFINIX_DEMO_PHONE_SUPPORT || TranWaterMark.TECNO_DEMO_PHONE_SUPPORT)) {
                    this.mTextPaint.setColor(-1);
                    canvas.drawText(TITLE_DEMO_PHONE, TITLE_MAGIN, TITLE_MAGIN, this.mTextPaint);
                }
                if (TranWaterMark.INTERNAL_PHONE_SUPPORT) {
                    this.mTextPaint.setColor(-65536);
                    canvas.drawText(TITLE_INTERNAL_TEST, (TranWaterMark.this.mLastDW - this.mTextPaint.measureText(TITLE_INTERNAL_TEST)) - TITLE_MAGIN, TITLE_MAGIN, this.mTextPaint);
                }
            }
            if (TranWaterMark.FANS_SUPPORT) {
                this.mTextPaint.setColor(Color.parseColor(FANS_TEXT_COLOR));
                this.mTextPaint.setTextSize(sp2px(FANS_TITLE_SIZE));
                canvas.drawText(TITLE_FANS, FANS_TITLE_MAGIN, TITLE_MAGIN, this.mTextPaint);
            }
            super.onDraw(canvas);
        }

        public int dip2px(float dipValue) {
            float scale = this.resources.getDisplayMetrics().density;
            return (int) ((dipValue * scale) + 0.5f);
        }

        public int sp2px(float spValue) {
            float fontScale = this.resources.getDisplayMetrics().scaledDensity;
            return (int) ((spValue * fontScale) + 0.5f);
        }
    }
}
