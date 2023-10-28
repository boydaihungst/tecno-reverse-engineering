package com.android.server.location.provider;

import android.location.Location;
import android.location.LocationResult;
import android.location.provider.ProviderRequest;
import android.os.Bundle;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.Preconditions;
import com.android.server.location.provider.AbstractLocationProvider;
import com.android.server.location.provider.MockableLocationProvider;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.function.UnaryOperator;
/* loaded from: classes.dex */
public class MockableLocationProvider extends AbstractLocationProvider {
    private MockLocationProvider mMockProvider;
    final Object mOwnerLock;
    private AbstractLocationProvider mProvider;
    private AbstractLocationProvider mRealProvider;
    private ProviderRequest mRequest;
    private boolean mStarted;

    public MockableLocationProvider(Object ownerLock) {
        super(ConcurrentUtils.DIRECT_EXECUTOR, null, null, Collections.emptySet());
        this.mOwnerLock = ownerLock;
        this.mRequest = ProviderRequest.EMPTY_REQUEST;
    }

    public AbstractLocationProvider getProvider() {
        AbstractLocationProvider abstractLocationProvider;
        synchronized (this.mOwnerLock) {
            abstractLocationProvider = this.mProvider;
        }
        return abstractLocationProvider;
    }

    public void setRealProvider(AbstractLocationProvider provider) {
        synchronized (this.mOwnerLock) {
            if (this.mRealProvider == provider) {
                return;
            }
            this.mRealProvider = provider;
            if (!isMock()) {
                setProviderLocked(this.mRealProvider);
            }
        }
    }

    public void setMockProvider(MockLocationProvider provider) {
        synchronized (this.mOwnerLock) {
            if (this.mMockProvider == provider) {
                return;
            }
            this.mMockProvider = provider;
            if (provider != null) {
                setProviderLocked(provider);
            } else {
                setProviderLocked(this.mRealProvider);
            }
        }
    }

