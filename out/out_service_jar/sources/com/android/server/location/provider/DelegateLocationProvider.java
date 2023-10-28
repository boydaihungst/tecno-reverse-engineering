package com.android.server.location.provider;

import android.location.LocationResult;
import android.location.provider.ProviderRequest;
import android.os.Bundle;
import com.android.internal.util.Preconditions;
import com.android.server.location.provider.AbstractLocationProvider;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.function.UnaryOperator;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class DelegateLocationProvider extends AbstractLocationProvider implements AbstractLocationProvider.Listener {
    protected final AbstractLocationProvider mDelegate;
    private final Object mInitializationLock;
    private boolean mInitialized;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DelegateLocationProvider(Executor executor, AbstractLocationProvider delegate) {
        super(executor, null, null, Collections.emptySet());
        this.mInitializationLock = new Object();
        this.mInitialized = false;
        this.mDelegate = delegate;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void initializeDelegate() {
        synchronized (this.mInitializationLock) {
            Preconditions.checkState(!this.mInitialized);
            setState(new UnaryOperator() { // from class: com.android.server.location.provider.DelegateLocationProvider$$ExternalSyntheticLambda1
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return DelegateLocationProvider.this.m4518xaa7f6389((AbstractLocationProvider.State) obj);
                }
            });
            this.mInitialized = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initializeDelegate$0$com-android-server-location-provider-DelegateLocationProvider  reason: not valid java name */
    public /* synthetic */ AbstractLocationProvider.State m4518xaa7f6389(AbstractLocationProvider.State previousState) {
        return this.mDelegate.getController().setListener(this);
    }

    protected final void waitForInitialization() {
        synchronized (this.mInitializationLock) {
            Preconditions.checkState(this.mInitialized);
        }
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider.Listener
    public void onStateChanged(AbstractLocationProvider.State oldState, final AbstractLocationProvider.State newState) {
        waitForInitialization();
        setState(new UnaryOperator() { // from class: com.android.server.location.provider.DelegateLocationProvider$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return DelegateLocationProvider.lambda$onStateChanged$1(AbstractLocationProvider.State.this, (AbstractLocationProvider.State) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ AbstractLocationProvider.State lambda$onStateChanged$1(AbstractLocationProvider.State newState, AbstractLocationProvider.State previousState) {
        return newState;
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider.Listener
    public void onReportLocation(LocationResult locationResult) {
        waitForInitialization();
        reportLocation(locationResult);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void onStart() {
        Preconditions.checkState(this.mInitialized);
        this.mDelegate.getController().start();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void onStop() {
        Preconditions.checkState(this.mInitialized);
        this.mDelegate.getController().stop();
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    protected void onSetRequest(ProviderRequest request) {
        Preconditions.checkState(this.mInitialized);
        this.mDelegate.getController().setRequest(request);
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    protected void onFlush(Runnable callback) {
        Preconditions.checkState(this.mInitialized);
        this.mDelegate.getController().flush(callback);
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    protected void onExtraCommand(int uid, int pid, String command, Bundle extras) {
        Preconditions.checkState(this.mInitialized);
        this.mDelegate.getController().sendExtraCommand(uid, pid, command, extras);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        Preconditions.checkState(this.mInitialized);
        this.mDelegate.dump(fd, pw, args);
    }
}
