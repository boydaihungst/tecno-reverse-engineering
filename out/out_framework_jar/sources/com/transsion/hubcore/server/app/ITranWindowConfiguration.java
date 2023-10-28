package com.transsion.hubcore.server.app;

import android.app.WindowConfiguration;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranWindowConfiguration {
    public static final TranClassInfo<ITranWindowConfiguration> classInfo = new TranClassInfo<>("com.transsion.hubcore.app.TranWindowConfigurationImpl", ITranWindowConfiguration.class, new Supplier() { // from class: com.transsion.hubcore.server.app.ITranWindowConfiguration$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranWindowConfiguration.lambda$static$0();
        }
    });

    static /* synthetic */ ITranWindowConfiguration lambda$static$0() {
        return new ITranWindowConfiguration() { // from class: com.transsion.hubcore.server.app.ITranWindowConfiguration.1
        };
    }

    static ITranWindowConfiguration Instance() {
        return classInfo.getImpl();
    }

    default boolean isThunderbackWindow(WindowConfiguration configuration) {
        return false;
    }

    default boolean isThunderbackWindowInteractive(WindowConfiguration configuration) {
        return false;
    }

    default boolean isThunderbackWindowNonInteractive(WindowConfiguration configuration) {
        return false;
    }

    default String multiWindowModeToString(int multiWindowMode) {
        return "";
    }

    default void setMultiWindowMode(WindowConfiguration configuration, int multiWindowMode) {
    }

    default void setMultiWindowId(WindowConfiguration configuration, int multiWindowId) {
    }

    default void setInLargeScreen(WindowConfiguration configuration, int inLargeScreen) {
    }

    default void setWindowResizable(WindowConfiguration configuration, int windowResiable) {
    }

    default int updateFrom(WindowConfiguration configuration, WindowConfiguration delta, int changed) {
        return -1;
    }

    default void setTo(WindowConfiguration configuration, WindowConfiguration delta, int mask) {
    }

    default long diff(WindowConfiguration other, WindowConfiguration configuration, boolean compareUndefined, long changes) {
        return -1L;
    }

    default int compareTo(WindowConfiguration configuration, WindowConfiguration that) {
        return 0;
    }
}
