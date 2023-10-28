package com.android.server.location.listeners;

import java.util.Objects;
import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public abstract class RemovableListenerRegistration<TRequest, TListener> extends RequestListenerRegistration<TRequest, TListener> {
    private volatile Object mKey;

    /* JADX INFO: Access modifiers changed from: protected */
    public abstract ListenerMultiplexer<?, ? super TListener, ?, ?> getOwner();

    /* JADX INFO: Access modifiers changed from: protected */
    public RemovableListenerRegistration(Executor executor, TRequest request, TListener listener) {
        super(executor, request, listener);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final Object getKey() {
        return Objects.requireNonNull(this.mKey);
    }

    public final void remove() {
        Object key = this.mKey;
        if (key != null) {
            getOwner().removeRegistration(key, this);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerRegistration
    public final void onRegister(Object key) {
        this.mKey = Objects.requireNonNull(key);
        onRemovableListenerRegister();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerRegistration
    public final void onUnregister() {
        onRemovableListenerUnregister();
        this.mKey = null;
    }

    protected void onRemovableListenerRegister() {
    }

    protected void onRemovableListenerUnregister() {
    }
}
