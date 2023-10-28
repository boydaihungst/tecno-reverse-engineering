package com.lucid.propertiesapi;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import com.android.server.pm.PackageManagerService;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidParameterException;
/* loaded from: classes2.dex */
public class PowerXtendControlAPIImpl implements PowerXtendControlAPI {
    public static final String TAG = "LUCID";
    private static String m_filesDir;
    ConfigData mConfigData;
    private Context mContext;
    private PackageManager mPackageManager;
    private String mPsVersion;
    protected boolean mSystemContext;
    protected boolean m_psLoaded;

    public static native String getBatteryLevelTriggersFromXml();

    public static native long getPSVersion();

    public static native int getState();

    public static native String getUserTablePath();

    public PowerXtendControlAPIImpl(Context c) throws IOException, InvalidParameterException, IllegalStateException {
        String psLib;
        this.mPsVersion = null;
        this.mContext = c;
        this.mPackageManager = c.getPackageManager();
        if (c == null) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl - InvalidParameterException context == null");
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        boolean isSystemAppOrService = isSystemAppOrService();
        this.mSystemContext = isSystemAppOrService;
        if (!isSystemAppOrService) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl - IllegalStateException not a system app or a system service");
            IllegalStateException e2 = new IllegalStateException();
            throw e2;
        }
        m_filesDir = null;
        this.m_psLoaded = false;
        String release = Build.VERSION.RELEASE;
        String pXVersion = Utils.getPXVersion();
        this.mPsVersion = pXVersion;
        if (pXVersion == null) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl failed to get pxVersion from PowerXtendCommonConfig.xml");
            IOException e3 = new IOException();
            throw e3;
        }
        if (pXVersion.equals("4.0")) {
            psLib = "PowerStretch";
        } else if (!this.mPsVersion.equals("4.1") && !this.mPsVersion.equals("4.2")) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl psVersion " + this.mPsVersion + " is not supported by this API");
            IOException e4 = new IOException();
            throw e4;
        } else {
            psLib = "PowerStretch_base";
        }
        Log.i("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl Android release = " + release + " psVersion = " + this.mPsVersion + " trying to load " + psLib);
        try {
            System.loadLibrary(psLib);
            this.m_psLoaded = true;
        } catch (UnsatisfiedLinkError e5) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl - UnsatisfiedLinkError at System.loadLibrary");
        }
        if (!this.m_psLoaded) {
            try {
                Log.i("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl default load error! reload Android release = " + release + " psVersion = " + this.mPsVersion + " trying to load PowerStretch.sprd");
                System.loadLibrary("PowerStretch.sprd");
                this.m_psLoaded = true;
            } catch (UnsatisfiedLinkError e6) {
                Log.e("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl - UnsatisfiedLinkError at System.loadLibrary PowerStretch.sprd");
            }
        }
        if (!this.m_psLoaded) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl - Native code library failed to load");
            return;
        }
        try {
            m_filesDir = getUserTablePath();
        } catch (Exception e7) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl - Failed to call getUserTablePath jni function.");
        }
        try {
            this.mConfigData = new ConfigData();
        } catch (IOException e8) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl - IOException - failed to init configuration file");
            throw e8;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetPowerXtendToOff() throws FileNotFoundException, IllegalStateException, IOException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendToOff - IllegalStateException Test application installed");
            IllegalStateException e = new IllegalStateException();
            throw e;
        } else if (!this.m_psLoaded) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendToOff - FileNotFoundException");
            FileNotFoundException e2 = new FileNotFoundException();
            throw e2;
        } else {
            try {
                this.mConfigData.setAutoMode(0);
                this.mConfigData.setParam(0);
            } catch (IOException e3) {
                Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendToOff - IOException");
                throw e3;
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetPowerXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException, IOException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendToAutomaticMode - IllegalStateException Test application installed");
            IllegalStateException e = new IllegalStateException();
            throw e;
        } else if (!this.m_psLoaded) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendToAutomaticMode - FileNotFoundException - powerStretch is not loaded");
            FileNotFoundException e2 = new FileNotFoundException();
            throw e2;
        } else {
            try {
                this.mConfigData.setAutoMode(1);
            } catch (IOException e3) {
                Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendToAutomaticMode - IOException");
                throw e3;
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetPowerXtendToManualMode(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException, IOException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendToManualMode - IllegalStateException Test application installed");
            IllegalStateException e = new IllegalStateException();
            throw e;
        } else if (!this.m_psLoaded) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendToManualMode - FileNotFoundException - powerStretch is not loaded");
            FileNotFoundException e2 = new FileNotFoundException();
            throw e2;
        } else {
            try {
                if (state != 0 && 2 != state && 3 != state && 4 != state) {
                    Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendToManualMode - InvalidParameterException - invalid value for state");
                    InvalidParameterException e3 = new InvalidParameterException();
                    throw e3;
                }
                this.mConfigData.setAutoMode(0);
                this.mConfigData.setParam(state);
            } catch (IOException e4) {
                Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendToManualMode - IOException");
                throw e4;
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetPowerXtendAutoModeTriggers(Triggers triggers) throws InvalidParameterException, FileNotFoundException, IllegalStateException, IOException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendAutoModeTriggers - IllegalStateException Test application installed");
            IllegalStateException e = new IllegalStateException();
            throw e;
        } else if (!this.m_psLoaded) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendAutoModeTriggers - FileNotFoundException - powerStretch is not loaded");
            FileNotFoundException e2 = new FileNotFoundException();
            throw e2;
        } else {
            try {
                if (triggers.normal > 100 || triggers.normal < 0 || triggers.high > 100 || triggers.high < 0 || triggers.high > triggers.normal || triggers.ultra > 100 || triggers.ultra < 0 || triggers.ultra > triggers.high) {
                    Log.e("LUCID", "PowerXtendControlAPIImpl::SetPowerXtendAutoModeTriggers - InvalidParameterException - invalid trigger value");
                    InvalidParameterException e3 = new InvalidParameterException();
                    throw e3;
                }
                this.mConfigData.setTriggers(triggers);
            } catch (IOException ioe) {
                throw ioe;
            } catch (InvalidParameterException e4) {
                throw e4;
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void ResetPowerXtendAutoModeTriggers() throws FileNotFoundException, IllegalStateException, IOException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::ResetPowerXtendAutoModeTriggers - IllegalStateException Test application installed");
            IllegalStateException e = new IllegalStateException();
            throw e;
        } else if (!this.m_psLoaded) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::ResetPowerXtendAutoModeTriggers - FileNotFoundException - powerStretch is not loaded");
            FileNotFoundException e2 = new FileNotFoundException();
            throw e2;
        } else {
            try {
                this.mConfigData.resetTriggers();
            } catch (IOException e3) {
                throw e3;
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public int GetPowerXtendMode() throws IllegalStateException, FileNotFoundException, IOException, InvalidParameterException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendMode - IllegalStateException Test application installed");
            IllegalStateException e = new IllegalStateException();
            throw e;
        } else if (!this.m_psLoaded) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendMode - FileNotFoundException - powerStretch is not loaded");
            FileNotFoundException e2 = new FileNotFoundException();
            throw e2;
        } else {
            try {
                int mode = this.mConfigData.getAutoMode();
                return mode;
            } catch (IOException e3) {
                Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendMode - IOException");
                throw e3;
            } catch (InvalidParameterException e4) {
                Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendMode - InvalidParameterException - invalid value");
                throw e4;
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public int GetPowerXtendCurrentState() throws FileNotFoundException, IllegalStateException, InvalidParameterException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendCurrentState - IllegalStateException Test application installed");
            IllegalStateException e = new IllegalStateException();
            throw e;
        } else if (!this.m_psLoaded) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendCurrentState - FileNotFoundException - powerStretch is not loaded");
            FileNotFoundException e2 = new FileNotFoundException();
            throw e2;
        } else {
            try {
                int state = getState();
                if (state != 0 && state != 2 && state != 3 && state != 4) {
                    Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendMode - InvalidParameterException - invalid state value");
                    InvalidParameterException e3 = new InvalidParameterException();
                    throw e3;
                }
                return state;
            } catch (Exception e4) {
                Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendMode - InvalidParameterException - invalid state value");
                InvalidParameterException ep = new InvalidParameterException();
                throw ep;
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public Triggers GetPowerXtendAutoModeTriggers() throws FileNotFoundException, IllegalStateException, IOException, InvalidParameterException {
        try {
            if (!this.mPsVersion.equals("4.0") && !this.mPsVersion.equals("4.1")) {
                Triggers triggers = GetPowerXtendAutoModeTriggers_42();
                return triggers;
            }
            Triggers triggers2 = GetPowerXtendAutoModeTriggers_pre_42();
            return triggers2;
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e2) {
            throw e2;
        } catch (IllegalStateException e3) {
            throw e3;
        } catch (InvalidParameterException e4) {
            throw e4;
        }
    }

    private Triggers GetPowerXtendAutoModeTriggers_42() throws FileNotFoundException, IllegalStateException, IOException, InvalidParameterException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendAutoModeTriggers - IllegalStateException Test application installed");
            throw new IllegalStateException();
        } else if (!this.m_psLoaded) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendAutoModeTriggers - FileNotFoundException - powerStretch is not loaded");
            throw new FileNotFoundException();
        } else {
            try {
                Triggers triggers = this.mConfigData.getTriggers();
                return triggers;
            } catch (IOException e) {
                Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendAutoModeTriggers - IOException");
                throw e;
            } catch (InvalidParameterException e2) {
                String triggersFromXmls = getBatteryLevelTriggersFromXml();
                if (triggersFromXmls == null) {
                    Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendAutoModeTriggers - InvalidParameterException - invalid value");
                    throw e2;
                }
                String[] elements = triggersFromXmls.replace("normal-", "").replace("high-", "").replace("ultra-", "").split(" ");
                if (elements == null || elements.length < 3 || Integer.parseInt(elements[0]) < 0 || Integer.parseInt(elements[0]) > 100 || Integer.parseInt(elements[1]) < 0 || Integer.parseInt(elements[1]) > 100 || Integer.parseInt(elements[2]) < 0 || Integer.parseInt(elements[2]) > 100) {
                    Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendAutoModeTriggers - InvalidParameterException - invalid value");
                    throw e2;
                }
                Triggers triggers2 = new Triggers(Integer.parseInt(elements[0]), Integer.parseInt(elements[1]), Integer.parseInt(elements[2]));
                return triggers2;
            }
        }
    }

    private Triggers GetPowerXtendAutoModeTriggers_pre_42() throws FileNotFoundException, IllegalStateException, IOException, InvalidParameterException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendAutoModeTriggers - IllegalStateException Test application installed");
            IllegalStateException e = new IllegalStateException();
            throw e;
        } else if (!this.m_psLoaded) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendAutoModeTriggers - FileNotFoundException - powerStretch is not loaded");
            FileNotFoundException e2 = new FileNotFoundException();
            throw e2;
        } else {
            try {
                Triggers triggers = this.mConfigData.getTriggers();
                return triggers;
            } catch (IOException e3) {
                Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendAutoModeTriggers - IOException");
                throw e3;
            } catch (InvalidParameterException e4) {
                Log.e("LUCID", "PowerXtendControlAPIImpl::GetPowerXtendAutoModeTriggers - InvalidParameterException - invalid value");
                throw e4;
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public boolean IsPowerXtendControlPanelInstalled() throws IllegalStateException {
        PackageManager packageManager = this.mPackageManager;
        if (packageManager == null) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::IsPowerXtendControlPanelInstalled - packageManager param is null");
            IllegalStateException e = new IllegalStateException();
            throw e;
        }
        try {
            packageManager.getPackageInfo("com.lucid.power", 1);
            return true;
        } catch (PackageManager.NameNotFoundException e2) {
            return false;
        }
    }

    public static String getConfigPath() {
        return m_filesDir;
    }

    public boolean isSystemAppOrService() {
        PackageManager pm = this.mContext.getPackageManager();
        String packageName = this.mContext.getPackageName();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            Log.e("LUCID", ">>>>>>packages is<<<<<<<<" + ai.publicSourceDir);
            if ((ai.flags & 129) != 0) {
                Log.e("LUCID", ">>>>>>packages is system package" + packageName);
                return true;
            }
            try {
                PackageInfo targetPkgInfo = pm.getPackageInfo(packageName, 64);
                PackageInfo sys = pm.getPackageInfo(PackageManagerService.PLATFORM_PACKAGE_NAME, 64);
                if (targetPkgInfo != null && targetPkgInfo.signatures != null && sys.signatures[0].equals(targetPkgInfo.signatures[0])) {
                    Log.e("LUCID", ">>>>>>packages is system package" + packageName);
                    return true;
                }
                return false;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        } catch (PackageManager.NameNotFoundException e2) {
            Log.e("LUCID", e2.getMessage());
            return false;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
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

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public int GetLogLevel() throws IllegalStateException, IOException, InvalidParameterException {
        try {
            if (!this.mPsVersion.equals("4.0") && !this.mPsVersion.equals("4.1")) {
                int logLevel = GetLogLevel_42();
                return logLevel;
            }
            int logLevel2 = GetLogLevel_pre_42();
            return logLevel2;
        } catch (IOException e) {
            throw e;
        } catch (IllegalStateException e2) {
            throw e2;
        } catch (InvalidParameterException e3) {
            throw e3;
        }
    }

    private int GetLogLevel_42() throws IllegalStateException, IOException, InvalidParameterException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::GetLogLevel - IllegalStateException Test application installed");
            IllegalStateException e = new IllegalStateException();
            throw e;
        }
        try {
            int logLevel = this.mConfigData.getLogLevel();
            if (logLevel < 0 || logLevel > 2) {
                InvalidParameterException ie = new InvalidParameterException();
                throw ie;
            }
            return logLevel;
        } catch (IOException e2) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::GetLogLevel - IOException");
            throw e2;
        }
    }

    private int GetLogLevel_pre_42() throws IllegalStateException, IOException, InvalidParameterException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::GetLogLevel - IllegalStateException Test application installed");
            IllegalStateException e = new IllegalStateException();
            throw e;
        }
        try {
            int logLevel = this.mConfigData.getLogLevel();
            if (logLevel < 0 || logLevel > 1) {
                InvalidParameterException ie = new InvalidParameterException();
                throw ie;
            }
            return logLevel;
        } catch (IOException e2) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::GetLogLevel - IOException");
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetLogLevel(int logLevel) throws IllegalStateException, InvalidParameterException, IOException {
        try {
            if (!this.mPsVersion.equals("4.0") && !this.mPsVersion.equals("4.1")) {
                SetLogLevel_42(logLevel);
                return;
            }
            SetLogLevel_pre_42(logLevel);
        } catch (IOException e) {
            throw e;
        } catch (IllegalStateException e2) {
            throw e2;
        } catch (InvalidParameterException e3) {
            throw e3;
        }
    }

    private void SetLogLevel_42(int logLevel) throws IllegalStateException, InvalidParameterException, IOException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::SetLogLevel - IllegalStateException Test application installed");
            IllegalStateException e = new IllegalStateException();
            throw e;
        } else if (logLevel < 0 || logLevel > 2) {
            InvalidParameterException e2 = new InvalidParameterException();
            throw e2;
        } else {
            try {
                this.mConfigData.setLogLevel(logLevel);
            } catch (IOException e3) {
                throw e3;
            }
        }
    }

    private void SetLogLevel_pre_42(int logLevel) throws IllegalStateException, InvalidParameterException, IOException {
        boolean isTestApplicationInstalled = IsPowerXtendControlPanelInstalled();
        if (isTestApplicationInstalled) {
            Log.e("LUCID", "PowerXtendControlAPIImpl::SetLogLevel - IllegalStateException Test application installed");
            IllegalStateException e = new IllegalStateException();
            throw e;
        } else if (logLevel < 0 || logLevel > 1) {
            InvalidParameterException e2 = new InvalidParameterException();
            throw e2;
        } else {
            try {
                this.mConfigData.setLogLevel(logLevel);
            } catch (IOException e3) {
                throw e3;
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void CleanGlobalConfiguration() throws IOException {
        this.mConfigData.cleanGlobalConfiguration();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetGameXtendToOff() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::SetGameXtendToOff - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetWebXtendToOff() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::SetWebXtendToOff - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetNavXtendToOff() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::SetNavXtendToOff - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetSocialXtendToOff() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::SetSocialXtendToOff - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetGameXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::SetGameXtendToAutomaticMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetWebXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::SetWebXtendToAutomaticMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetNavXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::SetNavXtendToAutomaticMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetSocialXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetGameXtendToManualMode(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::SetGameXtendToManualMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetWebXtendToManualMode(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::SetWebXtendToManualMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetNavXtendToManualMode(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::SetNavXtendToManualMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void SetSocialXtendToManualMode(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::SetSocialXtendToManualMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void GetGameXtendConfig(ModulePropertiesInfo mpi) throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::GetGameXtendConfig - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void GetWebXtendConfig(ModulePropertiesInfo mpi) throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::GetWebXtendConfig - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void GetNavXtendConfig(ModulePropertiesInfo mpi) throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::GetNavXtendConfig - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.PowerXtendControlAPI
    public void GetSocialXtendConfig(ModulePropertiesInfo mpi) throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "PowerXtendControlAPIImpl::GetSocialXtendConfig - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }
}
