package com.lucid.propertiesapi;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
/* loaded from: classes2.dex */
public class PowerXtendInternalInterfaceImpl implements PowerXtendInternalInterface {
    public static final String IRIS_FILES_EXTENTION = ".iris";
    public static final String LUCID_FILES_PATH = "/product/etc/lucid/";
    public static final String TAG = "LUCID";
    InternalConfigData mInternalConfigData;
    protected boolean m_psLoaded;

    public static native String getFilePermission(String str);

    public static native long getPSVersion();

    public static native long getXmlsVersion();

    public static native int isLib32OR64();

    public static native boolean validateXmls(String str);

    public PowerXtendInternalInterfaceImpl(Context c) throws IOException, UnsatisfiedLinkError {
        if (c != null) {
            try {
                this.mInternalConfigData = new InternalConfigData(c);
            } catch (IOException e) {
                throw e;
            }
        }
        this.m_psLoaded = false;
        try {
            System.loadLibrary("PowerStretch_base");
            this.m_psLoaded = true;
        } catch (UnsatisfiedLinkError e2) {
            System.err.println("Native code library failed to load.\n" + e2);
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public void SetControlPanelStatus(boolean status) throws IOException {
        try {
            this.mInternalConfigData.SetControlPanelStatus(Boolean.valueOf(status));
        } catch (IOException e) {
            throw e;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public void SetPSEngineOverrideParameter(int param) throws IOException, InvalidParameterException {
        if (param > 4 || param < 0 || param == 1) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            this.mInternalConfigData.SetPSEngineOverrideParameter(param);
        } catch (IOException e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public boolean GetControlPanelStatus() throws InvalidParameterException {
        try {
            boolean param = this.mInternalConfigData.GetControlPanelStatus();
            return param;
        } catch (InvalidParameterException e) {
            throw e;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public int GetPSEngineOverrideParameter() throws InvalidParameterException {
        try {
            int param = this.mInternalConfigData.GetPSEngineOverrideParameter();
            return param;
        } catch (InvalidParameterException e) {
            throw e;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public void SetICEEngineOverrideParameter(int param) throws IOException, InvalidParameterException {
        if (param > 99 || param < 0) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            this.mInternalConfigData.SetICEEngineOverrideParameter(param);
        } catch (InvalidParameterException e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public int GetICEEngineOverrideParameter() throws InvalidParameterException {
        try {
            int param = this.mInternalConfigData.GetICEEngineOverrideParameter();
            if (param > 99 || param < 0) {
                InvalidParameterException e = new InvalidParameterException();
                throw e;
            }
            return param;
        } catch (InvalidParameterException e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public void SetIrisEngineOverrideGPParameter(String param) throws IOException, InvalidParameterException {
        File f = new File("/product/etc/lucid/" + param + IRIS_FILES_EXTENTION);
        if ((!f.exists() || f.isDirectory()) && !param.equals("0")) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            this.mInternalConfigData.SetIrisEngineOverrideGPParameter(param);
        } catch (IOException e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public String GetIrisEngineOverrideGPParameter() throws InvalidParameterException {
        try {
            String param = this.mInternalConfigData.GetIrisEngineOverrideGPParameter();
            File f = new File("/product/etc/lucid/" + param + IRIS_FILES_EXTENTION);
            if ((!f.exists() || f.isDirectory()) && !param.equals("0")) {
                InvalidParameterException e = new InvalidParameterException();
                throw e;
            }
            return param;
        } catch (InvalidParameterException e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public void SetIrisEngineOverrideFSParameter(String param) throws IOException, InvalidParameterException {
        File f = new File("/product/etc/lucid/" + param + IRIS_FILES_EXTENTION);
        if ((!f.exists() || f.isDirectory()) && !param.equals("0")) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            this.mInternalConfigData.SetIrisEngineOverrideFSParameter(param);
        } catch (IOException e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public String GetIrisEngineOverrideFSParameter() throws InvalidParameterException {
        try {
            String param = this.mInternalConfigData.GetIrisEngineOverrideFSParameter();
            File f = new File("/product/etc/lucid/" + param + IRIS_FILES_EXTENTION);
            if ((!f.exists() || f.isDirectory()) && !param.equals("0")) {
                InvalidParameterException e = new InvalidParameterException();
                throw e;
            }
            return param;
        } catch (InvalidParameterException e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public void SetPowerXtendTouchToOn(int psMotion) throws IOException, InvalidParameterException {
        if (psMotion > 4 || psMotion < 0 || psMotion == 1) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            this.mInternalConfigData.SetPowerXtendTouchToOn(psMotion);
        } catch (IOException e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public void SetPowerXtendTouchToOff() throws IOException {
        try {
            this.mInternalConfigData.SetPowerXtendTouchToOff();
        } catch (IOException e) {
            throw e;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public int GetPowerXtendTouchEnabled() throws InvalidParameterException {
        try {
            int enabled = this.mInternalConfigData.GetPowerXtendTouchEnabled();
            if (enabled != 0 && enabled != 1) {
                InvalidParameterException e = new InvalidParameterException();
                throw e;
            }
            return enabled;
        } catch (InvalidParameterException e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public int GetPowerXtendTouchPsParam() throws InvalidParameterException {
        int param = this.mInternalConfigData.GetPowerXtendTouchPsParam();
        if (param > 4 || param < 0 || param == 1) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        return param;
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public void SetExclusionList(String list) throws IOException {
        try {
            this.mInternalConfigData.SetExclusionList(list);
        } catch (IOException e) {
            throw e;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public String GetExclusionList() throws InvalidParameterException {
        String list = this.mInternalConfigData.GetExclusionList();
        return list;
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public void SetLogLevel(int logLevel) throws InvalidParameterException, IOException {
        if (logLevel != 0 && logLevel != 1 && logLevel != 2) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            this.mInternalConfigData.SetLogLevel(logLevel);
        } catch (IOException e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public int GetLogLevel() throws InvalidParameterException {
        try {
            int logLevel = this.mInternalConfigData.GetLogLevel();
            return logLevel;
        } catch (InvalidParameterException e) {
            throw e;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public long GetPowerXtendRevision() throws IllegalStateException, UnsatisfiedLinkError {
        if (!this.m_psLoaded) {
            IllegalStateException e = new IllegalStateException();
            throw e;
        }
        try {
            long ps_version = getPSVersion();
            return ps_version;
        } catch (UnsatisfiedLinkError e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public long GetConfigXmlsRevision() throws IllegalStateException, UnsatisfiedLinkError {
        if (!this.m_psLoaded) {
            IllegalStateException e = new IllegalStateException();
            throw e;
        }
        try {
            long ps_version = getXmlsVersion();
            return ps_version;
        } catch (UnsatisfiedLinkError e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public boolean ValidateXmls(String dirPath) throws IllegalStateException, UnsatisfiedLinkError {
        if (!this.m_psLoaded) {
            IllegalStateException e = new IllegalStateException();
            throw e;
        }
        try {
            boolean valid = validateXmls(dirPath);
            return valid;
        } catch (UnsatisfiedLinkError e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public int getLib32OR64() throws IllegalStateException, UnsatisfiedLinkError {
        if (!this.m_psLoaded) {
            IllegalStateException e = new IllegalStateException();
            throw e;
        }
        try {
            return isLib32OR64();
        } catch (UnsatisfiedLinkError e2) {
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendInternalInterface
    public String getPermission(String filename) throws IllegalStateException, UnsatisfiedLinkError {
        if (!this.m_psLoaded) {
            IllegalStateException e = new IllegalStateException();
            throw e;
        }
        try {
            return getFilePermission(filename);
        } catch (UnsatisfiedLinkError e2) {
            throw e2;
        }
    }
}
