package com.transsion.hubcore.util;

import com.transsion.hubcore.util.ITranAtomFile;
import com.transsion.hubcore.utils.TranClassInfo;
import java.io.File;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranAtomFile {
    public static final TranClassInfo<ITranAtomFile> classInfo = new TranClassInfo<>("com.transsion.hubcore.util.TranAtomFileImpl", ITranAtomFile.class, new Supplier() { // from class: com.transsion.hubcore.util.ITranAtomFile$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAtomFile.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements ITranAtomFile {
    }

    static ITranAtomFile Instance() {
        return classInfo.getImpl();
    }

    default void verifyFileBeforeFinishWrite(File file) {
    }

    default void verifyFileBeforeFinishRead(File file) {
    }
}
