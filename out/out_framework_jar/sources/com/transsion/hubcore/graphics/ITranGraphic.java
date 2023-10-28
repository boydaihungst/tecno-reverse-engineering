package com.transsion.hubcore.graphics;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.LongSparseArray;
import android.util.SparseArray;
import com.transsion.hubcore.utils.TranClassInfo;
import java.io.PrintWriter;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranGraphic {
    public static final TranClassInfo<ITranGraphic> classInfo = new TranClassInfo<>("com.transsion.hubcore.graphics.TranGraphicImpl", ITranGraphic.class, new Supplier() { // from class: com.transsion.hubcore.graphics.ITranGraphic$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranGraphic.lambda$static$0();
        }
    });

    static /* synthetic */ ITranGraphic lambda$static$0() {
        return new ITranGraphic() { // from class: com.transsion.hubcore.graphics.ITranGraphic.1
        };
    }

    static ITranGraphic Instance() {
        return classInfo.getImpl();
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
