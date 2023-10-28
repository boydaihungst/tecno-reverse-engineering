package com.android.server;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IAdaptiveSmartNetworkService;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.android.server.UiModeManagerService;
import com.android.server.location.gnss.hal.GnssNative;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
/* loaded from: classes.dex */
public class AdaptiveSmartNetworkService extends IAdaptiveSmartNetworkService.Stub {
    private static final int NETWORK_CLASS_2_G = 1;
    private static final int NETWORK_CLASS_3_G = 2;
    private static final int NETWORK_CLASS_4_G = 3;
    private static final int NETWORK_CLASS_UNAVAILABLE = -1;
    private static final int NETWORK_CLASS_UNKNOWN = 0;
    private static final int NETWORK_CLASS_WIFI = -101;
    private static final int NETWORK_TYPE_UNAVAILABLE = -1;
    private static final int NETWORK_TYPE_WIFI = -101;
    private static final String TAG = "MWN";
    private Context mContext;
    private TelephonyManager mTelephonyManager;
    private boolean mSimExist = false;
    private boolean mFlightMode = false;
    private final String FILE_NODE_PATH = "/sys/power/pnpmgr/mwn";
    private final String FILE_NODE_GKI_PATH = "/sys/pnpmgr/mwn";
    private String node_value_old = "1";
    private final String ASN_ON = "3";
    private final String ASN_OFF = "0";
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() { // from class: com.android.server.AdaptiveSmartNetworkService.1
        @Override // android.telephony.PhoneStateListener
        public void onServiceStateChanged(ServiceState serviceState) {
            Slog.i(AdaptiveSmartNetworkService.TAG, "onServiceStateChanged...");
            AdaptiveSmartNetworkService.this.updateTelephonyState();
        }

        @Override // android.telephony.PhoneStateListener
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            Slog.i(AdaptiveSmartNetworkService.TAG, "onSignalStrengthsChanged...");
            AdaptiveSmartNetworkService.this.mSignalStrength = signalStrength;
            AdaptiveSmartNetworkService.this.updateTelephonyState();
        }
    };
    private SignalStrength mSignalStrength = new SignalStrength();

    static native int writeASNNode(byte b);

    public AdaptiveSmartNetworkService(Context context) {
        this.mContext = context;
    }

    public void startListener() {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mTelephonyManager = telephonyManager;
        telephonyManager.listen(this.mPhoneStateListener, 257);
    }

    public void asnServiceStart() {
        Slog.v(TAG, "asn service started");
    }

    public boolean isWifiAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == 1;
    }

    private boolean isAirplaneModeOn() {
        boolean ret = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
        return ret;
    }

    private int getNetworkClassByType(int networkType) {
        switch (networkType) {
            case GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_EXISTS /* -101 */:
                return GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_EXISTS;
            case -1:
                return -1;
            case 1:
            case 2:
            case 4:
            case 7:
            case 11:
            case 16:
                return 1;
            case 3:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 12:
            case 14:
            case 15:
            case 17:
                return 2;
            case 13:
            case 19:
                return 3;
            default:
                return 0;
        }
    }

    private int getNetworkClass() {
        int networkType = 0;
        try {
            if (this.mTelephonyManager.getSimState() == 5) {
                this.mSimExist = true;
            } else {
                this.mSimExist = false;
            }
            this.mFlightMode = isAirplaneModeOn();
            NetworkInfo network = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getActiveNetworkInfo();
            if (network != null && network.isAvailable() && network.isConnected()) {
                int type = network.getType();
                Slog.i(TAG, "network type = " + type);
                if (type == 1) {
                    networkType = GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_EXISTS;
                } else if (type == 0) {
                    networkType = this.mTelephonyManager.getNetworkType();
                    Slog.i(TAG, "mobile type = " + networkType);
                }
            } else {
                networkType = -1;
                Slog.i(TAG, "network type was unavailable.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return getNetworkClassByType(networkType);
    }

    public boolean isAsnOn() {
        Slog.i(TAG, "MWN was on.");
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTelephonyState() {
        int networkClass = getNetworkClass();
        String type = UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        String node_value = "0";
        boolean is_network_available = this.mSimExist && !this.mFlightMode;
        switch (networkClass) {
            case GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_ID_EXISTS /* -101 */:
                type = "Wi-Fi";
                break;
            case -1:
            case 0:
                type = UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
                if (is_network_available) {
                    node_value = "3";
                    break;
                }
                break;
            case 1:
                type = "2G";
                if (is_network_available) {
                    node_value = "3";
                    break;
                }
                break;
            case 2:
                type = "3G";
                if (is_network_available) {
                    int dataSignal = this.mSignalStrength.getLevel();
                    if (dataSignal <= 2) {
                        node_value = "3";
                    }
                    Slog.i(TAG, "mSignalStrength.getLevel = " + dataSignal);
                    break;
                }
                break;
            case 3:
                type = "4G";
                break;
        }
        Slog.i(TAG, "Write " + node_value + " to node. type=" + type + " mSimExist=" + this.mSimExist + " mFlightMode=" + this.mFlightMode);
        writeToAsnNode(node_value);
    }

    public int writeToAsnNode(String value) {
        try {
            File f = new File("/sys/power/pnpmgr/mwn");
            if (!f.exists()) {
                f = new File("/sys/pnpmgr/mwn");
            }
            FileOutputStream fos = new FileOutputStream(f);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write(value);
            bw.close();
            this.node_value_old = value;
        } catch (Exception e) {
            Slog.i(TAG, "write the node fail.", e);
        }
        Slog.i(TAG, "write the value:" + value + " to node successfully.");
        return 1;
    }
}
