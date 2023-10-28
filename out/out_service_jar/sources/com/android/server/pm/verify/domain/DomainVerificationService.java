package com.android.server.pm.verify.domain;

import android.content.Context;
import android.content.Intent;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.verify.domain.DomainOwner;
import android.content.pm.verify.domain.DomainVerificationInfo;
import android.content.pm.verify.domain.DomainVerificationState;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.content.pm.verify.domain.IDomainVerificationManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.PackageUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.internal.util.CollectionUtils;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.compat.PlatformCompat;
import com.android.server.pm.Computer;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageStateUtils;
import com.android.server.pm.pkg.PackageUserStateInternal;
import com.android.server.pm.pkg.PackageUserStateUtils;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.verify.domain.DomainVerificationManagerInternal;
import com.android.server.pm.verify.domain.DomainVerificationShell;
import com.android.server.pm.verify.domain.models.DomainVerificationInternalUserState;
import com.android.server.pm.verify.domain.models.DomainVerificationPkgState;
import com.android.server.pm.verify.domain.models.DomainVerificationStateMap;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxy;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxyUnavailable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class DomainVerificationService extends SystemService implements DomainVerificationManagerInternal, DomainVerificationShell.Callback {
    public static final boolean DEBUG_APPROVAL = false;
    private static final long SETTINGS_API_V2 = 178111421;
    private static final String TAG = "DomainVerificationService";
    private final DomainVerificationStateMap<DomainVerificationPkgState> mAttachedPkgStates;
    private boolean mCanSendBroadcasts;
    private final DomainVerificationCollector mCollector;
    private DomainVerificationManagerInternal.Connection mConnection;
    private final DomainVerificationDebug mDebug;
    private final DomainVerificationEnforcer mEnforcer;
    private final DomainVerificationLegacySettings mLegacySettings;
    private final Object mLock;
    private final PlatformCompat mPlatformCompat;
    private DomainVerificationProxy mProxy;
    private final DomainVerificationSettings mSettings;
    private final DomainVerificationShell mShell;
    private final IDomainVerificationManager.Stub mStub;
    private final SystemConfig mSystemConfig;

    public DomainVerificationService(Context context, SystemConfig systemConfig, PlatformCompat platformCompat) {
        super(context);
        this.mAttachedPkgStates = new DomainVerificationStateMap<>();
        this.mLock = new Object();
        this.mStub = new DomainVerificationManagerStub(this);
        this.mProxy = new DomainVerificationProxyUnavailable();
        this.mSystemConfig = systemConfig;
        this.mPlatformCompat = platformCompat;
        DomainVerificationCollector domainVerificationCollector = new DomainVerificationCollector(platformCompat, systemConfig);
        this.mCollector = domainVerificationCollector;
        this.mSettings = new DomainVerificationSettings(domainVerificationCollector);
        this.mEnforcer = new DomainVerificationEnforcer(context);
        this.mDebug = new DomainVerificationDebug(domainVerificationCollector);
        this.mShell = new DomainVerificationShell(this);
        this.mLegacySettings = new DomainVerificationLegacySettings();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("domain_verification", this.mStub);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void setConnection(DomainVerificationManagerInternal.Connection connection) {
        this.mConnection = connection;
        this.mEnforcer.setCallback(connection);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public DomainVerificationProxy getProxy() {
        return this.mProxy;
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        if (!hasRealVerifier()) {
            return;
        }
        switch (phase) {
            case SystemService.PHASE_ACTIVITY_MANAGER_READY /* 550 */:
                this.mCanSendBroadcasts = true;
                return;
            case 1000:
                verifyPackages(null, false);
                return;
            default:
                return;
        }
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocked(SystemService.TargetUser user) {
        super.onUserUnlocked(user);
        verifyPackages(null, false);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void setProxy(DomainVerificationProxy proxy) {
        this.mProxy = proxy;
    }

    public List<String> queryValidVerificationPackageNames() {
        this.mEnforcer.assertApprovedVerifier(this.mConnection.getCallingUid(), this.mProxy);
        List<String> packageNames = new ArrayList<>();
        synchronized (this.mLock) {
            int size = this.mAttachedPkgStates.size();
            for (int index = 0; index < size; index++) {
                DomainVerificationPkgState pkgState = this.mAttachedPkgStates.valueAt(index);
                if (pkgState.isHasAutoVerifyDomains()) {
                    packageNames.add(pkgState.getPackageName());
                }
            }
        }
        return packageNames;
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public UUID getDomainVerificationInfoId(String packageName) {
        synchronized (this.mLock) {
            DomainVerificationPkgState pkgState = this.mAttachedPkgStates.get(packageName);
            if (pkgState != null) {
                return pkgState.getId();
            }
            return null;
        }
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public DomainVerificationInfo getDomainVerificationInfo(String packageName) throws PackageManager.NameNotFoundException {
        this.mEnforcer.assertApprovedQuerent(this.mConnection.getCallingUid(), this.mProxy);
        Computer snapshot = this.mConnection.snapshot();
        synchronized (this.mLock) {
            PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
            AndroidPackage pkg = pkgSetting == null ? null : pkgSetting.getPkg();
            if (pkg == null) {
                throw DomainVerificationUtils.throwPackageUnavailable(packageName);
            }
            DomainVerificationPkgState pkgState = this.mAttachedPkgStates.get(packageName);
            if (pkgState == null) {
                return null;
            }
            ArrayMap<String, Integer> hostToStateMap = new ArrayMap<>(pkgState.getStateMap());
            ArraySet<String> domains = this.mCollector.collectValidAutoVerifyDomains(pkg);
            if (domains.isEmpty()) {
                return null;
            }
            int size = domains.size();
            for (int index = 0; index < size; index++) {
                hostToStateMap.putIfAbsent(domains.valueAt(index), 0);
            }
            int mapSize = hostToStateMap.size();
            for (int index2 = 0; index2 < mapSize; index2++) {
                int internalValue = hostToStateMap.valueAt(index2).intValue();
                int publicValue = DomainVerificationState.convertToInfoState(internalValue);
                hostToStateMap.setValueAt(index2, Integer.valueOf(publicValue));
            }
            return new DomainVerificationInfo(pkgState.getId(), packageName, hostToStateMap);
        }
    }

    public int setDomainVerificationStatus(UUID domainSetId, Set<String> domains, int state) throws PackageManager.NameNotFoundException {
        if (state < 1024 && state != 1) {
            throw new IllegalArgumentException("Caller is not allowed to set state code " + state);
        }
        return setDomainVerificationStatusInternal(this.mConnection.getCallingUid(), domainSetId, domains, state);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public int setDomainVerificationStatusInternal(int callingUid, UUID domainSetId, Set<String> domains, int state) throws PackageManager.NameNotFoundException {
        this.mEnforcer.assertApprovedVerifier(callingUid, this.mProxy);
        Computer snapshot = this.mConnection.snapshot();
        synchronized (this.mLock) {
            List<String> newlyVerifiedDomains = new ArrayList<>();
            GetAttachedResult result = getAndValidateAttachedLocked(domainSetId, domains, true, callingUid, null, snapshot);
            if (result.isError()) {
                return result.getErrorCode();
            }
            DomainVerificationPkgState pkgState = result.getPkgState();
            ArrayMap<String, Integer> stateMap = pkgState.getStateMap();
            for (String domain : domains) {
                Integer previousState = stateMap.get(domain);
                if (previousState == null || (previousState.intValue() != state && DomainVerificationState.isModifiable(previousState.intValue()))) {
                    if (DomainVerificationState.isVerified(state) && (previousState == null || !DomainVerificationState.isVerified(previousState.intValue()))) {
                        newlyVerifiedDomains.add(domain);
                    }
                    stateMap.put(domain, Integer.valueOf(state));
                }
            }
            int size = newlyVerifiedDomains.size();
            for (int index = 0; index < size; index++) {
                removeUserStatesForDomain(pkgState, newlyVerifiedDomains.get(index));
            }
            this.mConnection.scheduleWriteSettings();
            return 0;
        }
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationShell.Callback
    public void setDomainVerificationStatusInternal(String packageName, int state, ArraySet<String> domains) throws PackageManager.NameNotFoundException {
        ArraySet<String> validDomains;
        this.mEnforcer.assertInternal(this.mConnection.getCallingUid());
        switch (state) {
            case 0:
            case 1:
            case 2:
            case 3:
                if (packageName == null) {
                    Computer snapshot = this.mConnection.snapshot();
                    synchronized (this.mLock) {
                        ArraySet<String> validDomains2 = new ArraySet<>();
                        int size = this.mAttachedPkgStates.size();
                        for (int index = 0; index < size; index++) {
                            DomainVerificationPkgState pkgState = this.mAttachedPkgStates.valueAt(index);
                            String pkgName = pkgState.getPackageName();
                            PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(pkgName);
                            if (pkgSetting != null && pkgSetting.getPkg() != null) {
                                AndroidPackage pkg = pkgSetting.getPkg();
                                validDomains2.clear();
                                ArraySet<String> autoVerifyDomains = this.mCollector.collectValidAutoVerifyDomains(pkg);
                                if (domains == null) {
                                    validDomains2.addAll((ArraySet<? extends String>) autoVerifyDomains);
                                } else {
                                    validDomains2.addAll((ArraySet<? extends String>) domains);
                                    validDomains2.retainAll(autoVerifyDomains);
                                }
                                setDomainVerificationStatusInternal(pkgState, state, validDomains2);
                            }
                        }
                    }
                } else {
                    Computer snapshot2 = this.mConnection.snapshot();
                    synchronized (this.mLock) {
                        DomainVerificationPkgState pkgState2 = this.mAttachedPkgStates.get(packageName);
                        if (pkgState2 == null) {
                            throw DomainVerificationUtils.throwPackageUnavailable(packageName);
                        }
                        PackageStateInternal pkgSetting2 = snapshot2.getPackageStateInternal(packageName);
                        if (pkgSetting2 == null || pkgSetting2.getPkg() == null) {
                            throw DomainVerificationUtils.throwPackageUnavailable(packageName);
                        }
                        AndroidPackage pkg2 = pkgSetting2.getPkg();
                        if (domains == null) {
                            validDomains = this.mCollector.collectValidAutoVerifyDomains(pkg2);
                        } else {
                            validDomains = domains;
                            validDomains.retainAll(this.mCollector.collectValidAutoVerifyDomains(pkg2));
                        }
                        ArraySet<String> newlyVerifiedDomains = null;
                        if (DomainVerificationState.isVerified(state)) {
                            newlyVerifiedDomains = new ArraySet<>();
                            ArrayMap<String, Integer> stateMap = pkgState2.getStateMap();
                            int domainsSize = validDomains.size();
                            for (int domainIndex = 0; domainIndex < domainsSize; domainIndex++) {
                                String domain = validDomains.valueAt(domainIndex);
                                Integer oldState = stateMap.get(domain);
                                if (oldState == null || !DomainVerificationState.isVerified(oldState.intValue())) {
                                    newlyVerifiedDomains.add(domain);
                                }
                            }
                        }
                        setDomainVerificationStatusInternal(pkgState2, state, validDomains);
                        if (newlyVerifiedDomains != null) {
                            int domainsSize2 = newlyVerifiedDomains.size();
                            for (int domainIndex2 = 0; domainIndex2 < domainsSize2; domainIndex2++) {
                                removeUserStatesForDomain(pkgState2, newlyVerifiedDomains.valueAt(domainIndex2));
                            }
                        }
                    }
                }
                this.mConnection.scheduleWriteSettings();
                return;
            default:
                throw new IllegalArgumentException("State must be one of NO_RESPONSE, SUCCESS, APPROVED, or DENIED");
        }
    }

    private void setDomainVerificationStatusInternal(DomainVerificationPkgState pkgState, int state, ArraySet<String> validDomains) {
        ArrayMap<String, Integer> stateMap = pkgState.getStateMap();
        int size = validDomains.size();
        for (int index = 0; index < size; index++) {
            stateMap.put(validDomains.valueAt(index), Integer.valueOf(state));
        }
    }

    private void removeUserStatesForDomain(DomainVerificationPkgState owningPkgState, String domain) {
        SparseArray<DomainVerificationInternalUserState> owningUserStates = owningPkgState.getUserStates();
        synchronized (this.mLock) {
            int size = this.mAttachedPkgStates.size();
            for (int index = 0; index < size; index++) {
                DomainVerificationPkgState pkgState = this.mAttachedPkgStates.valueAt(index);
                SparseArray<DomainVerificationInternalUserState> array = pkgState.getUserStates();
                int arraySize = array.size();
                for (int arrayIndex = 0; arrayIndex < arraySize; arrayIndex++) {
                    int userId = array.keyAt(arrayIndex);
                    DomainVerificationInternalUserState owningUserState = owningUserStates.get(userId);
                    if (owningUserState == null || owningUserState.isLinkHandlingAllowed()) {
                        array.valueAt(arrayIndex).removeHost(domain);
                    }
                }
            }
        }
    }

    public void setDomainVerificationLinkHandlingAllowed(String packageName, boolean allowed, int userId) throws PackageManager.NameNotFoundException {
        if (!this.mEnforcer.assertApprovedUserSelector(this.mConnection.getCallingUid(), this.mConnection.getCallingUserId(), packageName, userId)) {
            throw DomainVerificationUtils.throwPackageUnavailable(packageName);
        }
        synchronized (this.mLock) {
            DomainVerificationPkgState pkgState = this.mAttachedPkgStates.get(packageName);
            if (pkgState == null) {
                throw DomainVerificationUtils.throwPackageUnavailable(packageName);
            }
            pkgState.getOrCreateUserState(userId).setLinkHandlingAllowed(allowed);
        }
        this.mConnection.scheduleWriteSettings();
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationShell.Callback
    public void setDomainVerificationLinkHandlingAllowedInternal(String packageName, boolean allowed, int userId) throws PackageManager.NameNotFoundException {
        int[] allUserIds;
        int[] allUserIds2;
        this.mEnforcer.assertInternal(this.mConnection.getCallingUid());
        if (packageName == null) {
            synchronized (this.mLock) {
                int pkgStateSize = this.mAttachedPkgStates.size();
                for (int pkgStateIndex = 0; pkgStateIndex < pkgStateSize; pkgStateIndex++) {
                    DomainVerificationPkgState pkgState = this.mAttachedPkgStates.valueAt(pkgStateIndex);
                    if (userId == -1) {
                        for (int aUserId : this.mConnection.getAllUserIds()) {
                            pkgState.getOrCreateUserState(aUserId).setLinkHandlingAllowed(allowed);
                        }
                    } else {
                        pkgState.getOrCreateUserState(userId).setLinkHandlingAllowed(allowed);
                    }
                }
            }
        } else {
            synchronized (this.mLock) {
                DomainVerificationPkgState pkgState2 = this.mAttachedPkgStates.get(packageName);
                if (pkgState2 == null) {
                    throw DomainVerificationUtils.throwPackageUnavailable(packageName);
                }
                if (userId == -1) {
                    for (int aUserId2 : this.mConnection.getAllUserIds()) {
                        pkgState2.getOrCreateUserState(aUserId2).setLinkHandlingAllowed(allowed);
                    }
                } else {
                    pkgState2.getOrCreateUserState(userId).setLinkHandlingAllowed(allowed);
                }
            }
        }
        this.mConnection.scheduleWriteSettings();
    }

    public int setDomainVerificationUserSelection(UUID domainSetId, Set<String> domains, boolean enabled, int userId) throws PackageManager.NameNotFoundException {
        int statusCode;
        int callingUid = this.mConnection.getCallingUid();
        if (!this.mEnforcer.assertApprovedUserSelector(callingUid, this.mConnection.getCallingUserId(), null, userId)) {
            return 1;
        }
        Computer snapshot = this.mConnection.snapshot();
        synchronized (this.mLock) {
            GetAttachedResult result = getAndValidateAttachedLocked(domainSetId, domains, false, callingUid, Integer.valueOf(userId), snapshot);
            if (result.isError()) {
                return result.getErrorCode();
            }
            DomainVerificationPkgState pkgState = result.getPkgState();
            DomainVerificationInternalUserState userState = pkgState.getOrCreateUserState(userId);
            if (!enabled || (statusCode = revokeOtherUserSelectionsLocked(userState, userId, domains, snapshot)) == 0) {
                if (enabled) {
                    userState.addHosts(domains);
                } else {
                    userState.removeHosts(domains);
                }
                this.mConnection.scheduleWriteSettings();
                return 0;
            }
            return statusCode;
        }
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationShell.Callback
    public void setDomainVerificationUserSelectionInternal(int userId, String packageName, boolean enabled, ArraySet<String> domains) throws PackageManager.NameNotFoundException {
        int[] allUserIds;
        this.mEnforcer.assertInternal(this.mConnection.getCallingUid());
        Computer snapshot = this.mConnection.snapshot();
        synchronized (this.mLock) {
            DomainVerificationPkgState pkgState = this.mAttachedPkgStates.get(packageName);
            if (pkgState == null) {
                throw DomainVerificationUtils.throwPackageUnavailable(packageName);
            }
            PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
            AndroidPackage pkg = pkgSetting == null ? null : pkgSetting.getPkg();
            if (pkg == null) {
                throw DomainVerificationUtils.throwPackageUnavailable(packageName);
            }
            Set<String> validDomains = domains == null ? this.mCollector.collectAllWebDomains(pkg) : domains;
            validDomains.retainAll(this.mCollector.collectAllWebDomains(pkg));
            if (userId == -1) {
                for (int aUserId : this.mConnection.getAllUserIds()) {
                    DomainVerificationInternalUserState userState = pkgState.getOrCreateUserState(aUserId);
                    revokeOtherUserSelectionsLocked(userState, aUserId, validDomains, snapshot);
                    if (enabled) {
                        userState.addHosts(validDomains);
                    } else {
                        userState.removeHosts(validDomains);
                    }
                }
            } else {
                DomainVerificationInternalUserState userState2 = pkgState.getOrCreateUserState(userId);
                revokeOtherUserSelectionsLocked(userState2, userId, validDomains, snapshot);
                if (enabled) {
                    userState2.addHosts(validDomains);
                } else {
                    userState2.removeHosts(validDomains);
                }
            }
        }
        this.mConnection.scheduleWriteSettings();
    }

    private int revokeOtherUserSelectionsLocked(DomainVerificationInternalUserState userState, int userId, Set<String> domains, Computer snapshot) {
        DomainVerificationInternalUserState approvedUserState;
        ArrayMap<String, List<String>> domainToApprovedPackages = new ArrayMap<>();
        for (String domain : domains) {
            if (!userState.getEnabledHosts().contains(domain)) {
                Pair<List<String>, Integer> packagesToLevel = getApprovedPackagesLocked(domain, userId, 1, snapshot);
                int highestApproval = ((Integer) packagesToLevel.second).intValue();
                if (highestApproval > 3) {
                    return 3;
                }
                domainToApprovedPackages.put(domain, (List) packagesToLevel.first);
            }
        }
        int mapSize = domainToApprovedPackages.size();
        for (int mapIndex = 0; mapIndex < mapSize; mapIndex++) {
            String domain2 = domainToApprovedPackages.keyAt(mapIndex);
            List<String> approvedPackages = domainToApprovedPackages.valueAt(mapIndex);
            int approvedSize = approvedPackages.size();
            for (int approvedIndex = 0; approvedIndex < approvedSize; approvedIndex++) {
                String approvedPackage = approvedPackages.get(approvedIndex);
                DomainVerificationPkgState approvedPkgState = this.mAttachedPkgStates.get(approvedPackage);
                if (approvedPkgState != null && (approvedUserState = approvedPkgState.getUserState(userId)) != null) {
                    approvedUserState.removeHost(domain2);
                }
            }
        }
        return 0;
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationShell.Callback
    public DomainVerificationUserState getDomainVerificationUserState(String packageName, int userId) throws PackageManager.NameNotFoundException {
        boolean z;
        int domainState;
        if (!this.mEnforcer.assertApprovedUserStateQuerent(this.mConnection.getCallingUid(), this.mConnection.getCallingUserId(), packageName, userId)) {
            throw DomainVerificationUtils.throwPackageUnavailable(packageName);
        }
        Computer snapshot = this.mConnection.snapshot();
        synchronized (this.mLock) {
            PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
            AndroidPackage pkg = pkgSetting == null ? null : pkgSetting.getPkg();
            if (pkg != null) {
                DomainVerificationPkgState pkgState = this.mAttachedPkgStates.get(packageName);
                if (pkgState == null) {
                    return null;
                }
                ArraySet<String> webDomains = this.mCollector.collectAllWebDomains(pkg);
                int webDomainsSize = webDomains.size();
                Map<String, Integer> domains = new ArrayMap<>(webDomainsSize);
                ArrayMap<String, Integer> stateMap = pkgState.getStateMap();
                DomainVerificationInternalUserState userState = pkgState.getUserState(userId);
                Set<String> enabledHosts = userState == null ? Collections.emptySet() : userState.getEnabledHosts();
                int index = 0;
                while (index < webDomainsSize) {
                    String host = webDomains.valueAt(index);
                    Integer state = stateMap.get(host);
                    if (state != null && DomainVerificationState.isVerified(state.intValue())) {
                        domainState = 2;
                    } else if (enabledHosts.contains(host)) {
                        domainState = 1;
                    } else {
                        domainState = 0;
                    }
                    domains.put(host, Integer.valueOf(domainState));
                    index++;
                    pkgSetting = pkgSetting;
                }
                if (userState != null && !userState.isLinkHandlingAllowed()) {
                    z = false;
                    boolean linkHandlingAllowed = z;
                    return new DomainVerificationUserState(pkgState.getId(), packageName, UserHandle.of(userId), linkHandlingAllowed, domains);
                }
                z = true;
                boolean linkHandlingAllowed2 = z;
                return new DomainVerificationUserState(pkgState.getId(), packageName, UserHandle.of(userId), linkHandlingAllowed2, domains);
            }
            throw DomainVerificationUtils.throwPackageUnavailable(packageName);
        }
    }

    public List<DomainOwner> getOwnersForDomain(String domain, int userId) {
        Objects.requireNonNull(domain);
        this.mEnforcer.assertOwnerQuerent(this.mConnection.getCallingUid(), this.mConnection.getCallingUserId(), userId);
        Computer snapshot = this.mConnection.snapshot();
        SparseArray<List<String>> levelToPackages = getOwnersForDomainInternal(domain, false, userId, snapshot);
        if (levelToPackages.size() == 0) {
            return Collections.emptyList();
        }
        List<DomainOwner> owners = new ArrayList<>();
        int size = levelToPackages.size();
        for (int index = 0; index < size; index++) {
            int level = levelToPackages.keyAt(index);
            boolean overrideable = level <= 3;
            List<String> packages = levelToPackages.valueAt(index);
            int packagesSize = packages.size();
            for (int packageIndex = 0; packageIndex < packagesSize; packageIndex++) {
                owners.add(new DomainOwner(packages.get(packageIndex), overrideable));
            }
        }
        return owners;
    }

    private SparseArray<List<String>> getOwnersForDomainInternal(String domain, boolean includeNegative, final int userId, final Computer snapshot) {
        SparseArray<List<String>> levelToPackages = new SparseArray<>();
        synchronized (this.mLock) {
            try {
                int size = this.mAttachedPkgStates.size();
                for (int index = 0; index < size; index++) {
                    DomainVerificationPkgState pkgState = this.mAttachedPkgStates.valueAt(index);
                    String packageName = pkgState.getPackageName();
                    PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
                    if (pkgSetting != null) {
                        int level = approvalLevelForDomain(pkgSetting, domain, includeNegative, userId, domain);
                        if (includeNegative || level > 0) {
                            List<String> list = levelToPackages.get(level);
                            if (list == null) {
                                list = new ArrayList();
                                levelToPackages.put(level, list);
                            }
                            list.add(packageName);
                        }
                    }
                }
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                throw th;
            }
        }
        int size2 = levelToPackages.size();
        if (size2 == 0) {
            return levelToPackages;
        }
        for (int index2 = 0; index2 < size2; index2++) {
            levelToPackages.valueAt(index2).sort(new Comparator() { // from class: com.android.server.pm.verify.domain.DomainVerificationService$$ExternalSyntheticLambda1
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return DomainVerificationService.lambda$getOwnersForDomainInternal$0(Computer.this, userId, (String) obj, (String) obj2);
                }
            });
        }
        return levelToPackages;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$getOwnersForDomainInternal$0(Computer snapshot, int userId, String first, String second) {
        PackageStateInternal firstPkgSetting = snapshot.getPackageStateInternal(first);
        PackageStateInternal secondPkgSetting = snapshot.getPackageStateInternal(second);
        long secondInstallTime = -1;
        long firstInstallTime = firstPkgSetting == null ? -1L : firstPkgSetting.getUserStateOrDefault(userId).getFirstInstallTime();
        if (secondPkgSetting != null) {
            secondInstallTime = secondPkgSetting.getUserStateOrDefault(userId).getFirstInstallTime();
        }
        if (firstInstallTime != secondInstallTime) {
            return (int) (firstInstallTime - secondInstallTime);
        }
        return first.compareToIgnoreCase(second);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public UUID generateNewId() {
        return UUID.randomUUID();
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void migrateState(PackageStateInternal oldPkgSetting, PackageStateInternal newPkgSetting) {
        SparseArray<DomainVerificationInternalUserState> newUserStates;
        ArrayMap<String, Integer> newStateMap;
        AndroidPackage oldPkg;
        AndroidPackage newPkg;
        int oldUserStatesSize;
        UUID oldDomainSetId;
        String pkgName = newPkgSetting.getPackageName();
        synchronized (this.mLock) {
            try {
                try {
                    UUID oldDomainSetId2 = oldPkgSetting.getDomainSetId();
                    UUID newDomainSetId = newPkgSetting.getDomainSetId();
                    DomainVerificationPkgState oldPkgState = this.mAttachedPkgStates.remove(oldDomainSetId2);
                    AndroidPackage oldPkg2 = oldPkgSetting.getPkg();
                    AndroidPackage newPkg2 = newPkgSetting.getPkg();
                    ArrayMap<String, Integer> newStateMap2 = new ArrayMap<>();
                    SparseArray<DomainVerificationInternalUserState> newUserStates2 = new SparseArray<>();
                    if (oldPkgState == null || oldPkg2 == null) {
                        newUserStates = newUserStates2;
                        newStateMap = newStateMap2;
                        oldPkg = oldPkg2;
                        newPkg = newPkg2;
                    } else if (newPkg2 != null) {
                        ArrayMap<String, Integer> oldStateMap = oldPkgState.getStateMap();
                        ArraySet<String> newAutoVerifyDomains = this.mCollector.collectValidAutoVerifyDomains(newPkg2);
                        int newDomainsSize = newAutoVerifyDomains.size();
                        int newDomainsIndex = 0;
                        while (newDomainsIndex < newDomainsSize) {
                            String domain = newAutoVerifyDomains.valueAt(newDomainsIndex);
                            Integer oldStateInteger = oldStateMap.get(domain);
                            if (oldStateInteger == null) {
                                oldDomainSetId = oldDomainSetId2;
                            } else {
                                int oldState = oldStateInteger.intValue();
                                if (!DomainVerificationState.shouldMigrate(oldState)) {
                                    oldDomainSetId = oldDomainSetId2;
                                } else {
                                    oldDomainSetId = oldDomainSetId2;
                                    newStateMap2.put(domain, Integer.valueOf(oldState));
                                }
                            }
                            newDomainsIndex++;
                            oldDomainSetId2 = oldDomainSetId;
                        }
                        SparseArray<DomainVerificationInternalUserState> oldUserStates = oldPkgState.getUserStates();
                        int oldUserStatesSize2 = oldUserStates.size();
                        if (oldUserStatesSize2 > 0) {
                            ArraySet<String> newWebDomains = this.mCollector.collectAllWebDomains(newPkg2);
                            int oldUserStatesIndex = 0;
                            while (oldUserStatesIndex < oldUserStatesSize2) {
                                int userId = oldUserStates.keyAt(oldUserStatesIndex);
                                DomainVerificationInternalUserState oldUserState = oldUserStates.valueAt(oldUserStatesIndex);
                                ArraySet<String> oldEnabledHosts = oldUserState.getEnabledHosts();
                                SparseArray<DomainVerificationInternalUserState> oldUserStates2 = oldUserStates;
                                ArraySet<String> newEnabledHosts = new ArraySet<>(oldEnabledHosts);
                                newEnabledHosts.retainAll(newWebDomains);
                                ArraySet<String> newWebDomains2 = newWebDomains;
                                DomainVerificationInternalUserState newUserState = new DomainVerificationInternalUserState(userId, newEnabledHosts, oldUserState.isLinkHandlingAllowed());
                                newUserStates2.put(userId, newUserState);
                                oldUserStatesIndex++;
                                oldUserStates = oldUserStates2;
                                newWebDomains = newWebDomains2;
                                oldUserStatesSize2 = oldUserStatesSize2;
                                newPkg2 = newPkg2;
                            }
                            oldUserStatesSize = oldUserStatesSize2;
                        } else {
                            oldUserStatesSize = oldUserStatesSize2;
                        }
                        boolean sendBroadcast = false;
                        boolean hasAutoVerifyDomains = newDomainsSize > 0;
                        boolean needsBroadcast = applyImmutableState(newPkgSetting, newStateMap2, newAutoVerifyDomains);
                        if (hasAutoVerifyDomains && needsBroadcast) {
                            sendBroadcast = true;
                        }
                        this.mAttachedPkgStates.put(pkgName, newDomainSetId, new DomainVerificationPkgState(pkgName, newDomainSetId, hasAutoVerifyDomains, newStateMap2, newUserStates2, null));
                        if (sendBroadcast) {
                            sendBroadcast(pkgName);
                            return;
                        }
                        return;
                    } else {
                        newUserStates = newUserStates2;
                        newStateMap = newStateMap2;
                        oldPkg = oldPkg2;
                        newPkg = newPkg2;
                    }
                    Slog.wtf(TAG, "Invalid state nullability old state = " + oldPkgState + ", old pkgSetting = " + oldPkgSetting + ", new pkgSetting = " + newPkgSetting + ", old pkg = " + oldPkg + ", new pkg = " + newPkg, new Exception());
                    DomainVerificationPkgState newPkgState = new DomainVerificationPkgState(pkgName, newDomainSetId, true, newStateMap, newUserStates, null);
                    this.mAttachedPkgStates.put(pkgName, newDomainSetId, newPkgState);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void addPackage(PackageStateInternal newPkgSetting) {
        DomainVerificationPkgState pkgState;
        DomainVerificationPkgState pkgState2;
        DomainVerificationPkgState pkgState3;
        UUID domainSetId = newPkgSetting.getDomainSetId();
        String pkgName = newPkgSetting.getPackageName();
        DomainVerificationPkgState pkgState4 = this.mSettings.removePendingState(pkgName);
        if (pkgState4 != null) {
            pkgState = null;
            pkgState2 = pkgState4;
        } else {
            DomainVerificationPkgState pkgState5 = this.mSettings.removeRestoredState(pkgName);
            if (pkgState5 == null || Objects.equals(pkgState5.getBackupSignatureHash(), PackageUtils.computeSignaturesSha256Digest(newPkgSetting.getSigningDetails().getSignatures()))) {
                pkgState = 1;
                pkgState2 = pkgState5;
            } else {
                pkgState = 1;
                pkgState2 = null;
            }
        }
        AndroidPackage pkg = newPkgSetting.getPkg();
        ArraySet<String> autoVerifyDomains = this.mCollector.collectValidAutoVerifyDomains(pkg);
        boolean hasAutoVerifyDomains = !autoVerifyDomains.isEmpty();
        boolean isPendingOrRestored = pkgState2 != null;
        if (isPendingOrRestored) {
            DomainVerificationPkgState pkgState6 = new DomainVerificationPkgState(pkgState2, domainSetId, hasAutoVerifyDomains);
            pkgState6.getStateMap().retainAll(autoVerifyDomains);
            Set<String> webDomains = this.mCollector.collectAllWebDomains(pkg);
            SparseArray<DomainVerificationInternalUserState> userStates = pkgState6.getUserStates();
            int size = userStates.size();
            for (int index = 0; index < size; index++) {
                userStates.valueAt(index).retainHosts(webDomains);
            }
            pkgState3 = pkgState6;
        } else {
            pkgState3 = new DomainVerificationPkgState(pkgName, domainSetId, hasAutoVerifyDomains);
        }
        boolean needsBroadcast = applyImmutableState(newPkgSetting, pkgState3.getStateMap(), autoVerifyDomains);
        if (needsBroadcast && !isPendingOrRestored) {
            ArraySet<String> webDomains2 = null;
            SparseIntArray legacyUserStates = this.mLegacySettings.getUserStates(pkgName);
            int userStateSize = legacyUserStates != null ? legacyUserStates.size() : 0;
            int index2 = 0;
            while (index2 < userStateSize) {
                int userId = legacyUserStates.keyAt(index2);
                boolean isPendingOrRestored2 = isPendingOrRestored;
                int legacyStatus = legacyUserStates.valueAt(index2);
                int userStateSize2 = userStateSize;
                if (legacyStatus == 2) {
                    if (webDomains2 == null) {
                        webDomains2 = this.mCollector.collectAllWebDomains(pkg);
                    }
                    pkgState3.getOrCreateUserState(userId).addHosts(webDomains2);
                }
                index2++;
                isPendingOrRestored = isPendingOrRestored2;
                userStateSize = userStateSize2;
            }
            IntentFilterVerificationInfo legacyInfo = this.mLegacySettings.remove(pkgName);
            if (legacyInfo != null && legacyInfo.getStatus() == 2) {
                ArrayMap<String, Integer> stateMap = pkgState3.getStateMap();
                int domainsSize = autoVerifyDomains.size();
                int index3 = 0;
                while (index3 < domainsSize) {
                    stateMap.put(autoVerifyDomains.valueAt(index3), 4);
                    index3++;
                    webDomains2 = webDomains2;
                    pkg = pkg;
                }
            }
        }
        synchronized (this.mLock) {
            this.mAttachedPkgStates.put(pkgName, domainSetId, pkgState3);
        }
        if (pkgState != null && hasAutoVerifyDomains) {
            sendBroadcast(pkgName);
        }
    }

    private boolean applyImmutableState(PackageStateInternal pkgSetting, ArrayMap<String, Integer> stateMap, ArraySet<String> autoVerifyDomains) {
        if (pkgSetting.isSystem() && this.mSystemConfig.getLinkedApps().contains(pkgSetting.getPackageName())) {
            int domainsSize = autoVerifyDomains.size();
            for (int index = 0; index < domainsSize; index++) {
                stateMap.put(autoVerifyDomains.valueAt(index), 7);
            }
            return false;
        }
        int size = stateMap.size();
        for (int index2 = size - 1; index2 >= 0; index2--) {
            Integer state = stateMap.valueAt(index2);
            if (state.intValue() == 7) {
                stateMap.removeAt(index2);
            }
        }
        return true;
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void writeSettings(final Computer snapshot, TypedXmlSerializer serializer, boolean includeSignatures, int userId) throws IOException {
        synchronized (this.mLock) {
            Function<String, String> pkgNameToSignature = null;
            if (includeSignatures) {
                pkgNameToSignature = new Function() { // from class: com.android.server.pm.verify.domain.DomainVerificationService$$ExternalSyntheticLambda0
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return DomainVerificationService.lambda$writeSettings$1(Computer.this, (String) obj);
                    }
                };
            }
            this.mSettings.writeSettings(serializer, this.mAttachedPkgStates, userId, pkgNameToSignature);
        }
        this.mLegacySettings.writeSettings(serializer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String lambda$writeSettings$1(Computer snapshot, String pkgName) {
        PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(pkgName);
        if (pkgSetting == null) {
            return null;
        }
        return PackageUtils.computeSignaturesSha256Digest(pkgSetting.getSigningDetails().getSignatures());
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void readSettings(Computer snapshot, TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        synchronized (this.mLock) {
            this.mSettings.readSettings(parser, this.mAttachedPkgStates, snapshot);
        }
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void readLegacySettings(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        this.mLegacySettings.readSettings(parser);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void restoreSettings(Computer snapshot, TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        synchronized (this.mLock) {
            this.mSettings.restoreSettings(parser, this.mAttachedPkgStates, snapshot);
        }
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void addLegacySetting(String packageName, IntentFilterVerificationInfo info) {
        this.mLegacySettings.add(packageName, info);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public boolean setLegacyUserState(String packageName, int userId, int state) {
        if (!this.mEnforcer.callerIsLegacyUserSelector(this.mConnection.getCallingUid(), this.mConnection.getCallingUserId(), packageName, userId)) {
            return false;
        }
        this.mLegacySettings.add(packageName, userId, state);
        this.mConnection.scheduleWriteSettings();
        return true;
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public int getLegacyState(String packageName, int userId) {
        if (!this.mEnforcer.callerIsLegacyUserQuerent(this.mConnection.getCallingUid(), this.mConnection.getCallingUserId(), packageName, userId)) {
            return 0;
        }
        return this.mLegacySettings.getUserState(packageName, userId);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void clearPackage(String packageName) {
        synchronized (this.mLock) {
            this.mAttachedPkgStates.remove(packageName);
            this.mSettings.removePackage(packageName);
        }
        this.mConnection.scheduleWriteSettings();
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void clearPackageForUser(String packageName, int userId) {
        synchronized (this.mLock) {
            DomainVerificationPkgState pkgState = this.mAttachedPkgStates.get(packageName);
            if (pkgState != null) {
                pkgState.removeUser(userId);
            }
            this.mSettings.removePackageForUser(packageName, userId);
        }
        this.mConnection.scheduleWriteSettings();
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void clearUser(int userId) {
        synchronized (this.mLock) {
            int attachedSize = this.mAttachedPkgStates.size();
            for (int index = 0; index < attachedSize; index++) {
                this.mAttachedPkgStates.valueAt(index).removeUser(userId);
            }
            this.mSettings.removeUser(userId);
        }
        this.mConnection.scheduleWriteSettings();
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public boolean runMessage(int messageCode, Object object) {
        return this.mProxy.runMessage(messageCode, object);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationShell.Callback
    public void printState(IndentingPrintWriter writer, String packageName, Integer userId) throws PackageManager.NameNotFoundException {
        printState(this.mConnection.snapshot(), writer, packageName, userId);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public void printState(Computer snapshot, IndentingPrintWriter writer, String packageName, Integer userId) throws PackageManager.NameNotFoundException {
        this.mEnforcer.assertApprovedQuerent(this.mConnection.getCallingUid(), this.mProxy);
        synchronized (this.mLock) {
            this.mDebug.printState(writer, packageName, userId, snapshot, this.mAttachedPkgStates);
        }
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationShell.Callback
    public void printOwnersForPackage(IndentingPrintWriter writer, String packageName, Integer userId) throws PackageManager.NameNotFoundException {
        Computer snapshot = this.mConnection.snapshot();
        synchronized (this.mLock) {
            if (packageName == null) {
                int size = this.mAttachedPkgStates.size();
                for (int index = 0; index < size; index++) {
                    try {
                        printOwnersForPackage(writer, this.mAttachedPkgStates.valueAt(index).getPackageName(), userId, snapshot);
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            } else {
                printOwnersForPackage(writer, packageName, userId, snapshot);
            }
        }
    }

    private void printOwnersForPackage(IndentingPrintWriter writer, String packageName, Integer userId, Computer snapshot) throws PackageManager.NameNotFoundException {
        PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
        AndroidPackage pkg = pkgSetting == null ? null : pkgSetting.getPkg();
        if (pkg == null) {
            throw DomainVerificationUtils.throwPackageUnavailable(packageName);
        }
        ArraySet<String> domains = this.mCollector.collectAllWebDomains(pkg);
        int size = domains.size();
        if (size == 0) {
            return;
        }
        writer.println(packageName + ":");
        writer.increaseIndent();
        for (int index = 0; index < size; index++) {
            printOwnersForDomain(writer, domains.valueAt(index), userId, snapshot);
        }
        writer.decreaseIndent();
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationShell.Callback
    public void printOwnersForDomains(IndentingPrintWriter writer, List<String> domains, Integer userId) {
        Computer snapshot = this.mConnection.snapshot();
        synchronized (this.mLock) {
            int size = domains.size();
            for (int index = 0; index < size; index++) {
                printOwnersForDomain(writer, domains.get(index), userId, snapshot);
            }
        }
    }

    private void printOwnersForDomain(IndentingPrintWriter writer, String domain, Integer userId, Computer snapshot) {
        int[] allUserIds;
        SparseArray<SparseArray<List<String>>> userIdToApprovalLevelToOwners = new SparseArray<>();
        if (userId == null || userId.intValue() == -1) {
            for (int aUserId : this.mConnection.getAllUserIds()) {
                userIdToApprovalLevelToOwners.put(aUserId, getOwnersForDomainInternal(domain, true, aUserId, snapshot));
            }
        } else {
            userIdToApprovalLevelToOwners.put(userId.intValue(), getOwnersForDomainInternal(domain, true, userId.intValue(), snapshot));
        }
        this.mDebug.printOwners(writer, domain, userIdToApprovalLevelToOwners);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public DomainVerificationShell getShell() {
        return this.mShell;
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public DomainVerificationCollector getCollector() {
        return this.mCollector;
    }

    private void sendBroadcast(String packageName) {
        sendBroadcast(Collections.singleton(packageName));
    }

    private void sendBroadcast(Set<String> packageNames) {
        if (!this.mCanSendBroadcasts) {
            return;
        }
        this.mProxy.sendBroadcastForPackages(packageNames);
    }

    private boolean hasRealVerifier() {
        return !(this.mProxy instanceof DomainVerificationProxyUnavailable);
    }

    private GetAttachedResult getAndValidateAttachedLocked(UUID domainSetId, Set<String> domains, boolean forAutoVerify, int callingUid, Integer userIdForFilter, Computer snapshot) throws PackageManager.NameNotFoundException {
        ArraySet<String> declaredDomains;
        if (domainSetId == null) {
            throw new IllegalArgumentException("domainSetId cannot be null");
        }
        DomainVerificationPkgState pkgState = this.mAttachedPkgStates.get(domainSetId);
        if (pkgState == null) {
            return GetAttachedResult.error(1);
        }
        String pkgName = pkgState.getPackageName();
        if (userIdForFilter != null && this.mConnection.filterAppAccess(pkgName, callingUid, userIdForFilter.intValue())) {
            return GetAttachedResult.error(1);
        }
        PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(pkgName);
        if (pkgSetting == null || pkgSetting.getPkg() == null) {
            throw DomainVerificationUtils.throwPackageUnavailable(pkgName);
        }
        if (CollectionUtils.isEmpty(domains)) {
            throw new IllegalArgumentException("Provided domain set cannot be empty");
        }
        AndroidPackage pkg = pkgSetting.getPkg();
        if (forAutoVerify) {
            declaredDomains = this.mCollector.collectValidAutoVerifyDomains(pkg);
        } else {
            declaredDomains = this.mCollector.collectAllWebDomains(pkg);
        }
        if (domains.retainAll(declaredDomains)) {
            return GetAttachedResult.error(2);
        }
        return GetAttachedResult.success(pkgState);
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationShell.Callback
    public void verifyPackages(List<String> packageNames, boolean reVerify) {
        this.mEnforcer.assertInternal(this.mConnection.getCallingUid());
        Set<String> packagesToBroadcast = new ArraySet<>();
        if (packageNames == null) {
            synchronized (this.mLock) {
                int pkgStatesSize = this.mAttachedPkgStates.size();
                for (int pkgStateIndex = 0; pkgStateIndex < pkgStatesSize; pkgStateIndex++) {
                    addIfShouldBroadcastLocked(packagesToBroadcast, this.mAttachedPkgStates.valueAt(pkgStateIndex), reVerify);
                }
            }
        } else {
            synchronized (this.mLock) {
                int size = packageNames.size();
                for (int index = 0; index < size; index++) {
                    String packageName = packageNames.get(index);
                    DomainVerificationPkgState pkgState = this.mAttachedPkgStates.get(packageName);
                    if (pkgState != null) {
                        addIfShouldBroadcastLocked(packagesToBroadcast, pkgState, reVerify);
                    }
                }
            }
        }
        if (!packagesToBroadcast.isEmpty()) {
            sendBroadcast(packagesToBroadcast);
        }
    }

    private void addIfShouldBroadcastLocked(Collection<String> packageNames, DomainVerificationPkgState pkgState, boolean reVerify) {
        if ((reVerify && pkgState.isHasAutoVerifyDomains()) || shouldReBroadcastPackage(pkgState)) {
            packageNames.add(pkgState.getPackageName());
        }
    }

    private boolean shouldReBroadcastPackage(DomainVerificationPkgState pkgState) {
        if (pkgState.isHasAutoVerifyDomains()) {
            ArrayMap<String, Integer> stateMap = pkgState.getStateMap();
            int statesSize = stateMap.size();
            for (int stateIndex = 0; stateIndex < statesSize; stateIndex++) {
                Integer state = stateMap.valueAt(stateIndex);
                if (!DomainVerificationState.isDefault(state.intValue())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationShell.Callback
    public void clearDomainVerificationState(List<String> packageNames) {
        this.mEnforcer.assertInternal(this.mConnection.getCallingUid());
        Computer snapshot = this.mConnection.snapshot();
        synchronized (this.mLock) {
            if (packageNames == null) {
                int size = this.mAttachedPkgStates.size();
                for (int index = 0; index < size; index++) {
                    DomainVerificationPkgState pkgState = this.mAttachedPkgStates.valueAt(index);
                    PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(pkgState.getPackageName());
                    if (pkgSetting != null && pkgSetting.getPkg() != null) {
                        resetDomainState(pkgState.getStateMap(), pkgSetting);
                    }
                }
            } else {
                int size2 = packageNames.size();
                for (int index2 = 0; index2 < size2; index2++) {
                    String pkgName = packageNames.get(index2);
                    DomainVerificationPkgState pkgState2 = this.mAttachedPkgStates.get(pkgName);
                    PackageStateInternal pkgSetting2 = snapshot.getPackageStateInternal(pkgName);
                    if (pkgSetting2 != null && pkgSetting2.getPkg() != null) {
                        resetDomainState(pkgState2.getStateMap(), pkgSetting2);
                    }
                }
            }
        }
        this.mConnection.scheduleWriteSettings();
    }

    private void resetDomainState(ArrayMap<String, Integer> stateMap, PackageStateInternal pkgSetting) {
        boolean reset;
        int size = stateMap.size();
        for (int index = size - 1; index >= 0; index--) {
            Integer state = stateMap.valueAt(index);
            switch (state.intValue()) {
                case 1:
                case 5:
                    reset = true;
                    break;
                default:
                    if (state.intValue() >= 1024) {
                        reset = true;
                        break;
                    } else {
                        reset = false;
                        break;
                    }
            }
            if (reset) {
                stateMap.removeAt(index);
            }
        }
        applyImmutableState(pkgSetting, stateMap, this.mCollector.collectValidAutoVerifyDomains(pkgSetting.getPkg()));
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationShell.Callback
    public void clearUserStates(List<String> packageNames, int userId) {
        this.mEnforcer.assertInternal(this.mConnection.getCallingUid());
        synchronized (this.mLock) {
            if (packageNames == null) {
                int size = this.mAttachedPkgStates.size();
                for (int index = 0; index < size; index++) {
                    DomainVerificationPkgState pkgState = this.mAttachedPkgStates.valueAt(index);
                    if (userId == -1) {
                        pkgState.removeAllUsers();
                    } else {
                        pkgState.removeUser(userId);
                    }
                }
            } else {
                int size2 = packageNames.size();
                for (int index2 = 0; index2 < size2; index2++) {
                    String pkgName = packageNames.get(index2);
                    DomainVerificationPkgState pkgState2 = this.mAttachedPkgStates.get(pkgName);
                    if (userId == -1) {
                        pkgState2.removeAllUsers();
                    } else {
                        pkgState2.removeUser(userId);
                    }
                }
            }
        }
        this.mConnection.scheduleWriteSettings();
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public Pair<List<ResolveInfo>, Integer> filterToApprovedApp(Intent intent, List<ResolveInfo> infos, int userId, Function<String, PackageStateInternal> pkgSettingFunction) {
        String domain = intent.getData().getHost();
        ArrayMap<ResolveInfo, Integer> infoApprovals = new ArrayMap<>();
        int infosSize = infos.size();
        for (int index = 0; index < infosSize; index++) {
            ResolveInfo info = infos.get(index);
            if (info.isAutoResolutionAllowed()) {
                infoApprovals.put(info, null);
            }
        }
        int highestApproval = fillMapWithApprovalLevels(infoApprovals, domain, userId, pkgSettingFunction);
        if (highestApproval <= 0) {
            return Pair.create(Collections.emptyList(), Integer.valueOf(highestApproval));
        }
        for (int index2 = infoApprovals.size() - 1; index2 >= 0; index2--) {
            if (infoApprovals.valueAt(index2).intValue() != highestApproval) {
                infoApprovals.removeAt(index2);
            }
        }
        if (highestApproval != 1) {
            filterToLastFirstInstalled(infoApprovals, pkgSettingFunction);
        }
        int size = infoApprovals.size();
        List<ResolveInfo> finalList = new ArrayList<>(size);
        for (int index3 = 0; index3 < size; index3++) {
            finalList.add(infoApprovals.keyAt(index3));
        }
        if (highestApproval != 1) {
            filterToLastDeclared(finalList, pkgSettingFunction);
        }
        return Pair.create(finalList, Integer.valueOf(highestApproval));
    }

    private int fillMapWithApprovalLevels(ArrayMap<ResolveInfo, Integer> inputMap, String domain, int userId, Function<String, PackageStateInternal> pkgSettingFunction) {
        int size = inputMap.size();
        int highestApproval = 0;
        for (int index = 0; index < size; index++) {
            if (inputMap.valueAt(index) == null) {
                ResolveInfo info = inputMap.keyAt(index);
                String packageName = info.getComponentInfo().packageName;
                PackageStateInternal pkgSetting = pkgSettingFunction.apply(packageName);
                if (pkgSetting == null) {
                    fillInfoMapForSamePackage(inputMap, packageName, 0);
                } else {
                    int approval = approvalLevelForDomain(pkgSetting, domain, false, userId, domain);
                    int highestApproval2 = Math.max(highestApproval, approval);
                    fillInfoMapForSamePackage(inputMap, packageName, approval);
                    highestApproval = highestApproval2;
                }
            }
        }
        return highestApproval;
    }

    private void fillInfoMapForSamePackage(ArrayMap<ResolveInfo, Integer> inputMap, String targetPackageName, int level) {
        int size = inputMap.size();
        for (int index = 0; index < size; index++) {
            String packageName = inputMap.keyAt(index).getComponentInfo().packageName;
            if (Objects.equals(targetPackageName, packageName)) {
                inputMap.setValueAt(index, Integer.valueOf(level));
            }
        }
    }

    private void filterToLastFirstInstalled(ArrayMap<ResolveInfo, Integer> inputMap, Function<String, PackageStateInternal> pkgSettingFunction) {
        String targetPackageName = null;
        long latestInstall = Long.MIN_VALUE;
        int size = inputMap.size();
        for (int index = 0; index < size; index++) {
            ResolveInfo info = inputMap.keyAt(index);
            String packageName = info.getComponentInfo().packageName;
            PackageStateInternal pkgSetting = pkgSettingFunction.apply(packageName);
            if (pkgSetting != null) {
                long installTime = PackageStateUtils.getEarliestFirstInstallTime(pkgSetting.getUserStates());
                if (installTime > latestInstall) {
                    latestInstall = installTime;
                    targetPackageName = packageName;
                }
            }
        }
        int index2 = inputMap.size();
        for (int index3 = index2 - 1; index3 >= 0; index3--) {
            ResolveInfo info2 = inputMap.keyAt(index3);
            if (!Objects.equals(targetPackageName, info2.getComponentInfo().packageName)) {
                inputMap.removeAt(index3);
            }
        }
    }

    private void filterToLastDeclared(List<ResolveInfo> inputList, Function<String, PackageStateInternal> pkgSettingFunction) {
        for (int index = 0; index < inputList.size(); index++) {
            ResolveInfo info = inputList.get(index);
            String targetPackageName = info.getComponentInfo().packageName;
            PackageStateInternal pkgSetting = pkgSettingFunction.apply(targetPackageName);
            AndroidPackage pkg = pkgSetting == null ? null : pkgSetting.getPkg();
            if (pkg != null) {
                ResolveInfo result = info;
                int highestIndex = indexOfIntentFilterEntry(pkg, result);
                int searchIndex = inputList.size();
                while (true) {
                    searchIndex--;
                    if (searchIndex < index + 1) {
                        break;
                    }
                    ResolveInfo searchInfo = inputList.get(searchIndex);
                    if (Objects.equals(targetPackageName, searchInfo.getComponentInfo().packageName)) {
                        int entryIndex = indexOfIntentFilterEntry(pkg, searchInfo);
                        if (entryIndex > highestIndex) {
                            highestIndex = entryIndex;
                            result = searchInfo;
                        }
                        inputList.remove(searchIndex);
                    }
                }
                inputList.set(index, result);
            }
        }
    }

    private int indexOfIntentFilterEntry(AndroidPackage pkg, ResolveInfo target) {
        List<ParsedActivity> activities = pkg.getActivities();
        int activitiesSize = activities.size();
        for (int activityIndex = 0; activityIndex < activitiesSize; activityIndex++) {
            if (Objects.equals(activities.get(activityIndex).getComponentName(), target.getComponentInfo().getComponentName())) {
                return activityIndex;
            }
        }
        return -1;
    }

    @Override // com.android.server.pm.verify.domain.DomainVerificationManagerInternal
    public int approvalLevelForDomain(PackageStateInternal pkgSetting, Intent intent, long resolveInfoFlags, int userId) {
        pkgSetting.getPackageName();
        if (!DomainVerificationUtils.isDomainVerificationIntent(intent, resolveInfoFlags)) {
            return 0;
        }
        return approvalLevelForDomain(pkgSetting, intent.getData().getHost(), false, userId, intent);
    }

    private int approvalLevelForDomain(PackageStateInternal pkgSetting, String host, boolean includeNegative, int userId, Object debugObject) {
        int approvalLevel = approvalLevelForDomainInternal(pkgSetting, host, includeNegative, userId, debugObject);
        if (includeNegative && approvalLevel == 0) {
            PackageUserStateInternal pkgUserState = pkgSetting.getUserStateOrDefault(userId);
            if (!pkgUserState.isInstalled()) {
                return -4;
            }
            AndroidPackage pkg = pkgSetting.getPkg();
            if (pkg != null) {
                if (!PackageUserStateUtils.isPackageEnabled(pkgUserState, pkg)) {
                    return -3;
                }
                if (this.mCollector.containsAutoVerifyDomain(pkgSetting.getPkg(), host)) {
                    return -1;
                }
            }
        }
        return approvalLevel;
    }

    private int approvalLevelForDomainInternal(PackageStateInternal pkgSetting, String host, boolean includeNegative, int userId, Object debugObject) {
        int enabledHostsSize;
        char c;
        char c2;
        String packageName = pkgSetting.getPackageName();
        AndroidPackage pkg = pkgSetting.getPkg();
        if (pkg != null && includeNegative && !this.mCollector.containsWebDomain(pkg, host)) {
            return -2;
        }
        PackageUserStateInternal pkgUserState = pkgSetting.getUserStates().get(userId);
        if (pkgUserState != null && pkgUserState.isInstalled() && PackageUserStateUtils.isPackageEnabled(pkgUserState, pkg) && !pkgUserState.isSuspended()) {
            if (pkg != null && !DomainVerificationUtils.isChangeEnabled(this.mPlatformCompat, pkg, SETTINGS_API_V2)) {
                switch (this.mLegacySettings.getUserState(packageName, userId)) {
                    case 1:
                    case 4:
                        return 1;
                    case 2:
                        return 2;
                    case 3:
                        return 0;
                }
            }
            synchronized (this.mLock) {
                try {
                    try {
                        DomainVerificationPkgState pkgState = this.mAttachedPkgStates.get(packageName);
                        if (pkgState == null) {
                            return 0;
                        }
                        DomainVerificationInternalUserState userState = pkgState.getUserState(userId);
                        if (userState != null && !userState.isLinkHandlingAllowed()) {
                            return 0;
                        }
                        if (pkg != null && pkgSetting.getUserStateOrDefault(userId).isInstantApp() && this.mCollector.collectValidAutoVerifyDomains(pkg).contains(host)) {
                            return 5;
                        }
                        ArrayMap<String, Integer> stateMap = pkgState.getStateMap();
                        Integer state = stateMap.get(host);
                        char c3 = 4;
                        if (state != null && DomainVerificationState.isVerified(state.intValue())) {
                            return 4;
                        }
                        int stateMapSize = stateMap.size();
                        int index = 0;
                        while (index < stateMapSize) {
                            if (!DomainVerificationState.isVerified(stateMap.valueAt(index).intValue())) {
                                c2 = c3;
                            } else {
                                String domain = stateMap.keyAt(index);
                                if (domain.startsWith("*.") && host.endsWith(domain.substring(2))) {
                                    return 4;
                                }
                                c2 = 4;
                            }
                            index++;
                            c3 = c2;
                        }
                        if (userState == null) {
                            return 0;
                        }
                        ArraySet<String> enabledHosts = userState.getEnabledHosts();
                        if (enabledHosts.contains(host)) {
                            return 3;
                        }
                        int enabledHostsSize2 = enabledHosts.size();
                        int index2 = 0;
                        while (index2 < enabledHostsSize2) {
                            String domain2 = enabledHosts.valueAt(index2);
                            ArraySet<String> enabledHosts2 = enabledHosts;
                            if (domain2.startsWith("*.")) {
                                enabledHostsSize = enabledHostsSize2;
                                if (host.endsWith(domain2.substring(2))) {
                                    return 3;
                                }
                                c = 3;
                            } else {
                                enabledHostsSize = enabledHostsSize2;
                                c = 3;
                            }
                            index2++;
                            enabledHostsSize2 = enabledHostsSize;
                            enabledHosts = enabledHosts2;
                        }
                        return 0;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        } else {
            return 0;
        }
    }

    private Pair<List<String>, Integer> getApprovedPackagesLocked(String domain, int userId, int minimumApproval, Computer snapshot) {
        int level;
        boolean includeNegative = minimumApproval < 0;
        List<String> approvedPackages = Collections.emptyList();
        int size = this.mAttachedPkgStates.size();
        int highestApproval = minimumApproval;
        List<String> approvedPackages2 = approvedPackages;
        for (int index = 0; index < size; index++) {
            DomainVerificationPkgState pkgState = this.mAttachedPkgStates.valueAt(index);
            String packageName = pkgState.getPackageName();
            PackageStateInternal pkgSetting = snapshot.getPackageStateInternal(packageName);
            if (pkgSetting != null && (level = approvalLevelForDomain(pkgSetting, domain, includeNegative, userId, domain)) >= minimumApproval) {
                if (level > highestApproval) {
                    approvedPackages2.clear();
                    approvedPackages2 = CollectionUtils.add(approvedPackages2, packageName);
                    highestApproval = level;
                } else if (level == highestApproval) {
                    approvedPackages2 = CollectionUtils.add(approvedPackages2, packageName);
                }
            }
        }
        if (approvedPackages2.isEmpty()) {
            return Pair.create(approvedPackages2, 0);
        }
        List<String> filteredPackages = new ArrayList<>();
        long latestInstall = Long.MIN_VALUE;
        int approvedSize = approvedPackages2.size();
        for (int index2 = 0; index2 < approvedSize; index2++) {
            String packageName2 = approvedPackages2.get(index2);
            PackageStateInternal pkgSetting2 = snapshot.getPackageStateInternal(packageName2);
            if (pkgSetting2 != null) {
                long installTime = pkgSetting2.getUserStateOrDefault(userId).getFirstInstallTime();
                if (installTime > latestInstall) {
                    latestInstall = installTime;
                    filteredPackages.clear();
                    filteredPackages.add(packageName2);
                } else if (installTime == latestInstall) {
                    filteredPackages.add(packageName2);
                }
            }
        }
        return Pair.create(filteredPackages, Integer.valueOf(highestApproval));
    }

    private void debugApproval(String packageName, Object debugObject, int userId, boolean approved, String reason) {
        String approvalString = approved ? "approved" : "denied";
        Slog.d("DomainVerificationServiceApproval", packageName + " was " + approvalString + " for " + debugObject + " for user " + userId + ": " + reason);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class GetAttachedResult {
        private int mErrorCode;
        private DomainVerificationPkgState mPkgState;

        GetAttachedResult(DomainVerificationPkgState pkgState, int errorCode) {
            this.mPkgState = pkgState;
            this.mErrorCode = errorCode;
        }

        static GetAttachedResult error(int errorCode) {
            return new GetAttachedResult(null, errorCode);
        }

        static GetAttachedResult success(DomainVerificationPkgState pkgState) {
            return new GetAttachedResult(pkgState, 0);
        }

        DomainVerificationPkgState getPkgState() {
            return this.mPkgState;
        }

        boolean isError() {
            return this.mErrorCode != 0;
        }

        public int getErrorCode() {
            return this.mErrorCode;
        }
    }
}
