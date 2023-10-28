package com.android.server.pm.verify.domain;

import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.server.pm.SettingsXml;
import com.android.server.pm.verify.domain.models.DomainVerificationInternalUserState;
import com.android.server.pm.verify.domain.models.DomainVerificationPkgState;
import com.android.server.pm.verify.domain.models.DomainVerificationStateMap;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class DomainVerificationPersistence {
    public static final String ATTR_ALLOW_LINK_HANDLING = "allowLinkHandling";
    private static final String ATTR_HAS_AUTO_VERIFY_DOMAINS = "hasAutoVerifyDomains";
    private static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_SIGNATURE = "signature";
    public static final String ATTR_STATE = "state";
    public static final String ATTR_USER_ID = "userId";
    private static final String TAG = "DomainVerificationPersistence";
    public static final String TAG_ACTIVE = "active";
    public static final String TAG_DOMAIN = "domain";
    public static final String TAG_DOMAIN_VERIFICATIONS = "domain-verifications";
    public static final String TAG_ENABLED_HOSTS = "enabled-hosts";
    public static final String TAG_HOST = "host";
    public static final String TAG_PACKAGE_STATE = "package-state";
    public static final String TAG_RESTORED = "restored";
    private static final String TAG_STATE = "state";
    public static final String TAG_USER_STATE = "user-state";
    private static final String TAG_USER_STATES = "user-states";

    public static void writeToXml(TypedXmlSerializer xmlSerializer, DomainVerificationStateMap<DomainVerificationPkgState> attached, ArrayMap<String, DomainVerificationPkgState> pending, ArrayMap<String, DomainVerificationPkgState> restored, int userId, Function<String, String> pkgNameToSignature) throws IOException {
        SettingsXml.Serializer serializer = SettingsXml.serializer(xmlSerializer);
        try {
            SettingsXml.WriteSection ignored = serializer.startSection(TAG_DOMAIN_VERIFICATIONS);
            ArraySet<DomainVerificationPkgState> active = new ArraySet<>();
            int attachedSize = attached.size();
            for (int attachedIndex = 0; attachedIndex < attachedSize; attachedIndex++) {
                active.add(attached.valueAt(attachedIndex));
            }
            int pendingSize = pending.size();
            for (int pendingIndex = 0; pendingIndex < pendingSize; pendingIndex++) {
                active.add(pending.valueAt(pendingIndex));
            }
            SettingsXml.WriteSection restoredSection = serializer.startSection(TAG_ACTIVE);
            try {
                writePackageStates(restoredSection, active, userId, pkgNameToSignature);
                if (restoredSection != null) {
                    restoredSection.close();
                }
                restoredSection = serializer.startSection(TAG_RESTORED);
                try {
                    writePackageStates(restoredSection, restored.values(), userId, pkgNameToSignature);
                    if (restoredSection != null) {
                        restoredSection.close();
                    }
                    if (ignored != null) {
                        ignored.close();
                    }
                    if (serializer != null) {
                        serializer.close();
                    }
                } finally {
                }
            } finally {
            }
        } catch (Throwable th) {
            if (serializer != null) {
                try {
                    serializer.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static void writePackageStates(SettingsXml.WriteSection section, Collection<DomainVerificationPkgState> states, int userId, Function<String, String> pkgNameToSignature) throws IOException {
        if (states.isEmpty()) {
            return;
        }
        for (DomainVerificationPkgState state : states) {
            writePkgStateToXml(section, state, userId, pkgNameToSignature);
        }
    }

    public static ReadResult readFromXml(TypedXmlPullParser parentParser) throws IOException, XmlPullParserException {
        ArrayMap<String, DomainVerificationPkgState> active = new ArrayMap<>();
        ArrayMap<String, DomainVerificationPkgState> restored = new ArrayMap<>();
        SettingsXml.ChildSection child = SettingsXml.parser(parentParser).children();
        while (child.moveToNext()) {
            String name = child.getName();
            char c = 65535;
            switch (name.hashCode()) {
                case -1422950650:
                    if (name.equals(TAG_ACTIVE)) {
                        c = 0;
                        break;
                    }
                    break;
                case -336625770:
                    if (name.equals(TAG_RESTORED)) {
                        c = 1;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    readPackageStates(child, active);
                    break;
                case 1:
                    readPackageStates(child, restored);
                    break;
            }
        }
        return new ReadResult(active, restored);
    }

    private static void readPackageStates(SettingsXml.ReadSection section, ArrayMap<String, DomainVerificationPkgState> map) {
        SettingsXml.ChildSection child = section.children();
        while (child.moveToNext(TAG_PACKAGE_STATE)) {
            DomainVerificationPkgState pkgState = createPkgStateFromXml(child);
            if (pkgState != null) {
                map.put(pkgState.getPackageName(), pkgState);
            }
        }
    }

    private static DomainVerificationPkgState createPkgStateFromXml(SettingsXml.ReadSection section) {
        String packageName = section.getString("packageName");
        String idString = section.getString(ATTR_ID);
        boolean hasAutoVerifyDomains = section.getBoolean(ATTR_HAS_AUTO_VERIFY_DOMAINS);
        String signature = section.getString(ATTR_SIGNATURE);
        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(idString)) {
            return null;
        }
        UUID id = UUID.fromString(idString);
        ArrayMap<String, Integer> stateMap = new ArrayMap<>();
        SparseArray<DomainVerificationInternalUserState> userStates = new SparseArray<>();
        SettingsXml.ChildSection child = section.children();
        while (child.moveToNext()) {
            String name = child.getName();
            char c = 65535;
            switch (name.hashCode()) {
                case -1576041916:
                    if (name.equals("user-states")) {
                        c = 1;
                        break;
                    }
                    break;
                case 109757585:
                    if (name.equals("state")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    readDomainStates(child, stateMap);
                    break;
                case 1:
                    readUserStates(child, userStates);
                    break;
            }
        }
        return new DomainVerificationPkgState(packageName, id, hasAutoVerifyDomains, stateMap, userStates, signature);
    }

    private static void readUserStates(SettingsXml.ReadSection section, SparseArray<DomainVerificationInternalUserState> userStates) {
        SettingsXml.ChildSection child = section.children();
        while (child.moveToNext("user-state")) {
            DomainVerificationInternalUserState userState = createUserStateFromXml(child);
            if (userState != null) {
                userStates.put(userState.getUserId(), userState);
            }
        }
    }

    private static void readDomainStates(SettingsXml.ReadSection stateSection, ArrayMap<String, Integer> stateMap) {
        SettingsXml.ChildSection child = stateSection.children();
        while (child.moveToNext(TAG_DOMAIN)) {
            String name = child.getString("name");
            int state = child.getInt("state", 0);
            stateMap.put(name, Integer.valueOf(state));
        }
    }

    private static void writePkgStateToXml(SettingsXml.WriteSection parentSection, DomainVerificationPkgState pkgState, int userId, Function<String, String> pkgNameToSignature) throws IOException {
        String packageName = pkgState.getPackageName();
        String signature = pkgNameToSignature == null ? null : pkgNameToSignature.apply(packageName);
        if (signature == null) {
            signature = pkgState.getBackupSignatureHash();
        }
        SettingsXml.WriteSection ignored = parentSection.startSection(TAG_PACKAGE_STATE).attribute("packageName", packageName).attribute(ATTR_ID, pkgState.getId().toString()).attribute(ATTR_HAS_AUTO_VERIFY_DOMAINS, pkgState.isHasAutoVerifyDomains()).attribute(ATTR_SIGNATURE, signature);
        try {
            writeStateMap(parentSection, pkgState.getStateMap());
            writeUserStates(parentSection, userId, pkgState.getUserStates());
            if (ignored != null) {
                ignored.close();
            }
        } catch (Throwable th) {
            if (ignored != null) {
                try {
                    ignored.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static void writeUserStates(SettingsXml.WriteSection parentSection, int userId, SparseArray<DomainVerificationInternalUserState> states) throws IOException {
        int size = states.size();
        if (size == 0) {
            return;
        }
        SettingsXml.WriteSection section = parentSection.startSection("user-states");
        try {
            if (userId == -1) {
                for (int index = 0; index < size; index++) {
                    writeUserStateToXml(section, states.valueAt(index));
                }
            } else {
                DomainVerificationInternalUserState userState = states.get(userId);
                if (userState != null) {
                    writeUserStateToXml(section, userState);
                }
            }
            if (section != null) {
                section.close();
            }
        } catch (Throwable th) {
            if (section != null) {
                try {
                    section.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static void writeStateMap(SettingsXml.WriteSection parentSection, ArrayMap<String, Integer> stateMap) throws IOException {
        if (stateMap.isEmpty()) {
            return;
        }
        SettingsXml.WriteSection stateSection = parentSection.startSection("state");
        try {
            int size = stateMap.size();
            for (int index = 0; index < size; index++) {
                stateSection.startSection(TAG_DOMAIN).attribute("name", stateMap.keyAt(index)).attribute("state", stateMap.valueAt(index).intValue()).finish();
            }
            if (stateSection != null) {
                stateSection.close();
            }
        } catch (Throwable th) {
            if (stateSection != null) {
                try {
                    stateSection.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static DomainVerificationInternalUserState createUserStateFromXml(SettingsXml.ReadSection section) {
        int userId = section.getInt("userId");
        if (userId == -1) {
            return null;
        }
        boolean allowLinkHandling = section.getBoolean(ATTR_ALLOW_LINK_HANDLING, false);
        ArraySet<String> enabledHosts = new ArraySet<>();
        SettingsXml.ChildSection child = section.children();
        while (child.moveToNext(TAG_ENABLED_HOSTS)) {
            readEnabledHosts(child, enabledHosts);
        }
        return new DomainVerificationInternalUserState(userId, enabledHosts, allowLinkHandling);
    }

    private static void readEnabledHosts(SettingsXml.ReadSection section, ArraySet<String> enabledHosts) {
        SettingsXml.ChildSection child = section.children();
        while (child.moveToNext("host")) {
            String hostName = child.getString("name");
            if (!TextUtils.isEmpty(hostName)) {
                enabledHosts.add(hostName);
            }
        }
    }

    private static void writeUserStateToXml(SettingsXml.WriteSection parentSection, DomainVerificationInternalUserState userState) throws IOException {
        SettingsXml.WriteSection section = parentSection.startSection("user-state").attribute("userId", userState.getUserId()).attribute(ATTR_ALLOW_LINK_HANDLING, userState.isLinkHandlingAllowed());
        try {
            ArraySet<String> enabledHosts = userState.getEnabledHosts();
            if (!enabledHosts.isEmpty()) {
                SettingsXml.WriteSection enabledHostsSection = section.startSection(TAG_ENABLED_HOSTS);
                int size = enabledHosts.size();
                for (int index = 0; index < size; index++) {
                    enabledHostsSection.startSection("host").attribute("name", enabledHosts.valueAt(index)).finish();
                }
                if (enabledHostsSection != null) {
                    enabledHostsSection.close();
                }
            }
            if (section != null) {
                section.close();
            }
        } catch (Throwable th) {
            if (section != null) {
                try {
                    section.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    /* loaded from: classes2.dex */
    public static class ReadResult {
        public final ArrayMap<String, DomainVerificationPkgState> active;
        public final ArrayMap<String, DomainVerificationPkgState> restored;

        public ReadResult(ArrayMap<String, DomainVerificationPkgState> active, ArrayMap<String, DomainVerificationPkgState> restored) {
            this.active = active;
            this.restored = restored;
        }
    }
}
