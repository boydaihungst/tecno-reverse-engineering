package android.service.cloudsearch;

import android.annotation.SystemApi;
import android.app.Service;
import android.app.cloudsearch.ICloudSearchManager;
import android.app.cloudsearch.SearchRequest;
import android.app.cloudsearch.SearchResponse;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.cloudsearch.ICloudSearchService;
import android.util.Slog;
import com.android.internal.util.function.pooled.PooledLambda;
import java.util.function.BiConsumer;
@SystemApi
/* loaded from: classes3.dex */
public abstract class CloudSearchService extends Service {
    private static final boolean DEBUG = false;
    public static final String SERVICE_INTERFACE = "android.service.cloudsearch.CloudSearchService";
    private static final String TAG = "CloudSearchService";
    private Handler mHandler;
    private final ICloudSearchService mInterface = new ICloudSearchService.Stub() { // from class: android.service.cloudsearch.CloudSearchService.1
        @Override // android.service.cloudsearch.ICloudSearchService
        public void onSearch(SearchRequest request) {
            CloudSearchService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: android.service.cloudsearch.CloudSearchService$1$$ExternalSyntheticLambda0
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((CloudSearchService) obj).onSearch((SearchRequest) obj2);
                }
            }, CloudSearchService.this, request));
        }
    };
    private ICloudSearchManager mService;

    public abstract void onSearch(SearchRequest searchRequest);

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        this.mHandler = new Handler(Looper.getMainLooper(), null, true);
        IBinder b = ServiceManager.getService(Context.CLOUDSEARCH_SERVICE);
        this.mService = ICloudSearchManager.Stub.asInterface(b);
    }

    public final void returnResults(String requestId, SearchResponse response) {
        try {
            this.mService.returnResults(this.mInterface.asBinder(), requestId, response);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Override // android.app.Service
    public final IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return this.mInterface.asBinder();
        }
        Slog.w(TAG, "Tried to bind to wrong intent (should be android.service.cloudsearch.CloudSearchService: " + intent);
        return null;
    }
}
