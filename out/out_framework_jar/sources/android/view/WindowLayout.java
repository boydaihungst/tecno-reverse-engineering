package android.view;

import android.app.WindowConfiguration;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioSystem;
import android.os.SystemProperties;
import android.util.Log;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.window.ClientWindowFrames;
/* loaded from: classes3.dex */
public class WindowLayout {
    private static final boolean DEBUG = false;
    static final int MAX_X = 100000;
    static final int MAX_Y = 100000;
    static final int MIN_X = -100000;
    static final int MIN_Y = -100000;
    private static final String TAG = WindowLayout.class.getSimpleName();
    private static final boolean TRAN_USER_ROOT_SUPPORT = "1".equals(SystemProperties.get("persist.sys.adb.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS));
    public static final int UNSPECIFIED_LENGTH = -1;
    private final Rect mTempDisplayCutoutSafeExceptMaybeBarsRect = new Rect();
    private final Rect mTempRect = new Rect();

    public void computeFrames(WindowManager.LayoutParams attrs, InsetsState state, Rect displayCutoutSafe, Rect windowBounds, int windowingMode, int requestedWidth, int requestedHeight, InsetsVisibilities requestedVisibilities, Rect attachedWindowFrame, float compatScale, ClientWindowFrames outFrames) {
        computeFrames(attrs, state, displayCutoutSafe, windowBounds, windowingMode, requestedWidth, requestedHeight, requestedVisibilities, attachedWindowFrame, compatScale, outFrames, false);
    }

    /* JADX WARN: Code restructure failed: missing block: B:171:0x02b9, code lost:
        r5 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:172:0x02be, code lost:
        if (r45.type == 1) goto L131;
     */
    /* JADX WARN: Code restructure failed: missing block: B:173:0x02c0, code lost:
        if (r4 != false) goto L131;
     */
    /* JADX WARN: Code restructure failed: missing block: B:175:0x02c3, code lost:
        r5 = false;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void computeFrames(WindowManager.LayoutParams attrs, InsetsState state, Rect displayCutoutSafe, Rect windowBounds, int windowingMode, int requestedWidth, int requestedHeight, InsetsVisibilities requestedVisibilities, Rect attachedWindowFrame, float compatScale, ClientWindowFrames outFrames, boolean isThunderBackWin) {
        Rect outFrame;
        int left;
        int type;
        int type2;
        int type3;
        int w;
        int h;
        int w2;
        float y;
        float x;
        Rect outFrame2;
        int i;
        String str;
        int w3;
        InsetsSource navSource;
        InsetsSource source;
        int type4 = attrs.type;
        int fl = attrs.flags;
        int pfl = attrs.privateFlags;
        boolean layoutInScreen = (fl & 256) == 256;
        Rect outDisplayFrame = outFrames.displayFrame;
        Rect outParentFrame = outFrames.parentFrame;
        Rect outFrame3 = outFrames.frame;
        Insets insets = state.calculateInsets(windowBounds, attrs.getFitInsetsTypes(), attrs.isFitInsetsIgnoringVisibility());
        int sides = attrs.getFitInsetsSides();
        if ((sides & 1) != 0) {
            outFrame = outFrame3;
            left = insets.left;
        } else {
            outFrame = outFrame3;
            left = 0;
        }
        int top = (sides & 2) != 0 ? insets.top : 0;
        if ((sides & 4) != 0) {
            type = type4;
            type2 = insets.right;
        } else {
            type = type4;
            type2 = 0;
        }
        int sides2 = (sides & 8) != 0 ? insets.bottom : 0;
        int i2 = windowBounds.left + left;
        int left2 = windowBounds.top;
        int i3 = left2 + top;
        int top2 = windowBounds.right;
        int i4 = top2 - type2;
        int right = windowBounds.bottom;
        outDisplayFrame.set(i2, i3, i4, right - sides2);
        if (attachedWindowFrame == null) {
            outParentFrame.set(outDisplayFrame);
            if ((1073741824 & pfl) != 0 && (source = state.peekSource(19)) != null) {
                outParentFrame.inset(source.calculateInsets(outParentFrame, false));
            }
        } else {
            outParentFrame.set(!layoutInScreen ? attachedWindowFrame : outDisplayFrame);
        }
        int cutoutMode = attrs.layoutInDisplayCutoutMode;
        DisplayCutout cutout = state.getDisplayCutout();
        Rect displayCutoutSafeExceptMaybeBars = this.mTempDisplayCutoutSafeExceptMaybeBarsRect;
        displayCutoutSafeExceptMaybeBars.set(displayCutoutSafe);
        outFrames.isParentFrameClippedByDisplayCutout = false;
        if (cutoutMode == 3 || cutout.isEmpty() || isThunderBackWin) {
            type3 = type;
        } else {
            Rect displayFrame = state.getDisplayFrame();
            InsetsSource statusBarSource = state.peekSource(0);
            if (statusBarSource != null) {
                int i5 = displayCutoutSafe.top;
                int bottom = displayFrame.top;
                if (i5 > bottom) {
                    displayCutoutSafeExceptMaybeBars.top = Math.max(statusBarSource.getFrame().bottom, displayCutoutSafe.top);
                }
            }
            if (cutoutMode == 1) {
                if (displayFrame.width() < displayFrame.height()) {
                    displayCutoutSafeExceptMaybeBars.top = Integer.MIN_VALUE;
                    displayCutoutSafeExceptMaybeBars.bottom = Integer.MAX_VALUE;
                } else {
                    displayCutoutSafeExceptMaybeBars.left = Integer.MIN_VALUE;
                    displayCutoutSafeExceptMaybeBars.right = Integer.MAX_VALUE;
                }
            }
            boolean layoutInsetDecor = (attrs.flags & 65536) != 0;
            if (layoutInScreen && layoutInsetDecor) {
                if (cutoutMode == 0 || cutoutMode == 1) {
                    Insets systemBarsInsets = state.calculateInsets(displayFrame, WindowInsets.Type.systemBars(), requestedVisibilities);
                    if (systemBarsInsets.left > 0) {
                        displayCutoutSafeExceptMaybeBars.left = Integer.MIN_VALUE;
                    }
                    if (systemBarsInsets.top > 0) {
                        displayCutoutSafeExceptMaybeBars.top = Integer.MIN_VALUE;
                    }
                    if (systemBarsInsets.right > 0) {
                        displayCutoutSafeExceptMaybeBars.right = Integer.MAX_VALUE;
                    }
                    if (systemBarsInsets.bottom > 0) {
                        displayCutoutSafeExceptMaybeBars.bottom = Integer.MAX_VALUE;
                    }
                }
            }
            type3 = type;
            if (type3 == 2011 && (navSource = state.peekSource(1)) != null && navSource.calculateInsets(displayFrame, true).bottom > 0) {
                displayCutoutSafeExceptMaybeBars.bottom = Integer.MAX_VALUE;
            }
            boolean attachedInParent = (attachedWindowFrame == null || layoutInScreen) ? false : true;
            boolean floatingInScreenWindow = (attrs.isFullscreen() || !layoutInScreen || type3 == 1) ? false : true;
            if (!attachedInParent && !floatingInScreenWindow) {
                this.mTempRect.set(outParentFrame);
                outParentFrame.intersectUnchecked(displayCutoutSafeExceptMaybeBars);
                outFrames.isParentFrameClippedByDisplayCutout = !this.mTempRect.equals(outParentFrame);
            }
            outDisplayFrame.intersectUnchecked(displayCutoutSafeExceptMaybeBars);
        }
        boolean noLimits = (attrs.flags & 512) != 0;
        boolean inMultiWindowMode = WindowConfiguration.inMultiWindowMode(windowingMode);
        if (noLimits && type3 != 2010 && !inMultiWindowMode) {
            outDisplayFrame.left = -100000;
            outDisplayFrame.top = -100000;
            outDisplayFrame.right = 100000;
            outDisplayFrame.bottom = 100000;
        }
        boolean hasCompatScale = compatScale != 1.0f;
        int pw = outParentFrame.width();
        int ph = outParentFrame.height();
        int pfl2 = attrs.privateFlags;
        boolean extendedByCutout = (pfl2 & 8192) != 0;
        int rw = requestedWidth;
        if (rw == -1 || extendedByCutout) {
            rw = attrs.width >= 0 ? attrs.width : pw;
        }
        int rh = requestedHeight;
        if (rh == -1 || extendedByCutout) {
            rh = attrs.height >= 0 ? attrs.height : ph;
        }
        if ((attrs.flags & 16384) != 0) {
            if (attrs.width < 0) {
                w3 = pw;
            } else if (hasCompatScale) {
                w3 = (int) ((attrs.width * compatScale) + 0.5f);
            } else {
                w3 = attrs.width;
            }
            int w4 = w3;
            int w5 = attrs.height;
            if (w5 < 0) {
                h = ph;
                w2 = w4;
            } else if (hasCompatScale) {
                h = (int) ((attrs.height * compatScale) + 0.5f);
                w2 = w4;
            } else {
                int h2 = attrs.height;
                h = h2;
                w2 = w4;
            }
        } else {
            int h3 = attrs.width;
            if (h3 == -1) {
                w = pw;
            } else if (hasCompatScale) {
                w = (int) ((rw * compatScale) + 0.5f);
            } else {
                w = rw;
            }
            int w6 = w;
            if (attrs.height == -1) {
                h = ph;
                w2 = w6;
            } else if (hasCompatScale) {
                h = (int) ((rh * compatScale) + 0.5f);
                w2 = w6;
            } else {
                int h4 = rh;
                h = h4;
                w2 = w6;
            }
        }
        if (hasCompatScale) {
            float x2 = attrs.x * compatScale;
            y = attrs.y * compatScale;
            x = x2;
        } else {
            float x3 = attrs.x;
            y = attrs.y;
            x = x3;
        }
        if (inMultiWindowMode && (attrs.privateFlags & 65536) == 0) {
            w2 = Math.min(w2, pw);
            h = Math.min(h, ph);
        }
        boolean inMultiWindowMode2 = true;
        Gravity.apply(attrs.gravity, w2, h, outParentFrame, (int) ((attrs.horizontalMargin * pw) + x), (int) (y + (attrs.verticalMargin * ph)), outFrame);
        if (!inMultiWindowMode2) {
            outFrame2 = outFrame;
        } else {
            outFrame2 = outFrame;
            Gravity.applyDisplay(attrs.gravity, outDisplayFrame, outFrame2);
        }
        if (extendedByCutout && !displayCutoutSafe.contains(outFrame2)) {
            this.mTempRect.set(outFrame2);
            Gravity.applyDisplay(attrs.gravity & (-285212673), displayCutoutSafe, this.mTempRect);
            if (this.mTempRect.intersect(outDisplayFrame)) {
                outFrame2.union(this.mTempRect);
            }
        }
        if (requestedWidth <= 65536) {
            i = requestedHeight;
            if (i <= 65536) {
                return;
            }
        } else {
            i = requestedHeight;
        }
        String str2 = TAG;
        StringBuilder append = new StringBuilder().append("computeWindowFrames ").append((Object) attrs.getTitle()).append(" outFrames=").append(outFrames).append(" windowBounds=").append(windowBounds.toShortString()).append(" attachedWindowFrame=");
        if (attachedWindowFrame != null) {
            str = attachedWindowFrame.toShortString();
        } else {
            str = "null";
        }
        Log.d(str2, append.append(str).append(" requestedWidth=").append(requestedWidth).append(" requestedHeight=").append(i).append(" compatScale=").append(compatScale).append(" windowingMode=").append(WindowConfiguration.windowingModeToString(windowingMode)).append(" displayCutoutSafe=").append(displayCutoutSafe).append(" attrs=").append(attrs).append(" state=").append(state).append(" requestedVisibilities=").append(requestedVisibilities).toString());
    }

    public static void computeSurfaceSize(WindowManager.LayoutParams attrs, Rect maxBounds, int requestedWidth, int requestedHeight, Rect winFrame, boolean dragResizing, Point outSurfaceSize) {
        int width;
        int height;
        if ((attrs.flags & 16384) != 0) {
            width = requestedWidth;
            height = requestedHeight;
        } else if (dragResizing) {
            width = maxBounds.width();
            height = maxBounds.height();
        } else {
            width = winFrame.width();
            height = winFrame.height();
        }
        if (width < 1) {
            width = 1;
        }
        if (height < 1) {
            height = 1;
        }
        Rect surfaceInsets = attrs.surfaceInsets;
        int width2 = width + surfaceInsets.left + surfaceInsets.right;
        int height2 = height + surfaceInsets.top + surfaceInsets.bottom;
        if (width2 > 65536 || height2 > 65536) {
            Log.d(TAG, "computeSurfaceSize dimension exceeds 65536:width=" + width2 + "  height=" + height2 + "  requestedWidth=" + requestedWidth + "  requestedHeight=" + requestedHeight + "  maxBounds.width=" + maxBounds.width() + "  maxBounds.height=" + maxBounds.height() + "  winFrame.width=" + winFrame.width() + "  winFrame.height=" + winFrame.height() + "  surfaceInsets=" + surfaceInsets + "  dragResizing=" + dragResizing + "  FLAG_SCALED=" + ((attrs.flags & 16384) != 0));
            if (TRAN_USER_ROOT_SUPPORT) {
                if (width2 > 65536) {
                    width2 = 65536;
                }
                if (height2 > 65536) {
                    height2 = 65536;
                }
            }
        }
        outSurfaceSize.set(width2, height2);
    }
}
