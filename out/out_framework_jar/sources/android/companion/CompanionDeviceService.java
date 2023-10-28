package android.companion;

import android.app.Service;
import android.companion.CompanionDeviceService;
import android.companion.ICompanionDeviceService;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import java.util.Objects;
/* loaded from: classes.dex */
public abstract class CompanionDeviceService extends Service {
    private static final String LOG_TAG = "CompanionDeviceService";
    public static final String SERVICE_INTERFACE = "android.companion.CompanionDeviceService";
    private final Stub mRemote = new Stub();

    @Deprecated
    public void onDeviceAppeared(String address) {
    }

    @Deprecated
    public void onDeviceDisappeared(String address) {
    }

    public void onDispatchMessage(int messageId, int associationId, byte[] message) {
    }

    public final void dispatchMessage(int messageId, int associationId, byte[] message) {
        CompanionDeviceManager companionDeviceManager = (CompanionDeviceManager) getSystemService(CompanionDeviceManager.class);
        companionDeviceManager.dispatchMessage(messageId, associationId, message);
    }

    public void onDeviceAppeared(AssociationInfo associationInfo) {
        if (!associationInfo.isSelfManaged()) {
            onDeviceAppeared(associationInfo.getDeviceMacAddressAsString());
        }
    }

    public void onDeviceDisappeared(AssociationInfo associationInfo) {
        if (!associationInfo.isSelfManaged()) {
            onDeviceDisappeared(associationInfo.getDeviceMacAddressAsString());
        }
    }

    @Override // android.app.Service
    public final IBinder onBind(Intent intent) {
        if (Objects.equals(intent.getAction(), SERVICE_INTERFACE)) {
            onBindCompanionDeviceService(intent);
            return this.mRemote;
        }
        Log.w(LOG_TAG, "Tried to bind to wrong intent (should be android.companion.CompanionDeviceService): " + intent);
        return null;
    }

    public void onBindCompanionDeviceService(Intent intent) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class Stub extends ICompanionDeviceService.Stub {
        final Handler mMainHandler;
        final CompanionDeviceService mService;

        private Stub() {
            this.mMainHandler = Handler.getMain();
            this.mService = CompanionDeviceService.this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDeviceAppeared$0$android-companion-CompanionDeviceService$Stub  reason: not valid java name */
        public /* synthetic */ void m694x9d58410e(AssociationInfo associationInfo) {
            this.mService.onDeviceAppeared(associationInfo);
        }

        @Override // android.companion.ICompanionDeviceService
        public void onDeviceAppeared(final AssociationInfo associationInfo) {
            this.mMainHandler.postAtFrontOfQueue(new Runnable() { // from class: android.companion.CompanionDeviceService$Stub$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    CompanionDeviceService.Stub.this.m694x9d58410e(associationInfo);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDeviceDisappeared$1$android-companion-CompanionDeviceService$Stub  reason: not valid java name */
        public /* synthetic */ void m695x9ca3b5b9(AssociationInfo associationInfo) {
            this.mService.onDeviceDisappeared(associationInfo);
        }

        @Override // android.companion.ICompanionDeviceService
        public void onDeviceDisappeared(final AssociationInfo associationInfo) {
            this.mMainHandler.postAtFrontOfQueue(new Runnable() { // from class: android.companion.CompanionDeviceService$Stub$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    CompanionDeviceService.Stub.this.m695x9ca3b5b9(associationInfo);
                }
            });
        }

        @Override // android.companion.ICompanionDeviceService
        public void onDispatchMessage(final int messageId, final int associationId, final byte[] message) {
            this.mMainHandler.postAtFrontOfQueue(new Runnable() { // from class: android.companion.CompanionDeviceService$Stub$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    CompanionDeviceService.Stub.this.m696x7d4a45db(messageId, associationId, message);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDispatchMessage$2$android-companion-CompanionDeviceService$Stub  reason: not valid java name */
        public /* synthetic */ void m696x7d4a45db(int messageId, int associationId, byte[] message) {
            this.mService.onDispatchMessage(messageId, associationId, message);
        }
    }
}
