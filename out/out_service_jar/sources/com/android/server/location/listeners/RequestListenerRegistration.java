package com.android.server.location.listeners;

import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public class RequestListenerRegistration<TRequest, TListener> extends ListenerRegistration<TListener> {
    private final TRequest mRequest;

    /* JADX INFO: Access modifiers changed from: protected */
    public RequestListenerRegistration(Executor executor, TRequest request, TListener listener) {
        super(executor, listener);
        this.mRequest = request;
    }

    public TRequest getRequest() {
        return this.mRequest;
    }

    @Override // com.android.server.location.listeners.ListenerRegistration
    public String toString() {
        TRequest trequest = this.mRequest;
        if (trequest == null) {
            return "[]";
        }
        return trequest.toString();
    }
}
