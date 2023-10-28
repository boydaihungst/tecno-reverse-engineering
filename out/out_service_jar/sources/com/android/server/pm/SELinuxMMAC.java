package com.android.server.pm;

import android.content.pm.ApplicationInfo;
import android.os.Environment;
import android.util.Slog;
import android.util.Xml;
import com.android.server.compat.PlatformCompat;
import com.android.server.pm.Policy;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.SharedUserApi;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public final class SELinuxMMAC {
    private static final boolean DEBUG_POLICY = false;
    private static final boolean DEBUG_POLICY_INSTALL = false;
    private static final boolean DEBUG_POLICY_ORDER = false;
    private static final String DEFAULT_SEINFO = "default";
    private static final String PRIVILEGED_APP_STR = ":privapp";
    static final long SELINUX_LATEST_CHANGES = 143539591;
    static final long SELINUX_R_CHANGES = 168782947;
    static final String TAG = "SELinuxMMAC";
    private static final String TARGETSDKVERSION_STR = ":targetSdkVersion=";
    private static List<File> sMacPermissions;
    private static List<Policy> sPolicies = new ArrayList();
    private static boolean sPolicyRead;

    static {
        ArrayList arrayList = new ArrayList();
        sMacPermissions = arrayList;
        arrayList.add(new File(Environment.getRootDirectory(), "/etc/selinux/plat_mac_permissions.xml"));
        File systemExtMacPermission = new File(Environment.getSystemExtDirectory(), "/etc/selinux/system_ext_mac_permissions.xml");
        if (systemExtMacPermission.exists()) {
            sMacPermissions.add(systemExtMacPermission);
        }
        File productMacPermission = new File(Environment.getProductDirectory(), "/etc/selinux/product_mac_permissions.xml");
        if (productMacPermission.exists()) {
            sMacPermissions.add(productMacPermission);
        }
        File vendorMacPermission = new File(Environment.getVendorDirectory(), "/etc/selinux/vendor_mac_permissions.xml");
        if (vendorMacPermission.exists()) {
            sMacPermissions.add(vendorMacPermission);
        }
        File odmMacPermission = new File(Environment.getOdmDirectory(), "/etc/selinux/odm_mac_permissions.xml");
        if (odmMacPermission.exists()) {
            sMacPermissions.add(odmMacPermission);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [207=5] */
    public static boolean readInstallPolicy() {
        synchronized (sPolicies) {
            if (sPolicyRead) {
                return true;
            }
            List<Policy> policies = new ArrayList<>();
            XmlPullParser parser = Xml.newPullParser();
            int count = sMacPermissions.size();
            FileReader policyFile = null;
            for (int i = 0; i < count; i++) {
                File macPermission = sMacPermissions.get(i);
                try {
                    try {
                        try {
                            policyFile = new FileReader(macPermission);
                            Slog.d(TAG, "Using policy file " + macPermission);
                            parser.setInput(policyFile);
                            parser.nextTag();
                            parser.require(2, null, "policy");
                            while (parser.next() != 3) {
                                if (parser.getEventType() == 2) {
                                    String name = parser.getName();
                                    char c = 65535;
                                    switch (name.hashCode()) {
                                        case -902467798:
                                            if (name.equals("signer")) {
                                                c = 0;
                                                break;
                                            }
                                    }
                                    switch (c) {
                                        case 0:
                                            policies.add(readSignerOrThrow(parser));
                                            break;
                                        default:
                                            skip(parser);
                                            break;
                                    }
                                }
                            }
                            IoUtils.closeQuietly(policyFile);
                        } catch (IOException ioe) {
                            Slog.w(TAG, "Exception parsing " + macPermission, ioe);
                            IoUtils.closeQuietly(policyFile);
                            return false;
                        }
                    } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException ex) {
                        Slog.w(TAG, "Exception @" + parser.getPositionDescription() + " while parsing " + macPermission + ":" + ex);
                        IoUtils.closeQuietly(policyFile);
                        return false;
                    }
                } catch (Throwable th) {
                    IoUtils.closeQuietly(policyFile);
                    throw th;
                }
            }
            PolicyComparator policySort = new PolicyComparator();
            Collections.sort(policies, policySort);
            if (policySort.foundDuplicate()) {
                Slog.w(TAG, "ERROR! Duplicate entries found parsing mac_permissions.xml files");
                return false;
            }
            synchronized (sPolicies) {
                sPolicies.clear();
                sPolicies.addAll(policies);
                sPolicyRead = true;
            }
            return true;
        }
    }

    private static Policy readSignerOrThrow(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, null, "signer");
        Policy.PolicyBuilder pb = new Policy.PolicyBuilder();
        String cert = parser.getAttributeValue(null, "signature");
        if (cert != null) {
            pb.addSignature(cert);
        }
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if ("seinfo".equals(tagName)) {
                    String seinfo = parser.getAttributeValue(null, "value");
                    pb.setGlobalSeinfoOrThrow(seinfo);
                    readSeinfo(parser);
                } else if ("package".equals(tagName)) {
                    readPackageOrThrow(parser, pb);
                } else if ("cert".equals(tagName)) {
                    String sig = parser.getAttributeValue(null, "signature");
                    pb.addSignature(sig);
                    readCert(parser);
                } else {
                    skip(parser);
                }
            }
        }
        return pb.build();
    }

    private static void readPackageOrThrow(XmlPullParser parser, Policy.PolicyBuilder pb) throws IOException, XmlPullParserException {
        parser.require(2, null, "package");
        String pkgName = parser.getAttributeValue(null, "name");
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if ("seinfo".equals(tagName)) {
                    String seinfo = parser.getAttributeValue(null, "value");
                    pb.addInnerPackageMapOrThrow(pkgName, seinfo);
                    readSeinfo(parser);
                } else {
                    skip(parser);
                }
            }
        }
    }

    private static void readCert(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, null, "cert");
        parser.nextTag();
    }

    private static void readSeinfo(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, null, "seinfo");
        parser.nextTag();
    }

    private static void skip(XmlPullParser p) throws IOException, XmlPullParserException {
        if (p.getEventType() != 2) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (p.next()) {
                case 2:
                    depth++;
                    break;
                case 3:
                    depth--;
                    break;
            }
        }
    }

    private static int getTargetSdkVersionForSeInfo(AndroidPackage pkg, SharedUserApi sharedUser, PlatformCompat compatibility) {
        if (sharedUser != null && sharedUser.getPackages().size() != 0) {
            return sharedUser.getSeInfoTargetSdkVersion();
        }
        ApplicationInfo appInfo = AndroidPackageUtils.generateAppInfoWithoutState(pkg);
        if (compatibility.isChangeEnabledInternal(SELINUX_LATEST_CHANGES, appInfo)) {
            return Math.max(10000, pkg.getTargetSdkVersion());
        }
        if (compatibility.isChangeEnabledInternal(SELINUX_R_CHANGES, appInfo)) {
            return Math.max(30, pkg.getTargetSdkVersion());
        }
        return pkg.getTargetSdkVersion();
    }

    public static String getSeInfo(AndroidPackage pkg, SharedUserApi sharedUser, PlatformCompat compatibility) {
        int targetSdkVersion = getTargetSdkVersionForSeInfo(pkg, sharedUser, compatibility);
        boolean isPrivileged = sharedUser != null ? sharedUser.isPrivileged() | pkg.isPrivileged() : pkg.isPrivileged();
        return getSeInfo(pkg, isPrivileged, targetSdkVersion);
    }

    public static String getSeInfo(AndroidPackage pkg, boolean isPrivileged, int targetSdkVersion) {
        String seInfo = null;
        synchronized (sPolicies) {
            if (sPolicyRead) {
                for (Policy policy : sPolicies) {
                    seInfo = policy.getMatchedSeInfo(pkg);
                    if (seInfo != null) {
                        break;
                    }
                }
            }
        }
        if (seInfo == null) {
            seInfo = "default";
        }
        if (isPrivileged) {
            seInfo = seInfo + PRIVILEGED_APP_STR;
        }
        return seInfo + TARGETSDKVERSION_STR + targetSdkVersion;
    }
}
