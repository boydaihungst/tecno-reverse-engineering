package com.android.server.location.listeners;

import android.location.util.identity.CallerIdentity;
import android.os.Process;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.FgThread;
import java.util.Objects;
import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public abstract class RemoteListenerRegistration<TRequest, TListener> extends RemovableListenerRegistration<TRequest, TListener> {
    public static final Executor IN_PROCESS_EXECUTOR = FgThread.getExecutor();
    private final CallerIdentity mIdentity;

    private static Executor chooseExecutor(CallerIdentity identity) {
        if (identity.getPid() == Process.myPid()) {
            return IN_PROCESS_EXECUTOR;
        }
        return ConcurrentUtils.DIRECT_EXECUTOR;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public RemoteListenerRegistration(TRequest request, CallerIdentity identity, TListener listener) {
        super(chooseExecutor(identity), request, listener);
        this.mIdentity = (CallerIdentity) Objects.requireNonNull(identity);
    }

    public final CallerIdentity getIdentity() {
        return this.mIdentity;
    }
}
