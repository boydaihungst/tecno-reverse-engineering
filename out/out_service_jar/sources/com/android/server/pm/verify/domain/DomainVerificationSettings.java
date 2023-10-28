package com.android.server.pm.verify.domain;

import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.server.pm.Computer;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.verify.domain.DomainVerificationPersistence;
import com.android.server.pm.verify.domain.models.DomainVerificationInternalUserState;
import com.android.server.pm.verify.domain.models.DomainVerificationPkgState;
import com.android.server.pm.verify.domain.models.DomainVerificationStateMap;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
class DomainVerificationSettings {
    private final DomainVerificationCollector mCollector;
    private final ArrayMap<String, DomainVerificationPkgState> mPendingPkgStates = new ArrayMap<>();
    private final ArrayMap<String, DomainVerificationPkgState> mRestoredPkgStates = new ArrayMap<>();
    private final Object mLock = new Object();

    public DomainVerificationSettings(DomainVerificationCollector collector) {
        this.mCollector = collector;
    }

    public void writeSettings(TypedXmlSerializer xmlSerializer, DomainVerificationStateMap<DomainVerificationPkgState> liveState, Function<String, String> pkgSignatureFunction) {
    }

    public void writeSettings(TypedXmlSerializer xmlSerializer, DomainVerificationStateMap<DomainVerificationPkgState> liveState, int userId, Function<String, String> pkgSignatureFunction) throws IOException {
        synchronized (this.mLock) {
            DomainVerificationPersistence.writeToXml(xmlSerializer, liveState, this.mPendingPkgStates, this.mRestoredPkgStates, userId, pkgSignatureFunction);
        }
    }

    public void readSettings(TypedXmlPullParser parser, DomainVerificationStateMap<DomainVerificationPkgState> liveState, Computer snapshot) throws IOException, XmlPullParserException {
        DomainVerificationPersistence.ReadResult result = DomainVerificationPersistence.readFromXml(parser);
        ArrayMap<String, DomainVerificationPkgState> active = result.active;
        ArrayMap<String, DomainVerificationPkgState> restored = result.restored;
        synchronized (this.mLock) {
            int activeSize = active.size();
            for (int activeIndex = 0; activeIndex < activeSize; activeIndex++) {
                DomainVerificationPkgState pkgState = active.valueAt(activeIndex);
                String pkgName = pkgState.getPackageName();
                DomainVerificationPkgState existingState = liveState.get(pkgName);
                if (existingState != null) {
                    if (!existingState.getId().equals(pkgState.getId())) {
                        mergePkgState(existingState, pkgState, snapshot);
                    }
                } else {
                    this.mPendingPkgStates.put(pkgName, pkgState);
                }
            }
            int restoredSize = restored.size();
            for (int restoredIndex = 0; restoredIndex < restoredSize; restoredIndex++) {
                DomainVerificationPkgState pkgState2 = restored.valueAt(restoredIndex);
                this.mRestoredPkgStates.put(pkgState2.getPackageName(), pkgState2);
            }
        }
    }

