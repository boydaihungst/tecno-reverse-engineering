package com.android.server.location.listeners;

import android.location.util.identity.CallerIdentity;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.listeners.ListenerExecutor;
/* loaded from: classes.dex */
public abstract class BinderListenerRegistration<TRequest, TListener> extends RemoteListenerRegistration<TRequest, TListener> implements IBinder.DeathRecipient {

    /* loaded from: classes.dex */
    public interface BinderKey {
        IBinder getBinder();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public BinderListenerRegistration(TRequest request, CallerIdentity callerIdentity, TListener listener) {
        super(request, callerIdentity, listener);
    }

    @Override // com.android.server.location.listeners.RemovableListenerRegistration
    protected final void onRemovableListenerRegister() {
        IBinder binder = getBinderFromKey(getKey());
        try {
            binder.linkToDeath(this, 0);
        } catch (RemoteException e) {
            remove();
        }
        onBinderListenerRegister();
    }

    @Override // com.android.server.location.listeners.RemovableListenerRegistration
    protected final void onRemovableListenerUnregister() {
        onBinderListenerUnregister();
        getBinderFromKey(getKey()).unlinkToDeath(this, 0);
    }

    protected void onBinderListenerRegister() {
    }

    protected void onBinderListenerUnregister() {
    }

    @Override // com.android.server.location.listeners.ListenerRegistration
    public void onOperationFailure(ListenerExecutor.ListenerOperation<TListener> operation, Exception e) {
        if (e instanceof RemoteException) {
            Log.w(getOwner().getTag(), "registration " + this + " removed", e);
            remove();
            return;
        }
        super.onOperationFailure(operation, e);
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        try {
            if (Log.isLoggable(getOwner().getTag(), 3)) {
                Log.d(getOwner().getTag(), "binder registration " + getIdentity() + " died");
            }
            remove();
        } catch (RuntimeException e) {
            throw new AssertionError(e);
        }
    }

    private static IBinder getBinderFromKey(Object key) {
        if (key instanceof IBinder) {
            return (IBinder) key;
        }
        if (key instanceof BinderKey) {
            return ((BinderKey) key).getBinder();
        }
        throw new IllegalArgumentException("key must be IBinder or BinderKey");
    }
}
