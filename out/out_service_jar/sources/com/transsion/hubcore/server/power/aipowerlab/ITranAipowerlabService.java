package com.transsion.hubcore.server.power.aipowerlab;

import android.content.Context;
import com.transsion.hubcore.server.power.aipowerlab.ITranAipowerlabService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAipowerlabService {
    public static final String IMPL_CLASS_NAME = "com.transsion.hubcore.server.power.aipowerlab.TranAipowerlabServiceImpl";
    public static final TranClassInfo<ITranAipowerlabService> classInfo = new TranClassInfo<>(IMPL_CLASS_NAME, ITranAipowerlabService.class, new Supplier() { // from class: com.transsion.hubcore.server.power.aipowerlab.ITranAipowerlabService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAipowerlabService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranAipowerlabService {
    }

    static ITranAipowerlabService Instance() {
        return (ITranAipowerlabService) classInfo.getImpl();
    }

    default void init(Context context) {
    }

    default void enable(boolean enable) {
    }

    default void systemReady() {
    }

    default void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
    }
}
