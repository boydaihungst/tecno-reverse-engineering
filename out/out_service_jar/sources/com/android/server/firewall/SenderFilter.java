package com.android.server.firewall;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.os.Process;
import android.os.RemoteException;
import android.util.Slog;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
class SenderFilter {
    private static final String ATTR_TYPE = "type";
    public static final FilterFactory FACTORY = new FilterFactory("sender") { // from class: com.android.server.firewall.SenderFilter.1
        @Override // com.android.server.firewall.FilterFactory
        public Filter newFilter(XmlPullParser parser) throws IOException, XmlPullParserException {
            String typeString = parser.getAttributeValue(null, "type");
            if (typeString == null) {
                throw new XmlPullParserException("type attribute must be specified for <sender>", parser, null);
            }
            if (typeString.equals("system")) {
                return SenderFilter.SYSTEM;
            }
            if (typeString.equals(SenderFilter.VAL_SIGNATURE)) {
                return SenderFilter.SIGNATURE;
            }
            if (typeString.equals(SenderFilter.VAL_SYSTEM_OR_SIGNATURE)) {
                return SenderFilter.SYSTEM_OR_SIGNATURE;
            }
            if (typeString.equals("userId")) {
                return SenderFilter.USER_ID;
            }
            throw new XmlPullParserException("Invalid type attribute for <sender>: " + typeString, parser, null);
        }
    };
    private static final Filter SIGNATURE = new Filter() { // from class: com.android.server.firewall.SenderFilter.2
        @Override // com.android.server.firewall.Filter
        public boolean matches(IntentFirewall ifw, ComponentName resolvedComponent, Intent intent, int callerUid, int callerPid, String resolvedType, int receivingUid) {
            return ifw.signaturesMatch(callerUid, receivingUid);
        }
    };
    private static final Filter SYSTEM = new Filter() { // from class: com.android.server.firewall.SenderFilter.3
        @Override // com.android.server.firewall.Filter
        public boolean matches(IntentFirewall ifw, ComponentName resolvedComponent, Intent intent, int callerUid, int callerPid, String resolvedType, int receivingUid) {
            return SenderFilter.isPrivilegedApp(callerUid, callerPid);
        }
    };
    private static final Filter SYSTEM_OR_SIGNATURE = new Filter() { // from class: com.android.server.firewall.SenderFilter.4
        @Override // com.android.server.firewall.Filter
        public boolean matches(IntentFirewall ifw, ComponentName resolvedComponent, Intent intent, int callerUid, int callerPid, String resolvedType, int receivingUid) {
            return SenderFilter.isPrivilegedApp(callerUid, callerPid) || ifw.signaturesMatch(callerUid, receivingUid);
        }
    };
    private static final Filter USER_ID = new Filter() { // from class: com.android.server.firewall.SenderFilter.5
        @Override // com.android.server.firewall.Filter
        public boolean matches(IntentFirewall ifw, ComponentName resolvedComponent, Intent intent, int callerUid, int callerPid, String resolvedType, int receivingUid) {
            return ifw.checkComponentPermission(null, callerPid, callerUid, receivingUid, false);
        }
    };
    private static final String VAL_SIGNATURE = "signature";
    private static final String VAL_SYSTEM = "system";
    private static final String VAL_SYSTEM_OR_SIGNATURE = "system|signature";
    private static final String VAL_USER_ID = "userId";

    SenderFilter() {
    }

    static boolean isPrivilegedApp(int callerUid, int callerPid) {
        if (callerUid == 1000 || callerUid == 0 || callerPid == Process.myPid() || callerPid == 0) {
            return true;
        }
        IPackageManager pm = AppGlobals.getPackageManager();
        try {
            if ((pm.getPrivateFlagsForUid(callerUid) & 8) != 0) {
                return true;
            }
            return false;
        } catch (RemoteException ex) {
            Slog.e("IntentFirewall", "Remote exception while retrieving uid flags", ex);
            return false;
        }
    }
}
