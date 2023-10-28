package com.android.server.devicepolicy;

import android.app.admin.DeviceAdminInfo;
import android.content.ComponentName;
import android.os.FileUtils;
import android.os.PersistableBundle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DebugUtils;
import android.util.IndentingPrintWriter;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.XmlUtils;
import com.android.server.utils.Slogf;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class DevicePolicyData {
    private static final String ATTR_ALIAS = "alias";
    private static final String ATTR_DEVICE_PAIRED = "device-paired";
    private static final String ATTR_DEVICE_PROVISIONING_CONFIG_APPLIED = "device-provisioning-config-applied";
    private static final String ATTR_DISABLED = "disabled";
    private static final String ATTR_FACTORY_RESET_FLAGS = "factory-reset-flags";
    private static final String ATTR_FACTORY_RESET_REASON = "factory-reset-reason";
    private static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_NEW_USER_DISCLAIMER = "new-user-disclaimer";
    private static final String ATTR_PERMISSION_POLICY = "permission-policy";
    private static final String ATTR_PERMISSION_PROVIDER = "permission-provider";
    private static final String ATTR_PROVISIONING_STATE = "provisioning-state";
    private static final String ATTR_SETUP_COMPLETE = "setup-complete";
    private static final String ATTR_VALUE = "value";
    public static final int FACTORY_RESET_FLAG_ON_BOOT = 1;
    public static final int FACTORY_RESET_FLAG_WIPE_EUICC = 4;
    public static final int FACTORY_RESET_FLAG_WIPE_EXTERNAL_STORAGE = 2;
    public static final int FACTORY_RESET_FLAG_WIPE_FACTORY_RESET_PROTECTION = 8;
    static final String NEW_USER_DISCLAIMER_ACKNOWLEDGED = "acked";
    static final String NEW_USER_DISCLAIMER_NEEDED = "needed";
    static final String NEW_USER_DISCLAIMER_NOT_NEEDED = "not_needed";
    private static final String TAG = "DevicePolicyManager";
    private static final String TAG_ACCEPTED_CA_CERTIFICATES = "accepted-ca-certificate";
    private static final String TAG_ADMIN_BROADCAST_PENDING = "admin-broadcast-pending";
    private static final String TAG_AFFILIATION_ID = "affiliation-id";
    private static final String TAG_APPS_SUSPENDED = "apps-suspended";
    private static final String TAG_BYPASS_ROLE_QUALIFICATIONS = "bypass-role-qualifications";
    private static final String TAG_CURRENT_INPUT_METHOD_SET = "current-ime-set";
    private static final String TAG_DO_NOT_ASK_CREDENTIALS_ON_BOOT = "do-not-ask-credentials-on-boot";
    private static final String TAG_INITIALIZATION_BUNDLE = "initialization-bundle";
    private static final String TAG_LAST_BUG_REPORT_REQUEST = "last-bug-report-request";
    private static final String TAG_LAST_NETWORK_LOG_RETRIEVAL = "last-network-log-retrieval";
    private static final String TAG_LAST_SECURITY_LOG_RETRIEVAL = "last-security-log-retrieval";
    private static final String TAG_LOCK_TASK_COMPONENTS = "lock-task-component";
    private static final String TAG_LOCK_TASK_FEATURES = "lock-task-features";
    private static final String TAG_OWNER_INSTALLED_CA_CERT = "owner-installed-ca-cert";
    private static final String TAG_PASSWORD_TOKEN_HANDLE = "password-token";
    private static final String TAG_PASSWORD_VALIDITY = "password-validity";
    private static final String TAG_PROTECTED_PACKAGES = "protected-packages";
    private static final String TAG_SECONDARY_LOCK_SCREEN = "secondary-lock-screen";
    private static final String TAG_STATUS_BAR = "statusbar";
    private static final boolean VERBOSE_LOG = false;
    String mCurrentRoleHolder;
    int mFactoryResetFlags;
    String mFactoryResetReason;
    int mPermissionPolicy;
    ComponentName mRestrictionsProvider;
    @Deprecated
    List<String> mUserControlDisabledPackages;
    final int mUserId;
    int mUserProvisioningState;
    int mFailedPasswordAttempts = 0;
    boolean mPasswordValidAtLastCheckpoint = true;
    int mPasswordOwner = -1;
    long mLastMaximumTimeToLock = -1;
    boolean mUserSetupComplete = false;
    boolean mBypassDevicePolicyManagementRoleQualifications = false;
    boolean mPaired = false;
    boolean mDeviceProvisioningConfigApplied = false;
    final ArrayMap<ComponentName, ActiveAdmin> mAdminMap = new ArrayMap<>();
    final ArrayList<ActiveAdmin> mAdminList = new ArrayList<>();
    final ArrayList<ComponentName> mRemovingAdmins = new ArrayList<>();
    final ArraySet<String> mAcceptedCaCertificates = new ArraySet<>();
    List<String> mLockTaskPackages = new ArrayList();
    int mLockTaskFeatures = 16;
    boolean mStatusBarDisabled = false;
    final ArrayMap<String, List<String>> mDelegationMap = new ArrayMap<>();
    boolean mDoNotAskCredentialsOnBoot = false;
    Set<String> mAffiliationIds = new ArraySet();
    long mLastSecurityLogRetrievalTime = -1;
    long mLastBugReportRequestTime = -1;
    long mLastNetworkLogsRetrievalTime = -1;
    boolean mCurrentInputMethodSet = false;
    boolean mSecondaryLockscreenEnabled = false;
    Set<String> mOwnerInstalledCaCerts = new ArraySet();
    boolean mAdminBroadcastPending = false;
    PersistableBundle mInitBundle = null;
    long mPasswordTokenHandle = 0;
    boolean mAppsSuspended = false;
    String mNewUserDisclaimer = NEW_USER_DISCLAIMER_NOT_NEEDED;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DevicePolicyData(int userId) {
        this.mUserId = userId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean store(DevicePolicyData policyData, JournaledFile file, boolean isFdeDevice) {
        String str;
        int n;
        String str2 = TAG_DO_NOT_ASK_CREDENTIALS_ON_BOOT;
        FileOutputStream stream = null;
        File chooseForWrite = null;
        try {
            File chooseForWrite2 = file.chooseForWrite();
            String str3 = TAG_AFFILIATION_ID;
            try {
                stream = new FileOutputStream(chooseForWrite2, false);
                TypedXmlSerializer out = Xml.resolveSerializer(stream);
                chooseForWrite = chooseForWrite2;
                out.startDocument((String) null, true);
                out.startTag((String) null, "policies");
                ComponentName componentName = policyData.mRestrictionsProvider;
                if (componentName == null) {
                    str = "policies";
                } else {
                    str = "policies";
                    out.attribute((String) null, ATTR_PERMISSION_PROVIDER, componentName.flattenToString());
                }
                if (policyData.mUserSetupComplete) {
                    out.attributeBoolean((String) null, ATTR_SETUP_COMPLETE, true);
                }
                if (policyData.mPaired) {
                    out.attributeBoolean((String) null, ATTR_DEVICE_PAIRED, true);
                }
                if (policyData.mDeviceProvisioningConfigApplied) {
                    out.attributeBoolean((String) null, ATTR_DEVICE_PROVISIONING_CONFIG_APPLIED, true);
                }
                int i = policyData.mUserProvisioningState;
                if (i != 0) {
                    out.attributeInt((String) null, ATTR_PROVISIONING_STATE, i);
                }
                int i2 = policyData.mPermissionPolicy;
                if (i2 != 0) {
                    out.attributeInt((String) null, ATTR_PERMISSION_POLICY, i2);
                }
                if (NEW_USER_DISCLAIMER_NEEDED.equals(policyData.mNewUserDisclaimer)) {
                    out.attribute((String) null, ATTR_NEW_USER_DISCLAIMER, policyData.mNewUserDisclaimer);
                }
                int i3 = policyData.mFactoryResetFlags;
                if (i3 != 0) {
                    out.attributeInt((String) null, ATTR_FACTORY_RESET_FLAGS, i3);
                }
                String str4 = policyData.mFactoryResetReason;
                if (str4 != null) {
                    out.attribute((String) null, ATTR_FACTORY_RESET_REASON, str4);
                }
                for (int i4 = 0; i4 < policyData.mDelegationMap.size(); i4++) {
                    String scope = policyData.mDelegationMap.keyAt(i4);
                    List<String> scopes = policyData.mDelegationMap.valueAt(i4);
                    for (String scope2 : scopes) {
                        out.startTag((String) null, "delegation");
                        out.attribute((String) null, "delegatePackage", scope);
                        String delegatePackage = scope;
                        out.attribute((String) null, "scope", scope2);
                        out.endTag((String) null, "delegation");
                        scopes = scopes;
                        str2 = str2;
                        scope = delegatePackage;
                    }
                }
                String str5 = str2;
                int n2 = policyData.mAdminList.size();
                int i5 = 0;
                while (i5 < n2) {
                    ActiveAdmin ap = policyData.mAdminList.get(i5);
                    if (ap == null) {
                        n = n2;
                    } else {
                        out.startTag((String) null, "admin");
                        n = n2;
                        out.attribute((String) null, "name", ap.info.getComponent().flattenToString());
                        ap.writeToXml(out);
                        out.endTag((String) null, "admin");
                    }
                    i5++;
                    n2 = n;
                }
                int n3 = policyData.mPasswordOwner;
                if (n3 >= 0) {
                    out.startTag((String) null, "password-owner");
                    out.attributeInt((String) null, ATTR_VALUE, policyData.mPasswordOwner);
                    out.endTag((String) null, "password-owner");
                }
                if (policyData.mFailedPasswordAttempts != 0) {
                    out.startTag((String) null, "failed-password-attempts");
                    out.attributeInt((String) null, ATTR_VALUE, policyData.mFailedPasswordAttempts);
                    out.endTag((String) null, "failed-password-attempts");
                }
                if (isFdeDevice) {
                    out.startTag((String) null, TAG_PASSWORD_VALIDITY);
                    out.attributeBoolean((String) null, ATTR_VALUE, policyData.mPasswordValidAtLastCheckpoint);
                    out.endTag((String) null, TAG_PASSWORD_VALIDITY);
                }
                for (int i6 = 0; i6 < policyData.mAcceptedCaCertificates.size(); i6++) {
                    out.startTag((String) null, TAG_ACCEPTED_CA_CERTIFICATES);
                    out.attribute((String) null, "name", policyData.mAcceptedCaCertificates.valueAt(i6));
                    out.endTag((String) null, TAG_ACCEPTED_CA_CERTIFICATES);
                }
                for (int i7 = 0; i7 < policyData.mLockTaskPackages.size(); i7++) {
                    String component = policyData.mLockTaskPackages.get(i7);
                    out.startTag((String) null, TAG_LOCK_TASK_COMPONENTS);
                    out.attribute((String) null, "name", component);
                    out.endTag((String) null, TAG_LOCK_TASK_COMPONENTS);
                }
                int i8 = policyData.mLockTaskFeatures;
                if (i8 != 0) {
                    out.startTag((String) null, TAG_LOCK_TASK_FEATURES);
                    out.attributeInt((String) null, ATTR_VALUE, policyData.mLockTaskFeatures);
                    out.endTag((String) null, TAG_LOCK_TASK_FEATURES);
                }
                if (policyData.mSecondaryLockscreenEnabled) {
                    out.startTag((String) null, TAG_SECONDARY_LOCK_SCREEN);
                    out.attributeBoolean((String) null, ATTR_VALUE, true);
                    out.endTag((String) null, TAG_SECONDARY_LOCK_SCREEN);
                }
                if (policyData.mStatusBarDisabled) {
                    out.startTag((String) null, TAG_STATUS_BAR);
                    out.attributeBoolean((String) null, "disabled", policyData.mStatusBarDisabled);
                    out.endTag((String) null, TAG_STATUS_BAR);
                }
                if (policyData.mDoNotAskCredentialsOnBoot) {
                    out.startTag((String) null, str5);
                    out.endTag((String) null, str5);
                }
                for (String id : policyData.mAffiliationIds) {
                    String str6 = str3;
                    out.startTag((String) null, str6);
                    out.attribute((String) null, ATTR_ID, id);
                    out.endTag((String) null, str6);
                    str3 = str6;
                }
                if (policyData.mLastSecurityLogRetrievalTime >= 0) {
                    out.startTag((String) null, TAG_LAST_SECURITY_LOG_RETRIEVAL);
                    out.attributeLong((String) null, ATTR_VALUE, policyData.mLastSecurityLogRetrievalTime);
                    out.endTag((String) null, TAG_LAST_SECURITY_LOG_RETRIEVAL);
                }
                if (policyData.mLastBugReportRequestTime >= 0) {
                    out.startTag((String) null, TAG_LAST_BUG_REPORT_REQUEST);
                    out.attributeLong((String) null, ATTR_VALUE, policyData.mLastBugReportRequestTime);
                    out.endTag((String) null, TAG_LAST_BUG_REPORT_REQUEST);
                }
                if (policyData.mLastNetworkLogsRetrievalTime >= 0) {
                    out.startTag((String) null, TAG_LAST_NETWORK_LOG_RETRIEVAL);
                    out.attributeLong((String) null, ATTR_VALUE, policyData.mLastNetworkLogsRetrievalTime);
                    out.endTag((String) null, TAG_LAST_NETWORK_LOG_RETRIEVAL);
                }
                if (policyData.mAdminBroadcastPending) {
                    out.startTag((String) null, TAG_ADMIN_BROADCAST_PENDING);
                    out.attributeBoolean((String) null, ATTR_VALUE, policyData.mAdminBroadcastPending);
                    out.endTag((String) null, TAG_ADMIN_BROADCAST_PENDING);
                }
                if (policyData.mInitBundle != null) {
                    out.startTag((String) null, TAG_INITIALIZATION_BUNDLE);
                    policyData.mInitBundle.saveToXml(out);
                    out.endTag((String) null, TAG_INITIALIZATION_BUNDLE);
                }
                if (policyData.mPasswordTokenHandle != 0) {
                    out.startTag((String) null, TAG_PASSWORD_TOKEN_HANDLE);
                    out.attributeLong((String) null, ATTR_VALUE, policyData.mPasswordTokenHandle);
                    out.endTag((String) null, TAG_PASSWORD_TOKEN_HANDLE);
                }
                if (policyData.mCurrentInputMethodSet) {
                    out.startTag((String) null, TAG_CURRENT_INPUT_METHOD_SET);
                    out.endTag((String) null, TAG_CURRENT_INPUT_METHOD_SET);
                }
                for (String cert : policyData.mOwnerInstalledCaCerts) {
                    out.startTag((String) null, TAG_OWNER_INSTALLED_CA_CERT);
                    out.attribute((String) null, ATTR_ALIAS, cert);
                    out.endTag((String) null, TAG_OWNER_INSTALLED_CA_CERT);
                }
                if (policyData.mAppsSuspended) {
                    out.startTag((String) null, TAG_APPS_SUSPENDED);
                    out.attributeBoolean((String) null, ATTR_VALUE, policyData.mAppsSuspended);
                    out.endTag((String) null, TAG_APPS_SUSPENDED);
                }
                if (policyData.mBypassDevicePolicyManagementRoleQualifications) {
                    out.startTag((String) null, TAG_BYPASS_ROLE_QUALIFICATIONS);
                    out.attribute((String) null, ATTR_VALUE, policyData.mCurrentRoleHolder);
                    out.endTag((String) null, TAG_BYPASS_ROLE_QUALIFICATIONS);
                }
                out.endTag((String) null, str);
                out.endDocument();
                stream.flush();
                FileUtils.sync(stream);
                stream.close();
                file.commit();
                return true;
            } catch (IOException | XmlPullParserException e) {
                e = e;
                chooseForWrite = chooseForWrite2;
                Slogf.w(TAG, e, "failed writing file %s", chooseForWrite);
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e2) {
                    }
                }
                file.rollback();
                return false;
            }
        } catch (IOException | XmlPullParserException e3) {
            e = e3;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [578=6, 580=6] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Not initialized variable reg: 17, insn: 0x03cc: MOVE  (r3 I:??[OBJECT, ARRAY]) = (r17 I:??[OBJECT, ARRAY] A[D('stream' java.io.FileInputStream)]), block:B:179:0x03cb */
    /* JADX WARN: Not initialized variable reg: 17, insn: 0x03d0: MOVE  (r3 I:??[OBJECT, ARRAY]) = (r17 I:??[OBJECT, ARRAY] A[D('stream' java.io.FileInputStream)]), block:B:181:0x03d0 */
    /* JADX WARN: Removed duplicated region for block: B:202:0x03f6 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static void load(DevicePolicyData policy, boolean isFdeDevice, JournaledFile journaledFile, Function<ComponentName, DeviceAdminInfo> adminInfoSupplier, ComponentName ownerComponent) {
        Exception e;
        TypedXmlPullParser parser;
        String tag;
        FileInputStream stream;
        FileInputStream stream2;
        int outerDepth;
        int provisioningState;
        int permissionPolicy;
        String name;
        RuntimeException e2;
        FileInputStream stream3 = null;
        File file = journaledFile.chooseForRead();
        try {
            stream3 = new FileInputStream(file);
            try {
                try {
                    parser = Xml.resolvePullParser(stream3);
                    while (true) {
                        int type = parser.next();
                        if (type == 1 || type == 2) {
                            break;
                        }
                    }
                    tag = parser.getName();
                    try {
                        try {
                        } catch (FileNotFoundException e3) {
                            stream3 = stream2;
                        }
                    } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e4) {
                        e = e4;
                        stream3 = stream;
                    }
                } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e5) {
                    e = e5;
                }
            } catch (FileNotFoundException e6) {
            }
        } catch (FileNotFoundException e7) {
        } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e8) {
            e = e8;
        }
        if (!"policies".equals(tag)) {
            throw new XmlPullParserException("Settings do not start with policies tag: found " + tag);
        }
        String permissionProvider = parser.getAttributeValue((String) null, ATTR_PERMISSION_PROVIDER);
        if (permissionProvider != null) {
            try {
                try {
                    policy.mRestrictionsProvider = ComponentName.unflattenFromString(permissionProvider);
                } catch (FileNotFoundException e9) {
                }
            } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e10) {
                e = e10;
                Slogf.w(TAG, e, "failed parsing %s", file);
                if (stream3 != null) {
                }
                policy.mAdminList.addAll(policy.mAdminMap.values());
            }
        }
        String userSetupComplete = parser.getAttributeValue((String) null, ATTR_SETUP_COMPLETE);
        if (Boolean.toString(true).equals(userSetupComplete)) {
            policy.mUserSetupComplete = true;
        }
        String paired = parser.getAttributeValue((String) null, ATTR_DEVICE_PAIRED);
        if (Boolean.toString(true).equals(paired)) {
            policy.mPaired = true;
        }
        String deviceProvisioningConfigApplied = parser.getAttributeValue((String) null, ATTR_DEVICE_PROVISIONING_CONFIG_APPLIED);
        if (Boolean.toString(true).equals(deviceProvisioningConfigApplied)) {
            policy.mDeviceProvisioningConfigApplied = true;
        }
        int provisioningState2 = parser.getAttributeInt((String) null, ATTR_PROVISIONING_STATE, -1);
        if (provisioningState2 != -1) {
            policy.mUserProvisioningState = provisioningState2;
        }
        try {
            try {
                int permissionPolicy2 = parser.getAttributeInt((String) null, ATTR_PERMISSION_POLICY, -1);
                if (permissionPolicy2 != -1) {
                    try {
                        policy.mPermissionPolicy = permissionPolicy2;
                    } catch (FileNotFoundException e11) {
                        stream3 = stream3;
                    } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e12) {
                        stream3 = stream3;
                        e = e12;
                        Slogf.w(TAG, e, "failed parsing %s", file);
                        if (stream3 != null) {
                        }
                        policy.mAdminList.addAll(policy.mAdminMap.values());
                    }
                }
                policy.mNewUserDisclaimer = parser.getAttributeValue((String) null, ATTR_NEW_USER_DISCLAIMER);
                policy.mFactoryResetFlags = parser.getAttributeInt((String) null, ATTR_FACTORY_RESET_FLAGS, 0);
                policy.mFactoryResetReason = parser.getAttributeValue((String) null, ATTR_FACTORY_RESET_REASON);
                int outerDepth2 = parser.getDepth();
                policy.mLockTaskPackages.clear();
                policy.mAdminList.clear();
                policy.mAdminMap.clear();
                policy.mAffiliationIds.clear();
                policy.mOwnerInstalledCaCerts.clear();
                policy.mUserControlDisabledPackages = null;
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth2)) {
                        break;
                    }
                    if (type2 == 3) {
                        outerDepth = outerDepth2;
                        provisioningState = provisioningState2;
                        permissionPolicy = permissionPolicy2;
                        name = null;
                    } else if (type2 == 4) {
                        outerDepth = outerDepth2;
                        provisioningState = provisioningState2;
                        permissionPolicy = permissionPolicy2;
                        name = null;
                    } else {
                        String tag2 = parser.getName();
                        if ("admin".equals(tag2)) {
                            String name2 = parser.getAttributeValue((String) null, "name");
                            try {
                                outerDepth = outerDepth2;
                                try {
                                    DeviceAdminInfo dai = adminInfoSupplier.apply(ComponentName.unflattenFromString(name2));
                                    if (dai != null) {
                                        provisioningState = provisioningState2;
                                        try {
                                            boolean overwritePolicies = !dai.getComponent().equals(ownerComponent);
                                            permissionPolicy = permissionPolicy2;
                                            try {
                                                ActiveAdmin ap = new ActiveAdmin(dai, false);
                                                ap.readFromXml(parser, overwritePolicies);
                                                policy.mAdminMap.put(ap.info.getComponent(), ap);
                                            } catch (RuntimeException e13) {
                                                e2 = e13;
                                                Slogf.w(TAG, e2, "Failed loading admin %s", name2);
                                                name = null;
                                                outerDepth2 = outerDepth;
                                                provisioningState2 = provisioningState;
                                                permissionPolicy2 = permissionPolicy;
                                            }
                                        } catch (RuntimeException e14) {
                                            permissionPolicy = permissionPolicy2;
                                            e2 = e14;
                                        }
                                    } else {
                                        provisioningState = provisioningState2;
                                        permissionPolicy = permissionPolicy2;
                                    }
                                } catch (RuntimeException e15) {
                                    provisioningState = provisioningState2;
                                    permissionPolicy = permissionPolicy2;
                                    e2 = e15;
                                }
                            } catch (RuntimeException e16) {
                                outerDepth = outerDepth2;
                                provisioningState = provisioningState2;
                                permissionPolicy = permissionPolicy2;
                                e2 = e16;
                            }
                            name = null;
                        } else {
                            outerDepth = outerDepth2;
                            provisioningState = provisioningState2;
                            permissionPolicy = permissionPolicy2;
                            if ("delegation".equals(tag2)) {
                                String delegatePackage = parser.getAttributeValue((String) null, "delegatePackage");
                                String scope = parser.getAttributeValue((String) null, "scope");
                                List<String> scopes = policy.mDelegationMap.get(delegatePackage);
                                if (scopes == null) {
                                    scopes = new ArrayList();
                                    policy.mDelegationMap.put(delegatePackage, scopes);
                                }
                                if (!scopes.contains(scope)) {
                                    scopes.add(scope);
                                }
                                name = null;
                            } else if ("failed-password-attempts".equals(tag2)) {
                                policy.mFailedPasswordAttempts = parser.getAttributeInt((String) null, ATTR_VALUE);
                                name = null;
                            } else if ("password-owner".equals(tag2)) {
                                policy.mPasswordOwner = parser.getAttributeInt((String) null, ATTR_VALUE);
                                name = null;
                            } else if (TAG_ACCEPTED_CA_CERTIFICATES.equals(tag2)) {
                                policy.mAcceptedCaCertificates.add(parser.getAttributeValue((String) null, "name"));
                                name = null;
                            } else if (TAG_LOCK_TASK_COMPONENTS.equals(tag2)) {
                                policy.mLockTaskPackages.add(parser.getAttributeValue((String) null, "name"));
                                name = null;
                            } else if (TAG_LOCK_TASK_FEATURES.equals(tag2)) {
                                policy.mLockTaskFeatures = parser.getAttributeInt((String) null, ATTR_VALUE);
                                name = null;
                            } else if (TAG_SECONDARY_LOCK_SCREEN.equals(tag2)) {
                                policy.mSecondaryLockscreenEnabled = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                                name = null;
                            } else if (TAG_STATUS_BAR.equals(tag2)) {
                                policy.mStatusBarDisabled = parser.getAttributeBoolean((String) null, "disabled", false);
                                name = null;
                            } else if (TAG_DO_NOT_ASK_CREDENTIALS_ON_BOOT.equals(tag2)) {
                                policy.mDoNotAskCredentialsOnBoot = true;
                                name = null;
                            } else if (TAG_AFFILIATION_ID.equals(tag2)) {
                                policy.mAffiliationIds.add(parser.getAttributeValue((String) null, ATTR_ID));
                                name = null;
                            } else if (TAG_LAST_SECURITY_LOG_RETRIEVAL.equals(tag2)) {
                                policy.mLastSecurityLogRetrievalTime = parser.getAttributeLong((String) null, ATTR_VALUE);
                                name = null;
                            } else if (TAG_LAST_BUG_REPORT_REQUEST.equals(tag2)) {
                                policy.mLastBugReportRequestTime = parser.getAttributeLong((String) null, ATTR_VALUE);
                                name = null;
                            } else if (TAG_LAST_NETWORK_LOG_RETRIEVAL.equals(tag2)) {
                                policy.mLastNetworkLogsRetrievalTime = parser.getAttributeLong((String) null, ATTR_VALUE);
                                name = null;
                            } else if (TAG_ADMIN_BROADCAST_PENDING.equals(tag2)) {
                                String pending = parser.getAttributeValue((String) null, ATTR_VALUE);
                                policy.mAdminBroadcastPending = Boolean.toString(true).equals(pending);
                                name = null;
                            } else if (TAG_INITIALIZATION_BUNDLE.equals(tag2)) {
                                policy.mInitBundle = PersistableBundle.restoreFromXml(parser);
                                name = null;
                            } else if (TAG_PASSWORD_VALIDITY.equals(tag2)) {
                                if (isFdeDevice) {
                                    policy.mPasswordValidAtLastCheckpoint = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                                    name = null;
                                } else {
                                    name = null;
                                }
                            } else if (TAG_PASSWORD_TOKEN_HANDLE.equals(tag2)) {
                                policy.mPasswordTokenHandle = parser.getAttributeLong((String) null, ATTR_VALUE);
                                name = null;
                            } else if (TAG_CURRENT_INPUT_METHOD_SET.equals(tag2)) {
                                policy.mCurrentInputMethodSet = true;
                                name = null;
                            } else if (TAG_OWNER_INSTALLED_CA_CERT.equals(tag2)) {
                                policy.mOwnerInstalledCaCerts.add(parser.getAttributeValue((String) null, ATTR_ALIAS));
                                name = null;
                            } else if (TAG_APPS_SUSPENDED.equals(tag2)) {
                                policy.mAppsSuspended = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                                name = null;
                            } else if (TAG_BYPASS_ROLE_QUALIFICATIONS.equals(tag2)) {
                                policy.mBypassDevicePolicyManagementRoleQualifications = true;
                                policy.mCurrentRoleHolder = parser.getAttributeValue((String) null, ATTR_VALUE);
                                name = null;
                            } else if (TAG_PROTECTED_PACKAGES.equals(tag2)) {
                                if (policy.mUserControlDisabledPackages == null) {
                                    policy.mUserControlDisabledPackages = new ArrayList();
                                }
                                name = null;
                                policy.mUserControlDisabledPackages.add(parser.getAttributeValue((String) null, "name"));
                            } else {
                                name = null;
                                Slogf.w(TAG, "Unknown tag: %s", tag2);
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                    outerDepth2 = outerDepth;
                    provisioningState2 = provisioningState;
                    permissionPolicy2 = permissionPolicy;
                }
                stream3 = stream3;
            } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e17) {
                e = e17;
                stream3 = stream3;
            }
        } catch (FileNotFoundException e18) {
            stream3 = stream3;
        }
        if (stream3 != null) {
            try {
                stream3.close();
            } catch (IOException e19) {
            }
        }
        policy.mAdminList.addAll(policy.mAdminMap.values());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void validatePasswordOwner() {
        if (this.mPasswordOwner >= 0) {
            boolean haveOwner = false;
            int i = this.mAdminList.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                } else if (this.mAdminList.get(i).getUid() != this.mPasswordOwner) {
                    i--;
                } else {
                    haveOwner = true;
                    break;
                }
            }
            if (!haveOwner) {
                Slogf.w(TAG, "Previous password owner %s no longer active; disabling", Integer.valueOf(this.mPasswordOwner));
                this.mPasswordOwner = -1;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDelayedFactoryReset(String reason, boolean wipeExtRequested, boolean wipeEuicc, boolean wipeResetProtectionData) {
        this.mFactoryResetReason = reason;
        this.mFactoryResetFlags = 1;
        if (wipeExtRequested) {
            this.mFactoryResetFlags = 1 | 2;
        }
        if (wipeEuicc) {
            this.mFactoryResetFlags |= 4;
        }
        if (wipeResetProtectionData) {
            this.mFactoryResetFlags |= 8;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNewUserDisclaimerAcknowledged() {
        String str = this.mNewUserDisclaimer;
        if (str == null) {
            int i = this.mUserId;
            if (i == 0) {
                return true;
            }
            Slogf.w(TAG, "isNewUserDisclaimerAcknowledged(%d): mNewUserDisclaimer is null", Integer.valueOf(i));
            return false;
        }
        char c = 65535;
        switch (str.hashCode()) {
            case -1238968671:
                if (str.equals(NEW_USER_DISCLAIMER_NOT_NEEDED)) {
                    c = 1;
                    break;
                }
                break;
            case -1049376843:
                if (str.equals(NEW_USER_DISCLAIMER_NEEDED)) {
                    c = 2;
                    break;
                }
                break;
            case 92636904:
                if (str.equals(NEW_USER_DISCLAIMER_ACKNOWLEDGED)) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
            case 1:
                return true;
            case 2:
                return false;
            default:
                Slogf.w(TAG, "isNewUserDisclaimerAcknowledged(%d): invalid value %d", Integer.valueOf(this.mUserId), this.mNewUserDisclaimer);
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        pw.println();
        pw.println("Enabled Device Admins (User " + this.mUserId + ", provisioningState: " + this.mUserProvisioningState + "):");
        int n = this.mAdminList.size();
        for (int i = 0; i < n; i++) {
            ActiveAdmin ap = this.mAdminList.get(i);
            if (ap != null) {
                pw.increaseIndent();
                pw.print(ap.info.getComponent().flattenToShortString());
                pw.println(":");
                pw.increaseIndent();
                ap.dump(pw);
                pw.decreaseIndent();
                pw.decreaseIndent();
            }
        }
        if (!this.mRemovingAdmins.isEmpty()) {
            pw.increaseIndent();
            pw.println("Removing Device Admins (User " + this.mUserId + "): " + this.mRemovingAdmins);
            pw.decreaseIndent();
        }
        pw.println();
        pw.increaseIndent();
        pw.print("mPasswordOwner=");
        pw.println(this.mPasswordOwner);
        pw.print("mPasswordTokenHandle=");
        pw.println(Long.toHexString(this.mPasswordTokenHandle));
        pw.print("mAppsSuspended=");
        pw.println(this.mAppsSuspended);
        pw.print("mUserSetupComplete=");
        pw.println(this.mUserSetupComplete);
        pw.print("mAffiliationIds=");
        pw.println(this.mAffiliationIds);
        pw.print("mNewUserDisclaimer=");
        pw.println(this.mNewUserDisclaimer);
        if (this.mFactoryResetFlags != 0) {
            pw.print("mFactoryResetFlags=");
            pw.print(this.mFactoryResetFlags);
            pw.print(" (");
            pw.print(factoryResetFlagsToString(this.mFactoryResetFlags));
            pw.println(')');
        }
        if (this.mFactoryResetReason != null) {
            pw.print("mFactoryResetReason=");
            pw.println(this.mFactoryResetReason);
        }
        pw.decreaseIndent();
    }

    static String factoryResetFlagsToString(int flags) {
        return DebugUtils.flagsToString(DevicePolicyData.class, "FACTORY_RESET_FLAG_", flags);
    }
}
