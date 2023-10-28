package com.mediatek.boostfwk.info;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.view.Window;
import android.view.WindowManager;
import com.mediatek.boostfwk.utils.LogUtil;
import com.mediatek.boostfwk.utils.TasksUtil;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes.dex */
public final class ActivityInfo {
    private ArrayList<ActivityChangeListener> activityChangeListeners;
    private WindowManager.LayoutParams attrs;
    private static ActivityInfo instance = null;
    private static final Object LOCK = new Object();
    private WeakReference<Context> activityContext = null;
    private float density = -1.0f;
    private String packageName = null;
    private int mRenderThreadTid = Integer.MIN_VALUE;
    ActivityStateListener mActivityStateListener = null;
    private volatile int mActivityCount = 0;
    private int activityType = -1;
    private int scrollingState = -1;

    private ActivityInfo() {
        this.activityChangeListeners = null;
        this.activityChangeListeners = new ArrayList<>(4);
    }

    public static ActivityInfo getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ActivityInfo();
                }
            }
        }
        return instance;
    }

    public Context getContext() {
        WeakReference<Context> weakReference = this.activityContext;
        if (weakReference == null) {
            return null;
        }
        return weakReference.get();
    }

    public void setContext(Context context) {
        if (context == null) {
            return;
        }
        WeakReference<Context> weakReference = this.activityContext;
        if (weakReference == null) {
            this.activityContext = new WeakReference<>(context);
            initialBasicInfo(context);
        } else if (!context.equals(weakReference.get())) {
            this.activityContext.clear();
            this.activityContext = new WeakReference<>(context);
            initialBasicInfo(context);
        }
        notifyActivityUpdate(context);
    }

    private void initialBasicInfo(Context context) {
        this.attrs = null;
        this.density = context.getResources().getDisplayMetrics().density;
        this.packageName = context.getPackageName();
        if (this.mActivityStateListener == null) {
            this.mActivityStateListener = new ActivityStateListener();
            if (context instanceof Activity) {
                Application app = ((Activity) context).getApplication();
                app.registerActivityLifecycleCallbacks(this.mActivityStateListener);
                this.mActivityCount++;
            }
        }
    }

    private void notifyActivityUpdate(Context context) {
        Iterator<ActivityChangeListener> it = this.activityChangeListeners.iterator();
        while (it.hasNext()) {
            ActivityChangeListener listener = it.next();
            listener.onChange(context);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyAllActivityPause(Context context) {
        Iterator<ActivityChangeListener> it = this.activityChangeListeners.iterator();
        while (it.hasNext()) {
            ActivityChangeListener listener = it.next();
            listener.onAllActivityPaused(context);
        }
    }

    public WindowManager.LayoutParams getWindowLayoutAttr() {
        WindowManager.LayoutParams layoutParams = this.attrs;
        if (layoutParams != null) {
            return layoutParams;
        }
        WeakReference<Context> weakReference = this.activityContext;
        if (weakReference == null) {
            return null;
        }
        Context context = weakReference.get();
        Window win = null;
        if (context != null && (context instanceof Activity)) {
            win = ((Activity) context).getWindow();
        }
        if (win != null) {
            WindowManager.LayoutParams attributes = win.getAttributes();
            this.attrs = attributes;
            return attributes;
        }
        return null;
    }

    public float getDensity() {
        return this.density;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setRenderThreadTid(int renderThreadTid) {
        this.mRenderThreadTid = renderThreadTid;
    }

    public int getRenderThreadTid() {
        if (this.mRenderThreadTid == Integer.MIN_VALUE) {
            this.mRenderThreadTid = TasksUtil.findRenderTheadTid(Process.myPid());
        }
        return this.mRenderThreadTid;
    }

    /* loaded from: classes.dex */
    public interface ActivityChangeListener {
        void onChange(Context context);

        default void onAllActivityPaused(Context c) {
        }
    }

    public void registerActivityListener(ActivityChangeListener changeListener) {
        if (changeListener == null) {
            return;
        }
        this.activityChangeListeners.add(changeListener);
    }

    /* loaded from: classes.dex */
    public class ActivityStateListener implements Application.ActivityLifecycleCallbacks {
        public ActivityStateListener() {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityDestroyed(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityStarted(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityResumed(Activity activity) {
            ActivityInfo.this.mActivityCount++;
            LogUtil.traceAndMLogd("TouchPolicy", "onActivityResumed " + activity + " " + ActivityInfo.this.mActivityCount);
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityPaused(Activity activity) {
            LogUtil.traceAndMLogd("TouchPolicy", "onActivityPaused " + activity + " " + ActivityInfo.this.mActivityCount);
            ActivityInfo activityInfo = ActivityInfo.this;
            int i = activityInfo.mActivityCount - 1;
            activityInfo.mActivityCount = i;
            if (i == 0) {
                ActivityInfo.this.notifyAllActivityPause(activity);
            }
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityStopped(Activity activity) {
            LogUtil.traceAndMLogd("TouchPolicy", "onActivityStopped " + activity);
        }
    }
}
