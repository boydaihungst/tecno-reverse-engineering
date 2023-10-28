package com.android.server.pm;

import com.android.server.pm.ApkChecksums;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class PackageManagerService$$ExternalSyntheticLambda6 implements ApkChecksums.Injector.Producer {
    public final /* synthetic */ PackageManagerServiceInjector f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.server.pm.PackageManagerService.lambda$requestChecksumsInternal$7$com-android-server-pm-PackageManagerService(android.os.Handler, java.util.List, int, int, java.lang.String, java.security.cert.Certificate[], android.content.pm.IOnChecksumsReadyListener):void, com.android.server.pm.PackageManagerService.lambda$requestFileChecksums$3$com-android-server-pm-PackageManagerService(android.os.Handler, java.util.List, int, int, java.lang.String, java.security.cert.Certificate[], android.content.pm.IOnChecksumsReadyListener):void] */
    public /* synthetic */ PackageManagerService$$ExternalSyntheticLambda6(PackageManagerServiceInjector packageManagerServiceInjector) {
        this.f$0 = packageManagerServiceInjector;
    }

    @Override // com.android.server.pm.ApkChecksums.Injector.Producer
    public final Object produce() {
        return this.f$0.getIncrementalManager();
    }
}
