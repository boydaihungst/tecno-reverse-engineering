package android.service.quickaccesswallet;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.service.quickaccesswallet.IQuickAccessWalletService;
import android.service.quickaccesswallet.QuickAccessWalletService;
import android.util.Log;
/* loaded from: classes3.dex */
public abstract class QuickAccessWalletService extends Service {
    public static final String ACTION_VIEW_WALLET = "android.service.quickaccesswallet.action.VIEW_WALLET";
    public static final String ACTION_VIEW_WALLET_SETTINGS = "android.service.quickaccesswallet.action.VIEW_WALLET_SETTINGS";
    public static final String SERVICE_INTERFACE = "android.service.quickaccesswallet.QuickAccessWalletService";
    public static final String SERVICE_META_DATA = "android.quickaccesswallet";
    private static final String TAG = "QAWalletService";
    public static final String TILE_SERVICE_META_DATA = "android.quickaccesswallet.tile";
    private IQuickAccessWalletServiceCallbacks mEventListener;
    private String mEventListenerId;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final IQuickAccessWalletService mInterface = new AnonymousClass1();

    public abstract void onWalletCardSelected(SelectWalletCardRequest selectWalletCardRequest);

    public abstract void onWalletCardsRequested(GetWalletCardsRequest getWalletCardsRequest, GetWalletCardsCallback getWalletCardsCallback);

