package com.android.server.display;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.DisplayModeDirector;
import com.android.server.display.OverlayDisplayWindow;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class OverlayDisplayAdapter extends DisplayAdapter {
    static final boolean DEBUG = false;
    private static final String DISPLAY_SPLITTER = ";";
    private static final String FLAG_SPLITTER = ",";
    private static final int MAX_HEIGHT = 4096;
    private static final int MAX_WIDTH = 4096;
    private static final int MIN_HEIGHT = 100;
    private static final int MIN_WIDTH = 100;
    private static final String MODE_SPLITTER = "\\|";
    private static final String OVERLAY_DISPLAY_FLAG_OWN_CONTENT_ONLY = "own_content_only";
    private static final String OVERLAY_DISPLAY_FLAG_SECURE = "secure";
    private static final String OVERLAY_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = "should_show_system_decorations";
    static final String TAG = "OverlayDisplayAdapter";
    private static final String UNIQUE_ID_PREFIX = "overlay:";
    private String mCurrentOverlaySetting;
    private final ArrayList<OverlayDisplayHandle> mOverlays;
    private final Handler mUiHandler;
    private static final Pattern DISPLAY_PATTERN = Pattern.compile("([^,]+)(,[,_a-z]+)*");
    private static final Pattern MODE_PATTERN = Pattern.compile("(\\d+)x(\\d+)/(\\d+)");

    public OverlayDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener, Handler uiHandler) {
        super(syncRoot, context, handler, listener, TAG);
        this.mOverlays = new ArrayList<>();
        this.mCurrentOverlaySetting = "";
        this.mUiHandler = uiHandler;
    }

    @Override // com.android.server.display.DisplayAdapter
    public void dumpLocked(PrintWriter pw) {
        super.dumpLocked(pw);
        pw.println("mCurrentOverlaySetting=" + this.mCurrentOverlaySetting);
        pw.println("mOverlays: size=" + this.mOverlays.size());
        Iterator<OverlayDisplayHandle> it = this.mOverlays.iterator();
        while (it.hasNext()) {
            OverlayDisplayHandle overlay = it.next();
            overlay.dumpLocked(pw);
        }
    }

    @Override // com.android.server.display.DisplayAdapter
    public void registerLocked() {
        super.registerLocked();
        getHandler().post(new Runnable() { // from class: com.android.server.display.OverlayDisplayAdapter.1
            @Override // java.lang.Runnable
            public void run() {
                OverlayDisplayAdapter.this.getContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor("overlay_display_devices"), true, new ContentObserver(OverlayDisplayAdapter.this.getHandler()) { // from class: com.android.server.display.OverlayDisplayAdapter.1.1
                    @Override // android.database.ContentObserver
                    public void onChange(boolean selfChange) {
                        OverlayDisplayAdapter.this.updateOverlayDisplayDevices();
                    }
                });
                OverlayDisplayAdapter.this.updateOverlayDisplayDevices();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateOverlayDisplayDevices() {
        synchronized (getSyncRoot()) {
            updateOverlayDisplayDevicesLocked();
        }
    }

    private void updateOverlayDisplayDevicesLocked() {
        String value;
        String modeString;
        Matcher displayMatcher;
        OverlayDisplayAdapter overlayDisplayAdapter = this;
        String value2 = Settings.Global.getString(getContext().getContentResolver(), "overlay_display_devices");
        if (value2 != null) {
            value = value2;
        } else {
            value = "";
        }
        if (value.equals(overlayDisplayAdapter.mCurrentOverlaySetting)) {
            return;
        }
        overlayDisplayAdapter.mCurrentOverlaySetting = value;
        if (!overlayDisplayAdapter.mOverlays.isEmpty()) {
            Slog.i(TAG, "Dismissing all overlay display devices.");
            Iterator<OverlayDisplayHandle> it = overlayDisplayAdapter.mOverlays.iterator();
            while (it.hasNext()) {
                OverlayDisplayHandle overlay = it.next();
                overlay.dismissLocked();
            }
            overlayDisplayAdapter.mOverlays.clear();
        }
        String[] split = value.split(DISPLAY_SPLITTER);
        int length = split.length;
        int count = 0;
        int i = 0;
        while (i < length) {
            String part = split[i];
            Matcher displayMatcher2 = DISPLAY_PATTERN.matcher(part);
            if (displayMatcher2.matches()) {
                if (count >= 4) {
                    Slog.w(TAG, "Too many overlay display devices specified: " + value);
                    return;
                }
                String modeString2 = displayMatcher2.group(1);
                String flagString = displayMatcher2.group(2);
                ArrayList<OverlayMode> modes = new ArrayList<>();
                String[] split2 = modeString2.split(MODE_SPLITTER);
                int length2 = split2.length;
                int i2 = 0;
                while (i2 < length2) {
                    String mode = split2[i2];
                    String[] strArr = split2;
                    Matcher modeMatcher = MODE_PATTERN.matcher(mode);
                    if (modeMatcher.matches()) {
                        modeString = modeString2;
                        try {
                            int width = Integer.parseInt(modeMatcher.group(1), 10);
                            displayMatcher = displayMatcher2;
                            try {
                                int height = Integer.parseInt(modeMatcher.group(2), 10);
                                try {
                                    int densityDpi = Integer.parseInt(modeMatcher.group(3), 10);
                                    if (width >= 100 && width <= 4096 && height >= 100 && height <= 4096 && densityDpi >= 120 && densityDpi <= 640) {
                                        modes.add(new OverlayMode(width, height, densityDpi));
                                    } else {
                                        Slog.w(TAG, "Ignoring out-of-range overlay display mode: " + mode);
                                    }
                                } catch (NumberFormatException e) {
                                }
                            } catch (NumberFormatException e2) {
                            }
                        } catch (NumberFormatException e3) {
                            displayMatcher = displayMatcher2;
                        }
                    } else {
                        modeString = modeString2;
                        displayMatcher = displayMatcher2;
                        mode.isEmpty();
                    }
                    i2++;
                    displayMatcher2 = displayMatcher;
                    split2 = strArr;
                    modeString2 = modeString;
                }
                if (!modes.isEmpty()) {
                    int count2 = count + 1;
                    String name = getContext().getResources().getString(17040164, Integer.valueOf(count2));
                    int gravity = chooseOverlayGravity(count2);
                    OverlayFlags flags = OverlayFlags.parseFlags(flagString);
                    Slog.i(TAG, "Showing overlay display device #" + count2 + ": name=" + name + ", modes=" + Arrays.toString(modes.toArray()) + ", flags=" + flags);
                    overlayDisplayAdapter.mOverlays.add(new OverlayDisplayHandle(name, modes, gravity, flags, count2));
                    count = count2;
                    i++;
                    overlayDisplayAdapter = this;
                }
            }
            Slog.w(TAG, "Malformed overlay display devices setting: " + value);
            i++;
            overlayDisplayAdapter = this;
        }
    }

    private static int chooseOverlayGravity(int overlayNumber) {
        switch (overlayNumber) {
            case 1:
                return 51;
            case 2:
                return 85;
            case 3:
                return 53;
            default:
                return 83;
        }
    }

    /* loaded from: classes.dex */
    private abstract class OverlayDisplayDevice extends DisplayDevice {
        private int mActiveMode;
        private final int mDefaultMode;
        private final long mDisplayPresentationDeadlineNanos;
        private final OverlayFlags mFlags;
        private DisplayDeviceInfo mInfo;
        private final Display.Mode[] mModes;
        private final String mName;
        private final List<OverlayMode> mRawModes;
        private final float mRefreshRate;
        private int mState;
        private Surface mSurface;
        private SurfaceTexture mSurfaceTexture;

        public abstract void onModeChangedLocked(int i);

        OverlayDisplayDevice(IBinder displayToken, String name, List<OverlayMode> modes, int activeMode, int defaultMode, float refreshRate, long presentationDeadlineNanos, OverlayFlags flags, int state, SurfaceTexture surfaceTexture, int number) {
            super(OverlayDisplayAdapter.this, displayToken, OverlayDisplayAdapter.UNIQUE_ID_PREFIX + number, OverlayDisplayAdapter.this.getContext());
            this.mName = name;
            this.mRefreshRate = refreshRate;
            this.mDisplayPresentationDeadlineNanos = presentationDeadlineNanos;
            this.mFlags = flags;
            this.mState = state;
            this.mSurfaceTexture = surfaceTexture;
            this.mRawModes = modes;
            this.mModes = new Display.Mode[modes.size()];
            for (int i = 0; i < modes.size(); i++) {
                OverlayMode mode = modes.get(i);
                this.mModes[i] = DisplayAdapter.createMode(mode.mWidth, mode.mHeight, refreshRate);
            }
            this.mActiveMode = activeMode;
            this.mDefaultMode = defaultMode;
        }

        public void destroyLocked() {
            this.mSurfaceTexture = null;
            Surface surface = this.mSurface;
            if (surface != null) {
                surface.release();
                this.mSurface = null;
            }
            SurfaceControl.destroyDisplay(getDisplayTokenLocked());
        }

        @Override // com.android.server.display.DisplayDevice
        public boolean hasStableUniqueId() {
            return false;
        }

        @Override // com.android.server.display.DisplayDevice
        public void performTraversalLocked(SurfaceControl.Transaction t) {
            if (this.mSurfaceTexture != null) {
                if (this.mSurface == null) {
                    this.mSurface = new Surface(this.mSurfaceTexture);
                }
                setSurfaceLocked(t, this.mSurface);
            }
        }

        public void setStateLocked(int state) {
            this.mState = state;
            this.mInfo = null;
        }

        @Override // com.android.server.display.DisplayDevice
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                Display.Mode[] modeArr = this.mModes;
                int i = this.mActiveMode;
                Display.Mode mode = modeArr[i];
                OverlayMode rawMode = this.mRawModes.get(i);
                DisplayDeviceInfo displayDeviceInfo = new DisplayDeviceInfo();
                this.mInfo = displayDeviceInfo;
                displayDeviceInfo.name = this.mName;
                this.mInfo.uniqueId = getUniqueId();
                this.mInfo.width = mode.getPhysicalWidth();
                this.mInfo.height = mode.getPhysicalHeight();
                this.mInfo.modeId = mode.getModeId();
                this.mInfo.defaultModeId = this.mModes[0].getModeId();
                this.mInfo.supportedModes = this.mModes;
                this.mInfo.densityDpi = rawMode.mDensityDpi;
                this.mInfo.xDpi = rawMode.mDensityDpi;
                this.mInfo.yDpi = rawMode.mDensityDpi;
                this.mInfo.presentationDeadlineNanos = this.mDisplayPresentationDeadlineNanos + (1000000000 / ((int) this.mRefreshRate));
                this.mInfo.flags = 64;
                if (this.mFlags.mSecure) {
                    this.mInfo.flags |= 4;
                }
                if (this.mFlags.mOwnContentOnly) {
                    this.mInfo.flags |= 128;
                }
                if (this.mFlags.mShouldShowSystemDecorations) {
                    this.mInfo.flags |= 4096;
                }
                this.mInfo.type = 4;
                this.mInfo.touch = 3;
                this.mInfo.state = this.mState;
                this.mInfo.flags |= 8192;
            }
            return this.mInfo;
        }

        @Override // com.android.server.display.DisplayDevice
        public void setDesiredDisplayModeSpecsLocked(DisplayModeDirector.DesiredDisplayModeSpecs displayModeSpecs) {
            int id = displayModeSpecs.baseModeId;
            int index = -1;
            if (id == 0) {
                index = 0;
            } else {
                int i = 0;
                while (true) {
                    Display.Mode[] modeArr = this.mModes;
                    if (i >= modeArr.length) {
                        break;
                    } else if (modeArr[i].getModeId() != id) {
                        i++;
                    } else {
                        index = i;
                        break;
                    }
                }
            }
            if (index == -1) {
                Slog.w(OverlayDisplayAdapter.TAG, "Unable to locate mode " + id + ", reverting to default.");
                index = this.mDefaultMode;
            }
            if (this.mActiveMode == index) {
                return;
            }
            this.mActiveMode = index;
            this.mInfo = null;
            OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
            onModeChangedLocked(index);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class OverlayDisplayHandle implements OverlayDisplayWindow.Listener {
        private static final int DEFAULT_MODE_INDEX = 0;
        private OverlayDisplayDevice mDevice;
        private final OverlayFlags mFlags;
        private final int mGravity;
        private final List<OverlayMode> mModes;
        private final String mName;
        private final int mNumber;
        private OverlayDisplayWindow mWindow;
        private final Runnable mShowRunnable = new Runnable() { // from class: com.android.server.display.OverlayDisplayAdapter.OverlayDisplayHandle.2
            @Override // java.lang.Runnable
            public void run() {
                OverlayMode mode = (OverlayMode) OverlayDisplayHandle.this.mModes.get(OverlayDisplayHandle.this.mActiveMode);
                OverlayDisplayWindow window = new OverlayDisplayWindow(OverlayDisplayAdapter.this.getContext(), OverlayDisplayHandle.this.mName, mode.mWidth, mode.mHeight, mode.mDensityDpi, OverlayDisplayHandle.this.mGravity, OverlayDisplayHandle.this.mFlags.mSecure, OverlayDisplayHandle.this);
                window.show();
                synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                    OverlayDisplayHandle.this.mWindow = window;
                }
            }
        };
        private final Runnable mDismissRunnable = new Runnable() { // from class: com.android.server.display.OverlayDisplayAdapter.OverlayDisplayHandle.3
            @Override // java.lang.Runnable
            public void run() {
                OverlayDisplayWindow window;
                synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                    window = OverlayDisplayHandle.this.mWindow;
                    OverlayDisplayHandle.this.mWindow = null;
                }
                if (window != null) {
                    window.dismiss();
                }
            }
        };
        private final Runnable mResizeRunnable = new Runnable() { // from class: com.android.server.display.OverlayDisplayAdapter.OverlayDisplayHandle.4
            @Override // java.lang.Runnable
            public void run() {
                synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                    if (OverlayDisplayHandle.this.mWindow == null) {
                        return;
                    }
                    OverlayMode mode = (OverlayMode) OverlayDisplayHandle.this.mModes.get(OverlayDisplayHandle.this.mActiveMode);
                    OverlayDisplayWindow window = OverlayDisplayHandle.this.mWindow;
                    window.resize(mode.mWidth, mode.mHeight, mode.mDensityDpi);
                }
            }
        };
        private int mActiveMode = 0;

        OverlayDisplayHandle(String name, List<OverlayMode> modes, int gravity, OverlayFlags flags, int number) {
            this.mName = name;
            this.mModes = modes;
            this.mGravity = gravity;
            this.mFlags = flags;
            this.mNumber = number;
            showLocked();
        }

        private void showLocked() {
            OverlayDisplayAdapter.this.mUiHandler.post(this.mShowRunnable);
        }

        public void dismissLocked() {
            OverlayDisplayAdapter.this.mUiHandler.removeCallbacks(this.mShowRunnable);
            OverlayDisplayAdapter.this.mUiHandler.post(this.mDismissRunnable);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onActiveModeChangedLocked(int index) {
            OverlayDisplayAdapter.this.mUiHandler.removeCallbacks(this.mResizeRunnable);
            this.mActiveMode = index;
            if (this.mWindow != null) {
                OverlayDisplayAdapter.this.mUiHandler.post(this.mResizeRunnable);
            }
        }

        @Override // com.android.server.display.OverlayDisplayWindow.Listener
        public void onWindowCreated(SurfaceTexture surfaceTexture, float refreshRate, long presentationDeadlineNanos, int state) {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                IBinder displayToken = SurfaceControl.createDisplay(this.mName, this.mFlags.mSecure);
                OverlayDisplayDevice overlayDisplayDevice = new OverlayDisplayDevice(displayToken, this.mName, this.mModes, this.mActiveMode, 0, refreshRate, presentationDeadlineNanos, this.mFlags, state, surfaceTexture, this.mNumber) { // from class: com.android.server.display.OverlayDisplayAdapter.OverlayDisplayHandle.1
                    {
                        OverlayDisplayAdapter overlayDisplayAdapter = OverlayDisplayAdapter.this;
                    }

                    @Override // com.android.server.display.OverlayDisplayAdapter.OverlayDisplayDevice
                    public void onModeChangedLocked(int index) {
                        OverlayDisplayHandle.this.onActiveModeChangedLocked(index);
                    }
                };
                this.mDevice = overlayDisplayDevice;
                OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(overlayDisplayDevice, 1);
            }
        }

        @Override // com.android.server.display.OverlayDisplayWindow.Listener
        public void onWindowDestroyed() {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                OverlayDisplayDevice overlayDisplayDevice = this.mDevice;
                if (overlayDisplayDevice != null) {
                    overlayDisplayDevice.destroyLocked();
                    OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 3);
                }
            }
        }

        @Override // com.android.server.display.OverlayDisplayWindow.Listener
        public void onStateChanged(int state) {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                OverlayDisplayDevice overlayDisplayDevice = this.mDevice;
                if (overlayDisplayDevice != null) {
                    overlayDisplayDevice.setStateLocked(state);
                    OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 2);
                }
            }
        }

        public void dumpLocked(PrintWriter pw) {
            pw.println("  " + this.mName + ":");
            pw.println("    mModes=" + Arrays.toString(this.mModes.toArray()));
            pw.println("    mActiveMode=" + this.mActiveMode);
            pw.println("    mGravity=" + this.mGravity);
            pw.println("    mFlags=" + this.mFlags);
            pw.println("    mNumber=" + this.mNumber);
            if (this.mWindow != null) {
                IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "    ");
                ipw.increaseIndent();
                DumpUtils.dumpAsync(OverlayDisplayAdapter.this.mUiHandler, this.mWindow, ipw, "", 200L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class OverlayMode {
        final int mDensityDpi;
        final int mHeight;
        final int mWidth;

        OverlayMode(int width, int height, int densityDpi) {
            this.mWidth = width;
            this.mHeight = height;
            this.mDensityDpi = densityDpi;
        }

        public String toString() {
            return "{width=" + this.mWidth + ", height=" + this.mHeight + ", densityDpi=" + this.mDensityDpi + "}";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class OverlayFlags {
        final boolean mOwnContentOnly;
        final boolean mSecure;
        final boolean mShouldShowSystemDecorations;

        OverlayFlags(boolean secure, boolean ownContentOnly, boolean shouldShowSystemDecorations) {
            this.mSecure = secure;
            this.mOwnContentOnly = ownContentOnly;
            this.mShouldShowSystemDecorations = shouldShowSystemDecorations;
        }

        static OverlayFlags parseFlags(String flagString) {
            String[] split;
            if (TextUtils.isEmpty(flagString)) {
                return new OverlayFlags(false, false, false);
            }
            boolean secure = false;
            boolean ownContentOnly = false;
            boolean shouldShowSystemDecorations = false;
            for (String flag : flagString.split(OverlayDisplayAdapter.FLAG_SPLITTER)) {
                if (OverlayDisplayAdapter.OVERLAY_DISPLAY_FLAG_SECURE.equals(flag)) {
                    secure = true;
                }
                if (OverlayDisplayAdapter.OVERLAY_DISPLAY_FLAG_OWN_CONTENT_ONLY.equals(flag)) {
                    ownContentOnly = true;
                }
                if (OverlayDisplayAdapter.OVERLAY_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS.equals(flag)) {
                    shouldShowSystemDecorations = true;
                }
            }
            return new OverlayFlags(secure, ownContentOnly, shouldShowSystemDecorations);
        }

        public String toString() {
            return "{secure=" + this.mSecure + ", ownContentOnly=" + this.mOwnContentOnly + ", shouldShowSystemDecorations=" + this.mShouldShowSystemDecorations + "}";
        }
    }
}
