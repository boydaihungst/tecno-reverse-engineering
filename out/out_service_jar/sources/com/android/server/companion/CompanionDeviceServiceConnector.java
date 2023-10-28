package com.android.server.companion;

import android.companion.AssociationInfo;
import android.companion.ICompanionDeviceService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import com.android.internal.infra.ServiceConnector;
import com.android.server.ServiceThread;
import java.util.function.Function;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class CompanionDeviceServiceConnector extends ServiceConnector.Impl<ICompanionDeviceService> {
    private static final boolean DEBUG = false;
    private static final String TAG = "CompanionDevice_ServiceConnector";
    private static volatile ServiceThread sServiceThread;
    private final ComponentName mComponentName;
    private Listener mListener;
    private final int mUserId;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Listener {
        void onBindingDied(int i, String str);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static CompanionDeviceServiceConnector newInstance(Context context, int userId, ComponentName componentName, boolean isSelfManaged) {
        int bindingFlags = isSelfManaged ? 268435456 : 65536;
        return new CompanionDeviceServiceConnector(context, userId, componentName, bindingFlags);
    }

    private CompanionDeviceServiceConnector(Context context, int userId, ComponentName componentName, int bindingFlags) {
        super(context, buildIntent(componentName), bindingFlags, userId, (Function) null);
        this.mUserId = userId;
        this.mComponentName = componentName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postOnDeviceAppeared(final AssociationInfo associationInfo) {
        post(new ServiceConnector.VoidJob() { // from class: com.android.server.companion.CompanionDeviceServiceConnector$$ExternalSyntheticLambda0
            public final void runNoResult(Object obj) {
                ((ICompanionDeviceService) obj).onDeviceAppeared(associationInfo);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postOnDeviceDisappeared(final AssociationInfo associationInfo) {
        post(new ServiceConnector.VoidJob() { // from class: com.android.server.companion.CompanionDeviceServiceConnector$$ExternalSyntheticLambda2
            public final void runNoResult(Object obj) {
                ((ICompanionDeviceService) obj).onDeviceDisappeared(associationInfo);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$postUnbind$2$com-android-server-companion-CompanionDeviceServiceConnector  reason: not valid java name */
    public /* synthetic */ void m2700x1e019d1c(ICompanionDeviceService it) throws Exception {
        unbind();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postUnbind() {
        post(new ServiceConnector.VoidJob() { // from class: com.android.server.companion.CompanionDeviceServiceConnector$$ExternalSyntheticLambda1
            public final void runNoResult(Object obj) {
                CompanionDeviceServiceConnector.this.m2700x1e019d1c((ICompanionDeviceService) obj);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    public void onServiceConnectionStatusChanged(ICompanionDeviceService service, boolean isConnected) {
    }

    public void onBindingDied(ComponentName name) {
        super.onBindingDied(name);
    }

    public void binderDied() {
        super.binderDied();
        Listener listener = this.mListener;
        if (listener != null) {
            listener.onBindingDied(this.mUserId, this.mComponentName.getPackageName());
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    /* renamed from: binderAsInterface */
    public ICompanionDeviceService m2701binderAsInterface(IBinder service) {
        return ICompanionDeviceService.Stub.asInterface(service);
    }

    protected Handler getJobHandler() {
        return getServiceThread().getThreadHandler();
    }

    protected long getAutoDisconnectTimeoutMs() {
        return -1L;
    }

    private static Intent buildIntent(ComponentName componentName) {
        return new Intent("android.companion.CompanionDeviceService").setComponent(componentName);
    }

    private static ServiceThread getServiceThread() {
        if (sServiceThread == null) {
            synchronized (CompanionDeviceManagerService.class) {
                if (sServiceThread == null) {
                    sServiceThread = new ServiceThread("companion-device-service-connector", 0, false);
                    sServiceThread.start();
                }
            }
        }
        return sServiceThread;
    }
}
