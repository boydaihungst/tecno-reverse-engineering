package com.android.server.pm.verify.domain;

import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.verify.domain.DomainVerificationState;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.PackageUtils;
import android.util.SparseArray;
import com.android.internal.util.CollectionUtils;
import com.android.server.pm.Computer;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.verify.domain.models.DomainVerificationInternalUserState;
import com.android.server.pm.verify.domain.models.DomainVerificationPkgState;
import com.android.server.pm.verify.domain.models.DomainVerificationStateMap;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes2.dex */
public class DomainVerificationDebug {
    public static final boolean DEBUG_ALL = false;
    public static final boolean DEBUG_ANY = false;
    public static final boolean DEBUG_APPROVAL = false;
    public static final boolean DEBUG_BROADCASTS = false;
    public static final boolean DEBUG_PROXIES = false;
    private final DomainVerificationCollector mCollector;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DomainVerificationDebug(DomainVerificationCollector collector) {
        this.mCollector = collector;
    }

    public void printState(IndentingPrintWriter writer, String packageName, Integer userId, Computer snapshot, DomainVerificationStateMap<DomainVerificationPkgState> stateMap) throws PackageManager.NameNotFoundException {
        int index;
        int size;
        ArrayMap<String, Integer> reusedMap = new ArrayMap<>();
        ArraySet<String> reusedSet = new ArraySet<>();
        if (packageName == null) {
            int size2 = stateMap.size();
            int index2 = 0;
            while (index2 < size2) {
                DomainVerificationPkgState pkgState = stateMap.valueAt(index2);
                String pkgName = pkgState.getPackageName();
                PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(pkgName);
                if (pkgSetting == null) {
                    index = index2;
                    size = size2;
                } else if (pkgSetting.getPkg() == null) {
                    index = index2;
                    size = size2;
                } else {
                    boolean wasHeaderPrinted = printState(writer, pkgState, pkgSetting.getPkg(), reusedMap, false);
                    index = index2;
                    size = size2;
                    printState(writer, pkgState, pkgSetting.getPkg(), userId, reusedSet, wasHeaderPrinted);
                }
                index2 = index + 1;
                size2 = size;
            }
            return;
        }
        DomainVerificationPkgState pkgState2 = stateMap.get(packageName);
        if (pkgState2 == null) {
            throw DomainVerificationUtils.throwPackageUnavailable(packageName);
        }
        PackageStateInternal pkgSetting2 = snapshot.getPackageStateInternal(packageName);
        if (pkgSetting2 == null || pkgSetting2.getPkg() == null) {
            throw DomainVerificationUtils.throwPackageUnavailable(packageName);
        }
        AndroidPackage pkg = pkgSetting2.getPkg();
        printState(writer, pkgState2, pkg, reusedMap, false);
        printState(writer, pkgState2, pkg, userId, reusedSet, true);
    }

    public void printOwners(IndentingPrintWriter writer, String domain, SparseArray<SparseArray<List<String>>> userIdToApprovalLevelToOwners) {
        SparseArray<SparseArray<List<String>>> sparseArray = userIdToApprovalLevelToOwners;
        writer.println(domain + ":");
        writer.increaseIndent();
        if (userIdToApprovalLevelToOwners.size() == 0) {
            writer.println("none");
            writer.decreaseIndent();
            return;
        }
        int usersSize = userIdToApprovalLevelToOwners.size();
        int userIndex = 0;
        while (userIndex < usersSize) {
            int userId = sparseArray.keyAt(userIndex);
            SparseArray<List<String>> approvalLevelToOwners = sparseArray.valueAt(userIndex);
            if (approvalLevelToOwners.size() != 0) {
                boolean printedUserHeader = false;
                int approvalsSize = approvalLevelToOwners.size();
                for (int approvalIndex = 0; approvalIndex < approvalsSize; approvalIndex++) {
                    int approvalLevel = approvalLevelToOwners.keyAt(approvalIndex);
                    if (approvalLevel >= -1) {
                        if (!printedUserHeader) {
                            writer.println("User " + userId + ":");
                            writer.increaseIndent();
                            printedUserHeader = true;
                        }
                        String approvalString = DomainVerificationManagerInternal.approvalLevelToDebugString(approvalLevel);
                        List<String> owners = approvalLevelToOwners.valueAt(approvalIndex);
                        writer.println(approvalString + "[" + approvalLevel + "]:");
                        writer.increaseIndent();
                        if (owners.size() == 0) {
                            writer.println("none");
                            writer.decreaseIndent();
                        } else {
                            int ownersIndex = 0;
                            for (int ownersSize = owners.size(); ownersIndex < ownersSize; ownersSize = ownersSize) {
                                writer.println(owners.get(ownersIndex));
                                ownersIndex++;
                            }
                            writer.decreaseIndent();
                        }
                    }
                }
                if (printedUserHeader) {
                    writer.decreaseIndent();
                }
            }
            userIndex++;
            sparseArray = userIdToApprovalLevelToOwners;
        }
        writer.decreaseIndent();
    }

