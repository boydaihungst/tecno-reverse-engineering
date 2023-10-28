package com.android.server.cloudsearch;

import android.app.ActivityManagerInternal;
import android.app.cloudsearch.ICloudSearchManager;
import android.app.cloudsearch.ICloudSearchManagerCallback;
import android.app.cloudsearch.SearchRequest;
import android.app.cloudsearch.SearchResponse;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.cloudsearch.CloudSearchManagerService;
import com.android.server.infra.AbstractMasterSystemService;
import com.android.server.infra.FrameworkResourcesServiceNameResolver;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class CloudSearchManagerService extends AbstractMasterSystemService<CloudSearchManagerService, CloudSearchPerUserService> {
    private static final boolean DEBUG = false;
    private static final int MAX_TEMP_SERVICE_DURATION_MS = 120000;
    private static final String TAG = CloudSearchManagerService.class.getSimpleName();
    private final ActivityTaskManagerInternal mActivityTaskManagerInternal;
    private final Context mContext;

    public CloudSearchManagerService(Context context) {
        super(context, new FrameworkResourcesServiceNameResolver(context, 17236018, true), null, 17);
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mContext = context;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public CloudSearchPerUserService newServiceLocked(int resolvedUserId, boolean disabled) {
        return new CloudSearchPerUserService(this, this.mLock, resolvedUserId, "");
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected List<CloudSearchPerUserService> newServiceListLocked(int resolvedUserId, boolean disabled, String[] serviceNames) {
        if (serviceNames == null) {
            return new ArrayList();
        }
        List<CloudSearchPerUserService> serviceList = new ArrayList<>(serviceNames.length);
        for (int i = 0; i < serviceNames.length; i++) {
            if (serviceNames[i] != null) {
                serviceList.add(new CloudSearchPerUserService(this, this.mLock, resolvedUserId, serviceNames[i]));
            }
        }
        return serviceList;
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("cloudsearch", new CloudSearchManagerStub());
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void enforceCallingPermissionForManagement() {
        getContext().enforceCallingPermission("android.permission.MANAGE_CLOUDSEARCH", TAG);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onServicePackageUpdatedLocked(int userId) {
        CloudSearchPerUserService service = peekServiceForUserLocked(userId);
        if (service != null) {
            service.onPackageUpdatedLocked();
        }
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onServicePackageRestartedLocked(int userId) {
        CloudSearchPerUserService service = peekServiceForUserLocked(userId);
        if (service != null) {
            service.onPackageRestartedLocked();
        }
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected int getMaximumTemporaryServiceDurationMs() {
        return MAX_TEMP_SERVICE_DURATION_MS;
    }

    /* loaded from: classes.dex */
    private class CloudSearchManagerStub extends ICloudSearchManager.Stub {
        private CloudSearchManagerStub() {
        }

        public void search(final SearchRequest searchRequest, final ICloudSearchManagerCallback callBack) {
            searchRequest.setCallerPackageName(CloudSearchManagerService.this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid()));
            runForUser("search", new Consumer() { // from class: com.android.server.cloudsearch.CloudSearchManagerService$CloudSearchManagerStub$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    CloudSearchManagerService.CloudSearchManagerStub.lambda$search$0(searchRequest, callBack, (CloudSearchPerUserService) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$search$0(SearchRequest searchRequest, ICloudSearchManagerCallback callBack, CloudSearchPerUserService service) {
            synchronized (service.mLock) {
                service.onSearchLocked(searchRequest, callBack);
            }
        }

        public void returnResults(final IBinder token, final String requestId, final SearchResponse response) {
            runForUser("returnResults", new Consumer() { // from class: com.android.server.cloudsearch.CloudSearchManagerService$CloudSearchManagerStub$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    CloudSearchManagerService.CloudSearchManagerStub.lambda$returnResults$1(token, requestId, response, (CloudSearchPerUserService) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$returnResults$1(IBinder token, String requestId, SearchResponse response, CloudSearchPerUserService service) {
            synchronized (service.mLock) {
                service.onReturnResultsLocked(token, requestId, response);
            }
        }

        public void destroy(final SearchRequest searchRequest) {
            runForUser("destroyCloudSearchSession", new Consumer() { // from class: com.android.server.cloudsearch.CloudSearchManagerService$CloudSearchManagerStub$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    CloudSearchManagerService.CloudSearchManagerStub.lambda$destroy$2(searchRequest, (CloudSearchPerUserService) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$destroy$2(SearchRequest searchRequest, CloudSearchPerUserService service) {
            synchronized (service.mLock) {
                service.onDestroyLocked(searchRequest.getRequestId());
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.cloudsearch.CloudSearchManagerService$CloudSearchManagerStub */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new CloudSearchManagerServiceShellCommand(CloudSearchManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        private void runForUser(String func, Consumer<CloudSearchPerUserService> c) {
            ActivityManagerInternal am = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            int userId = am.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), Binder.getCallingUserHandle().getIdentifier(), false, 0, (String) null, (String) null);
            Context ctx = CloudSearchManagerService.this.getContext();
            if (ctx.checkCallingPermission("android.permission.MANAGE_CLOUDSEARCH") != 0 && !CloudSearchManagerService.this.mServiceNameResolver.isTemporary(userId) && !CloudSearchManagerService.this.mActivityTaskManagerInternal.isCallerRecents(Binder.getCallingUid())) {
                String msg = "Permission Denial: Cannot call " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
                Slog.w(CloudSearchManagerService.TAG, msg);
                throw new SecurityException(msg);
            }
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (CloudSearchManagerService.this.mLock) {
                    List<CloudSearchPerUserService> services = CloudSearchManagerService.this.getServiceListForUserLocked(userId);
                    for (int i = 0; i < services.size(); i++) {
                        c.accept(services.get(i));
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }
}
