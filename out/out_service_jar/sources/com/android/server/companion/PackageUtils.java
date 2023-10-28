package com.android.server.companion;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FunctionalUtils;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
/* loaded from: classes.dex */
final class PackageUtils {
    private static final Intent COMPANION_SERVICE_INTENT = new Intent("android.companion.CompanionDeviceService");
    private static final String PROPERTY_PRIMARY_TAG = "android.companion.PROPERTY_PRIMARY_COMPANION_DEVICE_SERVICE";

    PackageUtils() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static PackageInfo getPackageInfo(Context context, final int userId, final String packageName) {
        final PackageManager pm = context.getPackageManager();
        final PackageManager.PackageInfoFlags flags = PackageManager.PackageInfoFlags.of(20480L);
        return (PackageInfo) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.companion.PackageUtils$$ExternalSyntheticLambda1
            public final Object getOrThrow() {
                PackageInfo packageInfoAsUser;
                packageInfoAsUser = pm.getPackageInfoAsUser(packageName, flags, userId);
                return packageInfoAsUser;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void enforceUsesCompanionDeviceFeature(Context context, int userId, String packageName) {
        boolean requested = ArrayUtils.contains(getPackageInfo(context, userId, packageName).reqFeatures, "android.software.companion_device_setup");
        if (requested) {
            throw new IllegalStateException("Must declare uses-feature android.software.companion_device_setup in manifest to use this API");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Map<String, List<ComponentName>> getCompanionServicesForUser(Context context, int userId) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> companionServices = pm.queryIntentServicesAsUser(COMPANION_SERVICE_INTENT, PackageManager.ResolveInfoFlags.of(0L), userId);
        Map<String, List<ComponentName>> packageNameToServiceInfoList = new HashMap<>();
        for (ResolveInfo resolveInfo : companionServices) {
            ServiceInfo service = resolveInfo.serviceInfo;
            boolean requiresPermission = "android.permission.BIND_COMPANION_DEVICE_SERVICE".equals(resolveInfo.serviceInfo.permission);
            if (!requiresPermission) {
                Slog.w("CompanionDeviceManagerService", "CompanionDeviceService " + service.getComponentName().flattenToShortString() + " must require android.permission.BIND_COMPANION_DEVICE_SERVICE");
            } else {
                LinkedList<ComponentName> services = (LinkedList) packageNameToServiceInfoList.computeIfAbsent(service.packageName, new Function() { // from class: com.android.server.companion.PackageUtils$$ExternalSyntheticLambda0
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return PackageUtils.lambda$getCompanionServicesForUser$1((String) obj);
                    }
                });
                ComponentName componentName = service.getComponentName();
                if (isPrimaryCompanionDeviceService(pm, componentName)) {
                    services.addFirst(componentName);
                } else {
                    services.addLast(componentName);
                }
            }
        }
        return packageNameToServiceInfoList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ List lambda$getCompanionServicesForUser$1(String it) {
        return new LinkedList();
    }

    private static boolean isPrimaryCompanionDeviceService(PackageManager pm, ComponentName componentName) {
        try {
            return pm.getProperty(PROPERTY_PRIMARY_TAG, componentName).getBoolean();
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