    public abstract void onWalletDismissed();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.service.quickaccesswallet.QuickAccessWalletService$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass1 extends IQuickAccessWalletService.Stub {
        AnonymousClass1() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onWalletCardsRequested$0$android-service-quickaccesswallet-QuickAccessWalletService$1  reason: not valid java name */
        public /* synthetic */ void m3600xf35e3349(GetWalletCardsRequest request, IQuickAccessWalletServiceCallbacks callback) {
            QuickAccessWalletService.this.onWalletCardsRequestedInternal(request, callback);
        }

        @Override // android.service.quickaccesswallet.IQuickAccessWalletService
        public void onWalletCardsRequested(final GetWalletCardsRequest request, final IQuickAccessWalletServiceCallbacks callback) {
            QuickAccessWalletService.this.mHandler.post(new Runnable() { // from class: android.service.quickaccesswallet.QuickAccessWalletService$1$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    QuickAccessWalletService.AnonymousClass1.this.m3600xf35e3349(request, callback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onWalletCardSelected$1$android-service-quickaccesswallet-QuickAccessWalletService$1  reason: not valid java name */
        public /* synthetic */ void m3599x60574728(SelectWalletCardRequest request) {
            QuickAccessWalletService.this.onWalletCardSelected(request);
        }

        @Override // android.service.quickaccesswallet.IQuickAccessWalletService
        public void onWalletCardSelected(final SelectWalletCardRequest request) {
            QuickAccessWalletService.this.mHandler.post(new Runnable() { // from class: android.service.quickaccesswallet.QuickAccessWalletService$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    QuickAccessWalletService.AnonymousClass1.this.m3599x60574728(request);
                }
            });
        }

        @Override // android.service.quickaccesswallet.IQuickAccessWalletService
        public void onWalletDismissed() {
            Handler handler = QuickAccessWalletService.this.mHandler;
            final QuickAccessWalletService quickAccessWalletService = QuickAccessWalletService.this;
            handler.post(new Runnable() { // from class: android.service.quickaccesswallet.QuickAccessWalletService$1$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    QuickAccessWalletService.this.onWalletDismissed();
                }
            });
        }

        @Override // android.service.quickaccesswallet.IQuickAccessWalletService
        public void onTargetActivityIntentRequested(final IQuickAccessWalletServiceCallbacks callbacks) {
            QuickAccessWalletService.this.mHandler.post(new Runnable() { // from class: android.service.quickaccesswallet.QuickAccessWalletService$1$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    QuickAccessWalletService.AnonymousClass1.this.m3598xd529d391(callbacks);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTargetActivityIntentRequested$2$android-service-quickaccesswallet-QuickAccessWalletService$1  reason: not valid java name */
        public /* synthetic */ void m3598xd529d391(IQuickAccessWalletServiceCallbacks callbacks) {
            QuickAccessWalletService.this.onTargetActivityIntentRequestedInternal(callbacks);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$registerWalletServiceEventListener$3$android-service-quickaccesswallet-QuickAccessWalletService$1  reason: not valid java name */
        public /* synthetic */ void m3601x80ba0c34(WalletServiceEventListenerRequest request, IQuickAccessWalletServiceCallbacks callback) {
            QuickAccessWalletService.this.registerDismissWalletListenerInternal(request, callback);
        }

        @Override // android.service.quickaccesswallet.IQuickAccessWalletService
        public void registerWalletServiceEventListener(final WalletServiceEventListenerRequest request, final IQuickAccessWalletServiceCallbacks callback) {
            QuickAccessWalletService.this.mHandler.post(new Runnable() { // from class: android.service.quickaccesswallet.QuickAccessWalletService$1$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    QuickAccessWalletService.AnonymousClass1.this.m3601x80ba0c34(request, callback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$unregisterWalletServiceEventListener$4$android-service-quickaccesswallet-QuickAccessWalletService$1  reason: not valid java name */
        public /* synthetic */ void m3602x74b89f9a(WalletServiceEventListenerRequest request) {
            QuickAccessWalletService.this.unregisterDismissWalletListenerInternal(request);
        }

        @Override // android.service.quickaccesswallet.IQuickAccessWalletService
        public void unregisterWalletServiceEventListener(final WalletServiceEventListenerRequest request) {
            QuickAccessWalletService.this.mHandler.post(new Runnable() { // from class: android.service.quickaccesswallet.QuickAccessWalletService$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    QuickAccessWalletService.AnonymousClass1.this.m3602x74b89f9a(request);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onWalletCardsRequestedInternal(GetWalletCardsRequest request, IQuickAccessWalletServiceCallbacks callback) {
        onWalletCardsRequested(request, new GetWalletCardsCallbackImpl(request, callback, this.mHandler));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTargetActivityIntentRequestedInternal(IQuickAccessWalletServiceCallbacks callbacks) {
        try {
            callbacks.onTargetActivityPendingIntentReceived(getTargetActivityPendingIntent());
        } catch (RemoteException e) {
            Log.w(TAG, "Error returning wallet cards", e);
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        if (Build.VERSION.SDK_INT < 30) {
            Log.w(TAG, "Warning: binding on pre-R device");
        }
        if (!SERVICE_INTERFACE.equals(intent.getAction())) {
            Log.w(TAG, "Wrong action");
            return null;
        }
        return this.mInterface.asBinder();
    }

    public final void sendWalletServiceEvent(final WalletServiceEvent serviceEvent) {
        this.mHandler.post(new Runnable() { // from class: android.service.quickaccesswallet.QuickAccessWalletService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                QuickAccessWalletService.this.m3597xe9c48799(serviceEvent);
            }
        });
    }

    public PendingIntent getTargetActivityPendingIntent() {
        return null;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: sendWalletServiceEventInternal */
    public void m3597xe9c48799(WalletServiceEvent serviceEvent) {
        IQuickAccessWalletServiceCallbacks iQuickAccessWalletServiceCallbacks = this.mEventListener;
        if (iQuickAccessWalletServiceCallbacks == null) {
            Log.i(TAG, "No dismiss listener registered");
            return;
        }
        try {
            iQuickAccessWalletServiceCallbacks.onWalletServiceEvent(serviceEvent);
        } catch (RemoteException e) {
            Log.w(TAG, "onWalletServiceEvent error", e);
            this.mEventListenerId = null;
            this.mEventListener = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerDismissWalletListenerInternal(WalletServiceEventListenerRequest request, IQuickAccessWalletServiceCallbacks callback) {
        this.mEventListenerId = request.getListenerId();
        this.mEventListener = callback;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterDismissWalletListenerInternal(WalletServiceEventListenerRequest request) {
        String str = this.mEventListenerId;
        if (str != null && str.equals(request.getListenerId())) {
            this.mEventListenerId = null;
            this.mEventListener = null;
            return;
        }
        Log.w(TAG, "dismiss listener missing or replaced");
    }
}
