package com.lucid.propertiesapi;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.pm.PackageManagerService;
import com.lucid.propertiesapi.ModulePropertiesInfo;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidParameterException;
/* loaded from: classes2.dex */
public class LucidPropertiesApiImpl implements LucidPropertiesAPI {
    private static final String TAG = "LUCID";
    private Context mContext;
    private PackageManager mPackageManager;
    private PowerXtendControlAPIImpl mPowerXtendControlAPIImpl;
    protected boolean mSystemAppOrService;
    protected boolean m_gameLoaded;
    protected boolean m_navLoaded;
    protected boolean m_psLoaded;
    protected boolean m_socialLoaded;
    protected boolean m_webLoaded;

    public static native int getAXState();

    public static native int getAXVersion();

    public static native int getGXState();

    public static native int getGXVersion();

    public static native int getNXState();

    public static native int getNXVersion();

    public static native int getPSVersion();

    public static native int getWXState();

    public static native int getWXVersion();

    public LucidPropertiesApiImpl(PackageManager pm, Context c) throws IOException {
        this.mPowerXtendControlAPIImpl = null;
        this.mPackageManager = pm;
        this.mContext = c;
        this.m_gameLoaded = false;
        this.m_navLoaded = false;
        this.m_webLoaded = false;
        this.m_socialLoaded = false;
        this.m_psLoaded = false;
        this.mSystemAppOrService = false;
        try {
            System.load("/system/lib64/libPowerStretch.so");
            this.m_psLoaded = true;
        } catch (UnsatisfiedLinkError e) {
        }
        if (!this.m_psLoaded) {
            try {
                System.load("/system/lib/libPowerStretch.so");
                this.m_psLoaded = true;
            } catch (UnsatisfiedLinkError e2) {
            }
        }
        try {
            System.load("/system/lib64/libWebXtend.so");
            this.m_webLoaded = true;
        } catch (UnsatisfiedLinkError e3) {
        }
        if (!this.m_webLoaded) {
            try {
                System.load("/system/lib/libWebXtend.so");
                this.m_webLoaded = true;
            } catch (UnsatisfiedLinkError e4) {
            }
        }
        try {
            System.load("/system/lib64/libGameXtend.so");
            this.m_gameLoaded = true;
        } catch (UnsatisfiedLinkError e5) {
        }
        if (!this.m_gameLoaded) {
            try {
                System.load("/system/lib/libGameXtend.so");
                this.m_gameLoaded = true;
            } catch (UnsatisfiedLinkError e6) {
            }
        }
        try {
            System.load("/system/lib64/libSocialXtend.so");
            this.m_socialLoaded = true;
        } catch (UnsatisfiedLinkError e7) {
        }
        if (!this.m_socialLoaded) {
            try {
                System.load("/system/lib/libSocialXtend.so");
                this.m_socialLoaded = true;
            } catch (UnsatisfiedLinkError e8) {
            }
        }
        try {
            System.load("/system/lib64/libNavXtend.so");
            this.m_navLoaded = true;
        } catch (UnsatisfiedLinkError e9) {
        }
        if (!this.m_navLoaded) {
            try {
                System.load("/system/lib/libNavXtend.so");
                this.m_navLoaded = true;
            } catch (UnsatisfiedLinkError e10) {
            }
        }
        this.mSystemAppOrService = isSystemAppOrService352();
        if (!isSoInstalled("Game")) {
            try {
                PowerXtendControlAPIImpl powerXtendControlAPIImpl = new PowerXtendControlAPIImpl(c);
                this.mPowerXtendControlAPIImpl = powerXtendControlAPIImpl;
                this.mSystemAppOrService = powerXtendControlAPIImpl.isSystemAppOrService();
            } catch (IOException e11) {
                throw e11;
            } catch (IllegalStateException e12) {
                IOException ie = new IOException();
                throw ie;
            } catch (InvalidParameterException e13) {
                IOException ie2 = new IOException();
                throw ie2;
            }
        }
    }

