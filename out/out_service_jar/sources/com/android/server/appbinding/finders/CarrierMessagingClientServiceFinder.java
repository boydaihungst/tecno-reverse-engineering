package com.android.server.appbinding.finders;

import android.app.role.OnRoleHoldersChangedListener;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.service.carrier.CarrierMessagingClientService;
import android.service.carrier.ICarrierMessagingClientService;
import android.text.TextUtils;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.CollectionUtils;
import com.android.server.appbinding.AppBindingConstants;
import java.util.function.BiConsumer;
/* loaded from: classes.dex */
public class CarrierMessagingClientServiceFinder extends AppServiceFinder<CarrierMessagingClientService, ICarrierMessagingClientService> {
    private final OnRoleHoldersChangedListener mRoleHolderChangedListener;
    private final RoleManager mRoleManager;

    public CarrierMessagingClientServiceFinder(Context context, BiConsumer<AppServiceFinder, Integer> listener, Handler callbackHandler) {
        super(context, listener, callbackHandler);
        this.mRoleHolderChangedListener = new OnRoleHoldersChangedListener() { // from class: com.android.server.appbinding.finders.CarrierMessagingClientServiceFinder$$ExternalSyntheticLambda0
            public final void onRoleHoldersChanged(String str, UserHandle userHandle) {
                CarrierMessagingClientServiceFinder.this.m1592x361e1306(str, userHandle);
            }
        };
        this.mRoleManager = (RoleManager) context.getSystemService(RoleManager.class);
    }

    @Override // com.android.server.appbinding.finders.AppServiceFinder
    protected boolean isEnabled(AppBindingConstants constants) {
        return constants.SMS_SERVICE_ENABLED && this.mContext.getResources().getBoolean(17891815);
    }

    @Override // com.android.server.appbinding.finders.AppServiceFinder
    public String getAppDescription() {
        return "[Default SMS app]";
    }

    @Override // com.android.server.appbinding.finders.AppServiceFinder
    protected Class<CarrierMessagingClientService> getServiceClass() {
        return CarrierMessagingClientService.class;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX DEBUG: Possible override for method com.android.server.appbinding.finders.AppServiceFinder.asInterface(Landroid/os/IBinder;)Landroid/os/IInterface; */
    @Override // com.android.server.appbinding.finders.AppServiceFinder
    public ICarrierMessagingClientService asInterface(IBinder obj) {
        return ICarrierMessagingClientService.Stub.asInterface(obj);
    }

    @Override // com.android.server.appbinding.finders.AppServiceFinder
    protected String getServiceAction() {
        return "android.telephony.action.CARRIER_MESSAGING_CLIENT_SERVICE";
    }

    @Override // com.android.server.appbinding.finders.AppServiceFinder
    protected String getServicePermission() {
        return "android.permission.BIND_CARRIER_MESSAGING_CLIENT_SERVICE";
    }

    @Override // com.android.server.appbinding.finders.AppServiceFinder
    public String getTargetPackage(int userId) {
        String ret = (String) CollectionUtils.firstOrNull(this.mRoleManager.getRoleHoldersAsUser("android.app.role.SMS", UserHandle.of(userId)));
        return ret;
    }

    @Override // com.android.server.appbinding.finders.AppServiceFinder
    public void startMonitoring() {
        this.mRoleManager.addOnRoleHoldersChangedListenerAsUser(BackgroundThread.getExecutor(), this.mRoleHolderChangedListener, UserHandle.ALL);
    }

    @Override // com.android.server.appbinding.finders.AppServiceFinder
    protected String validateService(ServiceInfo service) {
        String packageName = service.packageName;
        String process = service.processName;
        if (process == null || TextUtils.equals(packageName, process)) {
            return "Service must not run on the main process";
        }
        return null;
    }

    @Override // com.android.server.appbinding.finders.AppServiceFinder
    public int getBindFlags(AppBindingConstants constants) {
        return constants.SMS_APP_BIND_FLAGS;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-appbinding-finders-CarrierMessagingClientServiceFinder  reason: not valid java name */
    public /* synthetic */ void m1592x361e1306(String role, UserHandle user) {
        if ("android.app.role.SMS".equals(role)) {
            this.mListener.accept(this, Integer.valueOf(user.getIdentifier()));
        }
    }
}
