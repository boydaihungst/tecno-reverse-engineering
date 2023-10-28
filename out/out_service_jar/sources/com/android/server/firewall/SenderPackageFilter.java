package com.android.server.firewall;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.os.UserHandle;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class SenderPackageFilter implements Filter {
    private static final String ATTR_NAME = "name";
    public static final FilterFactory FACTORY = new FilterFactory("sender-package") { // from class: com.android.server.firewall.SenderPackageFilter.1
        @Override // com.android.server.firewall.FilterFactory
        public Filter newFilter(XmlPullParser parser) throws IOException, XmlPullParserException {
            String packageName = parser.getAttributeValue(null, "name");
            if (packageName == null) {
                throw new XmlPullParserException("A package name must be specified.", parser, null);
            }
            return new SenderPackageFilter(packageName);
        }
    };
    public final String mPackageName;

    public SenderPackageFilter(String packageName) {
        this.mPackageName = packageName;
    }

    @Override // com.android.server.firewall.Filter
    public boolean matches(IntentFirewall ifw, ComponentName resolvedComponent, Intent intent, int callerUid, int callerPid, String resolvedType, int receivingUid) {
        IPackageManager pm = AppGlobals.getPackageManager();
        int packageUid = -1;
        try {
            packageUid = pm.getPackageUid(this.mPackageName, 4194304L, 0);
        } catch (RemoteException e) {
        }
        if (packageUid == -1) {
            return false;
        }
        return UserHandle.isSameApp(packageUid, callerUid);
    }
}
