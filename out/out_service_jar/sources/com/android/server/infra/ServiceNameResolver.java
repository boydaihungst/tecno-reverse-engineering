package com.android.server.infra;

import java.io.PrintWriter;
/* loaded from: classes.dex */
public interface ServiceNameResolver {

    /* loaded from: classes.dex */
    public interface NameResolverListener {
        void onNameResolved(int i, String str, boolean z);
    }

    void dumpShort(PrintWriter printWriter);

    void dumpShort(PrintWriter printWriter, int i);

    String getDefaultServiceName(int i);

    default void setOnTemporaryServiceNameChangedCallback(NameResolverListener callback) {
    }

    default String[] getDefaultServiceNameList(int userId) {
        if (isConfiguredInMultipleMode()) {
            throw new UnsupportedOperationException("getting default service list not supported");
        }
        return new String[]{getDefaultServiceName(userId)};
    }

    default boolean isConfiguredInMultipleMode() {
        return false;
    }

    default String getServiceName(int userId) {
        return getDefaultServiceName(userId);
    }

    default String[] getServiceNameList(int userId) {
        return getDefaultServiceNameList(userId);
    }

    default boolean isTemporary(int userId) {
        return false;
    }

    default void setTemporaryService(int userId, String componentName, int durationMs) {
        throw new UnsupportedOperationException("temporary user not supported");
    }

    default void setTemporaryServices(int userId, String[] componentNames, int durationMs) {
        throw new UnsupportedOperationException("temporary user not supported");
    }

    default void resetTemporaryService(int userId) {
        throw new UnsupportedOperationException("temporary user not supported");
    }

    default boolean setDefaultServiceEnabled(int userId, boolean enabled) {
        throw new UnsupportedOperationException("changing default service not supported");
    }

    default boolean isDefaultServiceEnabled(int userId) {
        throw new UnsupportedOperationException("checking default service not supported");
    }
}
