package com.android.server.pm;

import android.app.role.RoleManager;
import android.os.Binder;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.util.CollectionUtils;
import com.android.server.FgThread;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class DefaultAppProvider {
    private final Supplier<RoleManager> mRoleManagerSupplier;
    private final Supplier<UserManagerInternal> mUserManagerInternalSupplier;

    public DefaultAppProvider(Supplier<RoleManager> roleManagerSupplier, Supplier<UserManagerInternal> userManagerInternalSupplier) {
        this.mRoleManagerSupplier = roleManagerSupplier;
        this.mUserManagerInternalSupplier = userManagerInternalSupplier;
    }

    public String getDefaultBrowser(int userId) {
        return getRoleHolder("android.app.role.BROWSER", userId);
    }

    public boolean setDefaultBrowser(String packageName, boolean async, int userId) {
        RoleManager roleManager;
        if (userId == -1 || (roleManager = this.mRoleManagerSupplier.get()) == null) {
            return false;
        }
        UserHandle user = UserHandle.of(userId);
        Executor executor = FgThread.getExecutor();
        final AndroidFuture<Void> future = new AndroidFuture<>();
        Consumer<Boolean> callback = new Consumer() { // from class: com.android.server.pm.DefaultAppProvider$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DefaultAppProvider.lambda$setDefaultBrowser$0(future, (Boolean) obj);
            }
        };
        long identity = Binder.clearCallingIdentity();
        try {
            if (packageName != null) {
                roleManager.addRoleHolderAsUser("android.app.role.BROWSER", packageName, 0, user, executor, callback);
            } else {
                roleManager.clearRoleHoldersAsUser("android.app.role.BROWSER", 0, user, executor, callback);
            }
            if (!async) {
                try {
                    future.get(5L, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    Slog.e("PackageManager", "Exception while setting default browser: " + packageName, e);
                    Binder.restoreCallingIdentity(identity);
                    return false;
                }
            }
            Binder.restoreCallingIdentity(identity);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setDefaultBrowser$0(AndroidFuture future, Boolean successful) {
        if (successful.booleanValue()) {
            future.complete((Object) null);
        } else {
            future.completeExceptionally(new RuntimeException());
        }
    }

    public String getDefaultDialer(int userId) {
        return getRoleHolder("android.app.role.DIALER", userId);
    }

    public String getDefaultHome(int userId) {
        return getRoleHolder("android.app.role.HOME", this.mUserManagerInternalSupplier.get().getProfileParentId(userId));
    }

    public boolean setDefaultHome(String packageName, int userId, Executor executor, Consumer<Boolean> callback) {
        RoleManager roleManager = this.mRoleManagerSupplier.get();
        if (roleManager == null) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            roleManager.addRoleHolderAsUser("android.app.role.HOME", packageName, 0, UserHandle.of(userId), executor, callback);
            Binder.restoreCallingIdentity(identity);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    private String getRoleHolder(String roleName, int userId) {
        RoleManager roleManager = this.mRoleManagerSupplier.get();
        if (roleManager == null) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            return (String) CollectionUtils.firstOrNull(roleManager.getRoleHoldersAsUser(roleName, UserHandle.of(userId)));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
