package com.android.server.pm;

import android.annotation.SystemApi;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
@SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
/* loaded from: classes2.dex */
public interface PackageManagerLocal {
    public static final int FLAG_STORAGE_CE = 2;
    public static final int FLAG_STORAGE_DE = 1;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface StorageFlags {
    }

    void reconcileSdkData(String str, String str2, List<String> list, int i, int i2, int i3, String str3, int i4) throws IOException;
}
