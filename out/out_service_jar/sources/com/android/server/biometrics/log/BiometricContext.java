package com.android.server.biometrics.log;

import android.content.Context;
import android.hardware.biometrics.common.OperationContext;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public interface BiometricContext {
    Integer getBiometricPromptSessionId();

    Integer getKeyguardEntrySessionId();

    boolean isAod();

    void subscribe(OperationContext operationContext, Consumer<OperationContext> consumer);

    void unsubscribe(OperationContext operationContext);

    OperationContext updateContext(OperationContext operationContext, boolean z);

    static BiometricContext getInstance(Context context) {
        return BiometricContextProvider.defaultProvider(context);
    }
}
