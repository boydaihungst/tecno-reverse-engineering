package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.text.PositionedGlyphs;
import android.graphics.text.TextRunShaper;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.text.Layout;
import android.text.TextShaper;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;
import com.android.internal.util.ArrayUtils;
import java.util.ArrayList;
/* loaded from: classes3.dex */
public class TextLine {
    private static final boolean DEBUG = false;
    private static final char TAB_CHAR = '\t';
    private static final int TAB_INCREMENT = 20;
    private static final TextLine[] sCached = new TextLine[3];
    private float mAddedWidthForJustify;
    private char[] mChars;
    private boolean mCharsValid;
    private PrecomputedText mComputed;
    private int mDir;
    private Layout.Directions mDirections;
    private int mEllipsisEnd;
    private int mEllipsisStart;
    private boolean mHasTabs;
    private boolean mIsJustifying;
    private int mLen;
    private TextPaint mPaint;
    private Spanned mSpanned;
    private int mStart;
    private Layout.TabStops mTabs;
    private CharSequence mText;
    private boolean mUseFallbackExtent = false;
    private final TextPaint mWorkPaint = new TextPaint();
    private final TextPaint mActivePaint = new TextPaint();
    private final SpanSet<MetricAffectingSpan> mMetricAffectingSpanSpanSet = new SpanSet<>(MetricAffectingSpan.class);
    private final SpanSet<CharacterStyle> mCharacterStyleSpanSet = new SpanSet<>(CharacterStyle.class);
    private final SpanSet<ReplacementSpan> mReplacementSpanSpanSet = new SpanSet<>(ReplacementSpan.class);
    private final DecorationInfo mDecorationInfo = new DecorationInfo();
    private final ArrayList<DecorationInfo> mDecorations = new ArrayList<>();

    public static TextLine obtain() {
        TextLine[] textLineArr;
        TextLine tl;
        TextLine[] textLineArr2 = sCached;
        synchronized (textLineArr2) {
            int i = textLineArr2.length;
            do {
                i--;
                if (i >= 0) {
                    textLineArr = sCached;
                    tl = textLineArr[i];
                } else {
                    return new TextLine();
                }
            } while (tl == null);
            textLineArr[i] = null;
            return tl;
        }
    }

    public static TextLine recycle(TextLine tl) {
        tl.mText = null;
        tl.mPaint = null;
        tl.mDirections = null;
        tl.mSpanned = null;
        tl.mTabs = null;
        tl.mChars = null;
        tl.mComputed = null;
        tl.mUseFallbackExtent = false;
        tl.mMetricAffectingSpanSpanSet.recycle();
        tl.mCharacterStyleSpanSet.recycle();
        tl.mReplacementSpanSpanSet.recycle();
        synchronized (sCached) {
            int i = 0;
            while (true) {
                TextLine[] textLineArr = sCached;
                if (i >= textLineArr.length) {
                    break;
                } else if (textLineArr[i] != null) {
                    i++;
                } else {
                    textLineArr[i] = tl;
                    break;
                }
            }
        }
        return null;
    }

