package com.android.server.pm;
/* loaded from: classes2.dex */
final class ReconcileFailure extends PackageManagerException {
    /* JADX INFO: Access modifiers changed from: package-private */
    public ReconcileFailure(String message) {
        super("Reconcile failed: " + message);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ReconcileFailure(int reason, String message) {
        super(reason, "Reconcile failed: " + message);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ReconcileFailure(PackageManagerException e) {
        this(e.error, e.getMessage());
    }
}
