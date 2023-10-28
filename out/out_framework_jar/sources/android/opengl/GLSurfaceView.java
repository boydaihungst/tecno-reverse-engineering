package android.opengl;

import android.content.Context;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;
/* loaded from: classes2.dex */
public class GLSurfaceView extends SurfaceView implements SurfaceHolder.Callback2 {
    public static final int DEBUG_CHECK_GL_ERROR = 1;
    public static final int DEBUG_LOG_GL_CALLS = 2;
    public static final int RENDERMODE_CONTINUOUSLY = 1;
    public static final int RENDERMODE_WHEN_DIRTY = 0;
    private static final String TAG = "GLSurfaceView";
    private int mDebugFlags;
    private boolean mDetached;
    private EGLConfigChooser mEGLConfigChooser;
    private int mEGLContextClientVersion;
    private EGLContextFactory mEGLContextFactory;
    private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
    private GLThread mGLThread;
    private GLWrapper mGLWrapper;
    private boolean mPreserveEGLContextOnPause;
    private Renderer mRenderer;
    private final WeakReference<GLSurfaceView> mThisWeakRef;
    private static final boolean LOG_ATTACH_DETACH = SystemProperties.getBoolean("debug.glsurfaceview.log", false);
    private static final boolean LOG_THREADS = SystemProperties.getBoolean("debug.glsurfaceview.log", false);
    private static final boolean LOG_PAUSE_RESUME = SystemProperties.getBoolean("debug.glsurfaceview.log", false);
    private static final boolean LOG_SURFACE = SystemProperties.getBoolean("debug.glsurfaceview.log", false);
    private static final boolean LOG_RENDERER = SystemProperties.getBoolean("debug.glsurfaceview.log", false);
    private static final boolean LOG_RENDERER_DRAW_FRAME = SystemProperties.getBoolean("debug.glsurfaceview.log", false);
    private static final boolean LOG_EGL = SystemProperties.getBoolean("debug.glsurfaceview.log", false);
    private static final GLThreadManager sGLThreadManager = new GLThreadManager();

    /* loaded from: classes2.dex */
    public interface EGLConfigChooser {
        javax.microedition.khronos.egl.EGLConfig chooseConfig(EGL10 egl10, javax.microedition.khronos.egl.EGLDisplay eGLDisplay);
    }

    /* loaded from: classes2.dex */
    public interface EGLContextFactory {
        javax.microedition.khronos.egl.EGLContext createContext(EGL10 egl10, javax.microedition.khronos.egl.EGLDisplay eGLDisplay, javax.microedition.khronos.egl.EGLConfig eGLConfig);

        void destroyContext(EGL10 egl10, javax.microedition.khronos.egl.EGLDisplay eGLDisplay, javax.microedition.khronos.egl.EGLContext eGLContext);
    }

    /* loaded from: classes2.dex */
    public interface EGLWindowSurfaceFactory {
        javax.microedition.khronos.egl.EGLSurface createWindowSurface(EGL10 egl10, javax.microedition.khronos.egl.EGLDisplay eGLDisplay, javax.microedition.khronos.egl.EGLConfig eGLConfig, Object obj);

        void destroySurface(EGL10 egl10, javax.microedition.khronos.egl.EGLDisplay eGLDisplay, javax.microedition.khronos.egl.EGLSurface eGLSurface);
    }

    /* loaded from: classes2.dex */
    public interface GLWrapper {
        GL wrap(GL gl);
    }

    /* loaded from: classes2.dex */
    public interface Renderer {
        void onDrawFrame(GL10 gl10);

        void onSurfaceChanged(GL10 gl10, int i, int i2);

