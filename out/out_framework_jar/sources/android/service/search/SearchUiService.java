package android.service.search;

import android.annotation.SystemApi;
import android.app.Service;
import android.app.search.ISearchCallback;
import android.app.search.Query;
import android.app.search.SearchContext;
import android.app.search.SearchSessionId;
import android.app.search.SearchTarget;
import android.app.search.SearchTargetEvent;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.service.search.ISearchUiService;
import android.service.search.SearchUiService;
import android.util.Slog;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
@SystemApi
/* loaded from: classes3.dex */
public abstract class SearchUiService extends Service {
    private static final boolean DEBUG = false;
    public static final String SERVICE_INTERFACE = "android.service.search.SearchUiService";
    private static final String TAG = "SearchUiService";
    private Handler mHandler;
    private final ISearchUiService mInterface = new AnonymousClass1();

    public abstract void onDestroy(SearchSessionId searchSessionId);

    public abstract void onNotifyEvent(SearchSessionId searchSessionId, Query query, SearchTargetEvent searchTargetEvent);

    public abstract void onQuery(SearchSessionId searchSessionId, Query query, Consumer<List<SearchTarget>> consumer);

    /* renamed from: android.service.search.SearchUiService$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    class AnonymousClass1 extends ISearchUiService.Stub {
        AnonymousClass1() {
        }

        @Override // android.service.search.ISearchUiService
        public void onCreateSearchSession(SearchContext context, SearchSessionId sessionId) {
            SearchUiService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: android.service.search.SearchUiService$1$$ExternalSyntheticLambda0
                @Override // com.android.internal.util.function.TriConsumer
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((SearchUiService) obj).onSearchSessionCreated((SearchContext) obj2, (SearchSessionId) obj3);
                }
            }, SearchUiService.this, context, sessionId));
            SearchUiService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: android.service.search.SearchUiService$1$$ExternalSyntheticLambda1
                @Override // com.android.internal.util.function.TriConsumer
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((SearchUiService) obj).onCreateSearchSession((SearchContext) obj2, (SearchSessionId) obj3);
                }
            }, SearchUiService.this, context, sessionId));
        }

        @Override // android.service.search.ISearchUiService
        public void onQuery(SearchSessionId sessionId, Query input, ISearchCallback callback) {
            SearchUiService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuadConsumer() { // from class: android.service.search.SearchUiService$1$$ExternalSyntheticLambda2
                @Override // com.android.internal.util.function.QuadConsumer
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                    ((SearchUiService) obj).onQuery((SearchSessionId) obj2, (Query) obj3, (SearchUiService.CallbackWrapper) obj4);
                }
            }, SearchUiService.this, sessionId, input, new CallbackWrapper(callback)));
        }

        @Override // android.service.search.ISearchUiService
        public void onNotifyEvent(SearchSessionId sessionId, Query query, SearchTargetEvent event) {
            SearchUiService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuadConsumer() { // from class: android.service.search.SearchUiService$1$$ExternalSyntheticLambda3
                @Override // com.android.internal.util.function.QuadConsumer
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                    ((SearchUiService) obj).onNotifyEvent((SearchSessionId) obj2, (Query) obj3, (SearchTargetEvent) obj4);
                }
            }, SearchUiService.this, sessionId, query, event));
        }

        @Override // android.service.search.ISearchUiService
        public void onDestroy(SearchSessionId sessionId) {
            SearchUiService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: android.service.search.SearchUiService$1$$ExternalSyntheticLambda4
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((SearchUiService) obj).doDestroy((SearchSessionId) obj2);
                }
            }, SearchUiService.this, sessionId));
        }
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        this.mHandler = new Handler(Looper.getMainLooper(), null, true);
    }

    @Override // android.app.Service
    public final IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return this.mInterface.asBinder();
        }
        Slog.w(TAG, "Tried to bind to wrong intent (should be android.service.search.SearchUiService: " + intent);
        return null;
    }

    @Deprecated
    public void onCreateSearchSession(SearchContext context, SearchSessionId sessionId) {
    }

    public void onSearchSessionCreated(SearchContext context, SearchSessionId sessionId) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doDestroy(SearchSessionId sessionId) {
        super.onDestroy();
        onDestroy(sessionId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static final class CallbackWrapper implements Consumer<List<SearchTarget>> {
        private ISearchCallback mCallback;

        CallbackWrapper(ISearchCallback callback) {
            this.mCallback = callback;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(List<SearchTarget> searchTargets) {
            try {
                ISearchCallback iSearchCallback = this.mCallback;
                if (iSearchCallback != null) {
                    iSearchCallback.onResult(new ParceledListSlice(searchTargets));
                }
            } catch (RemoteException e) {
                Slog.e(SearchUiService.TAG, "Error sending result:" + e);
            }
        }
    }
}
