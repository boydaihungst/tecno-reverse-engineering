package android.widget;

import android.app.INotificationManager;
import android.app.ITransientNotification;
import android.app.ITransientNotificationCallback;
import android.compat.Compatibility;
import android.content.Context;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.IAccessibilityManager;
import android.widget.Toast;
import com.android.internal.R;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes4.dex */
public class Toast {
    private static final long CHANGE_TEXT_TOASTS_IN_THE_SYSTEM = 147798919;
    public static final int LENGTH_LONG = 1;
    public static final int LENGTH_SHORT = 0;
    static final String TAG = "Toast";
    static final boolean localLOGV = false;
    private static INotificationManager sService;
    private final List<Callback> mCallbacks;
    private final Context mContext;
    int mDuration;
    private final Handler mHandler;
    private View mNextView;
    final TN mTN;
    private CharSequence mText;
    private final Binder mToken;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes4.dex */
    public @interface Duration {
    }

    /* renamed from: -$$Nest$smgetService  reason: not valid java name */
    static /* bridge */ /* synthetic */ INotificationManager m6151$$Nest$smgetService() {
        return getService();
    }

    public Toast(Context context) {
        this(context, null);
    }

    public Toast(Context context, Looper looper) {
        this.mContext = context;
        Binder binder = new Binder();
        this.mToken = binder;
        Looper looper2 = getLooper(looper);
        this.mHandler = new Handler(looper2);
        ArrayList arrayList = new ArrayList();
        this.mCallbacks = arrayList;
        TN tn = new TN(context, context.getPackageName(), binder, arrayList, looper2);
        this.mTN = tn;
        tn.mY = context.getResources().getDimensionPixelSize(R.dimen.toast_y_offset);
        tn.mGravity = context.getResources().getInteger(R.integer.config_toastDefaultGravity);
    }

    private Looper getLooper(Looper looper) {
        if (looper != null) {
            return looper;
        }
        return (Looper) Preconditions.checkNotNull(Looper.myLooper(), "Can't toast on a thread that has not called Looper.prepare()");
    }

    public void show() {
        if (Compatibility.isChangeEnabled((long) CHANGE_TEXT_TOASTS_IN_THE_SYSTEM)) {
            Preconditions.checkState((this.mNextView == null && this.mText == null) ? false : true, "You must either set a text or a view");
        } else if (this.mNextView == null) {
            throw new RuntimeException("setView must have been called");
        }
        INotificationManager service = getService();
        String pkg = this.mContext.getOpPackageName();
        TN tn = this.mTN;
        tn.mNextView = this.mNextView;
        int displayId = this.mContext.getDisplayId();
        try {
            if (Compatibility.isChangeEnabled((long) CHANGE_TEXT_TOASTS_IN_THE_SYSTEM)) {
                if (this.mNextView != null) {
                    service.enqueueToast(pkg, this.mToken, tn, this.mDuration, displayId);
                } else {
                    ITransientNotificationCallback callback = new CallbackBinder(this.mCallbacks, this.mHandler);
                    service.enqueueTextToast(pkg, this.mToken, this.mText, this.mDuration, displayId, callback);
                }
            } else {
                service.enqueueToast(pkg, this.mToken, tn, this.mDuration, displayId);
            }
        } catch (RemoteException e) {
        }
    }

