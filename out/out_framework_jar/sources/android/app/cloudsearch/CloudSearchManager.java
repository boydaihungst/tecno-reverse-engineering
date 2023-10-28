package android.app.cloudsearch;

import android.annotation.SystemApi;
import android.app.cloudsearch.CloudSearchManager;
import android.app.cloudsearch.ICloudSearchManagerCallback;
import android.os.RemoteException;
import java.util.Objects;
import java.util.concurrent.Executor;
@SystemApi
/* loaded from: classes.dex */
public class CloudSearchManager {
    private final ICloudSearchManager mService;

    /* loaded from: classes.dex */
    public interface CallBack {
        void onSearchFailed(SearchRequest searchRequest, SearchResponse searchResponse);

        void onSearchSucceeded(SearchRequest searchRequest, SearchResponse searchResponse);
    }

    public CloudSearchManager(ICloudSearchManager service) {
        this.mService = service;
    }

    @SystemApi
    public void search(SearchRequest request, Executor callbackExecutor, CallBack callback) {
        try {
            this.mService.search((SearchRequest) Objects.requireNonNull(request), new CallBackWrapper((SearchRequest) Objects.requireNonNull(request), (CallBack) Objects.requireNonNull(callback), (Executor) Objects.requireNonNull(callbackExecutor)));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class CallBackWrapper extends ICloudSearchManagerCallback.Stub {
        private final CallBack mCallback;
        private final Executor mCallbackExecutor;
        private final SearchRequest mSearchRequest;

        CallBackWrapper(SearchRequest searchRequest, CallBack callback, Executor callbackExecutor) {
            this.mSearchRequest = searchRequest;
            this.mCallback = callback;
            this.mCallbackExecutor = callbackExecutor;
        }

        @Override // android.app.cloudsearch.ICloudSearchManagerCallback
        public void onSearchSucceeded(final SearchResponse searchResponse) {
            this.mCallbackExecutor.execute(new Runnable() { // from class: android.app.cloudsearch.CloudSearchManager$CallBackWrapper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    CloudSearchManager.CallBackWrapper.this.m549x1b38a78f(searchResponse);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSearchSucceeded$0$android-app-cloudsearch-CloudSearchManager$CallBackWrapper  reason: not valid java name */
        public /* synthetic */ void m549x1b38a78f(SearchResponse searchResponse) {
            this.mCallback.onSearchSucceeded(this.mSearchRequest, searchResponse);
        }

        @Override // android.app.cloudsearch.ICloudSearchManagerCallback
        public void onSearchFailed(final SearchResponse searchResponse) {
            this.mCallbackExecutor.execute(new Runnable() { // from class: android.app.cloudsearch.CloudSearchManager$CallBackWrapper$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    CloudSearchManager.CallBackWrapper.this.m548xad74798a(searchResponse);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSearchFailed$1$android-app-cloudsearch-CloudSearchManager$CallBackWrapper  reason: not valid java name */
        public /* synthetic */ void m548xad74798a(SearchResponse searchResponse) {
            this.mCallback.onSearchFailed(this.mSearchRequest, searchResponse);
        }
    }
}
