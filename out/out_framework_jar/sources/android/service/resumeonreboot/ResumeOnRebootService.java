package android.service.resumeonreboot;

import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelableException;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.service.resumeonreboot.IResumeOnRebootService;
import android.service.resumeonreboot.ResumeOnRebootService;
import com.android.internal.os.BackgroundThread;
import java.io.IOException;
@SystemApi
/* loaded from: classes3.dex */
public abstract class ResumeOnRebootService extends Service {
    public static final String EXCEPTION_KEY = "exception_key";
    public static final String SERVICE_INTERFACE = "android.service.resumeonreboot.ResumeOnRebootService";
    public static final String UNWRAPPED_BLOB_KEY = "unrwapped_blob_key";
    public static final String WRAPPED_BLOB_KEY = "wrapped_blob_key";
    private final Handler mHandler = BackgroundThread.getHandler();
    private final IResumeOnRebootService mInterface = new AnonymousClass1();

    public abstract byte[] onUnwrap(byte[] bArr) throws IOException;

    public abstract byte[] onWrap(byte[] bArr, long j) throws IOException;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.service.resumeonreboot.ResumeOnRebootService$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass1 extends IResumeOnRebootService.Stub {
        AnonymousClass1() {
        }

        @Override // android.service.resumeonreboot.IResumeOnRebootService
        public void wrapSecret(final byte[] unwrappedBlob, final long lifeTimeInMillis, final RemoteCallback resultCallback) throws RemoteException {
            ResumeOnRebootService.this.mHandler.post(new Runnable() { // from class: android.service.resumeonreboot.ResumeOnRebootService$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ResumeOnRebootService.AnonymousClass1.this.m3628x55d3826c(unwrappedBlob, lifeTimeInMillis, resultCallback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$wrapSecret$0$android-service-resumeonreboot-ResumeOnRebootService$1  reason: not valid java name */
        public /* synthetic */ void m3628x55d3826c(byte[] unwrappedBlob, long lifeTimeInMillis, RemoteCallback resultCallback) {
            try {
                byte[] wrappedBlob = ResumeOnRebootService.this.onWrap(unwrappedBlob, lifeTimeInMillis);
                Bundle bundle = new Bundle();
                bundle.putByteArray(ResumeOnRebootService.WRAPPED_BLOB_KEY, wrappedBlob);
                resultCallback.sendResult(bundle);
            } catch (Throwable e) {
                Bundle bundle2 = new Bundle();
                bundle2.putParcelable(ResumeOnRebootService.EXCEPTION_KEY, new ParcelableException(e));
                resultCallback.sendResult(bundle2);
            }
        }

        @Override // android.service.resumeonreboot.IResumeOnRebootService
        public void unwrap(final byte[] wrappedBlob, final RemoteCallback resultCallback) throws RemoteException {
            ResumeOnRebootService.this.mHandler.post(new Runnable() { // from class: android.service.resumeonreboot.ResumeOnRebootService$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ResumeOnRebootService.AnonymousClass1.this.m3627x5554822(wrappedBlob, resultCallback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$unwrap$1$android-service-resumeonreboot-ResumeOnRebootService$1  reason: not valid java name */
        public /* synthetic */ void m3627x5554822(byte[] wrappedBlob, RemoteCallback resultCallback) {
            try {
                byte[] unwrappedBlob = ResumeOnRebootService.this.onUnwrap(wrappedBlob);
                Bundle bundle = new Bundle();
                bundle.putByteArray(ResumeOnRebootService.UNWRAPPED_BLOB_KEY, unwrappedBlob);
                resultCallback.sendResult(bundle);
            } catch (Throwable e) {
                Bundle bundle2 = new Bundle();
                bundle2.putParcelable(ResumeOnRebootService.EXCEPTION_KEY, new ParcelableException(e));
                resultCallback.sendResult(bundle2);
            }
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return this.mInterface.asBinder();
    }
}
