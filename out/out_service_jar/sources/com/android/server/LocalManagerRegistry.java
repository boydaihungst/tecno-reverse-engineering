package com.android.server;

import android.annotation.SystemApi;
import android.util.ArrayMap;
import java.util.Map;
import java.util.Objects;
@SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
/* loaded from: classes.dex */
public final class LocalManagerRegistry {
    private static final Map<Class<?>, Object> sManagers = new ArrayMap();

    private LocalManagerRegistry() {
    }

    public static <T> T getManager(Class<T> managerClass) {
        T t;
        Map<Class<?>, Object> map = sManagers;
        synchronized (map) {
            t = (T) map.get(managerClass);
        }
        return t;
    }

    public static <T> void addManager(Class<T> managerClass, T manager) {
        Objects.requireNonNull(managerClass, "managerClass");
        Objects.requireNonNull(manager, "manager");
        Map<Class<?>, Object> map = sManagers;
        synchronized (map) {
            if (map.containsKey(managerClass)) {
                throw new IllegalStateException(managerClass.getName() + " is already registered");
            }
            map.put(managerClass, manager);
        }
    }
}
