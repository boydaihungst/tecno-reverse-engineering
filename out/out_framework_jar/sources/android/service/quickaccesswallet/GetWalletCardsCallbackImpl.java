package android.service.quickaccesswallet;

import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes3.dex */
public final class GetWalletCardsCallbackImpl implements GetWalletCardsCallback {
    private static final String TAG = "QAWalletCallback";
    private final IQuickAccessWalletServiceCallbacks mCallback;
    private boolean mCalled;
    private final Handler mHandler;
    private final GetWalletCardsRequest mRequest;

    /* JADX INFO: Access modifiers changed from: package-private */
    public GetWalletCardsCallbackImpl(GetWalletCardsRequest request, IQuickAccessWalletServiceCallbacks callback, Handler handler) {
        this.mRequest = request;
        this.mCallback = callback;
        this.mHandler = handler;
    }

    @Override // android.service.quickaccesswallet.GetWalletCardsCallback
    public void onSuccess(final GetWalletCardsResponse response) {
        if (isValidResponse(response)) {
            this.mHandler.post(new Runnable() { // from class: android.service.quickaccesswallet.GetWalletCardsCallbackImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GetWalletCardsCallbackImpl.this.m3579x70bfdb43(response);
                }
            });
            return;
        }
        Log.w(TAG, "Invalid GetWalletCards response");
        this.mHandler.post(new Runnable() { // from class: android.service.quickaccesswallet.GetWalletCardsCallbackImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                GetWalletCardsCallbackImpl.this.m3580x4fe4ae2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSuccess$1$android-service-quickaccesswallet-GetWalletCardsCallbackImpl  reason: not valid java name */
    public /* synthetic */ void m3580x4fe4ae2() {
        m3578xabc4b41a(new GetWalletCardsError(null, null));
    }

    @Override // android.service.quickaccesswallet.GetWalletCardsCallback
    public void onFailure(final GetWalletCardsError error) {
        this.mHandler.post(new Runnable() { // from class: android.service.quickaccesswallet.GetWalletCardsCallbackImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                GetWalletCardsCallbackImpl.this.m3578xabc4b41a(error);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onSuccessInternal */
    public void m3579x70bfdb43(GetWalletCardsResponse response) {
        if (this.mCalled) {
            Log.w(TAG, "already called");
            return;
        }
        this.mCalled = true;
        try {
            this.mCallback.onGetWalletCardsSuccess(response);
        } catch (RemoteException e) {
            Log.w(TAG, "Error returning wallet cards", e);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onFailureInternal */
    public void m3578xabc4b41a(GetWalletCardsError error) {
        if (this.mCalled) {
            Log.w(TAG, "already called");
            return;
        }
        this.mCalled = true;
        try {
            this.mCallback.onGetWalletCardsFailure(error);
        } catch (RemoteException e) {
            Log.e(TAG, "Error returning failure message", e);
        }
    }

    private boolean isValidResponse(GetWalletCardsResponse response) {
        if (response == null) {
            Log.w(TAG, "Invalid response: response is null");
            return false;
        } else if (response.getWalletCards() == null) {
            Log.w(TAG, "Invalid response: walletCards is null");
            return false;
        } else if (response.getSelectedIndex() < 0) {
            Log.w(TAG, "Invalid response: selectedIndex is negative");
            return false;
        } else if (!response.getWalletCards().isEmpty() && response.getSelectedIndex() >= response.getWalletCards().size()) {
            Log.w(TAG, "Invalid response: selectedIndex out of bounds");
            return false;
        } else if (response.getWalletCards().size() > this.mRequest.getMaxCards()) {
            Log.w(TAG, "Invalid response: too many cards");
            return false;
        } else {
            for (WalletCard walletCard : response.getWalletCards()) {
                if (walletCard == null) {
                    Log.w(TAG, "Invalid response: card is null");
                    return false;
                } else if (walletCard.getCardId() == null) {
                    Log.w(TAG, "Invalid response: cardId is null");
                    return false;
                } else {
                    Icon cardImage = walletCard.getCardImage();
                    if (cardImage == null) {
                        Log.w(TAG, "Invalid response: cardImage is null");
                        return false;
                    } else if (cardImage.getType() == 1 && cardImage.getBitmap().getConfig() != Bitmap.Config.HARDWARE) {
                        Log.w(TAG, "Invalid response: cardImage bitmaps must be hardware bitmaps");
                        return false;
                    } else if (TextUtils.isEmpty(walletCard.getContentDescription())) {
                        Log.w(TAG, "Invalid response: contentDescription is null");
                        return false;
                    } else if (walletCard.getPendingIntent() == null) {
                        Log.w(TAG, "Invalid response: pendingIntent is null");
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