    public boolean isSystemAppOrService352() {
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

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetPowerXtendToOff() throws FileNotFoundException, IllegalStateException {
        if (isSoInstalled("Game")) {
            boolean isTestApplicationInstalled = isInternalTestAppInstalled352();
            if (isTestApplicationInstalled) {
                IllegalStateException e = new IllegalStateException();
                throw e;
            }
            boolean z = this.m_gameLoaded;
            boolean isPowerXtendProductInstalled = z || this.m_webLoaded || this.m_navLoaded || this.m_socialLoaded;
            if (isPowerXtendProductInstalled) {
                if (z) {
                    try {
                        SetGameXtendToOff352();
                    } catch (FileNotFoundException e2) {
                        return;
                    } catch (IllegalStateException e3) {
                        return;
                    }
                }
                if (this.m_webLoaded) {
                    SetWebXtendToOff352();
                }
                if (this.m_navLoaded) {
                    SetNavXtendToOff352();
                }
                if (this.m_socialLoaded) {
                    SetSocialXtendToOff352();
                    return;
                }
                return;
            }
            Log.e("LUCID", "SetPowerXtendToOff isPowerXtendProductInstalled failed");
            FileNotFoundException e4 = new FileNotFoundException();
            throw e4;
        }
        PowerXtendControlAPIImpl powerXtendControlAPIImpl = this.mPowerXtendControlAPIImpl;
        if (powerXtendControlAPIImpl == null) {
            Log.e("LUCID", "PowerXtendControlAPIImpl initiation failed");
            return;
        }
        try {
            powerXtendControlAPIImpl.SetPowerXtendToOff();
        } catch (FileNotFoundException e5) {
            throw e5;
        } catch (IOException e6) {
            FileNotFoundException fe = new FileNotFoundException();
            throw fe;
        } catch (IllegalStateException e7) {
            throw e7;
        }
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetPowerXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException {
        if (isSoInstalled("Game")) {
            boolean isTestApplicationInstalled = isInternalTestAppInstalled352();
            if (isTestApplicationInstalled) {
                IllegalStateException e = new IllegalStateException();
                throw e;
            }
            boolean z = this.m_gameLoaded;
            boolean isPowerXtendProductInstalled = z || this.m_webLoaded || this.m_navLoaded || this.m_socialLoaded;
            if (isPowerXtendProductInstalled) {
                if (z) {
                    try {
                        SetGameXtendToAutomaticMode352();
                    } catch (FileNotFoundException e2) {
                        return;
                    } catch (IllegalStateException e3) {
                        return;
                    }
                }
                if (this.m_webLoaded) {
                    SetWebXtendToAutomaticMode352();
                }
                if (this.m_navLoaded) {
                    SetNavXtendToAutomaticMode352();
                }
                if (this.m_socialLoaded) {
                    SetSocialXtendToAutomaticMode352();
                    return;
                }
                return;
            }
            Log.e("LUCID", "SetPowerXtendToAutomaticMode isPowerXtendProductInstalled failed");
            FileNotFoundException e4 = new FileNotFoundException();
            throw e4;
        }
        PowerXtendControlAPIImpl powerXtendControlAPIImpl = this.mPowerXtendControlAPIImpl;
        if (powerXtendControlAPIImpl == null) {
            Log.e("LUCID", "PowerXtendControlAPIImpl initiation failed");
            return;
        }
        try {
            powerXtendControlAPIImpl.SetPowerXtendToAutomaticMode();
        } catch (FileNotFoundException e5) {
            throw e5;
        } catch (IOException e6) {
            FileNotFoundException fe = new FileNotFoundException();
            throw fe;
        } catch (IllegalStateException e7) {
            throw e7;
        }
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetPowerXtendToManualMode(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        if (isSoInstalled("Game")) {
            boolean isTestApplicationInstalled = isInternalTestAppInstalled352();
            if (isTestApplicationInstalled) {
                IllegalStateException e = new IllegalStateException();
                throw e;
            }
            boolean z = this.m_gameLoaded;
            boolean isPowerXtendProductInstalled = z || this.m_webLoaded || this.m_navLoaded || this.m_socialLoaded;
            if (isPowerXtendProductInstalled) {
                if (z) {
                    try {
                        SetGameXtendToManualMode352(state);
                    } catch (FileNotFoundException e2) {
                        return;
                    } catch (IllegalStateException e3) {
                        return;
                    } catch (InvalidParameterException e4) {
                        return;
                    }
                }
                if (this.m_webLoaded) {
                    SetWebXtendToManualMode352(state);
                }
                if (this.m_navLoaded) {
                    SetNavXtendToManualMode352(state);
                }
                if (this.m_socialLoaded) {
                    SetSocialXtendToManualMode352(state);
                    return;
                }
                return;
            }
            FileNotFoundException e5 = new FileNotFoundException();
            Log.e("LUCID", "SetPowerXtendToManualMode isPowerXtendProductInstalled failed");
            throw e5;
        }
        PowerXtendControlAPIImpl powerXtendControlAPIImpl = this.mPowerXtendControlAPIImpl;
        if (powerXtendControlAPIImpl == null) {
            Log.e("LUCID", "PowerXtendControlAPIImpl initiation failed");
            return;
        }
        try {
            powerXtendControlAPIImpl.SetPowerXtendToManualMode(state);
        } catch (FileNotFoundException e6) {
            throw e6;
        } catch (IOException e7) {
            FileNotFoundException fe = new FileNotFoundException();
            throw fe;
        } catch (IllegalStateException e8) {
            throw e8;
        } catch (InvalidParameterException e9) {
            throw e9;
        }
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public boolean GetPowerXtendConfig(ModulePropertiesInfo mpi) throws FileNotFoundException {
        boolean isPowerXtendProductInstalled = true;
        if (isSoInstalled("Game")) {
            boolean isTestApplicationInstalled = isInternalTestAppInstalled352();
            if (isTestApplicationInstalled) {
                IllegalStateException e = new IllegalStateException();
                throw e;
            }
            if (!this.m_gameLoaded && !this.m_webLoaded && !this.m_navLoaded && !this.m_socialLoaded) {
                isPowerXtendProductInstalled = false;
            }
            boolean areAllEquals = false;
            if (isPowerXtendProductInstalled) {
                try {
                    ModulePropertiesInfo GameMPI = new ModulePropertiesInfo();
                    if (this.m_gameLoaded) {
                        GetGameXtendConfig352(GameMPI);
                    }
                    ModulePropertiesInfo WebMPI = new ModulePropertiesInfo();
                    if (this.m_webLoaded) {
                        GetWebXtendConfig352(WebMPI);
                    }
                    ModulePropertiesInfo NavMPI = new ModulePropertiesInfo();
                    if (this.m_navLoaded) {
                        GetNavXtendConfig352(NavMPI);
                    }
                    ModulePropertiesInfo SocialMPI = new ModulePropertiesInfo();
                    if (this.m_socialLoaded) {
                        GetSocialXtendConfig352(SocialMPI);
                    }
                    ModulePropertiesInfo TotalMPI = new ModulePropertiesInfo();
                    boolean isTotalMPIinited = false;
                    if (this.m_gameLoaded) {
                        TotalMPI.setMode(GameMPI.getMode());
                        TotalMPI.setState(GameMPI.getState());
                        isTotalMPIinited = true;
                        areAllEquals = true;
                    }
                    if (this.m_webLoaded) {
                        if (isTotalMPIinited) {
                            if (TotalMPI.getModeValue() != WebMPI.getModeValue() || TotalMPI.getStateValue() != WebMPI.getStateValue()) {
                                return false;
                            }
                        } else {
                            TotalMPI.setMode(WebMPI.getMode());
                            TotalMPI.setState(WebMPI.getState());
                            isTotalMPIinited = true;
                            areAllEquals = true;
                        }
                    }
                    if (this.m_navLoaded) {
                        if (isTotalMPIinited) {
                            if (TotalMPI.getModeValue() != NavMPI.getModeValue() || TotalMPI.getStateValue() != NavMPI.getStateValue()) {
                                return false;
                            }
                        } else {
                            TotalMPI.setMode(NavMPI.getMode());
                            TotalMPI.setState(NavMPI.getState());
                            isTotalMPIinited = true;
                            areAllEquals = true;
                        }
                    }
                    if (this.m_socialLoaded) {
                        if (isTotalMPIinited) {
                            if (TotalMPI.getModeValue() != SocialMPI.getModeValue() || TotalMPI.getStateValue() != SocialMPI.getStateValue()) {
                                return false;
                            }
                        } else {
                            TotalMPI.setMode(SocialMPI.getMode());
                            TotalMPI.setState(SocialMPI.getState());
                            isTotalMPIinited = true;
                            areAllEquals = true;
                        }
                    }
                    if (isTotalMPIinited && areAllEquals) {
                        mpi.setMode(TotalMPI.getMode());
                        mpi.setState(TotalMPI.getState());
                    }
                } catch (FileNotFoundException e2) {
                } catch (IllegalStateException e3) {
                }
                return areAllEquals;
            }
            FileNotFoundException e4 = new FileNotFoundException();
            Log.e("LUCID", "GetPowerXtendConfig modules are not installed");
            throw e4;
        }
        try {
            int mode = this.mPowerXtendControlAPIImpl.GetPowerXtendMode();
            try {
                int state = this.mPowerXtendControlAPIImpl.GetPowerXtendCurrentState();
                if (mode == 0) {
                    mpi.setMode(ModulePropertiesInfo.Mode.MANUAL_MODE);
                } else {
                    mpi.setMode(ModulePropertiesInfo.Mode.AUTOMATIC_MODE);
                }
                if (state == 0) {
                    mpi.setState(ModulePropertiesInfo.State.NO_SAVE);
                } else if (2 == state) {
                    mpi.setState(ModulePropertiesInfo.State.NORMAL_SAVE);
                } else if (3 == state) {
                    mpi.setState(ModulePropertiesInfo.State.HIGH_SAVE);
                } else {
                    mpi.setState(ModulePropertiesInfo.State.ULTRA_SAVE);
                }
                return true;
            } catch (IOException e5) {
                FileNotFoundException fe = new FileNotFoundException();
                throw fe;
            } catch (IllegalStateException e6) {
                FileNotFoundException fe2 = new FileNotFoundException();
                throw fe2;
            } catch (InvalidParameterException e7) {
                FileNotFoundException fe3 = new FileNotFoundException();
                throw fe3;
            }
        } catch (IOException e8) {
            FileNotFoundException fe4 = new FileNotFoundException();
            throw fe4;
        } catch (IllegalStateException e9) {
            FileNotFoundException fe5 = new FileNotFoundException();
            throw fe5;
        } catch (InvalidParameterException e10) {
            FileNotFoundException fe6 = new FileNotFoundException();
            throw fe6;
        }
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetGameXtendToOff() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::SetGameXtendToOff - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetWebXtendToOff() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::SetWebXtendToOff - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetNavXtendToOff() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::SetNavXtendToOff - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetSocialXtendToOff() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::SetSocialXtendToOff - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetGameXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::SetGameXtendToAutomaticMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetWebXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::SetWebXtendToAutomaticMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetNavXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::SetNavXtendToAutomaticMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetSocialXtendToAutomaticMode() throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::SetSocialXtendToAutomaticMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetGameXtendToManualMode(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::SetGameXtendToManualMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetWebXtendToManualMode(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::SetWebXtendToManualMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetNavXtendToManualMode(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::SetNavXtendToManualMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void SetSocialXtendToManualMode(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::SetSocialXtendToManualMode - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void GetGameXtendConfig(ModulePropertiesInfo mpi) throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::GetGameXtendConfig - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void GetWebXtendConfig(ModulePropertiesInfo mpi) throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::GetWebXtendConfig - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void GetNavXtendConfig(ModulePropertiesInfo mpi) throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::GetNavXtendConfig - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public void GetSocialXtendConfig(ModulePropertiesInfo mpi) throws FileNotFoundException, IllegalStateException {
        Log.e("LUCID", "LucidPropertiesApiImpl::GetSocialXtendConfig - this function is not supported anymore");
        throw new UnsupportedOperationException();
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public boolean isInternalTestAppInstalled() {
        if (isSoInstalled("Game")) {
            return isInternalTestAppInstalled352();
        }
        PowerXtendControlAPIImpl powerXtendControlAPIImpl = this.mPowerXtendControlAPIImpl;
        if (powerXtendControlAPIImpl == null) {
            Log.e("LUCID", "PowerXtendControlAPIImpl initiation failed");
            return false;
        }
        try {
            boolean rc = powerXtendControlAPIImpl.IsPowerXtendControlPanelInstalled();
            return rc;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public boolean isInternalTestAppInstalled352() {
        Context context = this.mContext;
        if (context == null && this.mPackageManager == null) {
            return false;
        }
        String packageName = context.getPackageName();
        if (packageName.contains("com.lucid.power")) {
            return false;
        }
        try {
            this.mPackageManager.getPackageInfo("com.lucid.power", 1);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public int getState(String module) {
        int state = -1;
        try {
            if (this.m_gameLoaded && module.equals("Game")) {
                state = getGXState();
            } else if (this.m_navLoaded && module.equals("Nav")) {
                state = getNXState();
            } else if (this.m_webLoaded && module.equals("Web")) {
                state = getWXState();
            } else if (this.m_socialLoaded && module.equals("Social")) {
                state = getAXState();
            } else {
                InvalidParameterException e = new InvalidParameterException();
                throw e;
            }
        } catch (InvalidParameterException e2) {
            System.err.println(e2);
        } catch (Exception e3) {
            System.err.println(e3);
        }
        return state;
    }

    @Override // com.lucid.propertiesapi.LucidPropertiesAPI
    public long GetPowerXtendRevision() throws IllegalStateException, UnsatisfiedLinkError {
        if (isSoInstalled("Game")) {
            try {
                long revision = GetPowerXtendRevision352();
                return revision;
            } catch (IllegalStateException e) {
                throw e;
            } catch (UnsatisfiedLinkError e2) {
                throw e2;
            }
        }
        PowerXtendControlAPIImpl powerXtendControlAPIImpl = this.mPowerXtendControlAPIImpl;
        if (powerXtendControlAPIImpl == null) {
            Log.e("LUCID", "PowerXtendControlAPIImpl initiation failed");
            return 0L;
        }
        try {
            long revision2 = powerXtendControlAPIImpl.GetPowerXtendRevision();
            return revision2;
        } catch (IllegalStateException e3) {
            throw e3;
        } catch (UnsatisfiedLinkError e4) {
            throw e4;
        }
    }

    public boolean isSoInstalled(String module) {
        return module.equals("PowerStretch") ? this.m_psLoaded : module.equals("Game") ? this.m_gameLoaded : module.equals("Nav") ? this.m_navLoaded : module.equals("Web") ? this.m_webLoaded : module.equals("Social") && this.m_socialLoaded;
    }

    protected void setParam(String name, int param) {
        String propName = "sys.lucid." + name + "Xtend.state";
        SystemProperties.set(propName, Integer.toString(param));
    }

    protected void setMode(String name, ModulePropertiesInfo.Mode mode) {
        String propName = "sys.lucid." + name + "Xtend.automode";
        int propValue = 0;
        if (mode == ModulePropertiesInfo.Mode.MANUAL_MODE) {
            propValue = 0;
        } else if (mode == ModulePropertiesInfo.Mode.AUTOMATIC_MODE) {
            propValue = 1;
        }
        SystemProperties.set(propName, Integer.toString(propValue));
    }

    private void SetProductToAutomaticMode(String name) throws FileNotFoundException, IllegalStateException {
        boolean isTestApplicationInstalled = isInternalTestAppInstalled352();
        if (isTestApplicationInstalled) {
            IllegalStateException e = new IllegalStateException();
            throw e;
        }
        boolean isInstalled = isSoInstalled(name);
        if (isInstalled) {
            if (this.mSystemAppOrService) {
                try {
                    setMode(name, ModulePropertiesInfo.Mode.AUTOMATIC_MODE);
                    return;
                } catch (IllegalStateException e2) {
                    Log.e("LUCID", "SetProductToAutomaticMode IllegalStateException");
                    return;
                }
            }
            return;
        }
        FileNotFoundException e3 = new FileNotFoundException();
        Log.e("LUCID", "SetProductToAutomaticMode FileNotFoundException");
        throw e3;
    }

    private void SetModuleXtendToOff(String name) throws FileNotFoundException, IllegalStateException {
        boolean isTestApplicationInstalled = isInternalTestAppInstalled352();
        if (isTestApplicationInstalled) {
            IllegalStateException e = new IllegalStateException();
            throw e;
        }
        boolean isInstalled = isSoInstalled(name);
        if (isInstalled) {
            try {
                setMode(name, ModulePropertiesInfo.Mode.MANUAL_MODE);
                setParam(name, 0);
                return;
            } catch (IllegalStateException e2) {
                Log.e("LUCID", "SetModuleXtendToOff IllegalStateException");
                return;
            }
        }
        FileNotFoundException e3 = new FileNotFoundException();
        Log.e("LUCID", "SetModuleXtendToOff FileNotFoundException");
        throw e3;
    }

    private void SetModuleToManualMode(String name, int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        boolean isTestApplicationInstalled = isInternalTestAppInstalled352();
        if (isTestApplicationInstalled) {
            IllegalStateException e = new IllegalStateException();
            throw e;
        }
        boolean isInstalled = isSoInstalled(name);
        if (isInstalled) {
            if (state != 0 && 2 != state && 3 != state && 4 != state) {
                InvalidParameterException e2 = new InvalidParameterException();
                throw e2;
            }
            setMode(name, ModulePropertiesInfo.Mode.MANUAL_MODE);
            setParam(name, state);
            return;
        }
        FileNotFoundException e3 = new FileNotFoundException();
        Log.e("LUCID", "SetModuleToManualMode FileNotFoundException");
        throw e3;
    }

    private void GetModuleConfig(ModulePropertiesInfo mpi, String name) throws FileNotFoundException, IllegalStateException {
        boolean isInstalled = isSoInstalled(name);
        if (isInstalled) {
            ModulePropertiesInfo.Mode mode = ModulePropertiesInfo.Mode.MANUAL_MODE;
            ModulePropertiesInfo.Mode AutoMode = getMode(name);
            int param = getState(name);
            if (AutoMode == ModulePropertiesInfo.Mode.OVERRIDE_MODE) {
                mpi.setMode(ModulePropertiesInfo.Mode.OVERRIDE_MODE);
            } else if (AutoMode == ModulePropertiesInfo.Mode.AUTOMATIC_MODE) {
                mpi.setMode(ModulePropertiesInfo.Mode.AUTOMATIC_MODE);
            } else {
                mpi.setMode(ModulePropertiesInfo.Mode.MANUAL_MODE);
            }
            switch (param) {
                case 0:
                    mpi.setState(ModulePropertiesInfo.State.NO_SAVE);
                    return;
                case 1:
                default:
                    return;
                case 2:
                    mpi.setState(ModulePropertiesInfo.State.NORMAL_SAVE);
                    return;
                case 3:
                    mpi.setState(ModulePropertiesInfo.State.HIGH_SAVE);
                    return;
                case 4:
                    mpi.setState(ModulePropertiesInfo.State.ULTRA_SAVE);
                    return;
            }
        }
        FileNotFoundException e = new FileNotFoundException();
        throw e;
    }

    private ModulePropertiesInfo.Mode getMode(String name) {
        String propName = "sys.lucid." + name + "Xtend.automode";
        String valStr = SystemProperties.get(propName, "0");
        if (valStr.equals("1")) {
            return ModulePropertiesInfo.Mode.AUTOMATIC_MODE;
        }
        if (valStr.equals("2")) {
            return ModulePropertiesInfo.Mode.OVERRIDE_MODE;
        }
        return ModulePropertiesInfo.Mode.MANUAL_MODE;
    }

    public void SetGameXtendToOff352() throws FileNotFoundException, IllegalStateException {
        SetModuleXtendToOff("Game");
    }

    public void SetWebXtendToOff352() throws FileNotFoundException, IllegalStateException {
        SetModuleXtendToOff("Web");
    }

    public void SetNavXtendToOff352() throws FileNotFoundException, IllegalStateException {
        SetModuleXtendToOff("Nav");
    }

    public void SetSocialXtendToOff352() throws FileNotFoundException, IllegalStateException {
        SetModuleXtendToOff("Social");
    }

    public void SetGameXtendToAutomaticMode352() throws FileNotFoundException, IllegalStateException {
        SetProductToAutomaticMode("Game");
    }

    public void SetWebXtendToAutomaticMode352() throws FileNotFoundException, IllegalStateException {
        SetProductToAutomaticMode("Web");
    }

    public void SetNavXtendToAutomaticMode352() throws FileNotFoundException, IllegalStateException {
        SetProductToAutomaticMode("Nav");
    }

    public void SetSocialXtendToAutomaticMode352() throws FileNotFoundException, IllegalStateException {
        SetProductToAutomaticMode("Social");
    }

    public void SetGameXtendToManualMode352(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        SetModuleToManualMode("Game", state);
    }

    public void SetWebXtendToManualMode352(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        SetModuleToManualMode("Web", state);
    }

    public void SetNavXtendToManualMode352(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        SetModuleToManualMode("Nav", state);
    }

    public void SetSocialXtendToManualMode352(int state) throws FileNotFoundException, InvalidParameterException, IllegalStateException {
        SetModuleToManualMode("Social", state);
    }

    public void GetGameXtendConfig352(ModulePropertiesInfo mpi) throws FileNotFoundException, IllegalStateException {
        GetModuleConfig(mpi, "Game");
    }

    public void GetWebXtendConfig352(ModulePropertiesInfo mpi) throws FileNotFoundException, IllegalStateException {
        GetModuleConfig(mpi, "Web");
    }

    public void GetNavXtendConfig352(ModulePropertiesInfo mpi) throws FileNotFoundException, IllegalStateException {
        GetModuleConfig(mpi, "Nav");
    }

    public void GetSocialXtendConfig352(ModulePropertiesInfo mpi) throws FileNotFoundException, IllegalStateException {
        GetModuleConfig(mpi, "Social");
    }

    public long GetPowerXtendRevision352() throws IllegalStateException, UnsatisfiedLinkError {
        long g_version = -1;
        long w_version = -1;
        long n_version = -1;
        if (!this.m_psLoaded) {
            IllegalStateException e = new IllegalStateException();
            throw e;
        }
        try {
            long ps_version = getPSVersion();
            long version = ps_version;
            if (this.m_gameLoaded) {
                try {
                    g_version = getGXVersion();
                    version = g_version;
                    if (g_version != -1 && ps_version != g_version) {
                        IllegalStateException e2 = new IllegalStateException();
                        throw e2;
                    }
                } catch (UnsatisfiedLinkError e3) {
                    throw e3;
                }
            }
            if (this.m_navLoaded) {
                try {
                    n_version = getNXVersion();
                    version = n_version;
                    if ((g_version != -1 && n_version != g_version) || n_version != ps_version) {
                        IllegalStateException e4 = new IllegalStateException();
                        throw e4;
                    }
                } catch (UnsatisfiedLinkError e5) {
                    throw e5;
                }
            }
            if (this.m_webLoaded) {
                try {
                    w_version = getWXVersion();
                    version = w_version;
                    if ((g_version != -1 && w_version != g_version) || ((n_version != -1 && w_version != n_version) || w_version != ps_version)) {
                        IllegalStateException e6 = new IllegalStateException();
                        throw e6;
                    }
                } catch (UnsatisfiedLinkError e7) {
                    throw e7;
                }
            }
            if (this.m_socialLoaded) {
                try {
                    long s_version = getAXVersion();
                    version = s_version;
                    if ((g_version != -1 && s_version != g_version) || ((n_version != -1 && s_version != n_version) || ((w_version != -1 && s_version != w_version) || s_version != ps_version))) {
                        IllegalStateException e8 = new IllegalStateException();
                        throw e8;
                    }
                } catch (UnsatisfiedLinkError e9) {
                    throw e9;
                }
            }
            return version;
        } catch (UnsatisfiedLinkError e10) {
            throw e10;
        }
    }
}
