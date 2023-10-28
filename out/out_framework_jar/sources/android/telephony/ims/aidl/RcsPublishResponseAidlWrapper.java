package android.telephony.ims.aidl;

import android.os.RemoteException;
import android.telephony.ims.ImsException;
import android.telephony.ims.stub.RcsCapabilityExchangeImplBase;
/* loaded from: classes3.dex */
public class RcsPublishResponseAidlWrapper implements RcsCapabilityExchangeImplBase.PublishResponseCallback {
    private final IPublishResponseCallback mResponseBinder;

    public RcsPublishResponseAidlWrapper(IPublishResponseCallback responseBinder) {
        this.mResponseBinder = responseBinder;
    }

    @Override // android.telephony.ims.stub.RcsCapabilityExchangeImplBase.PublishResponseCallback
    public void onCommandError(int code) throws ImsException {
        try {
            this.mResponseBinder.onCommandError(code);
        } catch (RemoteException e) {
            throw new ImsException(e.getMessage(), 1);
        }
    }

    @Override // android.telephony.ims.stub.RcsCapabilityExchangeImplBase.PublishResponseCallback
    public void onNetworkResponse(int code, String reason) throws ImsException {
        try {
            this.mResponseBinder.onNetworkResponse(code, reason);
        } catch (RemoteException e) {
            throw new ImsException(e.getMessage(), 1);
        }
    }

    @Override // android.telephony.ims.stub.RcsCapabilityExchangeImplBase.PublishResponseCallback
    public void onNetworkResponse(int code, String reasonPhrase, int reasonHeaderCause, String reasonHeaderText) throws ImsException {
        try {
            this.mResponseBinder.onNetworkRespHeader(code, reasonPhrase, reasonHeaderCause, reasonHeaderText);
        } catch (RemoteException e) {
            throw new ImsException(e.getMessage(), 1);
        }
    }
}
