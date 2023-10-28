package com.lucid.propertiesapi;

import com.android.server.slice.SliceClientPermissions;
import java.io.File;
/* loaded from: classes2.dex */
public class IRISEngineConfigParams {
    public String mIrisFsHigh;
    public String mIrisFsNormal;
    public String mIrisFsUltra;
    public String mIrisGpHigh;
    public String mIrisGpNormal;
    public String mIrisGpUltra;

    public IRISEngineConfigParams(String param, String fsparam) {
        this.mIrisGpNormal = param;
        this.mIrisGpHigh = param;
        this.mIrisGpUltra = param;
        this.mIrisFsNormal = fsparam;
        this.mIrisFsHigh = fsparam;
        this.mIrisFsUltra = fsparam;
    }

    public IRISEngineConfigParams(String normal, String high, String ultra, String fsnormal, String fshigh, String fsultra) {
        this.mIrisGpNormal = normal;
        this.mIrisGpHigh = high;
        this.mIrisGpUltra = ultra;
        this.mIrisFsNormal = fsnormal;
        this.mIrisFsHigh = fshigh;
        this.mIrisFsUltra = fsultra;
    }

    public String GpToString() {
        String content = this.mIrisGpNormal + "," + this.mIrisGpHigh + "," + this.mIrisGpUltra;
        return content;
    }

    public String FsToString() {
        String content = this.mIrisFsNormal + "," + this.mIrisFsHigh + "," + this.mIrisFsUltra;
        return content;
    }

    public boolean Validate() {
        if (validateFactor(this.mIrisGpNormal) && validateFactor(this.mIrisGpHigh) && validateFactor(this.mIrisGpUltra) && validateFactor(this.mIrisFsNormal) && validateFactor(this.mIrisFsHigh) && validateFactor(this.mIrisFsUltra)) {
            return true;
        }
        return false;
    }

    public boolean validateFactor(String factor) {
        File lucidFile = new File(PowerXtendDynamicTuningAPIImpl.LUCID_FILES_PATH + factor + "." + PowerXtendDynamicTuningAPIImpl.IRIS_FILES_EXTENTION);
        File dtFile = new File(PowerXtendDynamicTuningAPIImpl.getConfigPath() + SliceClientPermissions.SliceAuthority.DELIMITER + factor + "." + PowerXtendDynamicTuningAPIImpl.IRIS_FILES_EXTENTION);
        if (!lucidFile.exists() || lucidFile.isDirectory()) {
            if ((dtFile.exists() && !dtFile.isDirectory()) || factor.equals("0")) {
                return true;
            }
            return false;
        }
        return true;
    }
}