    boolean printState(IndentingPrintWriter writer, DomainVerificationPkgState pkgState, AndroidPackage pkg, ArrayMap<String, Integer> reusedMap, boolean wasHeaderPrinted) {
        reusedMap.clear();
        reusedMap.putAll((ArrayMap<? extends String, ? extends Integer>) pkgState.getStateMap());
        ArraySet<String> declaredDomains = this.mCollector.collectValidAutoVerifyDomains(pkg);
        int declaredSize = declaredDomains.size();
        for (int declaredIndex = 0; declaredIndex < declaredSize; declaredIndex++) {
            String domain = declaredDomains.valueAt(declaredIndex);
            reusedMap.putIfAbsent(domain, 0);
        }
        boolean printedHeader = false;
        if (!reusedMap.isEmpty()) {
            if (!wasHeaderPrinted) {
                Signature[] signatures = pkg.getSigningDetails().getSignatures();
                String signaturesDigest = signatures == null ? null : Arrays.toString(PackageUtils.computeSignaturesSha256Digests(pkg.getSigningDetails().getSignatures(), ":"));
                writer.println(pkgState.getPackageName() + ":");
                writer.increaseIndent();
                writer.println("ID: " + pkgState.getId());
                writer.println("Signatures: " + signaturesDigest);
                writer.decreaseIndent();
                printedHeader = true;
            }
            writer.increaseIndent();
            ArraySet<String> invalidDomains = this.mCollector.collectInvalidAutoVerifyDomains(pkg);
            if (!invalidDomains.isEmpty()) {
                writer.println("Invalid autoVerify domains:");
                writer.increaseIndent();
                int size = invalidDomains.size();
                for (int index = 0; index < size; index++) {
                    writer.println(invalidDomains.valueAt(index));
                }
                writer.decreaseIndent();
            }
            writer.println("Domain verification state:");
            writer.increaseIndent();
            int stateSize = reusedMap.size();
            for (int stateIndex = 0; stateIndex < stateSize; stateIndex++) {
                String domain2 = reusedMap.keyAt(stateIndex);
                Integer state = reusedMap.valueAt(stateIndex);
                writer.print(domain2);
                writer.print(": ");
                writer.println(DomainVerificationState.stateToDebugString(state.intValue()));
            }
            writer.decreaseIndent();
            writer.decreaseIndent();
        }
        return printedHeader;
    }

    void printState(IndentingPrintWriter writer, DomainVerificationPkgState pkgState, AndroidPackage pkg, Integer userId, ArraySet<String> reusedSet, boolean wasHeaderPrinted) {
        if (userId == null) {
            return;
        }
        ArraySet<String> allWebDomains = this.mCollector.collectAllWebDomains(pkg);
        SparseArray<DomainVerificationInternalUserState> userStates = pkgState.getUserStates();
        if (userId.intValue() == -1) {
            int size = userStates.size();
            if (size == 0) {
                printState(writer, pkgState, userId.intValue(), null, reusedSet, allWebDomains, wasHeaderPrinted);
                return;
            }
            for (int index = 0; index < size; index++) {
                DomainVerificationInternalUserState userState = userStates.valueAt(index);
                printState(writer, pkgState, userState.getUserId(), userState, reusedSet, allWebDomains, wasHeaderPrinted);
            }
            return;
        }
        printState(writer, pkgState, userId.intValue(), userStates.get(userId.intValue()), reusedSet, allWebDomains, wasHeaderPrinted);
    }

    boolean printState(IndentingPrintWriter writer, DomainVerificationPkgState pkgState, int userId, DomainVerificationInternalUserState userState, ArraySet<String> reusedSet, ArraySet<String> allWebDomains, boolean wasHeaderPrinted) {
        reusedSet.clear();
        reusedSet.addAll((ArraySet<? extends String>) allWebDomains);
        if (userState != null) {
            reusedSet.removeAll((ArraySet<? extends String>) userState.getEnabledHosts());
        }
        boolean printedHeader = false;
        ArraySet<String> enabledHosts = userState == null ? null : userState.getEnabledHosts();
        int enabledSize = CollectionUtils.size(enabledHosts);
        int disabledSize = reusedSet.size();
        if (enabledSize > 0 || disabledSize > 0) {
            if (!wasHeaderPrinted) {
                writer.println(pkgState.getPackageName() + " " + pkgState.getId() + ":");
                printedHeader = true;
            }
            boolean isLinkHandlingAllowed = userState == null || userState.isLinkHandlingAllowed();
            writer.increaseIndent();
            writer.print("User ");
            writer.print(userId == -1 ? "all" : Integer.valueOf(userId));
            writer.println(":");
            writer.increaseIndent();
            writer.print("Verification link handling allowed: ");
            writer.println(isLinkHandlingAllowed);
            writer.println("Selection state:");
            writer.increaseIndent();
            if (enabledSize > 0) {
                writer.println("Enabled:");
                writer.increaseIndent();
                for (int enabledIndex = 0; enabledIndex < enabledSize; enabledIndex++) {
                    writer.println(enabledHosts.valueAt(enabledIndex));
                }
                writer.decreaseIndent();
            }
            if (disabledSize > 0) {
                writer.println("Disabled:");
                writer.increaseIndent();
                for (int disabledIndex = 0; disabledIndex < disabledSize; disabledIndex++) {
                    writer.println(reusedSet.valueAt(disabledIndex));
                }
                writer.decreaseIndent();
            }
            writer.decreaseIndent();
            writer.decreaseIndent();
            writer.decreaseIndent();
        }
        return printedHeader;
    }
}
