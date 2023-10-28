package com.android.server;

import android.content.ComponentName;
import android.content.Context;
import android.os.CarrierAssociatedAppEntry;
import android.os.ISystemConfig;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import com.android.internal.util.ArrayUtils;
import com.android.server.SystemConfigService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
/* loaded from: classes.dex */
public class SystemConfigService extends SystemService {
    private final Context mContext;
    private final ISystemConfig.Stub mInterface;

    /* renamed from: com.android.server.SystemConfigService$1  reason: invalid class name */
    /* loaded from: classes.dex */
    class AnonymousClass1 extends ISystemConfig.Stub {
        AnonymousClass1() {
        }

        public List<String> getDisabledUntilUsedPreinstalledCarrierApps() {
            SystemConfigService.this.mContext.enforceCallingOrSelfPermission("android.permission.READ_CARRIER_APP_INFO", "getDisabledUntilUsedPreInstalledCarrierApps requires READ_CARRIER_APP_INFO");
            return new ArrayList(SystemConfig.getInstance().getDisabledUntilUsedPreinstalledCarrierApps());
        }

        public Map getDisabledUntilUsedPreinstalledCarrierAssociatedApps() {
            SystemConfigService.this.mContext.enforceCallingOrSelfPermission("android.permission.READ_CARRIER_APP_INFO", "getDisabledUntilUsedPreInstalledCarrierAssociatedApps requires READ_CARRIER_APP_INFO");
            return (Map) SystemConfig.getInstance().getDisabledUntilUsedPreinstalledCarrierAssociatedApps().entrySet().stream().collect(Collectors.toMap(new Function() { // from class: com.android.server.SystemConfigService$1$$ExternalSyntheticLambda1
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return (String) ((Map.Entry) obj).getKey();
                }
            }, new Function() { // from class: com.android.server.SystemConfigService$1$$ExternalSyntheticLambda2
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return SystemConfigService.AnonymousClass1.lambda$getDisabledUntilUsedPreinstalledCarrierAssociatedApps$1((Map.Entry) obj);
                }
            }));
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ List lambda$getDisabledUntilUsedPreinstalledCarrierAssociatedApps$1(Map.Entry e) {
            return (List) ((List) e.getValue()).stream().map(new Function() { // from class: com.android.server.SystemConfigService$1$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    String str;
                    str = ((CarrierAssociatedAppEntry) obj).packageName;
                    return str;
                }
            }).collect(Collectors.toList());
        }

        public Map getDisabledUntilUsedPreinstalledCarrierAssociatedAppEntries() {
            SystemConfigService.this.mContext.enforceCallingOrSelfPermission("android.permission.READ_CARRIER_APP_INFO", "getDisabledUntilUsedPreInstalledCarrierAssociatedAppEntries requires READ_CARRIER_APP_INFO");
            return SystemConfig.getInstance().getDisabledUntilUsedPreinstalledCarrierAssociatedApps();
        }

        public int[] getSystemPermissionUids(String permissionName) {
            SystemConfigService.this.mContext.enforceCallingOrSelfPermission("android.permission.GET_RUNTIME_PERMISSIONS", "getSystemPermissionUids requires GET_RUNTIME_PERMISSIONS");
            List<Integer> uids = new ArrayList<>();
            SparseArray<ArraySet<String>> systemPermissions = SystemConfig.getInstance().getSystemPermissions();
            for (int i = 0; i < systemPermissions.size(); i++) {
                ArraySet<String> permissions = systemPermissions.valueAt(i);
                if (permissions != null && permissions.contains(permissionName)) {
                    uids.add(Integer.valueOf(systemPermissions.keyAt(i)));
                }
            }
            return ArrayUtils.convertToIntArray(uids);
        }

        public List<ComponentName> getEnabledComponentOverrides(String packageName) {
            ArrayMap<String, Boolean> systemComponents = SystemConfig.getInstance().getComponentsEnabledStates(packageName);
            List<ComponentName> enabledComponent = new ArrayList<>();
            if (systemComponents != null) {
                for (Map.Entry<String, Boolean> entry : systemComponents.entrySet()) {
                    if (Boolean.TRUE.equals(entry.getValue())) {
                        enabledComponent.add(new ComponentName(packageName, entry.getKey()));
                    }
                }
            }
            return enabledComponent;
        }
    }

    public SystemConfigService(Context context) {
        super(context);
        this.mInterface = new AnonymousClass1();
        this.mContext = context;
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("system_config", this.mInterface);
    }
}