    public void cancel() {
        if (Compatibility.isChangeEnabled((long) CHANGE_TEXT_TOASTS_IN_THE_SYSTEM) && this.mNextView == null) {
            try {
                getService().cancelToast(this.mContext.getOpPackageName(), this.mToken);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        this.mTN.cancel();
    }

    @Deprecated
    public void setView(View view) {
        this.mNextView = view;
    }

    @Deprecated
    public View getView() {
        return this.mNextView;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
        this.mTN.mDuration = duration;
    }

    public int getDuration() {
        return this.mDuration;
    }

    public void setMargin(float horizontalMargin, float verticalMargin) {
        if (isSystemRenderedTextToast()) {
            Log.e(TAG, "setMargin() shouldn't be called on text toasts, the values won't be used");
        }
        this.mTN.mHorizontalMargin = horizontalMargin;
        this.mTN.mVerticalMargin = verticalMargin;
    }

    public float getHorizontalMargin() {
        if (isSystemRenderedTextToast()) {
            Log.e(TAG, "getHorizontalMargin() shouldn't be called on text toasts, the result may not reflect actual values.");
        }
        return this.mTN.mHorizontalMargin;
    }

    public float getVerticalMargin() {
        if (isSystemRenderedTextToast()) {
            Log.e(TAG, "getVerticalMargin() shouldn't be called on text toasts, the result may not reflect actual values.");
        }
        return this.mTN.mVerticalMargin;
    }

    public void setGravity(int gravity, int xOffset, int yOffset) {
        if (isSystemRenderedTextToast()) {
            Log.e(TAG, "setGravity() shouldn't be called on text toasts, the values won't be used");
        }
        this.mTN.mGravity = gravity;
        this.mTN.mX = xOffset;
        this.mTN.mY = yOffset;
    }

    public int getGravity() {
        if (isSystemRenderedTextToast()) {
            Log.e(TAG, "getGravity() shouldn't be called on text toasts, the result may not reflect actual values.");
        }
        return this.mTN.mGravity;
    }

    public int getXOffset() {
        if (isSystemRenderedTextToast()) {
            Log.e(TAG, "getXOffset() shouldn't be called on text toasts, the result may not reflect actual values.");
        }
        return this.mTN.mX;
    }

    public int getYOffset() {
        if (isSystemRenderedTextToast()) {
            Log.e(TAG, "getYOffset() shouldn't be called on text toasts, the result may not reflect actual values.");
        }
        return this.mTN.mY;
    }

    private boolean isSystemRenderedTextToast() {
        return Compatibility.isChangeEnabled((long) CHANGE_TEXT_TOASTS_IN_THE_SYSTEM) && this.mNextView == null;
    }

    public void addCallback(Callback callback) {
        Preconditions.checkNotNull(callback);
        synchronized (this.mCallbacks) {
            this.mCallbacks.add(callback);
        }
    }

    public void removeCallback(Callback callback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.remove(callback);
        }
    }

    public WindowManager.LayoutParams getWindowParams() {
        if (Compatibility.isChangeEnabled((long) CHANGE_TEXT_TOASTS_IN_THE_SYSTEM)) {
            if (this.mNextView != null) {
                return this.mTN.mParams;
            }
            return null;
        }
        return this.mTN.mParams;
    }

    public static Toast makeText(Context context, CharSequence text, int duration) {
        return makeText(context, null, text, duration);
    }

    public static Toast makeText(Context context, Looper looper, CharSequence text, int duration) {
        if (Compatibility.isChangeEnabled((long) CHANGE_TEXT_TOASTS_IN_THE_SYSTEM)) {
            Toast result = new Toast(context, looper);
            result.mText = text;
            result.mDuration = duration;
            return result;
        }
        Toast result2 = new Toast(context, looper);
        View v = ToastPresenter.getTextToastView(context, text);
        result2.mNextView = v;
        result2.mDuration = duration;
        return result2;
    }

    public static Toast makeText(Context context, int resId, int duration) throws Resources.NotFoundException {
        return makeText(context, context.getResources().getText(resId), duration);
    }

    public void setText(int resId) {
        setText(this.mContext.getText(resId));
    }

    public void setText(CharSequence s) {
        if (Compatibility.isChangeEnabled((long) CHANGE_TEXT_TOASTS_IN_THE_SYSTEM)) {
            if (this.mNextView != null) {
                throw new IllegalStateException("Text provided for custom toast, remove previous setView() calls if you want a text toast instead.");
            }
            this.mText = s;
            return;
        }
        View view = this.mNextView;
        if (view == null) {
            throw new RuntimeException("This Toast was not created with Toast.makeText()");
        }
        TextView tv = (TextView) view.findViewById(16908299);
        if (tv == null) {
            throw new RuntimeException("This Toast was not created with Toast.makeText()");
        }
        tv.setText(s);
    }

    private static INotificationManager getService() {
        INotificationManager iNotificationManager = sService;
        if (iNotificationManager != null) {
            return iNotificationManager;
        }
        INotificationManager asInterface = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        sService = asInterface;
        return asInterface;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class TN extends ITransientNotification.Stub {
        private static final int CANCEL = 2;
        private static final int HIDE = 1;
        private static final int SHOW = 0;
        private final List<Callback> mCallbacks;
        int mDuration;
        int mGravity;
        final Handler mHandler;
        float mHorizontalMargin;
        View mNextView;
        final String mPackageName;
        private final WindowManager.LayoutParams mParams;
        private final ToastPresenter mPresenter;
        final Binder mToken;
        float mVerticalMargin;
        View mView;
        WindowManager mWM;
        int mX;
        int mY;

        TN(Context context, String packageName, Binder token, List<Callback> callbacks, Looper looper) {
            IAccessibilityManager accessibilityManager = IAccessibilityManager.Stub.asInterface(ServiceManager.getService(Context.ACCESSIBILITY_SERVICE));
            ToastPresenter toastPresenter = new ToastPresenter(context, accessibilityManager, Toast.m6151$$Nest$smgetService(), packageName);
            this.mPresenter = toastPresenter;
            this.mParams = toastPresenter.getLayoutParams();
            this.mPackageName = packageName;
            this.mToken = token;
            this.mCallbacks = callbacks;
            this.mHandler = new Handler(looper, null) { // from class: android.widget.Toast.TN.1
                @Override // android.os.Handler
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 0:
                            IBinder token2 = (IBinder) msg.obj;
                            TN.this.handleShow(token2);
                            return;
                        case 1:
                            TN.this.handleHide();
                            TN.this.mNextView = null;
                            return;
                        case 2:
                            TN.this.handleHide();
                            TN.this.mNextView = null;
                            try {
                                Toast.m6151$$Nest$smgetService().cancelToast(TN.this.mPackageName, TN.this.mToken);
                                return;
                            } catch (RemoteException e) {
                                return;
                            }
                        default:
                            return;
                    }
                }
            };
        }

        private List<Callback> getCallbacks() {
            ArrayList arrayList;
            synchronized (this.mCallbacks) {
                arrayList = new ArrayList(this.mCallbacks);
            }
            return arrayList;
        }

        @Override // android.app.ITransientNotification
        public void show(IBinder windowToken) {
            this.mHandler.obtainMessage(0, windowToken).sendToTarget();
        }

        @Override // android.app.ITransientNotification
        public void hide() {
            this.mHandler.obtainMessage(1).sendToTarget();
        }

        public void cancel() {
            this.mHandler.obtainMessage(2).sendToTarget();
        }

        public void handleShow(IBinder windowToken) {
            if (!this.mHandler.hasMessages(2) && !this.mHandler.hasMessages(1) && this.mView != this.mNextView) {
                handleHide();
                View view = this.mNextView;
                this.mView = view;
                this.mPresenter.show(view, this.mToken, windowToken, this.mDuration, this.mGravity, this.mX, this.mY, this.mHorizontalMargin, this.mVerticalMargin, new CallbackBinder(getCallbacks(), this.mHandler));
            }
        }

        public void handleHide() {
            View view = this.mView;
            if (view != null) {
                Preconditions.checkState(view == this.mPresenter.getView(), "Trying to hide toast view different than the last one displayed");
                this.mPresenter.hide(new CallbackBinder(getCallbacks(), this.mHandler));
                this.mView = null;
            }
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Callback {
        public void onToastShown() {
        }

        public void onToastHidden() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class CallbackBinder extends ITransientNotificationCallback.Stub {
        private final List<Callback> mCallbacks;
        private final Handler mHandler;

        private CallbackBinder(List<Callback> callbacks, Handler handler) {
            this.mCallbacks = callbacks;
            this.mHandler = handler;
        }

        @Override // android.app.ITransientNotificationCallback
        public void onToastShown() {
            this.mHandler.post(new Runnable() { // from class: android.widget.Toast$CallbackBinder$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    Toast.CallbackBinder.this.m6153lambda$onToastShown$0$androidwidgetToast$CallbackBinder();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onToastShown$0$android-widget-Toast$CallbackBinder  reason: not valid java name */
        public /* synthetic */ void m6153lambda$onToastShown$0$androidwidgetToast$CallbackBinder() {
            for (Callback callback : getCallbacks()) {
                callback.onToastShown();
            }
        }

        @Override // android.app.ITransientNotificationCallback
        public void onToastHidden() {
            this.mHandler.post(new Runnable() { // from class: android.widget.Toast$CallbackBinder$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Toast.CallbackBinder.this.m6152lambda$onToastHidden$1$androidwidgetToast$CallbackBinder();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onToastHidden$1$android-widget-Toast$CallbackBinder  reason: not valid java name */
        public /* synthetic */ void m6152lambda$onToastHidden$1$androidwidgetToast$CallbackBinder() {
            for (Callback callback : getCallbacks()) {
                callback.onToastHidden();
            }
        }

        private List<Callback> getCallbacks() {
            ArrayList arrayList;
            synchronized (this.mCallbacks) {
                arrayList = new ArrayList(this.mCallbacks);
            }
            return arrayList;
        }
    }
}
