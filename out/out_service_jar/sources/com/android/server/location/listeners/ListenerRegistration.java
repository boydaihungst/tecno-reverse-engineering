package com.android.server.location.listeners;

import com.android.internal.listeners.ListenerExecutor;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class ListenerRegistration<TListener> implements ListenerExecutor {
    private boolean mActive = false;
    private final Executor mExecutor;
    private volatile TListener mListener;

    /* JADX INFO: Access modifiers changed from: protected */
    public ListenerRegistration(Executor executor, TListener listener) {
        this.mExecutor = (Executor) Objects.requireNonNull(executor);
        this.mListener = (TListener) Objects.requireNonNull(listener);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final Executor getExecutor() {
        return this.mExecutor;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onRegister(Object key) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onUnregister() {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onActive() {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onInactive() {
    }

    public final boolean isActive() {
        return this.mActive;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final boolean setActive(boolean active) {
        if (active != this.mActive) {
            this.mActive = active;
            return true;
        }
        return false;
    }

    public final boolean isRegistered() {
        return this.mListener != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void unregisterInternal() {
        this.mListener = null;
        onListenerUnregister();
    }

    protected void onListenerUnregister() {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onOperationFailure(ListenerExecutor.ListenerOperation<TListener> operation, Exception exception) {
        throw new AssertionError(exception);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void executeOperation(ListenerExecutor.ListenerOperation<TListener> operation) {
        executeSafely(this.mExecutor, new Supplier() { // from class: com.android.server.location.listeners.ListenerRegistration$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return ListenerRegistration.this.m4513x74a9b0bc();
            }
        }, operation, new ListenerExecutor.FailureCallback() { // from class: com.android.server.location.listeners.ListenerRegistration$$ExternalSyntheticLambda1
            public final void onFailure(ListenerExecutor.ListenerOperation listenerOperation, Exception exc) {
                ListenerRegistration.this.onOperationFailure(listenerOperation, exc);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$executeOperation$0$com-android-server-location-listeners-ListenerRegistration  reason: not valid java name */
    public /* synthetic */ Object m4513x74a9b0bc() {
        return this.mListener;
    }

    public String toString() {
        return "[]";
    }

    public final boolean equals(Object obj) {
        return this == obj;
    }

    public final int hashCode() {
        return super.hashCode();
    }
}
