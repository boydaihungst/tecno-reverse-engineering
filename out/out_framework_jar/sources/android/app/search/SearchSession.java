package android.app.search;

import android.annotation.SystemApi;
import android.app.search.ISearchCallback;
import android.app.search.ISearchUiManager;
import android.app.search.SearchSession;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import dalvik.system.CloseGuard;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
@SystemApi
/* loaded from: classes.dex */
public final class SearchSession implements AutoCloseable {
    private static final boolean DEBUG = false;
    private static final String TAG = SearchSession.class.getSimpleName();
    private final ISearchUiManager mInterface;
    private final SearchSessionId mSessionId;
    private final IBinder mToken;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final AtomicBoolean mIsClosed = new AtomicBoolean(false);

    /* JADX INFO: Access modifiers changed from: package-private */
    public SearchSession(Context context, SearchContext searchContext) {
        Binder binder = new Binder();
        this.mToken = binder;
        IBinder b = ServiceManager.getService(Context.SEARCH_UI_SERVICE);
        ISearchUiManager asInterface = ISearchUiManager.Stub.asInterface(b);
        this.mInterface = asInterface;
        SearchSessionId searchSessionId = new SearchSessionId(context.getPackageName() + ":" + UUID.randomUUID().toString(), context.getUserId());
        this.mSessionId = searchSessionId;
        searchContext.setPackageName(context.getPackageName());
        try {
            asInterface.createSearchSession(searchContext, searchSessionId, binder);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to search session", e);
            e.rethrowFromSystemServer();
        }
        this.mCloseGuard.open("SearchSession.close");
    }

    public void notifyEvent(Query query, SearchTargetEvent event) {
        if (this.mIsClosed.get()) {
            throw new IllegalStateException("This client has already been destroyed.");
        }
        try {
            this.mInterface.notifyEvent(this.mSessionId, query, event);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to notify event", e);
            e.rethrowFromSystemServer();
        }
    }

    public void query(Query input, Executor callbackExecutor, Consumer<List<SearchTarget>> callback) {
        if (this.mIsClosed.get()) {
            throw new IllegalStateException("This client has already been destroyed.");
        }
        try {
            this.mInterface.query(this.mSessionId, input, new CallbackWrapper(callbackExecutor, callback));
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to sort targets", e);
            e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void destroy() {
        if (!this.mIsClosed.getAndSet(true)) {
            this.mCloseGuard.close();
            try {
                this.mInterface.destroySearchSession(this.mSessionId);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to notify search target event", e);
                e.rethrowFromSystemServer();
                return;
            }
        }
        throw new IllegalStateException("This client has already been destroyed.");
    }

    protected void finalize() {
        try {
            CloseGuard closeGuard = this.mCloseGuard;
            if (closeGuard != null) {
                closeGuard.warnIfOpen();
            }
            if (!this.mIsClosed.get()) {
                destroy();
            }
            try {
                super.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        } catch (Throwable th) {
            try {
                super.finalize();
            } catch (Throwable throwable2) {
                throwable2.printStackTrace();
            }
            throw th;
        }
    }

    @Override // java.lang.AutoCloseable
    public void close() {
        try {
            finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class CallbackWrapper extends ISearchCallback.Stub {
        private final Consumer<List<SearchTarget>> mCallback;
        private final Executor mExecutor;

        CallbackWrapper(Executor callbackExecutor, Consumer<List<SearchTarget>> callback) {
            this.mCallback = callback;
            this.mExecutor = callbackExecutor;
        }

        @Override // android.app.search.ISearchCallback
        public void onResult(final ParceledListSlice result) {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.app.search.SearchSession$CallbackWrapper$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        SearchSession.CallbackWrapper.this.m619x299953c(result);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onResult$0$android-app-search-SearchSession$CallbackWrapper  reason: not valid java name */
        public /* synthetic */ void m619x299953c(ParceledListSlice result) {
            this.mCallback.accept(result.getList());
        }
    }
}
