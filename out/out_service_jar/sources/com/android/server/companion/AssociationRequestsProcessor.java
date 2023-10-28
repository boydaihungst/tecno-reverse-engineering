package com.android.server.companion;

import android.app.PendingIntent;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.IAssociationRequestCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.net.MacAddress;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FunctionalUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AssociationRequestsProcessor {
    private static final int ASSOCIATE_WITHOUT_PROMPT_MAX_PER_TIME_WINDOW = 5;
    private static final long ASSOCIATE_WITHOUT_PROMPT_WINDOW_MS = 3600000;
    private static final ComponentName ASSOCIATION_REQUEST_APPROVAL_ACTIVITY = ComponentName.createRelative("com.android.companiondevicemanager", ".CompanionDeviceActivity");
    private static final String EXTRA_APPLICATION_CALLBACK = "application_callback";
    private static final String EXTRA_ASSOCIATION = "association";
    private static final String EXTRA_ASSOCIATION_REQUEST = "association_request";
    private static final String EXTRA_MAC_ADDRESS = "mac_address";
    private static final String EXTRA_RESULT_RECEIVER = "result_receiver";
    private static final int RESULT_CODE_ASSOCIATION_APPROVED = 0;
    private static final int RESULT_CODE_ASSOCIATION_CREATED = 0;
    private static final String TAG = "CompanionDevice_AssociationRequestsProcessor";
    private final AssociationStore mAssociationStore;
    private final Context mContext;
    private final ResultReceiver mOnRequestConfirmationReceiver = new ResultReceiver(Handler.getMain()) { // from class: com.android.server.companion.AssociationRequestsProcessor.1
        @Override // android.os.ResultReceiver
        protected void onReceiveResult(int resultCode, Bundle data) {
            MacAddress macAddress;
            if (resultCode != 0) {
                Slog.w(AssociationRequestsProcessor.TAG, "Unknown result code:" + resultCode);
                return;
            }
            AssociationRequest request = (AssociationRequest) data.getParcelable(AssociationRequestsProcessor.EXTRA_ASSOCIATION_REQUEST);
            IAssociationRequestCallback callback = IAssociationRequestCallback.Stub.asInterface(data.getBinder(AssociationRequestsProcessor.EXTRA_APPLICATION_CALLBACK));
            ResultReceiver resultReceiver = (ResultReceiver) data.getParcelable(AssociationRequestsProcessor.EXTRA_RESULT_RECEIVER);
            Objects.requireNonNull(request);
            Objects.requireNonNull(callback);
            Objects.requireNonNull(resultReceiver);
            if (request.isSelfManaged()) {
                macAddress = null;
            } else {
                macAddress = (MacAddress) data.getParcelable(AssociationRequestsProcessor.EXTRA_MAC_ADDRESS);
                Objects.requireNonNull(macAddress);
            }
            AssociationRequestsProcessor.this.processAssociationRequestApproval(request, callback, resultReceiver, macAddress);
        }
    };
    private final PackageManagerInternal mPackageManager;
    private final CompanionDeviceManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AssociationRequestsProcessor(CompanionDeviceManagerService service, AssociationStore associationStore) {
        this.mContext = service.getContext();
        this.mService = service;
        this.mPackageManager = service.mPackageManagerInternal;
        this.mAssociationStore = associationStore;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void processNewAssociationRequest(AssociationRequest request, String packageName, int userId, IAssociationRequestCallback callback) {
        Objects.requireNonNull(request, "Request MUST NOT be null");
        if (request.isSelfManaged()) {
            Objects.requireNonNull(request.getDisplayName(), "AssociationRequest.displayName MUST NOT be null.");
        }
        Objects.requireNonNull(packageName, "Package name MUST NOT be null");
        Objects.requireNonNull(callback, "Callback MUST NOT be null");
        int packageUid = this.mPackageManager.getPackageUid(packageName, 0L, userId);
        PermissionsUtils.enforcePermissionsForAssociation(this.mContext, request, packageUid);
        PackageUtils.enforceUsesCompanionDeviceFeature(this.mContext, userId, packageName);
        if (request.isSelfManaged() && !request.isForceConfirmation() && !willAddRoleHolder(request, packageName, userId)) {
            createAssociationAndNotifyApplication(request, packageName, userId, null, callback);
            return;
        }
        request.setPackageName(packageName);
        request.setUserId(userId);
        request.setSkipPrompt(mayAssociateWithoutPrompt(packageName, userId));
        Bundle extras = new Bundle();
        extras.putParcelable(EXTRA_ASSOCIATION_REQUEST, request);
        extras.putBinder(EXTRA_APPLICATION_CALLBACK, callback.asBinder());
        extras.putParcelable(EXTRA_RESULT_RECEIVER, prepareForIpc(this.mOnRequestConfirmationReceiver));
        Intent intent = new Intent();
        intent.setComponent(ASSOCIATION_REQUEST_APPROVAL_ACTIVITY);
        intent.putExtras(extras);
        long token = Binder.clearCallingIdentity();
        try {
            PendingIntent pendingIntent = PendingIntent.getActivityAsUser(this.mContext, packageUid, intent, 1409286144, null, UserHandle.CURRENT);
            try {
                callback.onAssociationPending(pendingIntent);
            } catch (RemoteException e) {
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processAssociationRequestApproval(AssociationRequest request, IAssociationRequestCallback callback, ResultReceiver resultReceiver, MacAddress macAddress) {
        String packageName = request.getPackageName();
        int userId = request.getUserId();
        int packageUid = this.mPackageManager.getPackageUid(packageName, 0L, userId);
        try {
            PermissionsUtils.enforcePermissionsForAssociation(this.mContext, request, packageUid);
            AssociationInfo association = createAssociationAndNotifyApplication(request, packageName, userId, macAddress, callback);
            Bundle data = new Bundle();
            data.putParcelable(EXTRA_ASSOCIATION, association);
            resultReceiver.send(0, data);
        } catch (SecurityException e) {
            try {
                callback.onFailure(e.getMessage());
            } catch (RemoteException e2) {
            }
        }
    }

    private AssociationInfo createAssociationAndNotifyApplication(AssociationRequest request, String packageName, int userId, MacAddress macAddress, IAssociationRequestCallback callback) {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            AssociationInfo association = this.mService.createAssociation(userId, packageName, macAddress, request.getDisplayName(), request.getDeviceProfile(), request.isSelfManaged());
            try {
                callback.onAssociationCreated(association);
            } catch (RemoteException e) {
            }
            return association;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private boolean willAddRoleHolder(AssociationRequest request, final String packageName, final int userId) {
        final String deviceProfile = request.getDeviceProfile();
        if (deviceProfile == null) {
            return false;
        }
        boolean isRoleHolder = ((Boolean) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.companion.AssociationRequestsProcessor$$ExternalSyntheticLambda0
            public final Object getOrThrow() {
                return AssociationRequestsProcessor.this.m2675x506a646a(userId, packageName, deviceProfile);
            }
        })).booleanValue();
        return !isRoleHolder;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$willAddRoleHolder$0$com-android-server-companion-AssociationRequestsProcessor  reason: not valid java name */
    public /* synthetic */ Boolean m2675x506a646a(int userId, String packageName, String deviceProfile) throws Exception {
        return Boolean.valueOf(RolesUtils.isRoleHolder(this.mContext, userId, packageName, deviceProfile));
    }

    private boolean mayAssociateWithoutPrompt(String packageName, int userId) {
        String[] allowlistedPackages = this.mContext.getResources().getStringArray(17236015);
        int i = 0;
        if (ArrayUtils.contains(allowlistedPackages, packageName)) {
            long now = System.currentTimeMillis();
            List<AssociationInfo> associationForPackage = this.mAssociationStore.getAssociationsForPackage(userId, packageName);
            int recent = 0;
            for (AssociationInfo association : associationForPackage) {
                boolean isRecent = now - association.getTimeApprovedMs() < 3600000;
                if (isRecent && (recent = recent + 1) >= 5) {
                    Slog.w(TAG, "Too many associations: " + packageName + " already associated " + recent + " devices within the last 3600000ms");
                    return false;
                }
            }
            String[] allowlistedPackagesSignatureDigests = this.mContext.getResources().getStringArray(17236014);
            Set<String> allowlistedSignatureDigestsForRequestingPackage = new HashSet<>();
            for (int i2 = 0; i2 < allowlistedPackages.length; i2++) {
                if (allowlistedPackages[i2].equals(packageName)) {
                    String digest = allowlistedPackagesSignatureDigests[i2].replaceAll(":", "");
                    allowlistedSignatureDigestsForRequestingPackage.add(digest);
                }
            }
            Signature[] requestingPackageSignatures = this.mPackageManager.getPackage(packageName).getSigningDetails().getSignatures();
            String[] requestingPackageSignatureDigests = android.util.PackageUtils.computeSignaturesSha256Digests(requestingPackageSignatures);
            boolean requestingPackageSignatureAllowlisted = false;
            int length = requestingPackageSignatureDigests.length;
            while (true) {
                if (i >= length) {
                    break;
                }
                String signatureDigest = requestingPackageSignatureDigests[i];
                if (!allowlistedSignatureDigestsForRequestingPackage.contains(signatureDigest)) {
                    i++;
                } else {
                    requestingPackageSignatureAllowlisted = true;
                    break;
                }
            }
            if (!requestingPackageSignatureAllowlisted) {
                Slog.w(TAG, "Certificate mismatch for allowlisted package " + packageName);
            }
            return requestingPackageSignatureAllowlisted;
        }
        return false;
    }

    private static <T extends ResultReceiver> ResultReceiver prepareForIpc(T resultReceiver) {
        Parcel parcel = Parcel.obtain();
        resultReceiver.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ResultReceiver ipcFriendly = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return ipcFriendly;
    }
}