    public void restoreSettings(TypedXmlPullParser parser, DomainVerificationStateMap<DomainVerificationPkgState> liveState, Computer snapshot) throws IOException, XmlPullParserException {
        DomainVerificationPkgState newState;
        String pkgName;
        DomainVerificationPersistence.ReadResult result = DomainVerificationPersistence.readFromXml(parser);
        ArrayMap<String, DomainVerificationPkgState> stateList = result.restored;
        stateList.putAll((ArrayMap<? extends String, ? extends DomainVerificationPkgState>) result.active);
        synchronized (this.mLock) {
            for (int stateIndex = 0; stateIndex < stateList.size(); stateIndex++) {
                try {
                    newState = stateList.valueAt(stateIndex);
                    pkgName = newState.getPackageName();
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    DomainVerificationPkgState existingState = liveState.get(pkgName);
                    if (existingState == null) {
                        existingState = this.mPendingPkgStates.get(pkgName);
                    }
                    if (existingState == null) {
                        existingState = this.mRestoredPkgStates.get(pkgName);
                    }
                    if (existingState != null) {
                        try {
                            mergePkgState(existingState, newState, snapshot);
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    } else {
                        ArrayMap<String, Integer> stateMap = newState.getStateMap();
                        int size = stateMap.size();
                        for (int index = size - 1; index >= 0; index--) {
                            Integer stateInteger = stateMap.valueAt(index);
                            if (stateInteger != null) {
                                int state = stateInteger.intValue();
                                if (state != 1 && state != 5) {
                                    stateMap.removeAt(index);
                                }
                                stateMap.setValueAt(index, 5);
                            }
                        }
                        this.mRestoredPkgStates.put(pkgName, newState);
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void mergePkgState(DomainVerificationPkgState oldState, DomainVerificationPkgState newState, Computer snapshot) {
        PackageStateInternal pkgSetting;
        Integer oldStateCode;
        PackageStateInternal pkgSetting2 = snapshot.getPackageStateInternal(oldState.getPackageName());
        AndroidPackage pkg = pkgSetting2 == null ? null : pkgSetting2.getPkg();
        Set<String> validDomains = pkg == null ? Collections.emptySet() : this.mCollector.collectValidAutoVerifyDomains(pkg);
        ArrayMap<String, Integer> oldStateMap = oldState.getStateMap();
        ArrayMap<String, Integer> newStateMap = newState.getStateMap();
        int size = newStateMap.size();
        for (int index = 0; index < size; index++) {
            String domain = newStateMap.keyAt(index);
            Integer newStateCode = newStateMap.valueAt(index);
            if (validDomains.contains(domain) && (((oldStateCode = oldStateMap.get(domain)) == null || oldStateCode.intValue() == 0) && (newStateCode.intValue() == 1 || newStateCode.intValue() == 5))) {
                oldStateMap.put(domain, 5);
            }
        }
        SparseArray<DomainVerificationInternalUserState> oldSelectionStates = oldState.getUserStates();
        SparseArray<DomainVerificationInternalUserState> newSelectionStates = newState.getUserStates();
        int userStateSize = newSelectionStates.size();
        int index2 = 0;
        while (index2 < userStateSize) {
            int userId = newSelectionStates.keyAt(index2);
            DomainVerificationInternalUserState newUserState = newSelectionStates.valueAt(index2);
            if (newUserState == null) {
                pkgSetting = pkgSetting2;
            } else {
                ArraySet<String> newEnabledHosts = newUserState.getEnabledHosts();
                DomainVerificationInternalUserState oldUserState = oldSelectionStates.get(userId);
                pkgSetting = pkgSetting2;
                boolean linkHandlingAllowed = newUserState.isLinkHandlingAllowed();
                if (oldUserState == null) {
                    oldSelectionStates.put(userId, new DomainVerificationInternalUserState(userId, newEnabledHosts, linkHandlingAllowed));
                } else {
                    oldUserState.addHosts(newEnabledHosts).setLinkHandlingAllowed(linkHandlingAllowed);
                }
            }
            index2++;
            pkgSetting2 = pkgSetting;
        }
    }

    public void removePackage(String packageName) {
        synchronized (this.mLock) {
            this.mPendingPkgStates.remove(packageName);
            this.mRestoredPkgStates.remove(packageName);
        }
    }

    public void removePackageForUser(String packageName, int userId) {
        synchronized (this.mLock) {
            DomainVerificationPkgState pendingPkgState = this.mPendingPkgStates.get(packageName);
            if (pendingPkgState != null) {
                pendingPkgState.removeUser(userId);
            }
            DomainVerificationPkgState restoredPkgState = this.mRestoredPkgStates.get(packageName);
            if (restoredPkgState != null) {
                restoredPkgState.removeUser(userId);
            }
        }
    }

    public void removeUser(int userId) {
        synchronized (this.mLock) {
            int pendingSize = this.mPendingPkgStates.size();
            for (int index = 0; index < pendingSize; index++) {
                this.mPendingPkgStates.valueAt(index).removeUser(userId);
            }
            int restoredSize = this.mRestoredPkgStates.size();
            for (int index2 = 0; index2 < restoredSize; index2++) {
                this.mRestoredPkgStates.valueAt(index2).removeUser(userId);
            }
        }
    }

    public DomainVerificationPkgState removePendingState(String pkgName) {
        DomainVerificationPkgState remove;
        synchronized (this.mLock) {
            remove = this.mPendingPkgStates.remove(pkgName);
        }
        return remove;
    }

    public DomainVerificationPkgState removeRestoredState(String pkgName) {
        DomainVerificationPkgState remove;
        synchronized (this.mLock) {
            remove = this.mRestoredPkgStates.remove(pkgName);
        }
        return remove;
    }
}
