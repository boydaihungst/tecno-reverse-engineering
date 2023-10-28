package com.android.server.devicepolicy;

import android.content.Context;
import android.content.pm.VerifierDeviceIdentity;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.security.identity.Util;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.util.Preconditions;
import java.nio.ByteBuffer;
/* loaded from: classes.dex */
class EnterpriseSpecificIdCalculator {
    private static final int ESID_LENGTH = 16;
    private static final int PADDED_ENTERPRISE_ID_LENGTH = 64;
    private static final int PADDED_HW_ID_LENGTH = 16;
    private static final int PADDED_PROFILE_OWNER_LENGTH = 64;
    private final String mImei;
    private final String mMacAddress;
    private final String mMeid;
    private final String mSerialNumber;

    EnterpriseSpecificIdCalculator(String imei, String meid, String serialNumber, String macAddress) {
        this.mImei = imei;
        this.mMeid = meid;
        this.mSerialNumber = serialNumber;
        this.mMacAddress = macAddress;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public EnterpriseSpecificIdCalculator(Context context) {
        TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(TelephonyManager.class);
        Preconditions.checkState(telephonyService != null, "Unable to access telephony service");
        this.mImei = telephonyService.getImei(0);
        this.mMeid = telephonyService.getMeid(0);
        this.mSerialNumber = Build.getSerial();
        WifiManager wifiManager = (WifiManager) context.getSystemService(WifiManager.class);
        Preconditions.checkState(wifiManager != null, "Unable to access WiFi service");
        String[] macAddresses = wifiManager.getFactoryMacAddresses();
        if (macAddresses == null || macAddresses.length == 0) {
            this.mMacAddress = "";
        } else {
            this.mMacAddress = macAddresses[0];
        }
    }

    private static String getPaddedTruncatedString(String input, int maxLength) {
        String paddedValue = String.format("%" + maxLength + "s", input);
        return paddedValue.substring(0, maxLength);
    }

    private static String getPaddedHardwareIdentifier(String hardwareIdentifier) {
        if (hardwareIdentifier == null) {
            hardwareIdentifier = "";
        }
        return getPaddedTruncatedString(hardwareIdentifier, 16);
    }

    String getPaddedImei() {
        return getPaddedHardwareIdentifier(this.mImei);
    }

    String getPaddedMeid() {
        return getPaddedHardwareIdentifier(this.mMeid);
    }

    String getPaddedSerialNumber() {
        return getPaddedHardwareIdentifier(this.mSerialNumber);
    }

    String getPaddedProfileOwnerName(String profileOwnerPackage) {
        return getPaddedTruncatedString(profileOwnerPackage, 64);
    }

    String getPaddedEnterpriseId(String enterpriseId) {
        return getPaddedTruncatedString(enterpriseId, 64);
    }

    public String calculateEnterpriseId(String profileOwnerPackage, String enterpriseIdString) {
        String enterpriseIdString2;
        boolean z = true;
        Preconditions.checkArgument(!TextUtils.isEmpty(profileOwnerPackage), "owner package must be specified.");
        if (enterpriseIdString != null && enterpriseIdString.isEmpty()) {
            z = false;
        }
        Preconditions.checkArgument(z, "enterprise ID must either be null or non-empty.");
        if (enterpriseIdString != null) {
            enterpriseIdString2 = enterpriseIdString;
        } else {
            enterpriseIdString2 = "";
        }
        byte[] serialNumber = getPaddedSerialNumber().getBytes();
        byte[] imei = getPaddedImei().getBytes();
        byte[] meid = getPaddedMeid().getBytes();
        byte[] macAddress = this.mMacAddress.getBytes();
        int totalIdentifiersLength = serialNumber.length + imei.length + meid.length + macAddress.length;
        ByteBuffer fixedIdentifiers = ByteBuffer.allocate(totalIdentifiersLength);
        fixedIdentifiers.put(serialNumber);
        fixedIdentifiers.put(imei);
        fixedIdentifiers.put(meid);
        fixedIdentifiers.put(macAddress);
        byte[] dpcPackage = getPaddedProfileOwnerName(profileOwnerPackage).getBytes();
        byte[] enterpriseId = getPaddedEnterpriseId(enterpriseIdString2).getBytes();
        ByteBuffer info = ByteBuffer.allocate(dpcPackage.length + enterpriseId.length);
        info.put(dpcPackage);
        info.put(enterpriseId);
        byte[] esidBytes = Util.computeHkdf("HMACSHA256", fixedIdentifiers.array(), (byte[]) null, info.array(), 16);
        ByteBuffer esidByteBuffer = ByteBuffer.wrap(esidBytes);
        VerifierDeviceIdentity firstId = new VerifierDeviceIdentity(esidByteBuffer.getLong());
        VerifierDeviceIdentity secondId = new VerifierDeviceIdentity(esidByteBuffer.getLong());
        return firstId.toString() + secondId.toString();
    }
}
