package android.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.view.ISurfaceControlViewHost;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import android.view.ViewRootImpl;
import android.view.WindowManager;
import android.view.WindowlessWindowManager;
import android.view.accessibility.IAccessibilityEmbeddedConnection;
import android.window.WindowTokenClient;
import java.util.Objects;
/* loaded from: classes3.dex */
public class SurfaceControlViewHost {
    private static final String TAG = "SurfaceControlViewHost";
    private IAccessibilityEmbeddedConnection mAccessibilityEmbeddedConnection;
    private ViewRootImpl.ConfigChangedCallback mCallback;
    private boolean mReleased;
    private ISurfaceControlViewHost mRemoteInterface;
    private SurfaceControl mSurfaceControl;
    private final ViewRootImpl mViewRoot;
    private WindowlessWindowManager mWm;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public final class ISurfaceControlViewHostImpl extends ISurfaceControlViewHost.Stub {
        private ISurfaceControlViewHostImpl() {
        }

        @Override // android.view.ISurfaceControlViewHost
        public void onConfigurationChanged(final Configuration configuration) {
            if (SurfaceControlViewHost.this.mViewRoot == null) {
                return;
            }
            SurfaceControlViewHost.this.mViewRoot.mHandler.post(new Runnable() { // from class: android.view.SurfaceControlViewHost$ISurfaceControlViewHostImpl$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    SurfaceControlViewHost.ISurfaceControlViewHostImpl.this.m4962x59fd79eb(configuration);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onConfigurationChanged$0$android-view-SurfaceControlViewHost$ISurfaceControlViewHostImpl  reason: not valid java name */
        public /* synthetic */ void m4962x59fd79eb(Configuration configuration) {
            if (SurfaceControlViewHost.this.mWm != null) {
                SurfaceControlViewHost.this.mWm.setConfiguration(configuration);
            }
            if (SurfaceControlViewHost.this.mViewRoot != null) {
                SurfaceControlViewHost.this.mViewRoot.forceWmRelayout();
            }
        }

        @Override // android.view.ISurfaceControlViewHost
        public void onDispatchDetachedFromWindow() {
            if (SurfaceControlViewHost.this.mViewRoot == null) {
                return;
            }
            SurfaceControlViewHost.this.mViewRoot.mHandler.post(new Runnable() { // from class: android.view.SurfaceControlViewHost$ISurfaceControlViewHostImpl$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    SurfaceControlViewHost.ISurfaceControlViewHostImpl.this.m4963x30fead74();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDispatchDetachedFromWindow$1$android-view-SurfaceControlViewHost$ISurfaceControlViewHostImpl  reason: not valid java name */
        public /* synthetic */ void m4963x30fead74() {
            SurfaceControlViewHost.this.release();
        }

        @Override // android.view.ISurfaceControlViewHost
        public void onInsetsChanged(InsetsState state, final Rect frame) {
            if (SurfaceControlViewHost.this.mViewRoot != null) {
                SurfaceControlViewHost.this.mViewRoot.mHandler.post(new Runnable() { // from class: android.view.SurfaceControlViewHost$ISurfaceControlViewHostImpl$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        SurfaceControlViewHost.ISurfaceControlViewHostImpl.this.m4964x9ebc5175(frame);
                    }
                });
            }
            SurfaceControlViewHost.this.mWm.setInsetsState(state);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInsetsChanged$2$android-view-SurfaceControlViewHost$ISurfaceControlViewHostImpl  reason: not valid java name */
        public /* synthetic */ void m4964x9ebc5175(Rect frame) {
            SurfaceControlViewHost.this.mViewRoot.setOverrideInsetsFrame(frame);
        }
    }

    /* loaded from: classes3.dex */
    public static final class SurfacePackage implements Parcelable {
        public static final Parcelable.Creator<SurfacePackage> CREATOR = new Parcelable.Creator<SurfacePackage>() { // from class: android.view.SurfaceControlViewHost.SurfacePackage.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SurfacePackage createFromParcel(Parcel in) {
                return new SurfacePackage(in);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SurfacePackage[] newArray(int size) {
                return new SurfacePackage[size];
            }
        };
        private final IAccessibilityEmbeddedConnection mAccessibilityEmbeddedConnection;
        private final IBinder mInputToken;
        private final ISurfaceControlViewHost mRemoteInterface;
        private SurfaceControl mSurfaceControl;

        SurfacePackage(SurfaceControl sc, IAccessibilityEmbeddedConnection connection, IBinder inputToken, ISurfaceControlViewHost ri) {
            this.mSurfaceControl = sc;
            this.mAccessibilityEmbeddedConnection = connection;
            this.mInputToken = inputToken;
            this.mRemoteInterface = ri;
        }

        public SurfacePackage(SurfacePackage other) {
            SurfaceControl otherSurfaceControl = other.mSurfaceControl;
            if (otherSurfaceControl != null && otherSurfaceControl.isValid()) {
                SurfaceControl surfaceControl = new SurfaceControl();
                this.mSurfaceControl = surfaceControl;
                surfaceControl.copyFrom(otherSurfaceControl, "SurfacePackage");
            }
            this.mAccessibilityEmbeddedConnection = other.mAccessibilityEmbeddedConnection;
            this.mInputToken = other.mInputToken;
            this.mRemoteInterface = other.mRemoteInterface;
        }

        private SurfacePackage(Parcel in) {
            SurfaceControl surfaceControl = new SurfaceControl();
            this.mSurfaceControl = surfaceControl;
            surfaceControl.readFromParcel(in);
            this.mAccessibilityEmbeddedConnection = IAccessibilityEmbeddedConnection.Stub.asInterface(in.readStrongBinder());
            this.mInputToken = in.readStrongBinder();
            this.mRemoteInterface = ISurfaceControlViewHost.Stub.asInterface(in.readStrongBinder());
        }

        public SurfaceControl getSurfaceControl() {
            return this.mSurfaceControl;
        }

        public IAccessibilityEmbeddedConnection getAccessibilityEmbeddedConnection() {
            return this.mAccessibilityEmbeddedConnection;
        }

        public ISurfaceControlViewHost getRemoteInterface() {
            return this.mRemoteInterface;
        }

        public void notifyConfigurationChanged(Configuration c) {
            try {
                getRemoteInterface().onConfigurationChanged(c);
            } catch (RemoteException e) {
                e.rethrowAsRuntimeException();
            }
        }

        public void notifyDetachedFromWindow() {
            try {
                getRemoteInterface().onDispatchDetachedFromWindow();
            } catch (RemoteException e) {
                e.rethrowAsRuntimeException();
            }
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel out, int flags) {
            this.mSurfaceControl.writeToParcel(out, flags);
            out.writeStrongBinder(this.mAccessibilityEmbeddedConnection.asBinder());
            out.writeStrongBinder(this.mInputToken);
            out.writeStrongBinder(this.mRemoteInterface.asBinder());
        }

        public void release() {
            SurfaceControl surfaceControl = this.mSurfaceControl;
            if (surfaceControl != null) {
                surfaceControl.release();
            }
            this.mSurfaceControl = null;
        }

        public IBinder getInputToken() {
            return this.mInputToken;
        }
    }

    public SurfaceControlViewHost(Context c, Display d, WindowlessWindowManager wwm) {
        this(c, d, wwm, false);
    }

    public SurfaceControlViewHost(Context c, Display d, WindowlessWindowManager wwm, boolean useSfChoreographer) {
        this.mReleased = false;
        this.mRemoteInterface = new ISurfaceControlViewHostImpl();
        this.mWm = wwm;
        ViewRootImpl viewRootImpl = new ViewRootImpl(c, d, this.mWm, useSfChoreographer);
        this.mViewRoot = viewRootImpl;
        addConfigCallback(c, d);
        WindowManagerGlobal.getInstance().addWindowlessRoot(viewRootImpl);
        this.mAccessibilityEmbeddedConnection = viewRootImpl.getAccessibilityEmbeddedConnection();
    }

    public SurfaceControlViewHost(Context context, Display display, IBinder hostToken) {
        this.mReleased = false;
        this.mRemoteInterface = new ISurfaceControlViewHostImpl();
        this.mSurfaceControl = new SurfaceControl.Builder().setContainerLayer().setName(TAG).setCallsite(TAG).build();
        this.mWm = new WindowlessWindowManager(context.getResources().getConfiguration(), this.mSurfaceControl, hostToken);
        ViewRootImpl viewRootImpl = new ViewRootImpl(context, display, this.mWm);
        this.mViewRoot = viewRootImpl;
        addConfigCallback(context, display);
        WindowManagerGlobal.getInstance().addWindowlessRoot(viewRootImpl);
        this.mAccessibilityEmbeddedConnection = viewRootImpl.getAccessibilityEmbeddedConnection();
    }

    private void addConfigCallback(Context c, final Display d) {
        final IBinder token = c.getWindowContextToken();
        ViewRootImpl.ConfigChangedCallback configChangedCallback = new ViewRootImpl.ConfigChangedCallback() { // from class: android.view.SurfaceControlViewHost$$ExternalSyntheticLambda1
            @Override // android.view.ViewRootImpl.ConfigChangedCallback
            public final void onConfigurationChanged(Configuration configuration) {
                SurfaceControlViewHost.lambda$addConfigCallback$0(IBinder.this, d, configuration);
            }
        };
        this.mCallback = configChangedCallback;
        ViewRootImpl.addConfigCallback(configChangedCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$addConfigCallback$0(IBinder token, Display d, Configuration conf) {
        if (token instanceof WindowTokenClient) {
            WindowTokenClient w = (WindowTokenClient) token;
            w.onConfigurationChanged(conf, d.getDisplayId(), true);
        }
    }

    protected void finalize() throws Throwable {
        if (this.mReleased) {
            return;
        }
        Log.e(TAG, "SurfaceControlViewHost finalized without being released: " + this);
        this.mViewRoot.die(false);
        WindowManagerGlobal.getInstance().removeWindowlessRoot(this.mViewRoot);
    }

    public SurfacePackage getSurfacePackage() {
        if (this.mSurfaceControl != null && this.mAccessibilityEmbeddedConnection != null) {
            return new SurfacePackage(new SurfaceControl(this.mSurfaceControl, "getSurfacePackage"), this.mAccessibilityEmbeddedConnection, this.mWm.getFocusGrantToken(), this.mRemoteInterface);
        }
        return null;
    }

    public void setView(View view, int width, int height) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(width, height, 2, 0, -2);
        setView(view, lp);
    }

    public void setView(View view, WindowManager.LayoutParams attrs) {
        Objects.requireNonNull(view);
        attrs.flags |= 16777216;
        view.setLayoutParams(attrs);
        this.mViewRoot.setView(view, attrs, null);
    }

    public View getView() {
        return this.mViewRoot.getView();
    }

    public IWindow getWindowToken() {
        return this.mViewRoot.mWindow;
    }

    public WindowlessWindowManager getWindowlessWM() {
        return this.mWm;
    }

    public void relayout(WindowManager.LayoutParams attrs) {
        relayout(attrs, new WindowlessWindowManager.ResizeCompleteCallback() { // from class: android.view.SurfaceControlViewHost$$ExternalSyntheticLambda0
            @Override // android.view.WindowlessWindowManager.ResizeCompleteCallback
            public final void finished(SurfaceControl.Transaction transaction) {
                transaction.apply();
            }
        });
    }

    public void relayout(WindowManager.LayoutParams attrs, WindowlessWindowManager.ResizeCompleteCallback callback) {
        this.mViewRoot.setLayoutParams(attrs, false);
        this.mViewRoot.setReportNextDraw(true);
        this.mWm.setCompletionCallback(this.mViewRoot.mWindow.asBinder(), callback);
    }

    public void relayout(int width, int height) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(width, height, 2, 0, -2);
        relayout(lp);
    }

    public void release() {
        this.mViewRoot.die(true);
        WindowManagerGlobal.getInstance().removeWindowlessRoot(this.mViewRoot);
        this.mReleased = true;
    }

    public void removeConfigCallback() {
        ViewRootImpl.ConfigChangedCallback configChangedCallback = this.mCallback;
        if (configChangedCallback != null) {
            ViewRootImpl.removeConfigCallback(configChangedCallback);
        }
    }

    public IBinder getFocusGrantToken() {
        return this.mWm.getFocusGrantToken();
    }
}
