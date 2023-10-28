package com.android.framework.protobuf;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes4.dex */
public final class ExtensionRegistryFactory {
    static final Class<?> EXTENSION_REGISTRY_CLASS = reflectExtensionRegistry();
    static final String FULL_REGISTRY_CLASS_NAME = "com.android.framework.protobuf.ExtensionRegistry";

    ExtensionRegistryFactory() {
    }

    static Class<?> reflectExtensionRegistry() {
        try {
            return Class.forName(FULL_REGISTRY_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static ExtensionRegistryLite create() {
        if (EXTENSION_REGISTRY_CLASS != null) {
            try {
                return invokeSubclassFactory("newInstance");
            } catch (Exception e) {
            }
        }
        return new ExtensionRegistryLite();
    }

    public static ExtensionRegistryLite createEmpty() {
        if (EXTENSION_REGISTRY_CLASS != null) {
            try {
                return invokeSubclassFactory("getEmptyRegistry");
            } catch (Exception e) {
            }
        }
        return ExtensionRegistryLite.EMPTY_REGISTRY_LITE;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isFullRegistry(ExtensionRegistryLite registry) {
        Class<?> cls = EXTENSION_REGISTRY_CLASS;
        return cls != null && cls.isAssignableFrom(registry.getClass());
    }

    private static final ExtensionRegistryLite invokeSubclassFactory(String methodName) throws Exception {
        return (ExtensionRegistryLite) EXTENSION_REGISTRY_CLASS.getDeclaredMethod(methodName, new Class[0]).invoke(null, new Object[0]);
    }
}
