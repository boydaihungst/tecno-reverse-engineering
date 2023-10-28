package com.android.server.companion;

import android.companion.AssociationInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.infra.PerUser;
import com.android.internal.util.CollectionUtils;
import com.android.server.companion.CompanionDeviceServiceConnector;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class CompanionApplicationController {
    static final boolean DEBUG = false;
    private static final long REBIND_TIMEOUT = 10000;
    private static final String TAG = "CompanionDevice_ApplicationController";
    private final Callback mCallback;
    private final Context mContext;
    private final CompanionServicesRegister mCompanionServicesRegister = new CompanionServicesRegister();
    private final AndroidPackageMap<List<CompanionDeviceServiceConnector>> mBoundCompanionApplications = new AndroidPackageMap<>();
    private final AndroidPackageMap<Boolean> mScheduledForRebindingCompanionApplications = new AndroidPackageMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Callback {
        boolean onCompanionApplicationBindingDied(int i, String str);

        void onRebindCompanionApplicationTimeout(int i, String str);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CompanionApplicationController(Context context, Callback callback) {
        this.mContext = context;
        this.mCallback = callback;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackagesChanged(int userId) {
        this.mCompanionServicesRegister.invalidate(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void bindCompanionApplication(final int userId, String packageName, final boolean isSelfManaged) {
        List<ComponentName> companionServices = this.mCompanionServicesRegister.forPackage(userId, packageName);
        if (companionServices.isEmpty()) {
            Slog.w(TAG, "Can not bind companion applications u" + userId + SliceClientPermissions.SliceAuthority.DELIMITER + packageName + ": eligible CompanionDeviceService not found.\nA CompanionDeviceService should declare an intent-filter for \"android.companion.CompanionDeviceService\" action and require \"android.permission.BIND_COMPANION_DEVICE_SERVICE\" permission.");
            return;
        }
        synchronized (this.mBoundCompanionApplications) {
            if (this.mBoundCompanionApplications.containsValueForPackage(userId, packageName)) {
                return;
            }
            List<CompanionDeviceServiceConnector> serviceConnectors = CollectionUtils.map(companionServices, new Function() { // from class: com.android.server.companion.CompanionApplicationController$$ExternalSyntheticLambda1
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return CompanionApplicationController.this.m2678x6fd62827(userId, isSelfManaged, (ComponentName) obj);
                }
            });
            this.mBoundCompanionApplications.setValueForPackage(userId, packageName, serviceConnectors);
            serviceConnectors.get(0).setListener(new CompanionDeviceServiceConnector.Listener() { // from class: com.android.server.companion.CompanionApplicationController$$ExternalSyntheticLambda2
                @Override // com.android.server.companion.CompanionDeviceServiceConnector.Listener
                public final void onBindingDied(int i, String str) {
                    CompanionApplicationController.this.onPrimaryServiceBindingDied(i, str);
                }
            });
            for (CompanionDeviceServiceConnector serviceConnector : serviceConnectors) {
                serviceConnector.connect();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$bindCompanionApplication$0$com-android-server-companion-CompanionApplicationController  reason: not valid java name */
    public /* synthetic */ CompanionDeviceServiceConnector m2678x6fd62827(int userId, boolean isSelfManaged, ComponentName componentName) {
        return CompanionDeviceServiceConnector.newInstance(this.mContext, userId, componentName, isSelfManaged);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unbindCompanionApplication(int userId, String packageName) {
        List<CompanionDeviceServiceConnector> serviceConnectors;
        synchronized (this.mBoundCompanionApplications) {
            serviceConnectors = this.mBoundCompanionApplications.removePackage(userId, packageName);
        }
        if (serviceConnectors == null) {
            return;
        }
        for (CompanionDeviceServiceConnector serviceConnector : serviceConnectors) {
            serviceConnector.postUnbind();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCompanionApplicationBound(int userId, String packageName) {
        boolean containsValueForPackage;
        synchronized (this.mBoundCompanionApplications) {
            containsValueForPackage = this.mBoundCompanionApplications.containsValueForPackage(userId, packageName);
        }
        return containsValueForPackage;
    }

    private void scheduleRebinding(final int userId, final String packageName) {
        this.mScheduledForRebindingCompanionApplications.setValueForPackage(userId, packageName, true);
        Handler.getMain().postDelayed(new Runnable() { // from class: com.android.server.companion.CompanionApplicationController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CompanionApplicationController.this.m2679xa0a26c68(userId, packageName);
            }
        }, 10000L);
    }

    boolean isRebindingCompanionApplicationScheduled(int userId, String packageName) {
        return this.mScheduledForRebindingCompanionApplications.containsValueForPackage(userId, packageName);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onRebindingCompanionApplicationTimeout */
    public void m2679xa0a26c68(int userId, String packageName) {
        this.mScheduledForRebindingCompanionApplications.removePackage(userId, packageName);
        this.mCallback.onRebindCompanionApplicationTimeout(userId, packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyCompanionApplicationDeviceAppeared(AssociationInfo association) {
        int userId = association.getUserId();
        String packageName = association.getPackageName();
        CompanionDeviceServiceConnector primaryServiceConnector = getPrimaryServiceConnector(userId, packageName);
        if (primaryServiceConnector == null) {
            return;
        }
        primaryServiceConnector.postOnDeviceAppeared(association);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyCompanionApplicationDeviceDisappeared(AssociationInfo association) {
        int userId = association.getUserId();
        String packageName = association.getPackageName();
        CompanionDeviceServiceConnector primaryServiceConnector = getPrimaryServiceConnector(userId, packageName);
        if (primaryServiceConnector == null) {
            return;
        }
        primaryServiceConnector.postOnDeviceDisappeared(association);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter out) {
        out.append("Companion Device Application Controller: \n");
        synchronized (this.mBoundCompanionApplications) {
            out.append("  Bound Companion Applications: ");
            if (this.mBoundCompanionApplications.size() == 0) {
                out.append("<empty>\n");
            } else {
                out.append("\n");
                this.mBoundCompanionApplications.dump(out);
            }
        }
        out.append("  Companion Applications Scheduled For Rebinding: ");
        if (this.mScheduledForRebindingCompanionApplications.size() == 0) {
            out.append("<empty>\n");
            return;
        }
        out.append("\n");
        this.mScheduledForRebindingCompanionApplications.dump(out);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPrimaryServiceBindingDied(int userId, String packageName) {
        synchronized (this.mBoundCompanionApplications) {
            this.mBoundCompanionApplications.removePackage(userId, packageName);
        }
        boolean shouldScheduleRebind = this.mCallback.onCompanionApplicationBindingDied(userId, packageName);
        if (shouldScheduleRebind) {
            scheduleRebinding(userId, packageName);
        }
    }

    private CompanionDeviceServiceConnector getPrimaryServiceConnector(int userId, String packageName) {
        List<CompanionDeviceServiceConnector> connectors;
        synchronized (this.mBoundCompanionApplications) {
            connectors = this.mBoundCompanionApplications.getValueForPackage(userId, packageName);
        }
        if (connectors != null) {
            return connectors.get(0);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class CompanionServicesRegister extends PerUser<Map<String, List<ComponentName>>> {
        private CompanionServicesRegister() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        public synchronized Map<String, List<ComponentName>> forUser(int userId) {
            return (Map) super.forUser(userId);
        }

        synchronized List<ComponentName> forPackage(int userId, String packageName) {
            return forUser(userId).getOrDefault(packageName, Collections.emptyList());
        }

        synchronized void invalidate(int userId) {
            remove(userId);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        public final Map<String, List<ComponentName>> create(int userId) {
            return PackageUtils.getCompanionServicesForUser(CompanionApplicationController.this.mContext, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AndroidPackageMap<T> extends SparseArray<Map<String, T>> {
        private AndroidPackageMap() {
        }

        void setValueForPackage(int userId, String packageName, T value) {
            Map<String, T> forUser = get(userId);
            if (forUser == null) {
                forUser = new HashMap();
                put(userId, forUser);
            }
            forUser.put(packageName, value);
        }

        boolean containsValueForPackage(int userId, String packageName) {
            Map<String, T> map = get(userId);
            return map != null && map.containsKey(packageName);
        }

        T getValueForPackage(int userId, String packageName) {
            Map<String, T> forUser = get(userId);
            if (forUser != null) {
                return forUser.get(packageName);
            }
            return null;
        }

        T removePackage(int userId, String packageName) {
            Map<String, T> forUser = get(userId);
            if (forUser == null) {
                return null;
            }
            return forUser.remove(packageName);
        }

        void dump() {
            if (size() == 0) {
                Log.d(CompanionApplicationController.TAG, "<empty>");
                return;
            }
            for (int i = 0; i < size(); i++) {
                int userId = keyAt(i);
                Map<String, T> forUser = get(userId);
                if (forUser.isEmpty()) {
                    Log.d(CompanionApplicationController.TAG, "u" + userId + ": <empty>");
                }
                for (Map.Entry<String, T> packageValue : forUser.entrySet()) {
                    String packageName = packageValue.getKey();
                    T value = packageValue.getValue();
                    Log.d(CompanionApplicationController.TAG, "u" + userId + "\\" + packageName + " -> " + value);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(PrintWriter out) {
            for (int i = 0; i < size(); i++) {
                int userId = keyAt(i);
                Map<String, T> forUser = get(userId);
                if (forUser.isEmpty()) {
                    out.append("    u").append((CharSequence) String.valueOf(userId)).append(": <empty>\n");
                }
                for (Map.Entry<String, T> packageValue : forUser.entrySet()) {
                    String packageName = packageValue.getKey();
                    T value = packageValue.getValue();
                    out.append("    u").append((CharSequence) String.valueOf(userId)).append("\\").append((CharSequence) packageName).append(" -> ").append((CharSequence) value.toString()).append('\n');
                }
            }
        }
    }
}
