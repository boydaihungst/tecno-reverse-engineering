package com.android.net.module.util;

import android.net.INetd;
import android.net.InterfaceConfigurationParcel;
import android.net.IpPrefix;
import android.net.RouteInfo;
import android.net.TetherConfigParcel;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.system.OsConstants;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/* loaded from: classes.dex */
public class NetdUtils {
    private static final String TAG = NetdUtils.class.getSimpleName();

    /* loaded from: classes.dex */
    public enum ModifyOperation {
        ADD,
        REMOVE
    }

    public static InterfaceConfigurationParcel getInterfaceConfigParcel(INetd netd, String iface) {
        try {
            return netd.interfaceGetCfg(iface);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void validateFlag(String flag) {
        if (flag.indexOf(32) >= 0) {
            throw new IllegalArgumentException("flag contains space: " + flag);
        }
    }

    public static boolean hasFlag(InterfaceConfigurationParcel config, String flag) {
        validateFlag(flag);
        Set<String> flagList = new HashSet<>(Arrays.asList(config.flags));
        return flagList.contains(flag);
    }

    protected static String[] removeAndAddFlags(String[] flags, String remove, String add) {
        ArrayList<String> result = new ArrayList<>();
        try {
            validateFlag(add);
            for (String flag : flags) {
                if (!remove.equals(flag) && !add.equals(flag)) {
                    result.add(flag);
                }
            }
            result.add(add);
            return (String[]) result.toArray(new String[result.size()]);
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("Invalid InterfaceConfigurationParcel", iae);
        }
    }

    public static void setInterfaceConfig(INetd netd, InterfaceConfigurationParcel configParcel) {
        try {
            netd.interfaceSetCfg(configParcel);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void setInterfaceUp(INetd netd, String iface) {
        InterfaceConfigurationParcel configParcel = getInterfaceConfigParcel(netd, iface);
        configParcel.flags = removeAndAddFlags(configParcel.flags, INetd.IF_STATE_DOWN, INetd.IF_STATE_UP);
        setInterfaceConfig(netd, configParcel);
    }

    public static void setInterfaceDown(INetd netd, String iface) {
        InterfaceConfigurationParcel configParcel = getInterfaceConfigParcel(netd, iface);
        configParcel.flags = removeAndAddFlags(configParcel.flags, INetd.IF_STATE_UP, INetd.IF_STATE_DOWN);
        setInterfaceConfig(netd, configParcel);
    }

    public static void tetherStart(INetd netd, boolean usingLegacyDnsProxy, String[] dhcpRange) throws RemoteException, ServiceSpecificException {
        TetherConfigParcel config = new TetherConfigParcel();
        config.usingLegacyDnsProxy = usingLegacyDnsProxy;
        config.dhcpRanges = dhcpRange;
        netd.tetherStartWithConfiguration(config);
    }

    public static void tetherInterface(INetd netd, String iface, IpPrefix dest) throws RemoteException, ServiceSpecificException {
        tetherInterface(netd, iface, dest, 20, 50);
    }

    public static void tetherInterface(INetd netd, String iface, IpPrefix dest, int maxAttempts, int pollingIntervalMs) throws RemoteException, ServiceSpecificException {
        netd.tetherInterfaceAdd(iface);
        networkAddInterface(netd, iface, maxAttempts, pollingIntervalMs);
        List<RouteInfo> routes = new ArrayList<>();
        routes.add(new RouteInfo(dest, null, iface, 1));
        addRoutesToLocalNetwork(netd, iface, routes);
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

    public static void addRoutesToLocalNetwork(INetd netd, String iface, List<RouteInfo> routes) {
        for (RouteInfo route : routes) {
            if (!route.isDefaultRoute()) {
                modifyRoute(netd, ModifyOperation.ADD, 99, route);
            }
        }
        modifyRoute(netd, ModifyOperation.ADD, 99, new RouteInfo(new IpPrefix("fe80::/64"), null, iface, 1));
    }

    public static int removeRoutesFromLocalNetwork(INetd netd, List<RouteInfo> routes) {
        int failures = 0;
        for (RouteInfo route : routes) {
            try {
                modifyRoute(netd, ModifyOperation.REMOVE, 99, route);
            } catch (IllegalStateException e) {
                failures++;
            }
        }
        return failures;
    }

    private static String findNextHop(RouteInfo route) {
        switch (route.getType()) {
            case 1:
                if (route.hasGateway()) {
                    String nextHop = route.getGateway().getHostAddress();
                    return nextHop;
                }
                return "";
            case 7:
                return INetd.NEXTHOP_UNREACHABLE;
            case 9:
                return INetd.NEXTHOP_THROW;
            default:
                return "";
        }
    }

    public static void modifyRoute(INetd netd, ModifyOperation op, int netId, RouteInfo route) {
        String ifName = route.getInterface();
        String dst = route.getDestination().toString();
        String nextHop = findNextHop(route);
        try {
            switch (AnonymousClass1.$SwitchMap$com$android$net$module$util$NetdUtils$ModifyOperation[op.ordinal()]) {
                case 1:
                    netd.networkAddRoute(netId, ifName, dst, nextHop);
                    return;
                case 2:
                    netd.networkRemoveRoute(netId, ifName, dst, nextHop);
                    return;
                default:
                    throw new IllegalStateException("Unsupported modify operation:" + op);
            }
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.net.module.util.NetdUtils$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$net$module$util$NetdUtils$ModifyOperation;

        static {
            int[] iArr = new int[ModifyOperation.values().length];
            $SwitchMap$com$android$net$module$util$NetdUtils$ModifyOperation = iArr;
            try {
                iArr[ModifyOperation.ADD.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$net$module$util$NetdUtils$ModifyOperation[ModifyOperation.REMOVE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }
}
