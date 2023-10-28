package com.android.framework.protobuf;
/* loaded from: classes4.dex */
final class Android {
    private static final boolean IS_ROBOLECTRIC;
    private static final Class<?> MEMORY_CLASS = getClassForName("libcore.io.Memory");

    Android() {
    }

    static {
        IS_ROBOLECTRIC = getClassForName("org.robolectric.Robolectric") != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isOnAndroidDevice() {
        return (MEMORY_CLASS == null || IS_ROBOLECTRIC) ? false : true;
    }

    static Class<?> getMemoryClass() {
        return MEMORY_CLASS;
    }

    private static <T> Class<T> getClassForName(String name) {
        try {
            return (Class<T>) Class.forName(name);
        } catch (Throwable th) {
            return null;
        }
    }
}
