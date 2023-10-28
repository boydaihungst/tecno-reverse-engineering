package android.net.shared;

import android.net.INetd;
import android.net.TetherConfigParcel;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.system.OsConstants;
import android.util.Log;
/* loaded from: classes.dex */
public class NetdUtils {
    private static final String TAG = NetdUtils.class.getSimpleName();

    public static void tetherStart(INetd netd, boolean usingLegacyDnsProxy, String[] dhcpRange) throws RemoteException, ServiceSpecificException {
        TetherConfigParcel config = new TetherConfigParcel();
        config.usingLegacyDnsProxy = usingLegacyDnsProxy;
        config.dhcpRanges = dhcpRange;
        netd.tetherStartWithConfiguration(config);
    }

    private static void networkAddInterface(INetd netd, String iface, int maxAttempts, int pollingIntervalMs) throws ServiceSpecificException, RemoteException {
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                netd.networkAddInterface(99, iface);
                return;
            } catch (ServiceSpecificException e) {
                if (e.errorCode == OsConstants.EBUSY && i < maxAttempts) {
                    SystemClock.sleep(pollingIntervalMs);
                } else {
                    Log.e(TAG, "Retry Netd#networkAddInterface failure: " + e);
                    throw e;
                }
            }
        }
    }

    public static void untetherInterface(INetd netd, String iface) throws RemoteException, ServiceSpecificException {
        try {
            netd.tetherInterfaceRemove(iface);
        } finally {
            netd.networkRemoveInterface(99, iface);
        }
    }
}
