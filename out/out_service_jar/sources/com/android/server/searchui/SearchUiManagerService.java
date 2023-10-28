package com.android.server.searchui;

import android.app.ActivityManagerInternal;
import android.app.search.ISearchCallback;
import android.app.search.ISearchUiManager;
import android.app.search.Query;
import android.app.search.SearchContext;
import android.app.search.SearchSessionId;
import android.app.search.SearchTargetEvent;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.infra.AbstractMasterSystemService;
import com.android.server.infra.FrameworkResourcesServiceNameResolver;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.FileDescriptor;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class SearchUiManagerService extends AbstractMasterSystemService<SearchUiManagerService, SearchUiPerUserService> {
    private static final boolean DEBUG = false;
    private static final int MAX_TEMP_SERVICE_DURATION_MS = 120000;
    private static final String TAG = SearchUiManagerService.class.getSimpleName();
    private ActivityTaskManagerInternal mActivityTaskManagerInternal;

    public SearchUiManagerService(Context context) {
        super(context, new FrameworkResourcesServiceNameResolver(context, 17039942), null, 17);
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public SearchUiPerUserService newServiceLocked(int resolvedUserId, boolean disabled) {
        return new SearchUiPerUserService(this, this.mLock, resolvedUserId);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("search_ui", new SearchUiManagerStub());
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void enforceCallingPermissionForManagement() {
        getContext().enforceCallingPermission("android.permission.MANAGE_SEARCH_UI", TAG);
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onServicePackageUpdatedLocked(int userId) {
        SearchUiPerUserService service = peekServiceForUserLocked(userId);
        if (service != null) {
            service.onPackageUpdatedLocked();
        }
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected void onServicePackageRestartedLocked(int userId) {
        SearchUiPerUserService service = peekServiceForUserLocked(userId);
        if (service != null) {
            service.onPackageRestartedLocked();
        }
    }

    @Override // com.android.server.infra.AbstractMasterSystemService
    protected int getMaximumTemporaryServiceDurationMs() {
        return MAX_TEMP_SERVICE_DURATION_MS;
    }

    /* loaded from: classes2.dex */
    private class SearchUiManagerStub extends ISearchUiManager.Stub {
        private SearchUiManagerStub() {
        }

        public void createSearchSession(final SearchContext context, final SearchSessionId sessionId, final IBinder token) {
            runForUserLocked("createSearchSession", sessionId, new Consumer() { // from class: com.android.server.searchui.SearchUiManagerService$SearchUiManagerStub$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SearchUiPerUserService) obj).onCreateSearchSessionLocked(context, sessionId, token);
                }
            });
        }

        public void notifyEvent(final SearchSessionId sessionId, final Query query, final SearchTargetEvent event) {
            runForUserLocked("notifyEvent", sessionId, new Consumer() { // from class: com.android.server.searchui.SearchUiManagerService$SearchUiManagerStub$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SearchUiPerUserService) obj).notifyLocked(sessionId, query, event);
                }
            });
        }

        public void query(final SearchSessionId sessionId, final Query query, final ISearchCallback callback) {
            runForUserLocked("query", sessionId, new Consumer() { // from class: com.android.server.searchui.SearchUiManagerService$SearchUiManagerStub$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SearchUiPerUserService) obj).queryLocked(sessionId, query, callback);
                }
            });
        }

        public void destroySearchSession(final SearchSessionId sessionId) {
            runForUserLocked("destroySearchSession", sessionId, new Consumer() { // from class: com.android.server.searchui.SearchUiManagerService$SearchUiManagerStub$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SearchUiPerUserService) obj).onDestroyLocked(sessionId);
                }
            });
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.searchui.SearchUiManagerService$SearchUiManagerStub */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new SearchUiManagerServiceShellCommand(SearchUiManagerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        private void runForUserLocked(String func, SearchSessionId sessionId, Consumer<SearchUiPerUserService> c) {
            ActivityManagerInternal am = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            int userId = am.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), sessionId.getUserId(), false, 0, (String) null, (String) null);
            if (!SearchUiManagerService.this.mServiceNameResolver.isTemporary(userId) && !SearchUiManagerService.this.mActivityTaskManagerInternal.isCallerRecents(Binder.getCallingUid())) {
                String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
                Slog.w(SearchUiManagerService.TAG, msg);
                throw new SecurityException(msg);
            }
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (SearchUiManagerService.this.mLock) {
                    SearchUiPerUserService service = (SearchUiPerUserService) SearchUiManagerService.this.getServiceForUserLocked(userId);
                    c.accept(service);
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }
}