        void onSurfaceCreated(GL10 gl10, javax.microedition.khronos.egl.EGLConfig eGLConfig);
    }

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.opengl.GLSurfaceView.GLThread.guardedRun():void] */
    /* renamed from: -$$Nest$fgetmPreserveEGLContextOnPause  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m2881$$Nest$fgetmPreserveEGLContextOnPause(GLSurfaceView gLSurfaceView) {
        return gLSurfaceView.mPreserveEGLContextOnPause;
    }

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.opengl.GLSurfaceView.GLThread.guardedRun():void] */
    /* renamed from: -$$Nest$fgetmRenderer  reason: not valid java name */
    static /* bridge */ /* synthetic */ Renderer m2882$$Nest$fgetmRenderer(GLSurfaceView gLSurfaceView) {
        return gLSurfaceView.mRenderer;
    }

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.opengl.GLSurfaceView.GLThread.guardedRun():void] */
    /* renamed from: -$$Nest$sfgetLOG_PAUSE_RESUME  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m2884$$Nest$sfgetLOG_PAUSE_RESUME() {
        return LOG_PAUSE_RESUME;
    }

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.opengl.GLSurfaceView.GLThread.guardedRun():void] */
    /* renamed from: -$$Nest$sfgetLOG_RENDERER  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m2885$$Nest$sfgetLOG_RENDERER() {
        return LOG_RENDERER;
    }

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.opengl.GLSurfaceView.GLThread.guardedRun():void] */
    /* renamed from: -$$Nest$sfgetLOG_RENDERER_DRAW_FRAME  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m2886$$Nest$sfgetLOG_RENDERER_DRAW_FRAME() {
        return LOG_RENDERER_DRAW_FRAME;
    }

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.opengl.GLSurfaceView.GLThread.guardedRun():void] */
    /* renamed from: -$$Nest$sfgetLOG_SURFACE  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m2887$$Nest$sfgetLOG_SURFACE() {
        return LOG_SURFACE;
    }

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.opengl.GLSurfaceView.GLThread.guardedRun():void] */
    /* renamed from: -$$Nest$sfgetLOG_THREADS  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m2888$$Nest$sfgetLOG_THREADS() {
        return LOG_THREADS;
    }

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.opengl.GLSurfaceView.GLThread.guardedRun():void] */
    /* renamed from: -$$Nest$sfgetsGLThreadManager  reason: not valid java name */
    static /* bridge */ /* synthetic */ GLThreadManager m2889$$Nest$sfgetsGLThreadManager() {
        return sGLThreadManager;
    }

    public GLSurfaceView(Context context) {
        super(context);
        this.mThisWeakRef = new WeakReference<>(this);
        init();
    }

    public GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mThisWeakRef = new WeakReference<>(this);
        init();
    }

    protected void finalize() throws Throwable {
        try {
            GLThread gLThread = this.mGLThread;
            if (gLThread != null) {
                gLThread.requestExitAndWait();
            }
        } finally {
            super.finalize();
        }
    }

    private void init() {
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    }

    public void setGLWrapper(GLWrapper glWrapper) {
        this.mGLWrapper = glWrapper;
    }

    public void setDebugFlags(int debugFlags) {
        this.mDebugFlags = debugFlags;
    }

    public int getDebugFlags() {
        return this.mDebugFlags;
    }

    public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
        this.mPreserveEGLContextOnPause = preserveOnPause;
    }

    public boolean getPreserveEGLContextOnPause() {
        return this.mPreserveEGLContextOnPause;
    }

    public void setRenderer(Renderer renderer) {
        checkRenderThreadState();
        if (this.mEGLConfigChooser == null) {
            this.mEGLConfigChooser = new SimpleEGLConfigChooser(true);
        }
        if (this.mEGLContextFactory == null) {
            this.mEGLContextFactory = new DefaultContextFactory();
        }
        if (this.mEGLWindowSurfaceFactory == null) {
            this.mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
        }
        this.mRenderer = renderer;
        GLThread gLThread = new GLThread(this.mThisWeakRef);
        this.mGLThread = gLThread;
        gLThread.start();
    }

    public void setEGLContextFactory(EGLContextFactory factory) {
        checkRenderThreadState();
        this.mEGLContextFactory = factory;
    }

    public void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory) {
        checkRenderThreadState();
        this.mEGLWindowSurfaceFactory = factory;
    }

    public void setEGLConfigChooser(EGLConfigChooser configChooser) {
        checkRenderThreadState();
        this.mEGLConfigChooser = configChooser;
    }

    public void setEGLConfigChooser(boolean needDepth) {
        setEGLConfigChooser(new SimpleEGLConfigChooser(needDepth));
    }

    public void setEGLConfigChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
        setEGLConfigChooser(new ComponentSizeChooser(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize));
    }

    public void setEGLContextClientVersion(int version) {
        checkRenderThreadState();
        this.mEGLContextClientVersion = version;
    }

    public void setRenderMode(int renderMode) {
        this.mGLThread.setRenderMode(renderMode);
    }

    public int getRenderMode() {
        return this.mGLThread.getRenderMode();
    }

    public void requestRender() {
        this.mGLThread.requestRender();
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceCreated(SurfaceHolder holder) {
        this.mGLThread.surfaceCreated();
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.mGLThread.surfaceDestroyed();
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        this.mGLThread.onWindowResize(w, h);
    }

    @Override // android.view.SurfaceHolder.Callback2
    public void surfaceRedrawNeededAsync(SurfaceHolder holder, Runnable finishDrawing) {
        GLThread gLThread = this.mGLThread;
        if (gLThread != null) {
            gLThread.requestRenderAndNotify(finishDrawing);
        }
    }

    @Override // android.view.SurfaceHolder.Callback2
    @Deprecated
    public void surfaceRedrawNeeded(SurfaceHolder holder) {
    }

    public void onPause() {
        this.mGLThread.onPause();
    }

    public void onResume() {
        this.mGLThread.onResume();
    }

    public void queueEvent(Runnable r) {
        this.mGLThread.queueEvent(r);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.SurfaceView, android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onAttachedToWindow reattach =" + this.mDetached);
        }
        if (this.mDetached && this.mRenderer != null) {
            int renderMode = 1;
            GLThread gLThread = this.mGLThread;
            if (gLThread != null) {
                renderMode = gLThread.getRenderMode();
            }
            GLThread gLThread2 = new GLThread(this.mThisWeakRef);
            this.mGLThread = gLThread2;
            if (renderMode != 1) {
                gLThread2.setRenderMode(renderMode);
            }
            this.mGLThread.start();
        }
        this.mDetached = false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.SurfaceView, android.view.View
    public void onDetachedFromWindow() {
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onDetachedFromWindow");
        }
        GLThread gLThread = this.mGLThread;
        if (gLThread != null) {
            gLThread.requestExitAndWait();
        }
        this.mDetached = true;
        super.onDetachedFromWindow();
    }

    /* loaded from: classes2.dex */
    private class DefaultContextFactory implements EGLContextFactory {
        private int EGL_CONTEXT_CLIENT_VERSION;

        private DefaultContextFactory() {
            this.EGL_CONTEXT_CLIENT_VERSION = 12440;
        }

        @Override // android.opengl.GLSurfaceView.EGLContextFactory
        public javax.microedition.khronos.egl.EGLContext createContext(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display, javax.microedition.khronos.egl.EGLConfig config) {
            int[] attrib_list = {this.EGL_CONTEXT_CLIENT_VERSION, GLSurfaceView.this.mEGLContextClientVersion, 12344};
            return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, GLSurfaceView.this.mEGLContextClientVersion != 0 ? attrib_list : null);
        }

        @Override // android.opengl.GLSurfaceView.EGLContextFactory
        public void destroyContext(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display, javax.microedition.khronos.egl.EGLContext context) {
            if (!egl.eglDestroyContext(display, context)) {
                Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("DefaultContextFactory", "tid=" + Thread.currentThread().getId());
                }
                EglHelper.throwEglException("eglDestroyContex", egl.eglGetError());
            }
        }
    }

    /* loaded from: classes2.dex */
    private static class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {
        private DefaultWindowSurfaceFactory() {
        }

        @Override // android.opengl.GLSurfaceView.EGLWindowSurfaceFactory
        public javax.microedition.khronos.egl.EGLSurface createWindowSurface(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display, javax.microedition.khronos.egl.EGLConfig config, Object nativeWindow) {
            try {
                javax.microedition.khronos.egl.EGLSurface result = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
                return result;
            } catch (IllegalArgumentException e) {
                Log.e(GLSurfaceView.TAG, "eglCreateWindowSurface", e);
                return null;
            }
        }

        @Override // android.opengl.GLSurfaceView.EGLWindowSurfaceFactory
        public void destroySurface(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display, javax.microedition.khronos.egl.EGLSurface surface) {
            egl.eglDestroySurface(display, surface);
        }
    }

    /* loaded from: classes2.dex */
    private abstract class BaseConfigChooser implements EGLConfigChooser {
        protected int[] mConfigSpec;

        abstract javax.microedition.khronos.egl.EGLConfig chooseConfig(EGL10 egl10, javax.microedition.khronos.egl.EGLDisplay eGLDisplay, javax.microedition.khronos.egl.EGLConfig[] eGLConfigArr);

        public BaseConfigChooser(int[] configSpec) {
            this.mConfigSpec = filterConfigSpec(configSpec);
        }

        @Override // android.opengl.GLSurfaceView.EGLConfigChooser
        public javax.microedition.khronos.egl.EGLConfig chooseConfig(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display) {
            int[] num_config = new int[1];
            if (!egl.eglChooseConfig(display, this.mConfigSpec, null, 0, num_config)) {
                throw new IllegalArgumentException("eglChooseConfig failed");
            }
            int numConfigs = num_config[0];
            if (numConfigs <= 0) {
                throw new IllegalArgumentException("No configs match configSpec");
            }
            javax.microedition.khronos.egl.EGLConfig[] configs = new javax.microedition.khronos.egl.EGLConfig[numConfigs];
            if (!egl.eglChooseConfig(display, this.mConfigSpec, configs, numConfigs, num_config)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            javax.microedition.khronos.egl.EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        private int[] filterConfigSpec(int[] configSpec) {
            if (GLSurfaceView.this.mEGLContextClientVersion != 2 && GLSurfaceView.this.mEGLContextClientVersion != 3) {
                return configSpec;
            }
            int len = configSpec.length;
            int[] newConfigSpec = new int[len + 2];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
            newConfigSpec[len - 1] = 12352;
            if (GLSurfaceView.this.mEGLContextClientVersion == 2) {
                newConfigSpec[len] = 4;
            } else {
                newConfigSpec[len] = 64;
            }
            newConfigSpec[len + 1] = 12344;
            return newConfigSpec;
        }
    }

    /* loaded from: classes2.dex */
    private class ComponentSizeChooser extends BaseConfigChooser {
        protected int mAlphaSize;
        protected int mBlueSize;
        protected int mDepthSize;
        protected int mGreenSize;
        protected int mRedSize;
        protected int mStencilSize;
        private int[] mValue;

        public ComponentSizeChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
            super(new int[]{12324, redSize, 12323, greenSize, 12322, blueSize, 12321, alphaSize, 12325, depthSize, 12326, stencilSize, 12344});
            this.mValue = new int[1];
            this.mRedSize = redSize;
            this.mGreenSize = greenSize;
            this.mBlueSize = blueSize;
            this.mAlphaSize = alphaSize;
            this.mDepthSize = depthSize;
            this.mStencilSize = stencilSize;
        }

        @Override // android.opengl.GLSurfaceView.BaseConfigChooser
        public javax.microedition.khronos.egl.EGLConfig chooseConfig(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display, javax.microedition.khronos.egl.EGLConfig[] configs) {
            for (javax.microedition.khronos.egl.EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config, 12325, 0);
                int s = findConfigAttrib(egl, display, config, 12326, 0);
                if (d >= this.mDepthSize && s >= this.mStencilSize) {
                    int r = findConfigAttrib(egl, display, config, 12324, 0);
                    int g = findConfigAttrib(egl, display, config, 12323, 0);
                    int b = findConfigAttrib(egl, display, config, 12322, 0);
                    int a = findConfigAttrib(egl, display, config, 12321, 0);
                    if (r == this.mRedSize && g == this.mGreenSize && b == this.mBlueSize && a == this.mAlphaSize) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(EGL10 egl, javax.microedition.khronos.egl.EGLDisplay display, javax.microedition.khronos.egl.EGLConfig config, int attribute, int defaultValue) {
            if (egl.eglGetConfigAttrib(display, config, attribute, this.mValue)) {
                return this.mValue[0];
            }
            return defaultValue;
        }
    }

    /* loaded from: classes2.dex */
    private class SimpleEGLConfigChooser extends ComponentSizeChooser {
        public SimpleEGLConfigChooser(boolean withDepthBuffer) {
            super(8, 8, 8, 0, withDepthBuffer ? 16 : 0, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class EglHelper {
        EGL10 mEgl;
        javax.microedition.khronos.egl.EGLConfig mEglConfig;
        javax.microedition.khronos.egl.EGLContext mEglContext;
        javax.microedition.khronos.egl.EGLDisplay mEglDisplay;
        javax.microedition.khronos.egl.EGLSurface mEglSurface;
        private WeakReference<GLSurfaceView> mGLSurfaceViewWeakRef;

        public EglHelper(WeakReference<GLSurfaceView> glSurfaceViewWeakRef) {
            this.mGLSurfaceViewWeakRef = glSurfaceViewWeakRef;
        }

        public void start() {
            if (GLSurfaceView.LOG_EGL) {
                Log.w("EglHelper", "start() tid=" + Thread.currentThread().getId());
            }
            EGL10 egl10 = (EGL10) javax.microedition.khronos.egl.EGLContext.getEGL();
            this.mEgl = egl10;
            javax.microedition.khronos.egl.EGLDisplay eglGetDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            this.mEglDisplay = eglGetDisplay;
            if (eglGetDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed");
            }
            int[] version = new int[2];
            if (!this.mEgl.eglInitialize(this.mEglDisplay, version)) {
                throw new RuntimeException("eglInitialize failed");
            }
            GLSurfaceView view = this.mGLSurfaceViewWeakRef.get();
            if (view == null) {
                this.mEglConfig = null;
                this.mEglContext = null;
            } else {
                this.mEglConfig = view.mEGLConfigChooser.chooseConfig(this.mEgl, this.mEglDisplay);
                this.mEglContext = view.mEGLContextFactory.createContext(this.mEgl, this.mEglDisplay, this.mEglConfig);
            }
            javax.microedition.khronos.egl.EGLContext eGLContext = this.mEglContext;
            if (eGLContext == null || eGLContext == EGL10.EGL_NO_CONTEXT) {
                this.mEglContext = null;
                throwEglException("createContext");
            }
            if (GLSurfaceView.LOG_EGL) {
                Log.w("EglHelper", "createContext " + this.mEglContext + " tid=" + Thread.currentThread().getId());
            }
            this.mEglSurface = null;
        }

        public boolean createSurface() {
            if (GLSurfaceView.LOG_EGL) {
                Log.w("EglHelper", "createSurface()  tid=" + Thread.currentThread().getId());
            }
            if (this.mEgl == null) {
                throw new RuntimeException("egl not initialized");
            }
            if (this.mEglDisplay == null) {
                throw new RuntimeException("eglDisplay not initialized");
            }
            if (this.mEglConfig == null) {
                throw new RuntimeException("mEglConfig not initialized");
            }
            destroySurfaceImp();
            GLSurfaceView view = this.mGLSurfaceViewWeakRef.get();
            if (view != null) {
                this.mEglSurface = view.mEGLWindowSurfaceFactory.createWindowSurface(this.mEgl, this.mEglDisplay, this.mEglConfig, view.getHolder());
            } else {
                this.mEglSurface = null;
            }
            javax.microedition.khronos.egl.EGLSurface eGLSurface = this.mEglSurface;
            if (eGLSurface == null || eGLSurface == EGL10.EGL_NO_SURFACE) {
                int error = this.mEgl.eglGetError();
                if (error == 12299) {
                    Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                }
                return false;
            }
            EGL10 egl10 = this.mEgl;
            javax.microedition.khronos.egl.EGLDisplay eGLDisplay = this.mEglDisplay;
            javax.microedition.khronos.egl.EGLSurface eGLSurface2 = this.mEglSurface;
            if (!egl10.eglMakeCurrent(eGLDisplay, eGLSurface2, eGLSurface2, this.mEglContext)) {
                logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", this.mEgl.eglGetError());
                return false;
            }
            return true;
        }

        GL createGL() {
            GL gl = this.mEglContext.getGL();
            GLSurfaceView view = this.mGLSurfaceViewWeakRef.get();
            if (view != null) {
                if (view.mGLWrapper != null) {
                    gl = view.mGLWrapper.wrap(gl);
                }
                if ((view.mDebugFlags & 3) != 0) {
                    int configFlags = 0;
                    Writer log = null;
                    if ((view.mDebugFlags & 1) != 0) {
                        configFlags = 0 | 1;
                    }
                    if ((view.mDebugFlags & 2) != 0) {
                        log = new LogWriter();
                    }
                    return GLDebugHelper.wrap(gl, configFlags, log);
                }
                return gl;
            }
            return gl;
        }

        public int swap() {
            if (!this.mEgl.eglSwapBuffers(this.mEglDisplay, this.mEglSurface)) {
                return this.mEgl.eglGetError();
            }
            return 12288;
        }

        public void destroySurface() {
            if (GLSurfaceView.LOG_EGL) {
                Log.w("EglHelper", "destroySurface()  tid=" + Thread.currentThread().getId());
            }
            destroySurfaceImp();
        }

        private void destroySurfaceImp() {
            javax.microedition.khronos.egl.EGLSurface eGLSurface = this.mEglSurface;
            if (eGLSurface != null && eGLSurface != EGL10.EGL_NO_SURFACE) {
                this.mEgl.eglMakeCurrent(this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                GLSurfaceView view = this.mGLSurfaceViewWeakRef.get();
                if (view != null) {
                    view.mEGLWindowSurfaceFactory.destroySurface(this.mEgl, this.mEglDisplay, this.mEglSurface);
                }
                this.mEglSurface = null;
            }
        }

        public void finish() {
            if (GLSurfaceView.LOG_EGL) {
                Log.w("EglHelper", "finish() tid=" + Thread.currentThread().getId());
            }
            if (this.mEglContext != null) {
                GLSurfaceView view = this.mGLSurfaceViewWeakRef.get();
                if (view != null) {
                    view.mEGLContextFactory.destroyContext(this.mEgl, this.mEglDisplay, this.mEglContext);
                }
                this.mEglContext = null;
            }
            javax.microedition.khronos.egl.EGLDisplay eGLDisplay = this.mEglDisplay;
            if (eGLDisplay != null) {
                this.mEgl.eglTerminate(eGLDisplay);
                this.mEglDisplay = null;
            }
        }

        private void throwEglException(String function) {
            throwEglException(function, this.mEgl.eglGetError());
        }

        public static void throwEglException(String function, int error) {
            String message = formatEglError(function, error);
            if (GLSurfaceView.LOG_THREADS) {
                Log.e("EglHelper", "throwEglException tid=" + Thread.currentThread().getId() + " " + message);
            }
            throw new RuntimeException(message);
        }

        public static void logEglErrorAsWarning(String tag, String function, int error) {
            Log.w(tag, formatEglError(function, error));
        }

        public static String formatEglError(String function, int error) {
            return function + " failed: " + EGLLogWrapper.getErrorString(error);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class GLThread extends Thread {
        private EglHelper mEglHelper;
        private boolean mExited;
        private boolean mFinishedCreatingEglSurface;
        private WeakReference<GLSurfaceView> mGLSurfaceViewWeakRef;
        private boolean mHasSurface;
        private boolean mHaveEglContext;
        private boolean mHaveEglSurface;
        private boolean mPaused;
        private boolean mRenderComplete;
        private boolean mRequestPaused;
        private boolean mShouldExit;
        private boolean mShouldReleaseEglContext;
        private boolean mSurfaceIsBad;
        private boolean mWaitingForSurface;
        private ArrayList<Runnable> mEventQueue = new ArrayList<>();
        private boolean mSizeChanged = true;
        private Runnable mFinishDrawingRunnable = null;
        private int mWidth = 0;
        private int mHeight = 0;
        private boolean mRequestRender = true;
        private int mRenderMode = 1;
        private boolean mWantRenderNotification = false;

        GLThread(WeakReference<GLSurfaceView> glSurfaceViewWeakRef) {
            this.mGLSurfaceViewWeakRef = glSurfaceViewWeakRef;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            setName("GLThread " + getId());
            if (GLSurfaceView.LOG_THREADS) {
                Log.i("GLThread", "starting tid=" + getId());
            }
            try {
                guardedRun();
            } catch (InterruptedException e) {
            } catch (Throwable th) {
                GLSurfaceView.sGLThreadManager.threadExiting(this);
                throw th;
            }
            GLSurfaceView.sGLThreadManager.threadExiting(this);
        }

        private void stopEglSurfaceLocked() {
            if (this.mHaveEglSurface) {
                this.mHaveEglSurface = false;
                this.mEglHelper.destroySurface();
            }
        }

        private void stopEglContextLocked() {
            if (this.mHaveEglContext) {
                this.mEglHelper.finish();
                this.mHaveEglContext = false;
                GLSurfaceView.sGLThreadManager.releaseEglContextLocked(this);
            }
        }

        /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
            jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:191:0x0359
            	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
            	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
            	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
            */
        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1628=4, 1507=10] */
        private void guardedRun() throws java.lang.InterruptedException {
            /*
                r24 = this;
                r1 = r24
                android.opengl.GLSurfaceView$EglHelper r0 = new android.opengl.GLSurfaceView$EglHelper
                java.lang.ref.WeakReference<android.opengl.GLSurfaceView> r2 = r1.mGLSurfaceViewWeakRef
                r0.<init>(r2)
                r1.mEglHelper = r0
                r0 = 0
                r1.mHaveEglContext = r0
                r1.mHaveEglSurface = r0
                r1.mWantRenderNotification = r0
                r2 = 0
                r3 = 0
                r4 = 0
                r5 = 0
                r6 = 0
                r7 = 0
                r8 = 0
                r9 = 0
                r10 = 0
                r11 = 0
                r12 = 0
                r13 = 0
                r14 = 0
            L1f:
                android.opengl.GLSurfaceView$GLThreadManager r15 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L5a3
                monitor-enter(r15)     // Catch: java.lang.Throwable -> L5a3
            L24:
                boolean r0 = r1.mShouldExit     // Catch: java.lang.Throwable -> L596
                if (r0 == 0) goto L3e
                monitor-exit(r15)     // Catch: java.lang.Throwable -> L39
                android.opengl.GLSurfaceView$GLThreadManager r16 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()
                monitor-enter(r16)
                r24.stopEglSurfaceLocked()     // Catch: java.lang.Throwable -> L36
                r24.stopEglContextLocked()     // Catch: java.lang.Throwable -> L36
                monitor-exit(r16)     // Catch: java.lang.Throwable -> L36
                return
            L36:
                r0 = move-exception
                monitor-exit(r16)     // Catch: java.lang.Throwable -> L36
                throw r0
            L39:
                r0 = move-exception
                r17 = r2
                goto L59f
            L3e:
                java.util.ArrayList<java.lang.Runnable> r0 = r1.mEventQueue     // Catch: java.lang.Throwable -> L596
                boolean r0 = r0.isEmpty()     // Catch: java.lang.Throwable -> L596
                r17 = r2
                if (r0 != 0) goto L55
                java.util.ArrayList<java.lang.Runnable> r0 = r1.mEventQueue     // Catch: java.lang.Throwable -> L5a1
                r2 = 0
                java.lang.Object r0 = r0.remove(r2)     // Catch: java.lang.Throwable -> L5a1
                java.lang.Runnable r0 = (java.lang.Runnable) r0     // Catch: java.lang.Throwable -> L5a1
                r13 = r0
                r0 = 0
                goto L2c1
            L55:
                r0 = 0
                boolean r2 = r1.mPaused     // Catch: java.lang.Throwable -> L58e
                r19 = r0
                boolean r0 = r1.mRequestPaused     // Catch: java.lang.Throwable -> L58e
                if (r2 == r0) goto Lb2
                r2 = r0
                r1.mPaused = r0     // Catch: java.lang.Throwable -> Lab
                android.opengl.GLSurfaceView$GLThreadManager r0 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> Lab
                r0.notifyAll()     // Catch: java.lang.Throwable -> Lab
                boolean r0 = android.opengl.GLSurfaceView.m2884$$Nest$sfgetLOG_PAUSE_RESUME()     // Catch: java.lang.Throwable -> Lab
                if (r0 == 0) goto La4
                java.lang.String r0 = "GLThread"
                r19 = r2
                java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lab
                r2.<init>()     // Catch: java.lang.Throwable -> Lab
                r20 = r3
                java.lang.String r3 = "mPaused is now "
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L9d
                boolean r3 = r1.mPaused     // Catch: java.lang.Throwable -> L9d
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L9d
                java.lang.String r3 = " tid="
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L9d
                r21 = r4
                long r3 = r24.getId()     // Catch: java.lang.Throwable -> Le7
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Le7
                java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> Le7
                android.util.Log.i(r0, r2)     // Catch: java.lang.Throwable -> Le7
                goto Lb6
            L9d:
                r0 = move-exception
                r21 = r4
                r3 = r20
                goto L59f
            La4:
                r19 = r2
                r20 = r3
                r21 = r4
                goto Lb6
            Lab:
                r0 = move-exception
                r20 = r3
                r21 = r4
                goto L59f
            Lb2:
                r20 = r3
                r21 = r4
            Lb6:
                boolean r0 = r1.mShouldReleaseEglContext     // Catch: java.lang.Throwable -> L586
                if (r0 == 0) goto Lee
                boolean r0 = android.opengl.GLSurfaceView.m2887$$Nest$sfgetLOG_SURFACE()     // Catch: java.lang.Throwable -> Le7
                if (r0 == 0) goto Ldc
                java.lang.String r0 = "GLThread"
                java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Le7
                r2.<init>()     // Catch: java.lang.Throwable -> Le7
                java.lang.String r3 = "releasing EGL context because asked to tid="
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Le7
                long r3 = r24.getId()     // Catch: java.lang.Throwable -> Le7
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Le7
                java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> Le7
                android.util.Log.i(r0, r2)     // Catch: java.lang.Throwable -> Le7
            Ldc:
                r24.stopEglSurfaceLocked()     // Catch: java.lang.Throwable -> Le7
                r24.stopEglContextLocked()     // Catch: java.lang.Throwable -> Le7
                r0 = 0
                r1.mShouldReleaseEglContext = r0     // Catch: java.lang.Throwable -> Le7
                r10 = 1
                goto Lee
            Le7:
                r0 = move-exception
                r3 = r20
                r4 = r21
                goto L59f
            Lee:
                if (r6 == 0) goto Lf8
                r24.stopEglSurfaceLocked()     // Catch: java.lang.Throwable -> Le7
                r24.stopEglContextLocked()     // Catch: java.lang.Throwable -> Le7
                r0 = 0
                r6 = r0
            Lf8:
                if (r19 == 0) goto L123
                boolean r0 = r1.mHaveEglSurface     // Catch: java.lang.Throwable -> Le7
                if (r0 == 0) goto L123
                boolean r0 = android.opengl.GLSurfaceView.m2887$$Nest$sfgetLOG_SURFACE()     // Catch: java.lang.Throwable -> Le7
                if (r0 == 0) goto L120
                java.lang.String r0 = "GLThread"
                java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Le7
                r2.<init>()     // Catch: java.lang.Throwable -> Le7
                java.lang.String r3 = "releasing EGL surface because paused tid="
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Le7
                long r3 = r24.getId()     // Catch: java.lang.Throwable -> Le7
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Le7
                java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> Le7
                android.util.Log.i(r0, r2)     // Catch: java.lang.Throwable -> Le7
            L120:
                r24.stopEglSurfaceLocked()     // Catch: java.lang.Throwable -> Le7
            L123:
                if (r19 == 0) goto L16f
                boolean r0 = r1.mHaveEglContext     // Catch: java.lang.Throwable -> L586
                if (r0 == 0) goto L16f
                java.lang.ref.WeakReference<android.opengl.GLSurfaceView> r0 = r1.mGLSurfaceViewWeakRef     // Catch: java.lang.Throwable -> L586
                java.lang.Object r0 = r0.get()     // Catch: java.lang.Throwable -> L586
                android.opengl.GLSurfaceView r0 = (android.opengl.GLSurfaceView) r0     // Catch: java.lang.Throwable -> L586
                if (r0 != 0) goto L135
                r2 = 0
                goto L139
            L135:
                boolean r2 = android.opengl.GLSurfaceView.m2881$$Nest$fgetmPreserveEGLContextOnPause(r0)     // Catch: java.lang.Throwable -> L586
            L139:
                if (r2 != 0) goto L16a
                r24.stopEglContextLocked()     // Catch: java.lang.Throwable -> L586
                boolean r3 = android.opengl.GLSurfaceView.m2887$$Nest$sfgetLOG_SURFACE()     // Catch: java.lang.Throwable -> L586
                if (r3 == 0) goto L165
                java.lang.String r3 = "GLThread"
                java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L586
                r4.<init>()     // Catch: java.lang.Throwable -> L586
                r22 = r0
                java.lang.String r0 = "releasing EGL context because paused tid="
                java.lang.StringBuilder r0 = r4.append(r0)     // Catch: java.lang.Throwable -> L586
                r23 = r5
                long r4 = r24.getId()     // Catch: java.lang.Throwable -> L57e
                java.lang.StringBuilder r0 = r0.append(r4)     // Catch: java.lang.Throwable -> L57e
                java.lang.String r0 = r0.toString()     // Catch: java.lang.Throwable -> L57e
                android.util.Log.i(r3, r0)     // Catch: java.lang.Throwable -> L57e
                goto L171
            L165:
                r22 = r0
                r23 = r5
                goto L171
            L16a:
                r22 = r0
                r23 = r5
                goto L171
            L16f:
                r23 = r5
            L171:
                boolean r0 = r1.mHasSurface     // Catch: java.lang.Throwable -> L57e
                if (r0 != 0) goto L1af
                boolean r0 = r1.mWaitingForSurface     // Catch: java.lang.Throwable -> L57e
                if (r0 != 0) goto L1af
                boolean r0 = android.opengl.GLSurfaceView.m2887$$Nest$sfgetLOG_SURFACE()     // Catch: java.lang.Throwable -> L57e
                if (r0 == 0) goto L19b
                java.lang.String r0 = "GLThread"
                java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L57e
                r2.<init>()     // Catch: java.lang.Throwable -> L57e
                java.lang.String r3 = "noticed surfaceView surface lost tid="
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L57e
                long r3 = r24.getId()     // Catch: java.lang.Throwable -> L57e
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L57e
                java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L57e
                android.util.Log.i(r0, r2)     // Catch: java.lang.Throwable -> L57e
            L19b:
                boolean r0 = r1.mHaveEglSurface     // Catch: java.lang.Throwable -> L57e
                if (r0 == 0) goto L1a2
                r24.stopEglSurfaceLocked()     // Catch: java.lang.Throwable -> L57e
            L1a2:
                r0 = 1
                r1.mWaitingForSurface = r0     // Catch: java.lang.Throwable -> L57e
                r0 = 0
                r1.mSurfaceIsBad = r0     // Catch: java.lang.Throwable -> L57e
                android.opengl.GLSurfaceView$GLThreadManager r0 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L57e
                r0.notifyAll()     // Catch: java.lang.Throwable -> L57e
            L1af:
                boolean r0 = r1.mHasSurface     // Catch: java.lang.Throwable -> L57e
                if (r0 == 0) goto L1e3
                boolean r0 = r1.mWaitingForSurface     // Catch: java.lang.Throwable -> L57e
                if (r0 == 0) goto L1e3
                boolean r0 = android.opengl.GLSurfaceView.m2887$$Nest$sfgetLOG_SURFACE()     // Catch: java.lang.Throwable -> L57e
                if (r0 == 0) goto L1d9
                java.lang.String r0 = "GLThread"
                java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L57e
                r2.<init>()     // Catch: java.lang.Throwable -> L57e
                java.lang.String r3 = "noticed surfaceView surface acquired tid="
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L57e
                long r3 = r24.getId()     // Catch: java.lang.Throwable -> L57e
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L57e
                java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L57e
                android.util.Log.i(r0, r2)     // Catch: java.lang.Throwable -> L57e
            L1d9:
                r0 = 0
                r1.mWaitingForSurface = r0     // Catch: java.lang.Throwable -> L57e
                android.opengl.GLSurfaceView$GLThreadManager r0 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L57e
                r0.notifyAll()     // Catch: java.lang.Throwable -> L57e
            L1e3:
                if (r9 == 0) goto L215
                boolean r0 = android.opengl.GLSurfaceView.m2887$$Nest$sfgetLOG_SURFACE()     // Catch: java.lang.Throwable -> L57e
                if (r0 == 0) goto L207
                java.lang.String r0 = "GLThread"
                java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L57e
                r2.<init>()     // Catch: java.lang.Throwable -> L57e
                java.lang.String r3 = "sending render notification tid="
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L57e
                long r3 = r24.getId()     // Catch: java.lang.Throwable -> L57e
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L57e
                java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L57e
                android.util.Log.i(r0, r2)     // Catch: java.lang.Throwable -> L57e
            L207:
                r0 = 0
                r1.mWantRenderNotification = r0     // Catch: java.lang.Throwable -> L57e
                r9 = 0
                r0 = 1
                r1.mRenderComplete = r0     // Catch: java.lang.Throwable -> L57e
                android.opengl.GLSurfaceView$GLThreadManager r0 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L57e
                r0.notifyAll()     // Catch: java.lang.Throwable -> L57e
            L215:
                java.lang.Runnable r0 = r1.mFinishDrawingRunnable     // Catch: java.lang.Throwable -> L57e
                if (r0 == 0) goto L21d
                r14 = r0
                r0 = 0
                r1.mFinishDrawingRunnable = r0     // Catch: java.lang.Throwable -> L57e
            L21d:
                boolean r0 = r24.readyToDraw()     // Catch: java.lang.Throwable -> L57e
                if (r0 == 0) goto L491
                boolean r0 = r1.mHaveEglContext     // Catch: java.lang.Throwable -> L57e
                if (r0 != 0) goto L24a
                if (r10 == 0) goto L22e
                r0 = 0
                r10 = r0
                r3 = r20
                goto L24c
            L22e:
                android.opengl.GLSurfaceView$EglHelper r0 = r1.mEglHelper     // Catch: java.lang.RuntimeException -> L240 java.lang.Throwable -> L57e
                r0.start()     // Catch: java.lang.RuntimeException -> L240 java.lang.Throwable -> L57e
                r0 = 1
                r1.mHaveEglContext = r0     // Catch: java.lang.Throwable -> L57e
                r3 = 1
                android.opengl.GLSurfaceView$GLThreadManager r0 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L25b
                r0.notifyAll()     // Catch: java.lang.Throwable -> L25b
                goto L24c
            L240:
                r0 = move-exception
                android.opengl.GLSurfaceView$GLThreadManager r2 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L57e
                r2.releaseEglContextLocked(r1)     // Catch: java.lang.Throwable -> L57e
                throw r0     // Catch: java.lang.Throwable -> L57e
            L24a:
                r3 = r20
            L24c:
                boolean r0 = r1.mHaveEglContext     // Catch: java.lang.Throwable -> L488
                if (r0 == 0) goto L262
                boolean r0 = r1.mHaveEglSurface     // Catch: java.lang.Throwable -> L25b
                if (r0 != 0) goto L262
                r0 = 1
                r1.mHaveEglSurface = r0     // Catch: java.lang.Throwable -> L25b
                r4 = 1
                r5 = 1
                r7 = 1
                goto L266
            L25b:
                r0 = move-exception
                r4 = r21
                r5 = r23
                goto L59f
            L262:
                r4 = r21
                r5 = r23
            L266:
                boolean r0 = r1.mHaveEglSurface     // Catch: java.lang.Throwable -> Lab
                if (r0 == 0) goto L483
                boolean r0 = r1.mSizeChanged     // Catch: java.lang.Throwable -> Lab
                if (r0 == 0) goto L2a8
                r7 = 1
                int r0 = r1.mWidth     // Catch: java.lang.Throwable -> Lab
                r11 = r0
                int r0 = r1.mHeight     // Catch: java.lang.Throwable -> Lab
                r12 = r0
                r0 = 1
                r1.mWantRenderNotification = r0     // Catch: java.lang.Throwable -> Lab
                boolean r0 = android.opengl.GLSurfaceView.m2887$$Nest$sfgetLOG_SURFACE()     // Catch: java.lang.Throwable -> Lab
                if (r0 == 0) goto L29f
                java.lang.String r0 = "GLThread"
                java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lab
                r2.<init>()     // Catch: java.lang.Throwable -> Lab
                r20 = r3
                java.lang.String r3 = "noticing that we want render notification tid="
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L9d
                r21 = r4
                long r3 = r24.getId()     // Catch: java.lang.Throwable -> Le7
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> Le7
                java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> Le7
                android.util.Log.i(r0, r2)     // Catch: java.lang.Throwable -> Le7
                goto L2a3
            L29f:
                r20 = r3
                r21 = r4
            L2a3:
                r4 = 1
                r0 = 0
                r1.mSizeChanged = r0     // Catch: java.lang.Throwable -> L47e
                goto L2ac
            L2a8:
                r20 = r3
                r21 = r4
            L2ac:
                r0 = 0
                r1.mRequestRender = r0     // Catch: java.lang.Throwable -> L47e
                android.opengl.GLSurfaceView$GLThreadManager r2 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L47e
                r2.notifyAll()     // Catch: java.lang.Throwable -> L47e
                boolean r2 = r1.mWantRenderNotification     // Catch: java.lang.Throwable -> L47e
                if (r2 == 0) goto L2bf
                r2 = 1
                r8 = r2
                r3 = r20
                goto L2c1
            L2bf:
                r3 = r20
            L2c1:
                monitor-exit(r15)     // Catch: java.lang.Throwable -> L479
                if (r13 == 0) goto L2cc
                r13.run()     // Catch: java.lang.Throwable -> L5a3
                r13 = 0
                r2 = r17
                goto L1f
            L2cc:
                if (r4 == 0) goto L312
                boolean r2 = android.opengl.GLSurfaceView.m2887$$Nest$sfgetLOG_SURFACE()     // Catch: java.lang.Throwable -> L5a3
                if (r2 == 0) goto L2db
                java.lang.String r2 = "GLThread"
                java.lang.String r15 = "egl createSurface"
                android.util.Log.w(r2, r15)     // Catch: java.lang.Throwable -> L5a3
            L2db:
                android.opengl.GLSurfaceView$EglHelper r2 = r1.mEglHelper     // Catch: java.lang.Throwable -> L5a3
                boolean r2 = r2.createSurface()     // Catch: java.lang.Throwable -> L5a3
                if (r2 == 0) goto L2f9
                android.opengl.GLSurfaceView$GLThreadManager r2 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L5a3
                monitor-enter(r2)     // Catch: java.lang.Throwable -> L5a3
                r15 = 1
                r1.mFinishedCreatingEglSurface = r15     // Catch: java.lang.Throwable -> L2f6
                android.opengl.GLSurfaceView$GLThreadManager r15 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L2f6
                r15.notifyAll()     // Catch: java.lang.Throwable -> L2f6
                monitor-exit(r2)     // Catch: java.lang.Throwable -> L2f6
                r2 = 0
                r4 = r2
                goto L312
            L2f6:
                r0 = move-exception
                monitor-exit(r2)     // Catch: java.lang.Throwable -> L2f6
                throw r0     // Catch: java.lang.Throwable -> L5a3
            L2f9:
                android.opengl.GLSurfaceView$GLThreadManager r2 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L5a3
                monitor-enter(r2)     // Catch: java.lang.Throwable -> L5a3
                r15 = 1
                r1.mFinishedCreatingEglSurface = r15     // Catch: java.lang.Throwable -> L30f
                r1.mSurfaceIsBad = r15     // Catch: java.lang.Throwable -> L30f
                android.opengl.GLSurfaceView$GLThreadManager r15 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L30f
                r15.notifyAll()     // Catch: java.lang.Throwable -> L30f
                monitor-exit(r2)     // Catch: java.lang.Throwable -> L30f
                r2 = r17
                goto L1f
            L30f:
                r0 = move-exception
                monitor-exit(r2)     // Catch: java.lang.Throwable -> L30f
                throw r0     // Catch: java.lang.Throwable -> L5a3
            L312:
                if (r5 == 0) goto L31e
                android.opengl.GLSurfaceView$EglHelper r2 = r1.mEglHelper     // Catch: java.lang.Throwable -> L5a3
                javax.microedition.khronos.opengles.GL r2 = r2.createGL()     // Catch: java.lang.Throwable -> L5a3
                javax.microedition.khronos.opengles.GL10 r2 = (javax.microedition.khronos.opengles.GL10) r2     // Catch: java.lang.Throwable -> L5a3
                r5 = 0
                goto L320
            L31e:
                r2 = r17
            L320:
                r15 = r4
                r16 = r5
                if (r3 == 0) goto L368
                boolean r17 = android.opengl.GLSurfaceView.m2885$$Nest$sfgetLOG_RENDERER()     // Catch: java.lang.Throwable -> L5a3
                if (r17 == 0) goto L332
                java.lang.String r0 = "GLThread"
                java.lang.String r4 = "onSurfaceCreated"
                android.util.Log.w(r0, r4)     // Catch: java.lang.Throwable -> L5a3
            L332:
                java.lang.ref.WeakReference<android.opengl.GLSurfaceView> r0 = r1.mGLSurfaceViewWeakRef     // Catch: java.lang.Throwable -> L5a3
                java.lang.Object r0 = r0.get()     // Catch: java.lang.Throwable -> L5a3
                android.opengl.GLSurfaceView r0 = (android.opengl.GLSurfaceView) r0     // Catch: java.lang.Throwable -> L5a3
                r4 = r0
                if (r4 == 0) goto L363
                java.lang.String r0 = "onSurfaceCreated"
                r21 = r6
                r5 = 8
                android.os.Trace.traceBegin(r5, r0)     // Catch: java.lang.Throwable -> L357
                android.opengl.GLSurfaceView$Renderer r0 = android.opengl.GLSurfaceView.m2882$$Nest$fgetmRenderer(r4)     // Catch: java.lang.Throwable -> L357
                android.opengl.GLSurfaceView$EglHelper r5 = r1.mEglHelper     // Catch: java.lang.Throwable -> L357
                javax.microedition.khronos.egl.EGLConfig r5 = r5.mEglConfig     // Catch: java.lang.Throwable -> L357
                r0.onSurfaceCreated(r2, r5)     // Catch: java.lang.Throwable -> L357
                r5 = 8
                android.os.Trace.traceEnd(r5)     // Catch: java.lang.Throwable -> L5a3
                goto L365
            L357:
                r0 = move-exception
                goto L35c
            L359:
                r0 = move-exception
                r21 = r6
            L35c:
                r5 = 8
                android.os.Trace.traceEnd(r5)     // Catch: java.lang.Throwable -> L5a3
                throw r0     // Catch: java.lang.Throwable -> L5a3
            L363:
                r21 = r6
            L365:
                r0 = 0
                r3 = r0
                goto L36a
            L368:
                r21 = r6
            L36a:
                if (r7 == 0) goto L3c1
                boolean r0 = android.opengl.GLSurfaceView.m2885$$Nest$sfgetLOG_RENDERER()     // Catch: java.lang.Throwable -> L5a3
                if (r0 == 0) goto L39a
                java.lang.String r0 = "GLThread"
                java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L5a3
                r4.<init>()     // Catch: java.lang.Throwable -> L5a3
                java.lang.String r5 = "onSurfaceChanged("
                java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L5a3
                java.lang.StringBuilder r4 = r4.append(r11)     // Catch: java.lang.Throwable -> L5a3
                java.lang.String r5 = ", "
                java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L5a3
                java.lang.StringBuilder r4 = r4.append(r12)     // Catch: java.lang.Throwable -> L5a3
                java.lang.String r5 = ")"
                java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L5a3
                java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> L5a3
                android.util.Log.w(r0, r4)     // Catch: java.lang.Throwable -> L5a3
            L39a:
                java.lang.ref.WeakReference<android.opengl.GLSurfaceView> r0 = r1.mGLSurfaceViewWeakRef     // Catch: java.lang.Throwable -> L5a3
                java.lang.Object r0 = r0.get()     // Catch: java.lang.Throwable -> L5a3
                android.opengl.GLSurfaceView r0 = (android.opengl.GLSurfaceView) r0     // Catch: java.lang.Throwable -> L5a3
                r4 = r0
                if (r4 == 0) goto L3bf
                java.lang.String r0 = "onSurfaceChanged"
                r5 = 8
                android.os.Trace.traceBegin(r5, r0)     // Catch: java.lang.Throwable -> L3b7
                android.opengl.GLSurfaceView$Renderer r0 = android.opengl.GLSurfaceView.m2882$$Nest$fgetmRenderer(r4)     // Catch: java.lang.Throwable -> L3b7
                r0.onSurfaceChanged(r2, r11, r12)     // Catch: java.lang.Throwable -> L3b7
                android.os.Trace.traceEnd(r5)     // Catch: java.lang.Throwable -> L5a3
                goto L3bf
            L3b7:
                r0 = move-exception
                r5 = 8
                android.os.Trace.traceEnd(r5)     // Catch: java.lang.Throwable -> L5a3
                throw r0     // Catch: java.lang.Throwable -> L5a3
            L3bf:
                r0 = 0
                r7 = r0
            L3c1:
                boolean r0 = android.opengl.GLSurfaceView.m2886$$Nest$sfgetLOG_RENDERER_DRAW_FRAME()     // Catch: java.lang.Throwable -> L5a3
                if (r0 == 0) goto L3e3
                java.lang.String r0 = "GLThread"
                java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L5a3
                r4.<init>()     // Catch: java.lang.Throwable -> L5a3
                java.lang.String r5 = "onDrawFrame tid="
                java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L5a3
                long r5 = r24.getId()     // Catch: java.lang.Throwable -> L5a3
                java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L5a3
                java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> L5a3
                android.util.Log.w(r0, r4)     // Catch: java.lang.Throwable -> L5a3
            L3e3:
                java.lang.ref.WeakReference<android.opengl.GLSurfaceView> r0 = r1.mGLSurfaceViewWeakRef     // Catch: java.lang.Throwable -> L5a3
                java.lang.Object r0 = r0.get()     // Catch: java.lang.Throwable -> L5a3
                android.opengl.GLSurfaceView r0 = (android.opengl.GLSurfaceView) r0     // Catch: java.lang.Throwable -> L5a3
                r4 = r0
                if (r4 == 0) goto L410
                java.lang.String r0 = "onDrawFrame"
                r5 = 8
                android.os.Trace.traceBegin(r5, r0)     // Catch: java.lang.Throwable -> L408
                android.opengl.GLSurfaceView$Renderer r0 = android.opengl.GLSurfaceView.m2882$$Nest$fgetmRenderer(r4)     // Catch: java.lang.Throwable -> L408
                r0.onDrawFrame(r2)     // Catch: java.lang.Throwable -> L408
                if (r14 == 0) goto L402
                r14.run()     // Catch: java.lang.Throwable -> L408
                r14 = 0
            L402:
                r5 = 8
                android.os.Trace.traceEnd(r5)     // Catch: java.lang.Throwable -> L5a3
                goto L410
            L408:
                r0 = move-exception
                r5 = 8
                android.os.Trace.traceEnd(r5)     // Catch: java.lang.Throwable -> L5a3
                throw r0     // Catch: java.lang.Throwable -> L5a3
            L410:
                android.opengl.GLSurfaceView$EglHelper r0 = r1.mEglHelper     // Catch: java.lang.Throwable -> L5a3
                int r0 = r0.swap()     // Catch: java.lang.Throwable -> L5a3
                r4 = r0
                switch(r4) {
                    case 12288: goto L44b;
                    case 12302: goto L420;
                    default: goto L41a;
                }     // Catch: java.lang.Throwable -> L5a3
            L41a:
                r6 = r2
                r17 = r3
                java.lang.String r0 = "GLThread"
                goto L44f
            L420:
                boolean r0 = android.opengl.GLSurfaceView.m2887$$Nest$sfgetLOG_SURFACE()     // Catch: java.lang.Throwable -> L5a3
                if (r0 == 0) goto L446
                java.lang.String r0 = "GLThread"
                java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L5a3
                r5.<init>()     // Catch: java.lang.Throwable -> L5a3
                java.lang.String r6 = "egl context lost tid="
                java.lang.StringBuilder r5 = r5.append(r6)     // Catch: java.lang.Throwable -> L5a3
                r6 = r2
                r17 = r3
                long r2 = r24.getId()     // Catch: java.lang.Throwable -> L5a3
                java.lang.StringBuilder r2 = r5.append(r2)     // Catch: java.lang.Throwable -> L5a3
                java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L5a3
                android.util.Log.i(r0, r2)     // Catch: java.lang.Throwable -> L5a3
                goto L449
            L446:
                r6 = r2
                r17 = r3
            L449:
                r0 = 1
                goto L466
            L44b:
                r6 = r2
                r17 = r3
                goto L464
            L44f:
                java.lang.String r2 = "eglSwapBuffers"
                android.opengl.GLSurfaceView.EglHelper.logEglErrorAsWarning(r0, r2, r4)     // Catch: java.lang.Throwable -> L5a3
                android.opengl.GLSurfaceView$GLThreadManager r2 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L5a3
                monitor-enter(r2)     // Catch: java.lang.Throwable -> L5a3
                r0 = 1
                r1.mSurfaceIsBad = r0     // Catch: java.lang.Throwable -> L476
                android.opengl.GLSurfaceView$GLThreadManager r0 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L476
                r0.notifyAll()     // Catch: java.lang.Throwable -> L476
                monitor-exit(r2)     // Catch: java.lang.Throwable -> L476
            L464:
                r0 = r21
            L466:
                if (r8 == 0) goto L46c
                r2 = 1
                r3 = 0
                r9 = r2
                r8 = r3
            L46c:
                r2 = r6
                r4 = r15
                r5 = r16
                r3 = r17
                r6 = r0
                r0 = 0
                goto L1f
            L476:
                r0 = move-exception
                monitor-exit(r2)     // Catch: java.lang.Throwable -> L476
                throw r0     // Catch: java.lang.Throwable -> L5a3
            L479:
                r0 = move-exception
                r21 = r6
                goto L59f
            L47e:
                r0 = move-exception
                r3 = r20
                goto L59f
            L483:
                r20 = r3
                r21 = r4
                goto L4ac
            L488:
                r0 = move-exception
                r20 = r3
                r4 = r21
                r5 = r23
                goto L59f
            L491:
                if (r14 == 0) goto L4a6
                java.lang.String r0 = "GLSurfaceView"
                java.lang.String r2 = "Warning, !readyToDraw() but waiting for draw finished! Early reporting draw finished."
                android.util.Log.w(r0, r2)     // Catch: java.lang.Throwable -> L57e
                r14.run()     // Catch: java.lang.Throwable -> L57e
                r0 = 0
                r14 = r0
                r3 = r20
                r4 = r21
                r5 = r23
                goto L4ac
            L4a6:
                r3 = r20
                r4 = r21
                r5 = r23
            L4ac:
                boolean r0 = android.opengl.GLSurfaceView.m2888$$Nest$sfgetLOG_THREADS()     // Catch: java.lang.Throwable -> L578
                if (r0 == 0) goto L55e
                java.lang.String r0 = "GLThread"
                java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L578
                r2.<init>()     // Catch: java.lang.Throwable -> L578
                r16 = r3
                java.lang.String r3 = "waiting tid="
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L558
                r18 = r4
                long r3 = r24.getId()     // Catch: java.lang.Throwable -> L572
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                java.lang.String r3 = " mHaveEglContext: "
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                boolean r3 = r1.mHaveEglContext     // Catch: java.lang.Throwable -> L572
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                java.lang.String r3 = " mHaveEglSurface: "
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                boolean r3 = r1.mHaveEglSurface     // Catch: java.lang.Throwable -> L572
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                java.lang.String r3 = " mFinishedCreatingEglSurface: "
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                boolean r3 = r1.mFinishedCreatingEglSurface     // Catch: java.lang.Throwable -> L572
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                java.lang.String r3 = " mPaused: "
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                boolean r3 = r1.mPaused     // Catch: java.lang.Throwable -> L572
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                java.lang.String r3 = " mHasSurface: "
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                boolean r3 = r1.mHasSurface     // Catch: java.lang.Throwable -> L572
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                java.lang.String r3 = " mSurfaceIsBad: "
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                boolean r3 = r1.mSurfaceIsBad     // Catch: java.lang.Throwable -> L572
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                java.lang.String r3 = " mWaitingForSurface: "
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                boolean r3 = r1.mWaitingForSurface     // Catch: java.lang.Throwable -> L572
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                java.lang.String r3 = " mWidth: "
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                int r3 = r1.mWidth     // Catch: java.lang.Throwable -> L572
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                java.lang.String r3 = " mHeight: "
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                int r3 = r1.mHeight     // Catch: java.lang.Throwable -> L572
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                java.lang.String r3 = " mRequestRender: "
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                boolean r3 = r1.mRequestRender     // Catch: java.lang.Throwable -> L572
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                java.lang.String r3 = " mRenderMode: "
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                int r3 = r1.mRenderMode     // Catch: java.lang.Throwable -> L572
                java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.Throwable -> L572
                java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L572
                android.util.Log.i(r0, r2)     // Catch: java.lang.Throwable -> L572
                goto L562
            L558:
                r0 = move-exception
                r18 = r4
                r3 = r16
                goto L59f
            L55e:
                r16 = r3
                r18 = r4
            L562:
                android.opengl.GLSurfaceView$GLThreadManager r0 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()     // Catch: java.lang.Throwable -> L572
                r0.wait()     // Catch: java.lang.Throwable -> L572
                r3 = r16
                r2 = r17
                r4 = r18
                r0 = 0
                goto L24
            L572:
                r0 = move-exception
                r3 = r16
                r4 = r18
                goto L59f
            L578:
                r0 = move-exception
                r16 = r3
                r18 = r4
                goto L59f
            L57e:
                r0 = move-exception
                r3 = r20
                r4 = r21
                r5 = r23
                goto L59f
            L586:
                r0 = move-exception
                r23 = r5
                r3 = r20
                r4 = r21
                goto L59f
            L58e:
                r0 = move-exception
                r20 = r3
                r21 = r4
                r23 = r5
                goto L59f
            L596:
                r0 = move-exception
                r17 = r2
                r20 = r3
                r21 = r4
                r23 = r5
            L59f:
                monitor-exit(r15)     // Catch: java.lang.Throwable -> L5a1
                throw r0     // Catch: java.lang.Throwable -> L5a3
            L5a1:
                r0 = move-exception
                goto L59f
            L5a3:
                r0 = move-exception
                android.opengl.GLSurfaceView$GLThreadManager r2 = android.opengl.GLSurfaceView.m2889$$Nest$sfgetsGLThreadManager()
                monitor-enter(r2)
                r24.stopEglSurfaceLocked()     // Catch: java.lang.Throwable -> L5b1
                r24.stopEglContextLocked()     // Catch: java.lang.Throwable -> L5b1
                monitor-exit(r2)     // Catch: java.lang.Throwable -> L5b1
                throw r0
            L5b1:
                r0 = move-exception
                monitor-exit(r2)     // Catch: java.lang.Throwable -> L5b1
                throw r0
            */
            throw new UnsupportedOperationException("Method not decompiled: android.opengl.GLSurfaceView.GLThread.guardedRun():void");
        }

        public boolean ableToDraw() {
            return this.mHaveEglContext && this.mHaveEglSurface && readyToDraw();
        }

        private boolean readyToDraw() {
            return !this.mPaused && this.mHasSurface && !this.mSurfaceIsBad && this.mWidth > 0 && this.mHeight > 0 && (this.mRequestRender || this.mRenderMode == 1);
        }

        public void setRenderMode(int renderMode) {
            if (renderMode < 0 || renderMode > 1) {
                throw new IllegalArgumentException("renderMode");
            }
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mRenderMode = renderMode;
                GLSurfaceView.sGLThreadManager.notifyAll();
            }
        }

        public int getRenderMode() {
            int i;
            synchronized (GLSurfaceView.sGLThreadManager) {
                i = this.mRenderMode;
            }
            return i;
        }

        public void requestRender() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mRequestRender = true;
                GLSurfaceView.sGLThreadManager.notifyAll();
            }
        }

        public void requestRenderAndNotify(Runnable finishDrawing) {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (Thread.currentThread() == this) {
                    return;
                }
                this.mWantRenderNotification = true;
                this.mRequestRender = true;
                this.mRenderComplete = false;
                this.mFinishDrawingRunnable = finishDrawing;
                GLSurfaceView.sGLThreadManager.notifyAll();
            }
        }

        public void surfaceCreated() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("GLThread", "surfaceCreated tid=" + getId());
                }
                this.mHasSurface = true;
                this.mFinishedCreatingEglSurface = false;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (this.mWaitingForSurface && !this.mFinishedCreatingEglSurface && !this.mExited) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void surfaceDestroyed() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (GLSurfaceView.LOG_THREADS) {
                    Log.i("GLThread", "surfaceDestroyed tid=" + getId());
                }
                this.mHasSurface = false;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mWaitingForSurface && !this.mExited) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onPause() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (GLSurfaceView.LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onPause tid=" + getId());
                }
                this.mRequestPaused = true;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited && !this.mPaused) {
                    if (GLSurfaceView.LOG_PAUSE_RESUME) {
                        Log.i("Main thread", "onPause waiting for mPaused.");
                    }
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onResume() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                if (GLSurfaceView.LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onResume tid=" + getId());
                }
                this.mRequestPaused = false;
                this.mRequestRender = true;
                this.mRenderComplete = false;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited && this.mPaused && !this.mRenderComplete) {
                    if (GLSurfaceView.LOG_PAUSE_RESUME) {
                        Log.i("Main thread", "onResume waiting for !mPaused.");
                    }
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onWindowResize(int w, int h) {
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mWidth = w;
                this.mHeight = h;
                this.mSizeChanged = true;
                this.mRequestRender = true;
                this.mRenderComplete = false;
                if (Thread.currentThread() == this) {
                    return;
                }
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited && !this.mPaused && !this.mRenderComplete && ableToDraw()) {
                    if (GLSurfaceView.LOG_SURFACE) {
                        Log.i("Main thread", "onWindowResize waiting for render complete from tid=" + getId());
                    }
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void requestExitAndWait() {
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mShouldExit = true;
                GLSurfaceView.sGLThreadManager.notifyAll();
                while (!this.mExited) {
                    try {
                        GLSurfaceView.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void requestReleaseEglContextLocked() {
            this.mShouldReleaseEglContext = true;
            GLSurfaceView.sGLThreadManager.notifyAll();
        }

        public void queueEvent(Runnable r) {
            if (r == null) {
                throw new IllegalArgumentException("r must not be null");
            }
            synchronized (GLSurfaceView.sGLThreadManager) {
                this.mEventQueue.add(r);
                GLSurfaceView.sGLThreadManager.notifyAll();
            }
        }
    }

    /* loaded from: classes2.dex */
    static class LogWriter extends Writer {
        private StringBuilder mBuilder = new StringBuilder();

        LogWriter() {
        }

        @Override // java.io.Writer, java.io.Closeable, java.lang.AutoCloseable
        public void close() {
            flushBuilder();
        }

        @Override // java.io.Writer, java.io.Flushable
        public void flush() {
            flushBuilder();
        }

        @Override // java.io.Writer
        public void write(char[] buf, int offset, int count) {
            for (int i = 0; i < count; i++) {
                char c = buf[offset + i];
                if (c == '\n') {
                    flushBuilder();
                } else {
                    this.mBuilder.append(c);
                }
            }
        }

        private void flushBuilder() {
            if (this.mBuilder.length() > 0) {
                Log.v(GLSurfaceView.TAG, this.mBuilder.toString());
                StringBuilder sb = this.mBuilder;
                sb.delete(0, sb.length());
            }
        }
    }

    private void checkRenderThreadState() {
        if (this.mGLThread != null) {
            throw new IllegalStateException("setRenderer has already been called for this instance.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class GLThreadManager {
        private static String TAG = "GLThreadManager";

        private GLThreadManager() {
        }

        public synchronized void threadExiting(GLThread thread) {
            if (GLSurfaceView.LOG_THREADS) {
                Log.i("GLThread", "exiting tid=" + thread.getId());
            }
            thread.mExited = true;
            notifyAll();
        }

        public void releaseEglContextLocked(GLThread thread) {
            notifyAll();
        }
    }
}