    public void set(TextPaint paint, CharSequence text, int start, int limit, int dir, Layout.Directions directions, boolean hasTabs, Layout.TabStops tabStops, int ellipsisStart, int ellipsisEnd, boolean useFallbackLineSpacing) {
        this.mPaint = paint;
        this.mText = text;
        this.mStart = start;
        this.mLen = limit - start;
        this.mDir = dir;
        this.mDirections = directions;
        this.mUseFallbackExtent = useFallbackLineSpacing;
        if (directions == null) {
            throw new IllegalArgumentException("Directions cannot be null");
        }
        this.mHasTabs = hasTabs;
        this.mSpanned = null;
        boolean hasReplacement = false;
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            this.mSpanned = spanned;
            this.mReplacementSpanSpanSet.init(spanned, start, limit);
            hasReplacement = this.mReplacementSpanSpanSet.numberOfSpans > 0;
        }
        this.mComputed = null;
        if (text instanceof PrecomputedText) {
            PrecomputedText precomputedText = (PrecomputedText) text;
            this.mComputed = precomputedText;
            if (!precomputedText.getParams().getTextPaint().equalsForTextMeasurement(paint)) {
                this.mComputed = null;
            }
        }
        this.mCharsValid = hasReplacement;
        if (hasReplacement) {
            char[] cArr = this.mChars;
            if (cArr == null || cArr.length < this.mLen) {
                this.mChars = ArrayUtils.newUnpaddedCharArray(this.mLen);
            }
            TextUtils.getChars(text, start, limit, this.mChars, 0);
            if (hasReplacement) {
                char[] chars = this.mChars;
                int i = start;
                while (i < limit) {
                    int inext = this.mReplacementSpanSpanSet.getNextTransition(i, limit);
                    if (this.mReplacementSpanSpanSet.hasSpansIntersecting(i, inext) && (i - start >= ellipsisEnd || inext - start <= ellipsisStart)) {
                        chars[i - start] = 65532;
                        int e = inext - start;
                        for (int j = (i - start) + 1; j < e; j++) {
                            chars[j] = 65279;
                        }
                    }
                    i = inext;
                }
            }
        }
        this.mTabs = tabStops;
        this.mAddedWidthForJustify = 0.0f;
        this.mIsJustifying = false;
        this.mEllipsisStart = ellipsisStart != ellipsisEnd ? ellipsisStart : 0;
        this.mEllipsisEnd = ellipsisStart != ellipsisEnd ? ellipsisEnd : 0;
    }

    private char charAt(int i) {
        return this.mCharsValid ? this.mChars[i] : this.mText.charAt(this.mStart + i);
    }

    public void justify(float justifyWidth) {
        int end = this.mLen;
        while (end > 0 && isLineEndSpace(this.mText.charAt((this.mStart + end) - 1))) {
            end--;
        }
        int spaces = countStretchableSpaces(0, end);
        if (spaces != 0) {
            float width = Math.abs(measure(end, false, null));
            this.mAddedWidthForJustify = (justifyWidth - width) / spaces;
            this.mIsJustifying = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void draw(Canvas c, float x, int top, int y, int bottom) {
        int runCount;
        int runCount2;
        float h = 0.0f;
        int j = this.mDirections.getRunCount();
        int runIndex = 0;
        while (runIndex < j) {
            int runStart = this.mDirections.getRunStart(runIndex);
            if (runStart <= this.mLen) {
                int runLimit = Math.min(this.mDirections.getRunLength(runIndex) + runStart, this.mLen);
                boolean runIsRtl = this.mDirections.isRunRtl(runIndex);
                float h2 = h;
                int segStart = runStart;
                int j2 = this.mHasTabs ? runStart : runLimit;
                while (j2 <= runLimit) {
                    if (j2 == runLimit || charAt(j2) == '\t') {
                        float f = x + h2;
                        boolean z = (runIndex == j + (-1) && j2 == this.mLen) ? false : true;
                        runCount = j;
                        runCount2 = j2;
                        h2 += drawRun(c, segStart, j2, runIsRtl, f, top, y, bottom, z);
                        if (runCount2 != runLimit) {
                            int i = this.mDir;
                            h2 = i * nextTab(i * h2);
                        }
                        segStart = runCount2 + 1;
                    } else {
                        runCount = j;
                        runCount2 = j2;
                    }
                    j2 = runCount2 + 1;
                    j = runCount;
                }
                int runCount3 = j;
                runIndex++;
                h = h2;
                j = runCount3;
            } else {
                return;
            }
        }
    }

    public float metrics(Paint.FontMetricsInt fmi) {
        return measure(this.mLen, false, fmi);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void shape(TextShaper.GlyphsConsumer consumer) {
        float x;
        int j;
        int horizontal = 0;
        float j2 = 0;
        int runCount = this.mDirections.getRunCount();
        int runIndex = 0;
        while (runIndex < runCount) {
            int runStart = this.mDirections.getRunStart(runIndex);
            if (runStart <= this.mLen) {
                int runLimit = Math.min(this.mDirections.getRunLength(runIndex) + runStart, this.mLen);
                boolean runIsRtl = this.mDirections.isRunRtl(runIndex);
                float horizontal2 = horizontal;
                int segStart = runStart;
                int j3 = this.mHasTabs ? runStart : runLimit;
                while (j3 <= runLimit) {
                    if (j3 == runLimit || charAt(j3) == '\t') {
                        float f = j2 + horizontal2;
                        x = j2;
                        j = j3;
                        horizontal2 += shapeRun(consumer, segStart, j3, runIsRtl, f, (runIndex == runCount + (-1) && j3 == this.mLen) ? false : true);
                        if (j != runLimit) {
                            int i = this.mDir;
                            horizontal2 = i * nextTab(i * horizontal2);
                        }
                        segStart = j + 1;
                    } else {
                        x = j2;
                        j = j3;
                    }
                    j3 = j + 1;
                    j2 = x;
                }
                float x2 = j2;
                runIndex++;
                horizontal = horizontal2;
                j2 = x2;
            } else {
                return;
            }
        }
    }

    public float measure(int offset, boolean trailing, Paint.FontMetricsInt fmi) {
        int runStart;
        if (offset > this.mLen) {
            throw new IndexOutOfBoundsException("offset(" + offset + ") should be less than line limit(" + this.mLen + NavigationBarInflaterView.KEY_CODE_END);
        }
        int target = trailing ? offset - 1 : offset;
        if (target < 0) {
            return 0.0f;
        }
        float h = 0.0f;
        int runIndex = 0;
        while (runIndex < this.mDirections.getRunCount() && (runStart = this.mDirections.getRunStart(runIndex)) <= this.mLen) {
            int runLimit = Math.min(this.mDirections.getRunLength(runIndex) + runStart, this.mLen);
            boolean runIsRtl = this.mDirections.isRunRtl(runIndex);
            float h2 = h;
            int segStart = runStart;
            int j = this.mHasTabs ? runStart : runLimit;
            while (j <= runLimit) {
                if (j == runLimit || charAt(j) == '\t') {
                    boolean targetIsInThisSegment = target >= segStart && target < j;
                    boolean sameDirection = (this.mDir == -1) == runIsRtl;
                    if (targetIsInThisSegment && sameDirection) {
                        return measureRun(segStart, offset, j, runIsRtl, fmi) + h2;
                    }
                    float segmentWidth = measureRun(segStart, j, j, runIsRtl, fmi);
                    h2 += sameDirection ? segmentWidth : -segmentWidth;
                    if (targetIsInThisSegment) {
                        return measureRun(segStart, offset, j, runIsRtl, null) + h2;
                    }
                    if (j != runLimit) {
                        if (offset == j) {
                            return h2;
                        }
                        int i = this.mDir;
                        float h3 = i * nextTab(i * h2);
                        if (target != j) {
                            h2 = h3;
                        } else {
                            return h3;
                        }
                    }
                    segStart = j + 1;
                }
                j++;
            }
            runIndex++;
            h = h2;
        }
        return h;
    }

    public float[] measureAllOffsets(boolean[] trailing, Paint.FontMetricsInt fmi) {
        int runStart;
        int j;
        int offset;
        float w;
        int i = this.mLen;
        float[] measurement = new float[i + 1];
        boolean z = true;
        int[] target = new int[i + 1];
        for (int offset2 = 0; offset2 < target.length; offset2++) {
            target[offset2] = trailing[offset2] ? offset2 - 1 : offset2;
        }
        if (target[0] < 0) {
            measurement[0] = 0.0f;
        }
        float h = 0.0f;
        int runIndex = 0;
        while (runIndex < this.mDirections.getRunCount() && (runStart = this.mDirections.getRunStart(runIndex)) <= this.mLen) {
            int runLimit = Math.min(this.mDirections.getRunLength(runIndex) + runStart, this.mLen);
            boolean runIsRtl = this.mDirections.isRunRtl(runIndex);
            float h2 = h;
            int segStart = runStart;
            int j2 = this.mHasTabs ? runStart : runLimit;
            while (j2 <= runLimit) {
                if (j2 == runLimit || charAt(j2) == '\t') {
                    float oldh = h2;
                    boolean advance = (this.mDir == -1 ? z : false) == runIsRtl ? z : false;
                    j = j2;
                    int segStart2 = segStart;
                    float w2 = measureRun(segStart, j2, j2, runIsRtl, fmi);
                    h2 += advance ? w2 : -w2;
                    float baseh = advance ? oldh : h2;
                    Paint.FontMetricsInt crtfmi = advance ? fmi : null;
                    int offset3 = segStart2;
                    while (offset3 <= j && offset3 <= this.mLen) {
                        int segStart3 = segStart2;
                        if (target[offset3] < segStart3 || target[offset3] >= j) {
                            segStart2 = segStart3;
                            offset = offset3;
                            w = w2;
                        } else {
                            segStart2 = segStart3;
                            int segStart4 = offset3;
                            offset = offset3;
                            w = w2;
                            measurement[offset] = baseh + measureRun(segStart3, segStart4, j, runIsRtl, crtfmi);
                        }
                        offset3 = offset + 1;
                        w2 = w;
                    }
                    if (j != runLimit) {
                        if (target[j] == j) {
                            measurement[j] = h2;
                        }
                        int i2 = this.mDir;
                        float h3 = i2 * nextTab(i2 * h2);
                        if (target[j + 1] == j) {
                            measurement[j + 1] = h3;
                        }
                        h2 = h3;
                    }
                    segStart = j + 1;
                } else {
                    j = j2;
                }
                j2 = j + 1;
                z = true;
            }
            runIndex++;
            h = h2;
            z = true;
        }
        int i3 = this.mLen;
        if (target[i3] == i3) {
            measurement[i3] = h;
        }
        return measurement;
    }

    private float drawRun(Canvas c, int start, int limit, boolean runIsRtl, float x, int top, int y, int bottom, boolean needWidth) {
        if ((this.mDir == 1) == runIsRtl) {
            float w = -measureRun(start, limit, limit, runIsRtl, null);
            handleRun(start, limit, limit, runIsRtl, c, null, x + w, top, y, bottom, null, false);
            return w;
        }
        return handleRun(start, limit, limit, runIsRtl, c, null, x, top, y, bottom, null, needWidth);
    }

    private float measureRun(int start, int offset, int limit, boolean runIsRtl, Paint.FontMetricsInt fmi) {
        return handleRun(start, offset, limit, runIsRtl, null, null, 0.0f, 0, 0, 0, fmi, true);
    }

    private float shapeRun(TextShaper.GlyphsConsumer consumer, int start, int limit, boolean runIsRtl, float x, boolean needWidth) {
        if ((this.mDir == 1) == runIsRtl) {
            float w = -measureRun(start, limit, limit, runIsRtl, null);
            handleRun(start, limit, limit, runIsRtl, null, consumer, x + w, 0, 0, 0, null, false);
            return w;
        }
        return handleRun(start, limit, limit, runIsRtl, null, consumer, x, 0, 0, 0, null, needWidth);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:110:0x0186, code lost:
        r8 = r6;
        r1 = -1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:111:0x018c, code lost:
        if (r8 != (-1)) goto L59;
     */
    /* JADX WARN: Code restructure failed: missing block: B:112:0x018e, code lost:
        if (r0 == false) goto L57;
     */
    /* JADX WARN: Code restructure failed: missing block: B:113:0x0190, code lost:
        r1 = r27.mLen + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:114:0x0194, code lost:
        r6 = r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:115:0x0196, code lost:
        if (r8 > r11) goto L47;
     */
    /* JADX WARN: Code restructure failed: missing block: B:116:0x0198, code lost:
        if (r0 == false) goto L64;
     */
    /* JADX WARN: Code restructure failed: missing block: B:117:0x019a, code lost:
        r1 = r11;
     */
    /* JADX WARN: Code restructure failed: missing block: B:118:0x019c, code lost:
        r1 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:119:0x019d, code lost:
        r6 = r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:137:?, code lost:
        return r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:138:?, code lost:
        return r6;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int getOffsetToLeftRightOf(int cursor, boolean toLeft) {
        int runLevel;
        int runStart;
        int runLimit;
        boolean trailing;
        int runLimit2;
        int runIndex;
        int[] runs;
        int newCaret;
        int runStart2;
        int runStart3;
        int newCaret2;
        int otherRunLimit;
        int lineEnd = this.mLen;
        boolean paraIsRtl = this.mDir == -1;
        int[] runs2 = this.mDirections.mDirections;
        int runStart4 = 0;
        int runLimit3 = lineEnd;
        boolean trailing2 = false;
        if (cursor == 0) {
            runLevel = 0;
            runIndex = -2;
            runs = runs2;
            newCaret = -1;
        } else if (cursor == lineEnd) {
            runLevel = 0;
            runIndex = runs2.length;
            runs = runs2;
            newCaret = -1;
        } else {
            int runIndex2 = 0;
            while (true) {
                if (runIndex2 >= runs2.length) {
                    runLevel = 0;
                    runStart = runStart4;
                    runLimit = runLimit3;
                    trailing = false;
                    runLimit2 = runIndex2;
                    break;
                }
                int prevRunLevel = 0 + runs2[runIndex2];
                if (cursor >= prevRunLevel) {
                    int runLimit4 = (runs2[runIndex2 + 1] & 67108863) + prevRunLevel;
                    if (runLimit4 > lineEnd) {
                        runLimit4 = lineEnd;
                    }
                    if (cursor >= runLimit4) {
                        runStart2 = prevRunLevel;
                        runLimit3 = runLimit4;
                    } else {
                        int runLevel2 = (runs2[runIndex2 + 1] >>> 26) & 63;
                        if (cursor != prevRunLevel) {
                            runStart = prevRunLevel;
                            runLevel = runLevel2;
                            trailing = false;
                            runLimit = runLimit4;
                            runLimit2 = runIndex2;
                        } else {
                            int pos = cursor - 1;
                            int prevRunIndex = 0;
                            while (true) {
                                if (prevRunIndex >= runs2.length) {
                                    break;
                                }
                                int prevRunStart = runs2[prevRunIndex] + 0;
                                if (pos >= prevRunStart) {
                                    int prevRunLimit = prevRunStart + (runs2[prevRunIndex + 1] & 67108863);
                                    if (prevRunLimit > lineEnd) {
                                        prevRunLimit = lineEnd;
                                    }
                                    if (pos >= prevRunLimit) {
                                        runStart3 = prevRunLevel;
                                    } else {
                                        runStart3 = prevRunLevel;
                                        int runStart5 = (runs2[prevRunIndex + 1] >>> 26) & 63;
                                        if (runStart5 < runLevel2) {
                                            runIndex2 = prevRunIndex;
                                            runLevel2 = runStart5;
                                            runLimit4 = prevRunLimit;
                                            trailing2 = true;
                                            prevRunLevel = prevRunStart;
                                            break;
                                        }
                                    }
                                } else {
                                    runStart3 = prevRunLevel;
                                }
                                prevRunIndex += 2;
                                prevRunLevel = runStart3;
                            }
                            runLevel = runLevel2;
                            runStart = prevRunLevel;
                            trailing = trailing2;
                            runLimit = runLimit4;
                            runLimit2 = runIndex2;
                        }
                    }
                } else {
                    runStart2 = prevRunLevel;
                }
                runIndex2 += 2;
                runStart4 = runStart2;
            }
            int runLevel3 = runs2.length;
            if (runLimit2 == runLevel3) {
                runIndex = runLimit2;
                runs = runs2;
            } else {
                boolean runIsRtl = (runLevel & 1) != 0;
                boolean advance = toLeft == runIsRtl;
                if (cursor == (advance ? runLimit : runStart) && advance == trailing) {
                    runIndex = runLimit2;
                    runs = runs2;
                } else {
                    runIndex = runLimit2;
                    runs = runs2;
                    int newCaret3 = getOffsetBeforeAfter(runLimit2, runStart, runLimit, runIsRtl, cursor, advance);
                    if (newCaret3 == (advance ? runLimit : runStart)) {
                        newCaret = newCaret3;
                    } else {
                        return newCaret3;
                    }
                }
            }
            newCaret = -1;
        }
        while (true) {
            boolean advance2 = toLeft == paraIsRtl;
            int otherRunIndex = runIndex + (advance2 ? 2 : -2);
            if (otherRunIndex < 0 || otherRunIndex >= runs.length) {
                break;
            }
            int otherRunStart = 0 + runs[otherRunIndex];
            int otherRunLimit2 = otherRunStart + (runs[otherRunIndex + 1] & 67108863);
            if (otherRunLimit2 <= lineEnd) {
                otherRunLimit = otherRunLimit2;
            } else {
                otherRunLimit = lineEnd;
            }
            int otherRunLevel = (runs[otherRunIndex + 1] >>> 26) & 63;
            boolean otherRunIsRtl = (otherRunLevel & 1) != 0;
            boolean advance3 = toLeft == otherRunIsRtl;
            if (newCaret == -1) {
                int[] runs3 = runs;
                newCaret = getOffsetBeforeAfter(otherRunIndex, otherRunStart, otherRunLimit, otherRunIsRtl, advance3 ? otherRunStart : otherRunLimit, advance3);
                if (newCaret != (advance3 ? otherRunLimit : otherRunStart)) {
                    return newCaret;
                }
                runIndex = otherRunIndex;
                runLevel = otherRunLevel;
                runs = runs3;
            } else {
                newCaret2 = newCaret;
                if (otherRunLevel < runLevel) {
                    int newCaret4 = advance3 ? otherRunStart : otherRunLimit;
                    return newCaret4;
                }
            }
        }
        return newCaret2;
    }

    /* JADX WARN: Removed duplicated region for block: B:45:0x0092  */
    /* JADX WARN: Removed duplicated region for block: B:48:0x0098  */
    /* JADX WARN: Removed duplicated region for block: B:50:0x00a8  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int getOffsetBeforeAfter(int runIndex, int runStart, int runLimit, boolean runIsRtl, int offset, boolean after) {
        int spanStart;
        int spanLimit;
        int i;
        int spanLimit2;
        if (runIndex >= 0) {
            if (offset != (after ? this.mLen : 0)) {
                TextPaint wp = this.mWorkPaint;
                wp.set(this.mPaint);
                if (this.mIsJustifying) {
                    wp.setWordSpacing(this.mAddedWidthForJustify);
                }
                int spanStart2 = runStart;
                if (this.mSpanned != null && runStart != runLimit) {
                    int target = after ? offset + 1 : offset;
                    int limit = this.mStart + runLimit;
                    while (true) {
                        int nextSpanTransition = this.mSpanned.nextSpanTransition(this.mStart + spanStart2, limit, MetricAffectingSpan.class);
                        i = this.mStart;
                        spanLimit2 = nextSpanTransition - i;
                        if (spanLimit2 >= target) {
                            break;
                        }
                        spanStart2 = spanLimit2;
                    }
                    MetricAffectingSpan[] spans = (MetricAffectingSpan[]) TextUtils.removeEmptySpans((MetricAffectingSpan[]) this.mSpanned.getSpans(i + spanStart2, i + spanLimit2, MetricAffectingSpan.class), this.mSpanned, MetricAffectingSpan.class);
                    if (spans.length > 0) {
                        ReplacementSpan replacement = null;
                        for (MetricAffectingSpan span : spans) {
                            if (span instanceof ReplacementSpan) {
                                replacement = (ReplacementSpan) span;
                            } else {
                                span.updateMeasureState(wp);
                            }
                        }
                        if (replacement != null) {
                            return after ? spanLimit2 : spanStart2;
                        }
                    }
                    spanStart = spanStart2;
                    spanLimit = spanLimit2;
                    int cursorOpt = after ? 0 : 2;
                    if (!this.mCharsValid) {
                        return wp.getTextRunCursor(this.mChars, spanStart, spanLimit - spanStart, runIsRtl, offset, cursorOpt);
                    }
                    CharSequence charSequence = this.mText;
                    int i2 = this.mStart;
                    return wp.getTextRunCursor(charSequence, i2 + spanStart, i2 + spanLimit, runIsRtl, i2 + offset, cursorOpt) - this.mStart;
                }
                spanStart = spanStart2;
                spanLimit = runLimit;
                int cursorOpt2 = after ? 0 : 2;
                if (!this.mCharsValid) {
                }
            }
        }
        return after ? TextUtils.getOffsetAfter(this.mText, this.mStart + offset) - this.mStart : TextUtils.getOffsetBefore(this.mText, this.mStart + offset) - this.mStart;
    }

    private static void expandMetricsFromPaint(Paint.FontMetricsInt fmi, TextPaint wp) {
        int previousTop = fmi.top;
        int previousAscent = fmi.ascent;
        int previousDescent = fmi.descent;
        int previousBottom = fmi.bottom;
        int previousLeading = fmi.leading;
        wp.getFontMetricsInt(fmi);
        updateMetrics(fmi, previousTop, previousAscent, previousDescent, previousBottom, previousLeading);
    }

    private void expandMetricsFromPaint(TextPaint wp, int start, int end, int contextStart, int contextEnd, boolean runIsRtl, Paint.FontMetricsInt fmi) {
        int previousTop = fmi.top;
        int previousAscent = fmi.ascent;
        int previousDescent = fmi.descent;
        int previousBottom = fmi.bottom;
        int previousLeading = fmi.leading;
        int count = end - start;
        int contextCount = contextEnd - contextStart;
        if (this.mCharsValid) {
            wp.getFontMetricsInt(this.mChars, start, count, contextStart, contextCount, runIsRtl, fmi);
        } else {
            PrecomputedText precomputedText = this.mComputed;
            if (precomputedText == null) {
                CharSequence charSequence = this.mText;
                int i = this.mStart;
                wp.getFontMetricsInt(charSequence, i + start, count, i + contextStart, contextCount, runIsRtl, fmi);
            } else {
                int i2 = this.mStart;
                precomputedText.getFontMetricsInt(i2 + start, i2 + end, fmi);
            }
        }
        updateMetrics(fmi, previousTop, previousAscent, previousDescent, previousBottom, previousLeading);
    }

    static void updateMetrics(Paint.FontMetricsInt fmi, int previousTop, int previousAscent, int previousDescent, int previousBottom, int previousLeading) {
        fmi.top = Math.min(fmi.top, previousTop);
        fmi.ascent = Math.min(fmi.ascent, previousAscent);
        fmi.descent = Math.max(fmi.descent, previousDescent);
        fmi.bottom = Math.max(fmi.bottom, previousBottom);
        fmi.leading = Math.max(fmi.leading, previousLeading);
    }

    private static void drawStroke(TextPaint wp, Canvas c, int color, float position, float thickness, float xleft, float xright, float baseline) {
        float strokeTop = baseline + wp.baselineShift + position;
        int previousColor = wp.getColor();
        Paint.Style previousStyle = wp.getStyle();
        boolean previousAntiAlias = wp.isAntiAlias();
        wp.setStyle(Paint.Style.FILL);
        wp.setAntiAlias(true);
        wp.setColor(color);
        c.drawRect(xleft, strokeTop, xright, strokeTop + thickness, wp);
        wp.setStyle(previousStyle);
        wp.setColor(previousColor);
        wp.setAntiAlias(previousAntiAlias);
    }

    private float getRunAdvance(TextPaint wp, int start, int end, int contextStart, int contextEnd, boolean runIsRtl, int offset) {
        if (this.mCharsValid) {
            return wp.getRunAdvance(this.mChars, start, end, contextStart, contextEnd, runIsRtl, offset);
        }
        int delta = this.mStart;
        PrecomputedText precomputedText = this.mComputed;
        if (precomputedText == null) {
            return wp.getRunAdvance(this.mText, delta + start, delta + end, delta + contextStart, delta + contextEnd, runIsRtl, delta + offset);
        }
        return precomputedText.getWidth(start + delta, end + delta);
    }

    private float handleText(TextPaint wp, int start, int end, int contextStart, int contextEnd, boolean runIsRtl, Canvas c, TextShaper.GlyphsConsumer consumer, float x, int top, int y, int bottom, Paint.FontMetricsInt fmi, boolean needWidth, int offset, ArrayList<DecorationInfo> decorations) {
        int numDecorations;
        float leftX;
        float rightX;
        float totalWidth;
        float decorationXLeft;
        float decorationXRight;
        float f;
        if (this.mIsJustifying) {
            wp.setWordSpacing(this.mAddedWidthForJustify);
        }
        if (fmi != null) {
            expandMetricsFromPaint(fmi, wp);
        }
        if (end == start) {
            return 0.0f;
        }
        float totalWidth2 = 0.0f;
        int numDecorations2 = decorations == null ? 0 : decorations.size();
        if (needWidth || ((c != null || consumer != null) && (wp.bgColor != 0 || numDecorations2 != 0 || runIsRtl))) {
            numDecorations = numDecorations2;
            totalWidth2 = getRunAdvance(wp, start, end, contextStart, contextEnd, runIsRtl, offset);
        } else {
            numDecorations = numDecorations2;
        }
        if (runIsRtl) {
            float leftX2 = x - totalWidth2;
            leftX = leftX2;
            rightX = x;
        } else {
            leftX = x;
            rightX = x + totalWidth2;
        }
        if (consumer == null) {
            totalWidth = totalWidth2;
        } else {
            totalWidth = totalWidth2;
            float totalWidth3 = leftX;
            shapeTextRun(consumer, wp, start, end, contextStart, contextEnd, runIsRtl, totalWidth3);
        }
        if (this.mUseFallbackExtent && fmi != null) {
            expandMetricsFromPaint(wp, start, end, contextStart, contextEnd, runIsRtl, fmi);
        }
        if (c != null) {
            if (wp.bgColor != 0) {
                int previousColor = wp.getColor();
                Paint.Style previousStyle = wp.getStyle();
                wp.setColor(wp.bgColor);
                wp.setStyle(Paint.Style.FILL);
                c.drawRect(leftX, top, rightX, bottom, wp);
                wp.setStyle(previousStyle);
                wp.setColor(previousColor);
            }
            drawTextRun(c, wp, start, end, contextStart, contextEnd, runIsRtl, leftX, y + wp.baselineShift);
            if (numDecorations != 0) {
                int i = 0;
                while (i < numDecorations) {
                    DecorationInfo info = decorations.get(i);
                    int numDecorations3 = numDecorations;
                    int decorationStart = Math.max(info.start, start);
                    int decorationEnd = Math.min(info.end, offset);
                    float decorationStartAdvance = getRunAdvance(wp, start, end, contextStart, contextEnd, runIsRtl, decorationStart);
                    float decorationEndAdvance = getRunAdvance(wp, start, end, contextStart, contextEnd, runIsRtl, decorationEnd);
                    if (runIsRtl) {
                        float decorationXLeft2 = rightX - decorationEndAdvance;
                        decorationXLeft = decorationXLeft2;
                        decorationXRight = rightX - decorationStartAdvance;
                    } else {
                        float decorationXLeft3 = leftX + decorationStartAdvance;
                        decorationXLeft = decorationXLeft3;
                        decorationXRight = leftX + decorationEndAdvance;
                    }
                    if (info.underlineColor != 0) {
                        drawStroke(wp, c, info.underlineColor, wp.getUnderlinePosition(), info.underlineThickness, decorationXLeft, decorationXRight, y);
                    }
                    if (!info.isUnderlineText) {
                        f = 1.0f;
                    } else {
                        float thickness = Math.max(wp.getUnderlineThickness(), 1.0f);
                        f = 1.0f;
                        drawStroke(wp, c, wp.getColor(), wp.getUnderlinePosition(), thickness, decorationXLeft, decorationXRight, y);
                    }
                    if (info.isStrikeThruText) {
                        float thickness2 = Math.max(wp.getStrikeThruThickness(), f);
                        drawStroke(wp, c, wp.getColor(), wp.getStrikeThruPosition(), thickness2, decorationXLeft, decorationXRight, y);
                    }
                    i++;
                    numDecorations = numDecorations3;
                }
            }
        }
        return runIsRtl ? -totalWidth : totalWidth;
    }

    private float handleReplacement(ReplacementSpan replacement, TextPaint wp, int start, int limit, boolean runIsRtl, Canvas c, float x, int top, int y, int bottom, Paint.FontMetricsInt fmi, boolean needWidth) {
        int previousTop;
        int previousAscent;
        int previousDescent;
        int previousBottom;
        int previousLeading;
        float x2;
        float ret = 0.0f;
        int i = this.mStart;
        int textStart = i + start;
        int textLimit = i + limit;
        if (needWidth || (c != null && runIsRtl)) {
            boolean needUpdateMetrics = fmi != null;
            if (!needUpdateMetrics) {
                previousTop = 0;
                previousAscent = 0;
                previousDescent = 0;
                previousBottom = 0;
                previousLeading = 0;
            } else {
                int previousTop2 = fmi.top;
                int previousAscent2 = fmi.ascent;
                int previousDescent2 = fmi.descent;
                int previousBottom2 = fmi.bottom;
                int previousLeading2 = fmi.leading;
                previousTop = previousTop2;
                previousAscent = previousAscent2;
                previousDescent = previousDescent2;
                previousBottom = previousBottom2;
                previousLeading = previousLeading2;
            }
            ret = replacement.getSize(wp, this.mText, textStart, textLimit, fmi);
            if (needUpdateMetrics) {
                updateMetrics(fmi, previousTop, previousAscent, previousDescent, previousBottom, previousLeading);
            }
        }
        float ret2 = ret;
        if (c != null) {
            if (!runIsRtl) {
                x2 = x;
            } else {
                x2 = x - ret2;
            }
            replacement.draw(c, this.mText, textStart, textLimit, x2, top, y, bottom, wp);
        }
        return runIsRtl ? -ret2 : ret2;
    }

    private int adjustStartHyphenEdit(int start, int startHyphenEdit) {
        if (start > 0) {
            return 0;
        }
        return startHyphenEdit;
    }

    private int adjustEndHyphenEdit(int limit, int endHyphenEdit) {
        if (limit < this.mLen) {
            return 0;
        }
        return endHyphenEdit;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static final class DecorationInfo {
        public int end;
        public boolean isStrikeThruText;
        public boolean isUnderlineText;
        public int start;
        public int underlineColor;
        public float underlineThickness;

        private DecorationInfo() {
            this.start = -1;
            this.end = -1;
        }

        public boolean hasDecoration() {
            return this.isStrikeThruText || this.isUnderlineText || this.underlineColor != 0;
        }

        public DecorationInfo copyInfo() {
            DecorationInfo copy = new DecorationInfo();
            copy.isStrikeThruText = this.isStrikeThruText;
            copy.isUnderlineText = this.isUnderlineText;
            copy.underlineColor = this.underlineColor;
            copy.underlineThickness = this.underlineThickness;
            return copy;
        }
    }

    private void extractDecorationInfo(TextPaint paint, DecorationInfo info) {
        info.isStrikeThruText = paint.isStrikeThruText();
        if (info.isStrikeThruText) {
            paint.setStrikeThruText(false);
        }
        info.isUnderlineText = paint.isUnderlineText();
        if (info.isUnderlineText) {
            paint.setUnderlineText(false);
        }
        info.underlineColor = paint.underlineColor;
        info.underlineThickness = paint.underlineThickness;
        paint.setUnderlineText(0, 0.0f);
    }

    private float handleRun(int start, int measureLimit, int limit, boolean runIsRtl, Canvas c, TextShaper.GlyphsConsumer consumer, float x, int top, int y, int bottom, Paint.FontMetricsInt fmi, boolean needWidth) {
        boolean needsSpanMeasurement;
        int inext;
        int mlimit;
        boolean z;
        int jnext;
        int mlimit2;
        int j;
        DecorationInfo decorationInfo;
        int activeStart;
        int i;
        TextPaint wp;
        TextPaint activePaint;
        TextLine textLine;
        int i2;
        TextPaint wp2;
        boolean z2;
        int jnext2;
        if (measureLimit < start || measureLimit > limit) {
            throw new IndexOutOfBoundsException("measureLimit (" + measureLimit + ") is out of start (" + start + ") and limit (" + limit + ") bounds");
        }
        if (start == measureLimit) {
            TextPaint wp3 = this.mWorkPaint;
            wp3.set(this.mPaint);
            if (fmi != null) {
                expandMetricsFromPaint(fmi, wp3);
                return 0.0f;
            }
            return 0.0f;
        }
        Spanned spanned = this.mSpanned;
        if (spanned == null) {
            needsSpanMeasurement = false;
        } else {
            SpanSet<MetricAffectingSpan> spanSet = this.mMetricAffectingSpanSpanSet;
            int i3 = this.mStart;
            spanSet.init(spanned, i3 + start, i3 + limit);
            SpanSet<CharacterStyle> spanSet2 = this.mCharacterStyleSpanSet;
            Spanned spanned2 = this.mSpanned;
            int i4 = this.mStart;
            spanSet2.init(spanned2, i4 + start, i4 + limit);
            needsSpanMeasurement = (this.mMetricAffectingSpanSpanSet.numberOfSpans == 0 && this.mCharacterStyleSpanSet.numberOfSpans == 0) ? false : true;
        }
        if (!needsSpanMeasurement) {
            TextPaint wp4 = this.mWorkPaint;
            wp4.set(this.mPaint);
            wp4.setStartHyphenEdit(adjustStartHyphenEdit(start, wp4.getStartHyphenEdit()));
            wp4.setEndHyphenEdit(adjustEndHyphenEdit(limit, wp4.getEndHyphenEdit()));
            return handleText(wp4, start, limit, start, limit, runIsRtl, c, consumer, x, top, y, bottom, fmi, needWidth, measureLimit, null);
        }
        float x2 = x;
        int i5 = start;
        while (i5 < measureLimit) {
            TextPaint wp5 = this.mWorkPaint;
            wp5.set(this.mPaint);
            SpanSet<MetricAffectingSpan> spanSet3 = this.mMetricAffectingSpanSpanSet;
            int i6 = this.mStart;
            int inext2 = spanSet3.getNextTransition(i6 + i5, i6 + limit) - this.mStart;
            int mlimit3 = Math.min(inext2, measureLimit);
            ReplacementSpan replacement = null;
            for (int j2 = 0; j2 < this.mMetricAffectingSpanSpanSet.numberOfSpans; j2++) {
                if (this.mMetricAffectingSpanSpanSet.spanStarts[j2] < this.mStart + mlimit3) {
                    int i7 = this.mMetricAffectingSpanSpanSet.spanEnds[j2];
                    int i8 = this.mStart;
                    if (i7 > i8 + i5) {
                        boolean insideEllipsis = i8 + this.mEllipsisStart <= this.mMetricAffectingSpanSpanSet.spanStarts[j2] && this.mMetricAffectingSpanSpanSet.spanEnds[j2] <= this.mStart + this.mEllipsisEnd;
                        MetricAffectingSpan span = this.mMetricAffectingSpanSpanSet.spans[j2];
                        if (span instanceof ReplacementSpan) {
                            replacement = !insideEllipsis ? (ReplacementSpan) span : null;
                        } else {
                            span.updateDrawState(wp5);
                        }
                    }
                }
            }
            if (replacement != null) {
                inext = inext2;
                x2 += handleReplacement(replacement, wp5, i5, mlimit3, runIsRtl, c, x2, top, y, bottom, fmi, needWidth || mlimit3 < measureLimit);
            } else {
                int mlimit4 = mlimit3;
                inext = inext2;
                TextPaint wp6 = wp5;
                TextLine textLine2 = this;
                TextPaint activePaint2 = textLine2.mActivePaint;
                activePaint2.set(textLine2.mPaint);
                int activeStart2 = i5;
                DecorationInfo decorationInfo2 = textLine2.mDecorationInfo;
                textLine2.mDecorations.clear();
                int activeEnd = mlimit4;
                int j3 = i5;
                float x3 = x2;
                int activeStart3 = activeStart2;
                while (true) {
                    mlimit = mlimit4;
                    if (j3 >= mlimit) {
                        break;
                    }
                    SpanSet<CharacterStyle> spanSet4 = textLine2.mCharacterStyleSpanSet;
                    int i9 = textLine2.mStart;
                    int jnext3 = spanSet4.getNextTransition(i9 + j3, i9 + inext) - textLine2.mStart;
                    int offset = Math.min(jnext3, mlimit);
                    TextPaint wp7 = wp6;
                    wp7.set(textLine2.mPaint);
                    for (int k = 0; k < textLine2.mCharacterStyleSpanSet.numberOfSpans; k++) {
                        if (textLine2.mCharacterStyleSpanSet.spanStarts[k] < textLine2.mStart + offset && textLine2.mCharacterStyleSpanSet.spanEnds[k] > textLine2.mStart + j3) {
                            textLine2.mCharacterStyleSpanSet.spans[k].updateDrawState(wp7);
                        }
                    }
                    textLine2.extractDecorationInfo(wp7, decorationInfo2);
                    if (j3 == i5) {
                        activePaint2.set(wp7);
                        jnext = jnext3;
                        mlimit2 = mlimit;
                        j = j3;
                        decorationInfo = decorationInfo2;
                        activeStart = activeStart3;
                        i = i5;
                        wp = wp7;
                        activePaint = activePaint2;
                        textLine = textLine2;
                    } else if (equalAttributes(wp7, activePaint2)) {
                        jnext = jnext3;
                        mlimit2 = mlimit;
                        j = j3;
                        decorationInfo = decorationInfo2;
                        activeStart = activeStart3;
                        i = i5;
                        wp = wp7;
                        activePaint = activePaint2;
                        textLine = textLine2;
                    } else {
                        activePaint2.setStartHyphenEdit(textLine2.adjustStartHyphenEdit(activeStart3, textLine2.mPaint.getStartHyphenEdit()));
                        activePaint2.setEndHyphenEdit(textLine2.adjustEndHyphenEdit(activeEnd, textLine2.mPaint.getEndHyphenEdit()));
                        if (needWidth) {
                            i2 = measureLimit;
                            wp2 = wp7;
                        } else {
                            i2 = measureLimit;
                            wp2 = wp7;
                            if (activeEnd >= i2) {
                                z2 = false;
                                jnext = jnext3;
                                mlimit2 = mlimit;
                                j = j3;
                                decorationInfo = decorationInfo2;
                                i = i5;
                                x3 += handleText(activePaint2, activeStart3, activeEnd, i5, inext, runIsRtl, c, consumer, x3, top, y, bottom, fmi, z2, Math.min(activeEnd, mlimit), textLine2.mDecorations);
                                wp = wp2;
                                activePaint = activePaint2;
                                activePaint.set(wp);
                                textLine = this;
                                textLine.mDecorations.clear();
                                activeStart = j;
                            }
                        }
                        z2 = true;
                        jnext = jnext3;
                        mlimit2 = mlimit;
                        j = j3;
                        decorationInfo = decorationInfo2;
                        i = i5;
                        x3 += handleText(activePaint2, activeStart3, activeEnd, i5, inext, runIsRtl, c, consumer, x3, top, y, bottom, fmi, z2, Math.min(activeEnd, mlimit), textLine2.mDecorations);
                        wp = wp2;
                        activePaint = activePaint2;
                        activePaint.set(wp);
                        textLine = this;
                        textLine.mDecorations.clear();
                        activeStart = j;
                    }
                    activeEnd = jnext;
                    if (!decorationInfo.hasDecoration()) {
                        jnext2 = jnext;
                    } else {
                        DecorationInfo copy = decorationInfo.copyInfo();
                        copy.start = j;
                        jnext2 = jnext;
                        copy.end = jnext2;
                        textLine.mDecorations.add(copy);
                    }
                    j3 = jnext2;
                    textLine2 = textLine;
                    activePaint2 = activePaint;
                    wp6 = wp;
                    mlimit4 = mlimit2;
                    decorationInfo2 = decorationInfo;
                    activeStart3 = activeStart;
                    i5 = i;
                }
                int activeStart4 = activeStart3;
                int i10 = i5;
                TextPaint activePaint3 = activePaint2;
                TextLine textLine3 = textLine2;
                activePaint3.setStartHyphenEdit(textLine3.adjustStartHyphenEdit(activeStart4, textLine3.mPaint.getStartHyphenEdit()));
                activePaint3.setEndHyphenEdit(textLine3.adjustEndHyphenEdit(activeEnd, textLine3.mPaint.getEndHyphenEdit()));
                if (!needWidth && activeEnd >= measureLimit) {
                    z = false;
                    x2 = x3 + handleText(activePaint3, activeStart4, activeEnd, i10, inext, runIsRtl, c, consumer, x3, top, y, bottom, fmi, z, Math.min(activeEnd, mlimit), textLine3.mDecorations);
                }
                z = true;
                x2 = x3 + handleText(activePaint3, activeStart4, activeEnd, i10, inext, runIsRtl, c, consumer, x3, top, y, bottom, fmi, z, Math.min(activeEnd, mlimit), textLine3.mDecorations);
            }
            i5 = inext;
        }
        return x2 - x;
    }

    private void drawTextRun(Canvas c, TextPaint wp, int start, int end, int contextStart, int contextEnd, boolean runIsRtl, float x, int y) {
        if (this.mCharsValid) {
            int count = end - start;
            int contextCount = contextEnd - contextStart;
            c.drawTextRun(this.mChars, start, count, contextStart, contextCount, x, y, runIsRtl, wp);
            return;
        }
        int delta = this.mStart;
        c.drawTextRun(this.mText, delta + start, delta + end, delta + contextStart, delta + contextEnd, x, y, runIsRtl, wp);
    }

    private void shapeTextRun(TextShaper.GlyphsConsumer consumer, TextPaint paint, int start, int end, int contextStart, int contextEnd, boolean runIsRtl, float x) {
        PositionedGlyphs glyphs;
        int count = end - start;
        int contextCount = contextEnd - contextStart;
        if (this.mCharsValid) {
            glyphs = TextRunShaper.shapeTextRun(this.mChars, start, count, contextStart, contextCount, x, 0.0f, runIsRtl, paint);
        } else {
            CharSequence charSequence = this.mText;
            int i = this.mStart;
            glyphs = TextRunShaper.shapeTextRun(charSequence, i + start, count, i + contextStart, contextCount, x, 0.0f, runIsRtl, paint);
        }
        consumer.accept(start, count, glyphs, paint);
    }

    float nextTab(float h) {
        Layout.TabStops tabStops = this.mTabs;
        if (tabStops != null) {
            return tabStops.nextTab(h);
        }
        return Layout.TabStops.nextDefaultStop(h, 20.0f);
    }

    private boolean isStretchableWhitespace(int ch) {
        return ch == 32;
    }

    private int countStretchableSpaces(int start, int end) {
        int count = 0;
        for (int i = start; i < end; i++) {
            char c = this.mCharsValid ? this.mChars[i] : this.mText.charAt(this.mStart + i);
            if (isStretchableWhitespace(c)) {
                count++;
            }
        }
        return count;
    }

    public static boolean isLineEndSpace(char ch) {
        return ch == ' ' || ch == '\t' || ch == 5760 || (8192 <= ch && ch <= 8202 && ch != 8199) || ch == 8287 || ch == 12288;
    }

    private static boolean equalAttributes(TextPaint lp, TextPaint rp) {
        return lp.getColorFilter() == rp.getColorFilter() && lp.getMaskFilter() == rp.getMaskFilter() && lp.getShader() == rp.getShader() && lp.getTypeface() == rp.getTypeface() && lp.getXfermode() == rp.getXfermode() && lp.getTextLocales().equals(rp.getTextLocales()) && TextUtils.equals(lp.getFontFeatureSettings(), rp.getFontFeatureSettings()) && TextUtils.equals(lp.getFontVariationSettings(), rp.getFontVariationSettings()) && lp.getShadowLayerRadius() == rp.getShadowLayerRadius() && lp.getShadowLayerDx() == rp.getShadowLayerDx() && lp.getShadowLayerDy() == rp.getShadowLayerDy() && lp.getShadowLayerColor() == rp.getShadowLayerColor() && lp.getFlags() == rp.getFlags() && lp.getHinting() == rp.getHinting() && lp.getStyle() == rp.getStyle() && lp.getColor() == rp.getColor() && lp.getStrokeWidth() == rp.getStrokeWidth() && lp.getStrokeMiter() == rp.getStrokeMiter() && lp.getStrokeCap() == rp.getStrokeCap() && lp.getStrokeJoin() == rp.getStrokeJoin() && lp.getTextAlign() == rp.getTextAlign() && lp.isElegantTextHeight() == rp.isElegantTextHeight() && lp.getTextSize() == rp.getTextSize() && lp.getTextScaleX() == rp.getTextScaleX() && lp.getTextSkewX() == rp.getTextSkewX() && lp.getLetterSpacing() == rp.getLetterSpacing() && lp.getWordSpacing() == rp.getWordSpacing() && lp.getStartHyphenEdit() == rp.getStartHyphenEdit() && lp.getEndHyphenEdit() == rp.getEndHyphenEdit() && lp.bgColor == rp.bgColor && lp.baselineShift == rp.baselineShift && lp.linkColor == rp.linkColor && lp.drawableState == rp.drawableState && lp.density == rp.density && lp.underlineColor == rp.underlineColor && lp.underlineThickness == rp.underlineThickness;
    }
}
