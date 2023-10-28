package com.android.server.app;

import android.content.pm.PackageManager;
import android.os.UserHandle;
/* loaded from: classes.dex */
final class GameClassifierImpl implements GameClassifier {
    private final PackageManager mPackageManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public GameClassifierImpl(PackageManager packageManager) {
        this.mPackageManager = packageManager;
    }

    @Override // com.android.server.app.GameClassifier
    public boolean isGame(String packageName, UserHandle userHandle) {
        try {
            int applicationCategory = this.mPackageManager.getApplicationInfoAsUser(packageName, 0, userHandle.getIdentifier()).category;
            return applicationCategory == 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
