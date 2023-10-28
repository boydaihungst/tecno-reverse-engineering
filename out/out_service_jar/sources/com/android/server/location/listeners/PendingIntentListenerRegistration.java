package com.android.server.location.listeners;

import android.app.PendingIntent;
import android.location.util.identity.CallerIdentity;
import android.util.Log;
import com.android.internal.listeners.ListenerExecutor;
/* loaded from: classes.dex */
public abstract class PendingIntentListenerRegistration<TRequest, TListener> extends RemoteListenerRegistration<TRequest, TListener> implements PendingIntent.CancelListener {

    /* loaded from: classes.dex */
    public interface PendingIntentKey {
        PendingIntent getPendingIntent();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public PendingIntentListenerRegistration(TRequest request, CallerIdentity callerIdentity, TListener listener) {
        super(request, callerIdentity, listener);
    }

    @Override // com.android.server.location.listeners.RemovableListenerRegistration
    protected final void onRemovableListenerRegister() {
        getPendingIntentFromKey(getKey()).registerCancelListener(this);
        onPendingIntentListenerRegister();
    }

    @Override // com.android.server.location.listeners.RemovableListenerRegistration
    protected final void onRemovableListenerUnregister() {
        onPendingIntentListenerUnregister();
        getPendingIntentFromKey(getKey()).unregisterCancelListener(this);
    }

    protected void onPendingIntentListenerRegister() {
    }

    protected void onPendingIntentListenerUnregister() {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerRegistration
    public void onOperationFailure(ListenerExecutor.ListenerOperation<TListener> operation, Exception e) {
        if (e instanceof PendingIntent.CanceledException) {
            Log.w(getOwner().getTag(), "registration " + this + " removed", e);
            remove();
            return;
        }
        super.onOperationFailure(operation, e);
    }

    public void onCanceled(PendingIntent intent) {
        if (Log.isLoggable(getOwner().getTag(), 3)) {
            Log.d(getOwner().getTag(), "pending intent registration " + getIdentity() + " canceled");
        }
        remove();
    }

    private PendingIntent getPendingIntentFromKey(Object key) {
        if (key instanceof PendingIntent) {
            return (PendingIntent) key;
        }
        if (key instanceof PendingIntentKey) {
            return ((PendingIntentKey) key).getPendingIntent();
        }
        throw new IllegalArgumentException("key must be PendingIntent or PendingIntentKey");
    }
}
