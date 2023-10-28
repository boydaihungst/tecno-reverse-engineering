package com.android.server.display;

import android.content.Context;
import android.graphics.BLASTBufferQueue;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManagerInternal;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.server.LocalServices;
import com.android.server.wm.utils.RotationAnimationUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import libcore.io.Streams;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class ColorFade {
    private static final int COLOR_FADE_LAYER = 1073741825;
    private static final boolean DEBUG = SystemProperties.getBoolean("dbg.dms.colorfade", false);
    private static final int DEJANK_FRAMES = 3;
    private static final int EGL_GL_COLORSPACE_DISPLAY_P3_PASSTHROUGH_EXT = 13456;
    private static final int EGL_GL_COLORSPACE_KHR = 12445;
    private static final int EGL_PROTECTED_CONTENT_EXT = 12992;
    public static final int MODE_COOL_DOWN = 1;
    public static final int MODE_FADE = 2;
    public static final int MODE_WARM_UP = 0;
    private static final String TAG = "ColorFade";
    private BLASTBufferQueue mBLASTBufferQueue;
    private SurfaceControl mBLASTSurfaceControl;
    private boolean mCreatedResources;
    private int mDisplayHeight;
    private final int mDisplayId;
    private int mDisplayLayerStack;
    private int mDisplayWidth;
    private EGLConfig mEglConfig;
    private EGLContext mEglContext;
    private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface;
    private int mGammaLoc;
    private boolean mLastWasProtectedContent;
    private boolean mLastWasWideColor;
    private int mMode;
    private int mOpacityLoc;
    private boolean mPrepared;
    private int mProgram;
    private int mProjMatrixLoc;
    private Surface mSurface;
    private float mSurfaceAlpha;
    private SurfaceControl mSurfaceControl;
    private NaturalSurfaceLayout mSurfaceLayout;
    private boolean mSurfaceVisible;
    private int mTexCoordLoc;
    private int mTexMatrixLoc;
    private boolean mTexNamesGenerated;
    private int mTexUnitLoc;
    private int mVertexLoc;
    private final int[] mTexNames = new int[1];
    private final float[] mTexMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final int[] mGLBuffers = new int[2];
    private final FloatBuffer mVertexBuffer = createNativeFloatBuffer(8);
    private final FloatBuffer mTexCoordBuffer = createNativeFloatBuffer(8);
    private final SurfaceControl.Transaction mTransaction = new SurfaceControl.Transaction();
    private final DisplayManagerInternal mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);

    public ColorFade(int displayId) {
        this.mDisplayId = displayId;
    }

    public boolean prepare(Context context, int mode) {
        if (DEBUG) {
            Slog.d(TAG, "prepare: mode=" + mode);
        }
        this.mMode = mode;
        DisplayInfo displayInfo = this.mDisplayManagerInternal.getDisplayInfo(this.mDisplayId);
        if (displayInfo == null) {
            return false;
        }
        this.mDisplayLayerStack = displayInfo.layerStack;
        this.mDisplayWidth = displayInfo.getNaturalWidth();
        this.mDisplayHeight = displayInfo.getNaturalHeight();
        IBinder token = SurfaceControl.getInternalDisplayToken();
        if (token == null) {
            Slog.e(TAG, "Failed to take screenshot because internal display is disconnected");
            return false;
        }
        boolean isWideColor = SurfaceControl.getDynamicDisplayInfo(token).activeColorMode == 9;
        this.mPrepared = true;
        Boolean res = IDisplayManagerServiceLice.Instance().onColorFadePrepare(context, this, displayInfo);
        if (res != null) {
            return res.booleanValue();
        }
        SurfaceControl.ScreenshotHardwareBuffer hardwareBuffer = captureScreen();
        if (hardwareBuffer == null) {
            dismiss();
            return false;
        }
        boolean isProtected = RotationAnimationUtils.hasProtectedContent(hardwareBuffer.getHardwareBuffer());
        if (!createSurfaceControl(hardwareBuffer.containsSecureLayers())) {
            dismiss();
            return false;
        } else if (this.mMode == 2) {
            return true;
        } else {
            if (!createEglContext(isProtected) || !createEglSurface(isProtected, isWideColor) || !setScreenshotTextureAndSetViewport(hardwareBuffer)) {
                dismiss();
                return false;
            } else if (!attachEglContext()) {
                return false;
            } else {
                try {
                    if (!initGLShaders(context) || !initGLBuffers() || checkGlErrors("prepare")) {
                        detachEglContext();
                        dismiss();
                        return false;
                    }
                    detachEglContext();
                    this.mCreatedResources = true;
                    this.mLastWasProtectedContent = isProtected;
                    this.mLastWasWideColor = isWideColor;
                    if (mode == 1) {
                        for (int i = 0; i < 3; i++) {
                            draw(1.0f);
                        }
                    }
                    return true;
                } finally {
                    detachEglContext();
                }
            }
        }
    }

    private String readFile(Context context, int resourceId) {
        try {
            InputStream stream = context.getResources().openRawResource(resourceId);
            return new String(Streams.readFully(new InputStreamReader(stream)));
        } catch (IOException e) {
            Slog.e(TAG, "Unrecognized shader " + Integer.toString(resourceId));
            throw new RuntimeException(e);
        }
    }

    private int loadShader(Context context, int resourceId, int type) {
        String source = IDisplayManagerServiceLice.Instance().onColorFadeLoadShader(context, this.mDisplayId, resourceId, type);
        if (source == null) {
            source = readFile(context, resourceId);
        }
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, 35713, compiled, 0);
        if (compiled[0] == 0) {
            Slog.e(TAG, "Could not compile shader " + shader + ", " + type + ":");
            Slog.e(TAG, GLES20.glGetShaderSource(shader));
            Slog.e(TAG, GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private boolean initGLShaders(Context context) {
        int vshader = loadShader(context, 17825795, 35633);
        int fshader = loadShader(context, 17825794, 35632);
        GLES20.glReleaseShaderCompiler();
        if (vshader == 0 || fshader == 0) {
            return false;
        }
        int glCreateProgram = GLES20.glCreateProgram();
        this.mProgram = glCreateProgram;
        GLES20.glAttachShader(glCreateProgram, vshader);
        GLES20.glAttachShader(this.mProgram, fshader);
        GLES20.glDeleteShader(vshader);
        GLES20.glDeleteShader(fshader);
        GLES20.glLinkProgram(this.mProgram);
        this.mVertexLoc = GLES20.glGetAttribLocation(this.mProgram, "position");
        this.mTexCoordLoc = GLES20.glGetAttribLocation(this.mProgram, "uv");
        this.mProjMatrixLoc = GLES20.glGetUniformLocation(this.mProgram, "proj_matrix");
        this.mTexMatrixLoc = GLES20.glGetUniformLocation(this.mProgram, "tex_matrix");
        this.mOpacityLoc = GLES20.glGetUniformLocation(this.mProgram, "opacity");
        this.mGammaLoc = GLES20.glGetUniformLocation(this.mProgram, "gamma");
        this.mTexUnitLoc = GLES20.glGetUniformLocation(this.mProgram, "texUnit");
        IDisplayManagerServiceLice.Instance().onColorFadeInitGLShaders(context, this.mDisplayId, this.mProgram);
        GLES20.glUseProgram(this.mProgram);
        GLES20.glUniform1i(this.mTexUnitLoc, 0);
        GLES20.glUseProgram(0);
        return true;
    }

    private void destroyGLShaders() {
        GLES20.glDeleteProgram(this.mProgram);
        checkGlErrors("glDeleteProgram");
    }

    private boolean initGLBuffers() {
        Boolean res = IDisplayManagerServiceLice.Instance().onColorFadeInitGLBuffers(this.mDisplayId, this.mProjMatrix, this.mTexNames, this.mGLBuffers);
        if (res != null) {
            return res.booleanValue();
        }
        setQuad(this.mVertexBuffer, 0.0f, 0.0f, this.mDisplayWidth, this.mDisplayHeight);
        GLES20.glBindTexture(36197, this.mTexNames[0]);
        GLES20.glTexParameteri(36197, 10240, 9728);
        GLES20.glTexParameteri(36197, 10241, 9728);
        GLES20.glTexParameteri(36197, 10242, 33071);
        GLES20.glTexParameteri(36197, 10243, 33071);
        GLES20.glBindTexture(36197, 0);
        GLES20.glGenBuffers(2, this.mGLBuffers, 0);
        GLES20.glBindBuffer(34962, this.mGLBuffers[0]);
        GLES20.glBufferData(34962, this.mVertexBuffer.capacity() * 4, this.mVertexBuffer, 35044);
        GLES20.glBindBuffer(34962, this.mGLBuffers[1]);
        GLES20.glBufferData(34962, this.mTexCoordBuffer.capacity() * 4, this.mTexCoordBuffer, 35044);
        GLES20.glBindBuffer(34962, 0);
        return true;
    }

    private void destroyGLBuffers() {
        GLES20.glDeleteBuffers(2, this.mGLBuffers, 0);
        checkGlErrors("glDeleteBuffers");
    }

    private static void setQuad(FloatBuffer vtx, float x, float y, float w, float h) {
        if (DEBUG) {
            Slog.d(TAG, "setQuad: x=" + x + ", y=" + y + ", w=" + w + ", h=" + h);
        }
        vtx.put(0, x);
        vtx.put(1, y);
        vtx.put(2, x);
        vtx.put(3, y + h);
        vtx.put(4, x + w);
        vtx.put(5, y + h);
        vtx.put(6, x + w);
        vtx.put(7, y);
    }

    public void dismissResources() {
        if (DEBUG) {
            Slog.d(TAG, "dismissResources");
        }
        if (this.mCreatedResources) {
            attachEglContext();
            try {
                destroyScreenshotTexture();
                destroyGLShaders();
                destroyGLBuffers();
                destroyEglSurface();
                detachEglContext();
                GLES20.glFlush();
                this.mCreatedResources = false;
            } catch (Throwable th) {
                detachEglContext();
                throw th;
            }
        }
    }

    public void dismiss() {
        if (DEBUG) {
            Slog.d(TAG, "dismiss");
        }
        if (this.mPrepared) {
            IDisplayManagerServiceLice.Instance().onColorFadeDismiss(this.mDisplayId);
            dismissResources();
            destroySurface();
            this.mPrepared = false;
        }
    }

    public boolean draw(float level) {
        if (DEBUG) {
            Slog.d(TAG, "drawFrame: level=" + level);
        }
        if (!this.mPrepared) {
            return false;
        }
        Boolean res = IDisplayManagerServiceLice.Instance().onColorFadeDraw(this.mDisplayId, level);
        if (res != null) {
            return res.booleanValue();
        }
        if (this.mMode == 2) {
            return showSurface(1.0f - level);
        }
        if (!attachEglContext()) {
            return false;
        }
        try {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(16384);
            double one_minus_level = 1.0f - level;
            double cos = Math.cos(3.141592653589793d * one_minus_level);
            double sign = cos < 0.0d ? -1.0d : 1.0d;
            try {
                float opacity = ((float) (-Math.pow(one_minus_level, 2.0d))) + 1.0f;
                float gamma = (float) ((((sign * 0.5d * Math.pow(cos, 2.0d)) + 0.5d) * 0.9d) + 0.1d);
                drawFaded(opacity, 1.0f / gamma);
                if (!checkGlErrors("drawFrame")) {
                    EGL14.eglSwapBuffers(this.mEglDisplay, this.mEglSurface);
                    detachEglContext();
                    return showSurface(1.0f);
                }
                detachEglContext();
                return false;
            } catch (Throwable th) {
                th = th;
                detachEglContext();
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void drawFaded(float opacity, float gamma) {
        if (DEBUG) {
            Slog.d(TAG, "drawFaded: opacity=" + opacity + ", gamma=" + gamma);
        }
        if (IDisplayManagerServiceLice.Instance().onColorFadeDrawFaded(this.mDisplayId, this.mProgram, this.mProjMatrixLoc, this.mProjMatrix, this.mTexMatrixLoc, this.mTexMatrix, this.mOpacityLoc, opacity, this.mGammaLoc, gamma, this.mTexNames, this.mGLBuffers, this.mVertexLoc, this.mTexCoordLoc)) {
            return;
        }
        GLES20.glUseProgram(this.mProgram);
        GLES20.glUniformMatrix4fv(this.mProjMatrixLoc, 1, false, this.mProjMatrix, 0);
        GLES20.glUniformMatrix4fv(this.mTexMatrixLoc, 1, false, this.mTexMatrix, 0);
        GLES20.glUniform1f(this.mOpacityLoc, opacity);
        GLES20.glUniform1f(this.mGammaLoc, gamma);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(36197, this.mTexNames[0]);
        GLES20.glBindBuffer(34962, this.mGLBuffers[0]);
        GLES20.glEnableVertexAttribArray(this.mVertexLoc);
        GLES20.glVertexAttribPointer(this.mVertexLoc, 2, 5126, false, 0, 0);
        GLES20.glBindBuffer(34962, this.mGLBuffers[1]);
        GLES20.glEnableVertexAttribArray(this.mTexCoordLoc);
        GLES20.glVertexAttribPointer(this.mTexCoordLoc, 2, 5126, false, 0, 0);
        GLES20.glDrawArrays(6, 0, 4);
        GLES20.glBindTexture(36197, 0);
        GLES20.glBindBuffer(34962, 0);
    }

    private void ortho(float left, float right, float bottom, float top, float znear, float zfar) {
        float[] fArr = this.mProjMatrix;
        fArr[0] = 2.0f / (right - left);
        fArr[1] = 0.0f;
        fArr[2] = 0.0f;
        fArr[3] = 0.0f;
        fArr[4] = 0.0f;
        fArr[5] = 2.0f / (top - bottom);
        fArr[6] = 0.0f;
        fArr[7] = 0.0f;
        fArr[8] = 0.0f;
        fArr[9] = 0.0f;
        fArr[10] = (-2.0f) / (zfar - znear);
        fArr[11] = 0.0f;
        fArr[12] = (-(right + left)) / (right - left);
        fArr[13] = (-(top + bottom)) / (top - bottom);
        fArr[14] = (-(zfar + znear)) / (zfar - znear);
        fArr[15] = 1.0f;
    }

    private boolean setScreenshotTextureAndSetViewport(SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer) {
        if (attachEglContext()) {
            try {
                if (!this.mTexNamesGenerated) {
                    GLES20.glGenTextures(1, this.mTexNames, 0);
                    if (checkGlErrors("glGenTextures")) {
                        return false;
                    }
                    this.mTexNamesGenerated = true;
                }
                SurfaceTexture st = new SurfaceTexture(this.mTexNames[0]);
                Surface s = new Surface(st);
                s.attachAndQueueBufferWithColorSpace(screenshotBuffer.getHardwareBuffer(), screenshotBuffer.getColorSpace());
                st.updateTexImage();
                st.getTransformMatrix(this.mTexMatrix);
                s.release();
                st.release();
                this.mTexCoordBuffer.put(0, 0.0f);
                this.mTexCoordBuffer.put(1, 0.0f);
                this.mTexCoordBuffer.put(2, 0.0f);
                this.mTexCoordBuffer.put(3, 1.0f);
                this.mTexCoordBuffer.put(4, 1.0f);
                this.mTexCoordBuffer.put(5, 1.0f);
                this.mTexCoordBuffer.put(6, 1.0f);
                this.mTexCoordBuffer.put(7, 0.0f);
                GLES20.glViewport(0, 0, this.mDisplayWidth, this.mDisplayHeight);
                ortho(0.0f, this.mDisplayWidth, 0.0f, this.mDisplayHeight, -1.0f, 1.0f);
                return true;
            } finally {
                detachEglContext();
            }
        }
        return false;
    }

    private void destroyScreenshotTexture() {
        if (this.mTexNamesGenerated) {
            this.mTexNamesGenerated = false;
            GLES20.glDeleteTextures(1, this.mTexNames, 0);
            checkGlErrors("glDeleteTextures");
        }
    }

    private SurfaceControl.ScreenshotHardwareBuffer captureScreen() {
        SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer = IDisplayManagerServiceLice.Instance().onColorFadeCaptureScreen(this.mDisplayId, this.mDisplayManagerInternal);
        if (screenshotBuffer == null) {
            screenshotBuffer = this.mDisplayManagerInternal.systemScreenshot(this.mDisplayId);
        }
        if (screenshotBuffer == null) {
            Slog.e(TAG, "Failed to take screenshot. Buffer is null");
            return null;
        }
        return screenshotBuffer;
    }

    private boolean createSurfaceControl(boolean isSecure) {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl != null) {
            this.mTransaction.setSecure(surfaceControl, isSecure).apply();
            return true;
        }
        try {
            SurfaceControl.Builder builder = new SurfaceControl.Builder().setName(TAG).setSecure(isSecure).setCallsite("ColorFade.createSurface");
            if (this.mMode == 2) {
                builder.setColorLayer();
            } else {
                builder.setContainerLayer();
            }
            SurfaceControl build = builder.build();
            this.mSurfaceControl = build;
            this.mTransaction.setLayerStack(build, this.mDisplayLayerStack);
            this.mTransaction.setWindowCrop(this.mSurfaceControl, this.mDisplayWidth, this.mDisplayHeight);
            NaturalSurfaceLayout naturalSurfaceLayout = new NaturalSurfaceLayout(this.mDisplayManagerInternal, this.mDisplayId, this.mSurfaceControl);
            this.mSurfaceLayout = naturalSurfaceLayout;
            naturalSurfaceLayout.onDisplayTransaction(this.mTransaction);
            this.mTransaction.apply();
            if (this.mMode != 2) {
                SurfaceControl.Builder b = new SurfaceControl.Builder().setName("ColorFade BLAST").setParent(this.mSurfaceControl).setHidden(false).setSecure(isSecure).setBLASTLayer();
                this.mBLASTSurfaceControl = b.build();
                BLASTBufferQueue bLASTBufferQueue = new BLASTBufferQueue(TAG, this.mBLASTSurfaceControl, this.mDisplayWidth, this.mDisplayHeight, -3);
                this.mBLASTBufferQueue = bLASTBufferQueue;
                this.mSurface = bLASTBufferQueue.createSurface();
            }
            return true;
        } catch (Surface.OutOfResourcesException ex) {
            Slog.e(TAG, "Unable to create surface.", ex);
            return false;
        }
    }

    private boolean createEglContext(boolean isProtected) {
        if (this.mEglDisplay == null) {
            EGLDisplay eglGetDisplay = EGL14.eglGetDisplay(0);
            this.mEglDisplay = eglGetDisplay;
            if (eglGetDisplay == EGL14.EGL_NO_DISPLAY) {
                logEglError("eglGetDisplay");
                return false;
            }
            int[] version = new int[2];
            if (!EGL14.eglInitialize(this.mEglDisplay, version, 0, version, 1)) {
                this.mEglDisplay = null;
                logEglError("eglInitialize");
                return false;
            }
        }
        if (this.mEglConfig == null) {
            int[] eglConfigAttribList = {12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 8, 12344};
            int[] numEglConfigs = new int[1];
            EGLConfig[] eglConfigs = new EGLConfig[1];
            if (!EGL14.eglChooseConfig(this.mEglDisplay, eglConfigAttribList, 0, eglConfigs, 0, eglConfigs.length, numEglConfigs, 0)) {
                logEglError("eglChooseConfig");
                return false;
            } else if (numEglConfigs[0] <= 0) {
                Slog.e(TAG, "no valid config found");
                return false;
            } else {
                this.mEglConfig = eglConfigs[0];
            }
        }
        EGLContext eGLContext = this.mEglContext;
        if (eGLContext != null && isProtected != this.mLastWasProtectedContent) {
            EGL14.eglDestroyContext(this.mEglDisplay, eGLContext);
            this.mEglContext = null;
        }
        if (this.mEglContext == null) {
            int[] eglContextAttribList = {12440, 2, 12344, 12344, 12344};
            if (isProtected) {
                eglContextAttribList[2] = EGL_PROTECTED_CONTENT_EXT;
                eglContextAttribList[3] = 1;
            }
            EGLContext eglCreateContext = EGL14.eglCreateContext(this.mEglDisplay, this.mEglConfig, EGL14.EGL_NO_CONTEXT, eglContextAttribList, 0);
            this.mEglContext = eglCreateContext;
            if (eglCreateContext == null) {
                logEglError("eglCreateContext");
                return false;
            }
        }
        return true;
    }

    public void destroy() {
        destroyEglSurface();
        EGLContext eGLContext = this.mEglContext;
        if (eGLContext != null) {
            EGL14.eglDestroyContext(this.mEglDisplay, eGLContext);
            this.mEglContext = null;
        }
        EGLDisplay eGLDisplay = this.mEglDisplay;
        if (eGLDisplay != null) {
            EGL14.eglTerminate(eGLDisplay);
            this.mEglDisplay = null;
        }
    }

    private boolean createEglSurface(boolean isProtected, boolean isWideColor) {
        boolean didContentAttributesChange = (isProtected == this.mLastWasProtectedContent && isWideColor == this.mLastWasWideColor) ? false : true;
        EGLSurface eGLSurface = this.mEglSurface;
        if (eGLSurface != null && didContentAttributesChange) {
            EGL14.eglDestroySurface(this.mEglDisplay, eGLSurface);
            this.mEglSurface = null;
        }
        if (this.mEglSurface == null) {
            int[] eglSurfaceAttribList = {12344, 12344, 12344, 12344, 12344};
            int index = 0;
            if (isWideColor) {
                int index2 = 0 + 1;
                eglSurfaceAttribList[0] = EGL_GL_COLORSPACE_KHR;
                index = index2 + 1;
                eglSurfaceAttribList[index2] = EGL_GL_COLORSPACE_DISPLAY_P3_PASSTHROUGH_EXT;
            }
            if (isProtected) {
                eglSurfaceAttribList[index] = EGL_PROTECTED_CONTENT_EXT;
                eglSurfaceAttribList[index + 1] = 1;
            }
            EGLSurface eglCreateWindowSurface = EGL14.eglCreateWindowSurface(this.mEglDisplay, this.mEglConfig, this.mSurface, eglSurfaceAttribList, 0);
            this.mEglSurface = eglCreateWindowSurface;
            if (eglCreateWindowSurface == null) {
                logEglError("eglCreateWindowSurface");
                return false;
            }
        }
        return true;
    }

    private void destroyEglSurface() {
        EGLSurface eGLSurface = this.mEglSurface;
        if (eGLSurface != null) {
            if (!EGL14.eglDestroySurface(this.mEglDisplay, eGLSurface)) {
                logEglError("eglDestroySurface");
            }
            this.mEglSurface = null;
        }
    }

    private void destroySurface() {
        if (this.mSurfaceControl != null) {
            this.mSurfaceLayout.dispose();
            this.mSurfaceLayout = null;
            this.mTransaction.remove(this.mSurfaceControl).apply();
            Surface surface = this.mSurface;
            if (surface != null) {
                surface.release();
                this.mSurface = null;
            }
            SurfaceControl surfaceControl = this.mBLASTSurfaceControl;
            if (surfaceControl != null) {
                surfaceControl.release();
                this.mBLASTSurfaceControl = null;
                this.mBLASTBufferQueue.destroy();
                this.mBLASTBufferQueue = null;
            }
            this.mSurfaceControl = null;
            this.mSurfaceVisible = false;
            this.mSurfaceAlpha = 0.0f;
        }
    }

    private boolean showSurface(float alpha) {
        if (!this.mSurfaceVisible || this.mSurfaceAlpha != alpha) {
            this.mTransaction.setLayer(this.mSurfaceControl, 1073741825).setAlpha(this.mSurfaceControl, alpha).show(this.mSurfaceControl);
            IDisplayManagerServiceLice.Instance().onColorFadeShowSurface(this.mDisplayId, this.mTransaction, this.mSurfaceControl, 1073741825, alpha);
            this.mTransaction.apply();
            this.mSurfaceVisible = true;
            this.mSurfaceAlpha = alpha;
        }
        return true;
    }

    private boolean attachEglContext() {
        EGLSurface eGLSurface = this.mEglSurface;
        if (eGLSurface == null) {
            return false;
        }
        if (!EGL14.eglMakeCurrent(this.mEglDisplay, eGLSurface, eGLSurface, this.mEglContext)) {
            logEglError("eglMakeCurrent");
            return false;
        }
        return true;
    }

    private void detachEglContext() {
        EGLDisplay eGLDisplay = this.mEglDisplay;
        if (eGLDisplay != null) {
            EGL14.eglMakeCurrent(eGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        }
    }

    private static FloatBuffer createNativeFloatBuffer(int size) {
        ByteBuffer bb = ByteBuffer.allocateDirect(size * 4);
        bb.order(ByteOrder.nativeOrder());
        return bb.asFloatBuffer();
    }

    private static void logEglError(String func) {
        Slog.e(TAG, func + " failed: error " + EGL14.eglGetError(), new Throwable());
    }

    private static boolean checkGlErrors(String func) {
        return checkGlErrors(func, true);
    }

    private static boolean checkGlErrors(String func, boolean log) {
        boolean hadError = false;
        while (true) {
            int error = GLES20.glGetError();
            if (error != 0) {
                if (log) {
                    Slog.e(TAG, func + " failed: error " + error, new Throwable());
                }
                hadError = true;
            } else {
                return hadError;
            }
        }
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Color Fade State:");
        pw.println("  mPrepared=" + this.mPrepared);
        pw.println("  mMode=" + this.mMode);
        pw.println("  mDisplayLayerStack=" + this.mDisplayLayerStack);
        pw.println("  mDisplayWidth=" + this.mDisplayWidth);
        pw.println("  mDisplayHeight=" + this.mDisplayHeight);
        pw.println("  mSurfaceVisible=" + this.mSurfaceVisible);
        pw.println("  mSurfaceAlpha=" + this.mSurfaceAlpha);
        IDisplayManagerServiceLice.Instance().onColorFadeDump(this.mDisplayId, pw);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class NaturalSurfaceLayout implements DisplayManagerInternal.DisplayTransactionListener {
        private final int mDisplayId;
        private final DisplayManagerInternal mDisplayManagerInternal;
        private SurfaceControl mSurfaceControl;

        public NaturalSurfaceLayout(DisplayManagerInternal displayManagerInternal, int displayId, SurfaceControl surfaceControl) {
            this.mDisplayManagerInternal = displayManagerInternal;
            this.mDisplayId = displayId;
            this.mSurfaceControl = surfaceControl;
            displayManagerInternal.registerDisplayTransactionListener(this);
        }

        public void dispose() {
            synchronized (this) {
                this.mSurfaceControl = null;
            }
            this.mDisplayManagerInternal.unregisterDisplayTransactionListener(this);
        }

        public void onDisplayTransaction(SurfaceControl.Transaction t) {
            synchronized (this) {
                if (this.mSurfaceControl == null) {
                    return;
                }
                DisplayInfo displayInfo = this.mDisplayManagerInternal.getDisplayInfo(this.mDisplayId);
                if (displayInfo == null) {
                    return;
                }
                switch (displayInfo.rotation) {
                    case 0:
                        t.setPosition(this.mSurfaceControl, 0.0f, 0.0f);
                        t.setMatrix(this.mSurfaceControl, 1.0f, 0.0f, 0.0f, 1.0f);
                        break;
                    case 1:
                        t.setPosition(this.mSurfaceControl, 0.0f, displayInfo.logicalHeight);
                        t.setMatrix(this.mSurfaceControl, 0.0f, -1.0f, 1.0f, 0.0f);
                        break;
                    case 2:
                        t.setPosition(this.mSurfaceControl, displayInfo.logicalWidth, displayInfo.logicalHeight);
                        t.setMatrix(this.mSurfaceControl, -1.0f, 0.0f, 0.0f, -1.0f);
                        break;
                    case 3:
                        t.setPosition(this.mSurfaceControl, displayInfo.logicalWidth, 0.0f);
                        t.setMatrix(this.mSurfaceControl, 0.0f, 1.0f, -1.0f, 0.0f);
                        break;
                }
            }
        }
    }
}