    private void setProviderLocked(AbstractLocationProvider provider) {
        final AbstractLocationProvider.State newState;
        if (this.mProvider == provider) {
            return;
        }
        AbstractLocationProvider oldProvider = this.mProvider;
        this.mProvider = provider;
        if (oldProvider != null) {
            oldProvider.getController().setListener(null);
            if (oldProvider.getController().isStarted()) {
                oldProvider.getController().setRequest(ProviderRequest.EMPTY_REQUEST);
                oldProvider.getController().stop();
            }
        }
        AbstractLocationProvider abstractLocationProvider = this.mProvider;
        if (abstractLocationProvider != null) {
            newState = abstractLocationProvider.getController().setListener(new ListenerWrapper(this.mProvider));
            if (this.mStarted) {
                if (!this.mProvider.getController().isStarted()) {
                    this.mProvider.getController().start();
                }
                this.mProvider.getController().setRequest(this.mRequest);
            } else if (this.mProvider.getController().isStarted()) {
                this.mProvider.getController().setRequest(ProviderRequest.EMPTY_REQUEST);
                this.mProvider.getController().stop();
            }
        } else {
            newState = AbstractLocationProvider.State.EMPTY_STATE;
        }
        setState(new UnaryOperator() { // from class: com.android.server.location.provider.MockableLocationProvider$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return MockableLocationProvider.lambda$setProviderLocked$0(AbstractLocationProvider.State.this, (AbstractLocationProvider.State) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ AbstractLocationProvider.State lambda$setProviderLocked$0(AbstractLocationProvider.State newState, AbstractLocationProvider.State prevState) {
        return newState;
    }

    public boolean isMock() {
        boolean z;
        synchronized (this.mOwnerLock) {
            MockLocationProvider mockLocationProvider = this.mMockProvider;
            z = mockLocationProvider != null && this.mProvider == mockLocationProvider;
        }
        return z;
    }

    public void setMockProviderAllowed(boolean allowed) {
        synchronized (this.mOwnerLock) {
            Preconditions.checkState(isMock());
            this.mMockProvider.setProviderAllowed(allowed);
        }
    }

    public void setMockProviderLocation(Location location) {
        synchronized (this.mOwnerLock) {
            Preconditions.checkState(isMock());
            this.mMockProvider.setProviderLocation(location);
        }
    }

    public ProviderRequest getCurrentRequest() {
        ProviderRequest providerRequest;
        synchronized (this.mOwnerLock) {
            providerRequest = this.mRequest;
        }
        return providerRequest;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void onStart() {
        synchronized (this.mOwnerLock) {
            Preconditions.checkState(!this.mStarted);
            this.mStarted = true;
            AbstractLocationProvider abstractLocationProvider = this.mProvider;
            if (abstractLocationProvider != null) {
                abstractLocationProvider.getController().start();
                if (!this.mRequest.equals(ProviderRequest.EMPTY_REQUEST)) {
                    this.mProvider.getController().setRequest(this.mRequest);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void onStop() {
        synchronized (this.mOwnerLock) {
            Preconditions.checkState(this.mStarted);
            this.mStarted = false;
            if (this.mProvider != null) {
                if (!this.mRequest.equals(ProviderRequest.EMPTY_REQUEST)) {
                    this.mProvider.getController().setRequest(ProviderRequest.EMPTY_REQUEST);
                }
                this.mProvider.getController().stop();
            }
            this.mRequest = ProviderRequest.EMPTY_REQUEST;
        }
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    protected void onSetRequest(ProviderRequest request) {
        synchronized (this.mOwnerLock) {
            if (request == this.mRequest) {
                return;
            }
            this.mRequest = request;
            AbstractLocationProvider abstractLocationProvider = this.mProvider;
            if (abstractLocationProvider != null) {
                abstractLocationProvider.getController().setRequest(request);
            }
        }
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    protected void onFlush(Runnable callback) {
        synchronized (this.mOwnerLock) {
            AbstractLocationProvider abstractLocationProvider = this.mProvider;
            if (abstractLocationProvider != null) {
                abstractLocationProvider.getController().flush(callback);
            } else {
                callback.run();
            }
        }
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    protected void onExtraCommand(int uid, int pid, String command, Bundle extras) {
        synchronized (this.mOwnerLock) {
            AbstractLocationProvider abstractLocationProvider = this.mProvider;
            if (abstractLocationProvider != null) {
                abstractLocationProvider.getController().sendExtraCommand(uid, pid, command, extras);
            }
        }
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        AbstractLocationProvider provider;
        AbstractLocationProvider.State providerState;
        Preconditions.checkState(!Thread.holdsLock(this.mOwnerLock));
        synchronized (this.mOwnerLock) {
            provider = this.mProvider;
            providerState = getState();
        }
        pw.println("allowed=" + providerState.allowed);
        if (providerState.identity != null) {
            pw.println("identity=" + providerState.identity);
        }
        if (!providerState.extraAttributionTags.isEmpty()) {
            pw.println("extra attribution tags=" + providerState.extraAttributionTags);
        }
        if (providerState.properties != null) {
            pw.println("properties=" + providerState.properties);
        }
        if (provider != null) {
            provider.dump(fd, pw, args);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ListenerWrapper implements AbstractLocationProvider.Listener {
        private final AbstractLocationProvider mListenerProvider;

        ListenerWrapper(AbstractLocationProvider listenerProvider) {
            this.mListenerProvider = listenerProvider;
        }

        @Override // com.android.server.location.provider.AbstractLocationProvider.Listener
        public final void onStateChanged(AbstractLocationProvider.State oldState, final AbstractLocationProvider.State newState) {
            synchronized (MockableLocationProvider.this.mOwnerLock) {
                if (this.mListenerProvider != MockableLocationProvider.this.mProvider) {
                    return;
                }
                MockableLocationProvider.this.setState(new UnaryOperator() { // from class: com.android.server.location.provider.MockableLocationProvider$ListenerWrapper$$ExternalSyntheticLambda0
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return MockableLocationProvider.ListenerWrapper.lambda$onStateChanged$0(AbstractLocationProvider.State.this, (AbstractLocationProvider.State) obj);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ AbstractLocationProvider.State lambda$onStateChanged$0(AbstractLocationProvider.State newState, AbstractLocationProvider.State prevState) {
            return newState;
        }

        @Override // com.android.server.location.provider.AbstractLocationProvider.Listener
        public final void onReportLocation(LocationResult locationResult) {
            synchronized (MockableLocationProvider.this.mOwnerLock) {
                if (this.mListenerProvider != MockableLocationProvider.this.mProvider) {
                    return;
                }
                MockableLocationProvider.this.reportLocation(locationResult);
            }
        }
    }
}
