package android.graphics;

import android.graphics.IGraphicLice;
import android.graphics.Paint;
import android.util.LongSparseArray;
import android.util.SparseArray;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.io.PrintWriter;
import java.util.Map;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_FRAMEWORK)
/* loaded from: classes.dex */
public interface IGraphicLice {
    public static final LiceInfo<IGraphicLice> sLiceInfo = new LiceInfo<>("com.transsion.graphics.GraphicLice", IGraphicLice.class, new Supplier() { // from class: android.graphics.IGraphicLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IGraphicLice.DefaultImpl();
        }
    });

    /* loaded from: classes.dex */
    public static class DefaultImpl implements IGraphicLice {
    }

    static IGraphicLice Instance() {
        return sLiceInfo.getImpl();
    }

    default void onGetNativeInstance(Paint paint, long nativePaint) {
    }

    default void onGetFontMetrics(Paint paint, Paint.FontMetrics metrics) {
    }

    default Paint onPaintSet(Paint target, Paint src) {
        return src;
    }

    default void onGetFontMetricsInt(Paint paint, Paint.FontMetricsInt fmi) {
    }

    default void onInitSystemDefaultTypefaces(Map<String, Typeface> systemFontMap) {
    }

    default Typeface onCreateStyleTypeface(LongSparseArray<SparseArray<Typeface>> styledTypefaceCache, Typeface base, int style) {
        return null;
    }

    default Typeface onCreateWeightStyleTypeface(LongSparseArray<SparseArray<Typeface>> weightTypefaceCache, Typeface base, int key, int weight, boolean italic) {
        return null;
    }

    default void dumpTypeface(PrintWriter pw) {
    }

    default void onSetTypeface(Paint paint, long typefaceNative) {
    }

    default void onPaintAscent(Paint paint) {
    }

    default void onPaintMeasureText(Paint paint) {
    }

    default void onGetRunAdvance(Paint paint) {
    }
}
