package com.lucid.propertiesapi;

import android.os.Build;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
/* loaded from: classes2.dex */
public class PowerXtendDynamicTuningAPIImpl implements PowerXtendDynamicTuningAPI {
    public static final String IRIS_FILES_EXTENTION = "iris";
    public static final String LUCID_FILES_PATH = "/system/etc/lucid/";
    public static final String LUCID_FILES_PATH_TSSI = "/product/etc/lucid/";
    public static final String TAG = "LUCID";
    private static String m_filesDir;
    private static boolean m_psLoaded;
    private Properties mProp;
    private String mPsVersion;

    public static native String getUserTablePath();

    public static native void initPxCompatibilityDb();

    public static native boolean isPowerXtendApproved(String str);

    public static native boolean isPowerXtendCompatible(String str);

    public PowerXtendDynamicTuningAPIImpl() {
        this.mPsVersion = null;
        m_psLoaded = false;
        m_filesDir = null;
        String psLib = null;
        String release = Build.VERSION.RELEASE;
        String pXVersion = Utils.getPXVersion();
        this.mPsVersion = pXVersion;
        if (pXVersion == null) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::PowerXtendDynamicTuningAPIImpl failed to get pxVersion from PowerXtendCommonConfig.xml");
        }
        if (this.mPsVersion.equals("4.0")) {
            psLib = "PowerStretch";
        } else if (!this.mPsVersion.equals("4.1") && !this.mPsVersion.equals("4.2")) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::PowerXtendDynamicTuningAPIImpl psVersion " + this.mPsVersion + " is not supported by this API");
        } else {
            psLib = "PowerStretch_base";
        }
        Log.i("LUCID", "PowerXtendDynamicTuningAPIImpl::PowerXtendDynamicTuningAPIImpl Android release = " + release + " psVersion = " + this.mPsVersion + " trying to load " + psLib);
        try {
            System.loadLibrary(psLib);
            m_psLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::PowerXtendDynamicTuningAPIImpl - UnsatisfiedLinkError at System.loadLibrary");
        }
        if (!m_psLoaded) {
            try {
                Log.i("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl default load error! reload Android release = " + release + " psVersion = " + this.mPsVersion + " trying to load PowerStretch.sprd");
                System.loadLibrary("PowerStretch.sprd");
                m_psLoaded = true;
            } catch (UnsatisfiedLinkError e2) {
                Log.e("LUCID", "PowerXtendControlAPIImpl::PowerXtendControlAPIImpl - UnsatisfiedLinkError at System.loadLibrary PowerStretch.sprd");
            }
        }
        if (!m_psLoaded) {
            Log.e("LUCID", "Native code library failed to load");
            return;
        }
        try {
            initPxCompatibilityDb();
            m_filesDir = getUserTablePath();
        } catch (Exception e3) {
            Log.e("LUCID", "Failed to call a jni function.");
        }
        if (m_filesDir == null) {
            Log.e("LUCID", "DB folder id NULL Override mechanism is switched off.");
            return;
        }
        File f = new File(m_filesDir);
        if (!f.isDirectory()) {
            File directory = new File(m_filesDir);
            directory.mkdirs();
            boolean rc = directory.setReadable(true, false);
            if (!rc) {
                Log.e("LUCID", "failed to setReadable to " + m_filesDir);
            }
            boolean rc2 = directory.setWritable(true, false);
            if (!rc2) {
                Log.e("LUCID", "failed to setWritable to " + m_filesDir);
            }
            boolean rc3 = directory.setExecutable(true, false);
            if (!rc3) {
                Log.e("LUCID", "failed to setExecutable " + m_filesDir);
            }
        }
        boolean rc4 = f.isDirectory();
        if (!rc4) {
            Log.e("LUCID", "DB folder could not be created Override mechanism is switched off, .");
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public boolean IsPowerXtendCompatible(String packageName) throws InvalidParameterException {
        Boolean.valueOf(false);
        String packageName2 = cleanPackageName(packageName);
        if (!isValidPackageName(packageName2)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::IsPowerXtendCompatible - Invalid package name packageName " + packageName2 + "throwing InvalidParameterException");
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            Boolean rc = Boolean.valueOf(isPowerXtendCompatible(packageName2));
            return rc.booleanValue();
        } catch (UnsatisfiedLinkError e2) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::IsPowerXtendCompatible - UnsatisfiedLinkError - failing to call isPowerXtendCompatible JNI");
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public boolean IsPowerXtendApproved(String packageName) throws InvalidParameterException {
        Boolean.valueOf(false);
        String packageName2 = cleanPackageName(packageName);
        if (!isValidPackageName(packageName2)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::IsPowerXtendApproved - Invalid package name packageName " + packageName2 + "throwing InvalidParameterException");
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            Boolean rc = Boolean.valueOf(isPowerXtendApproved(packageName2));
            return rc.booleanValue();
        } catch (UnsatisfiedLinkError e2) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::IsPowerXtendApproved - UnsatisfiedLinkError - failing to call isPowerXtendApproved JNI");
            throw e2;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public void SetDynamicTuningState(Boolean active) throws IOException {
        try {
            setMasterProperty("ENABLED", active.booleanValue() ? "1" : "0");
        } catch (IOException e) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningState - IOException on writeToFile\n" + e);
            throw e;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public boolean GetDynamicTuningState() throws FileNotFoundException, IOException, IllegalArgumentException {
        Boolean.valueOf(false);
        try {
            String rcVal = getMasterProperty("ENABLED");
            if (rcVal == null) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningState IllegalArgumentException - property does not exist");
                IllegalArgumentException e = new IllegalArgumentException();
                throw e;
            }
            if (!rcVal.equals("0") && !rcVal.equals("1")) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningState IllegalArgumentException - invalid value for property");
                IllegalArgumentException e2 = new IllegalArgumentException();
                throw e2;
            }
            Boolean rc = Boolean.valueOf(rcVal.equals("1"));
            return rc.booleanValue();
        } catch (FileNotFoundException e3) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningState FileNotFoundException");
            throw e3;
        } catch (IOException e4) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningState IOException");
            throw e4;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public void SetDynamicTuningStatePerApp(String packageName, Boolean active) throws InvalidParameterException, IOException {
        String packageName2 = cleanPackageName(packageName);
        if (!isValidPackageName(packageName2)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningStatePerApp - Invalid package name packageName " + packageName2 + "throwing InvalidParameterException");
            throw new InvalidParameterException();
        }
        try {
            setProperty(packageName2, "ENABLED", active.booleanValue() ? "1" : "0");
        } catch (IOException e) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningStatePerApp - IOException on writeToFile\n" + e);
            throw e;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public boolean GetDynamicTuningStatePerApp(String packageName) throws InvalidParameterException, FileNotFoundException, IOException, IllegalArgumentException {
        Boolean.valueOf(false);
        String packageName2 = cleanPackageName(packageName);
        if (!isValidPackageName(packageName2)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningStatePerApp - Invalid package name packageName " + packageName2 + " InvalidParameterException");
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            String rcVal = getProperty(packageName2, "ENABLED");
            if (rcVal == null) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningStatePerApp IllegalArgumentException - property does not exist");
                IllegalArgumentException e2 = new IllegalArgumentException();
                throw e2;
            }
            if (!rcVal.equals("0") && !rcVal.equals("1")) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningStatePerApp IllegalArgumentException - invalid value for property");
                IllegalArgumentException e3 = new IllegalArgumentException();
                throw e3;
            }
            Boolean rc = Boolean.valueOf(rcVal.equals("1"));
            return rc.booleanValue();
        } catch (FileNotFoundException e4) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningStatePerApp FileNotFoundException");
            throw e4;
        } catch (IOException e5) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningStatePerApp IOException");
            throw e5;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public void ResetDynamicTuningParamsPerApp(String packageName) throws InvalidParameterException, IOException {
        FileLock lock = null;
        String packageName2 = cleanPackageName(packageName);
        if (!isValidPackageName(packageName2)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::ResetDynamicTuningParamsPerApp - Invalid package name packageName " + packageName2);
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        File fileToDelete = new File(m_filesDir, packageName2 + ".properties");
        if (!fileToDelete.exists()) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::ResetDynamicTuningParamsPerApp - file to delete does not exist dir = " + m_filesDir + " file = " + packageName2);
            return;
        }
        try {
            FileChannel fileChannel = new RandomAccessFile(fileToDelete, "rw").getChannel();
            lock = fileChannel.lock();
            if (!fileToDelete.delete()) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::ResetDynamicTuningParamsPerApp - File delete operation failed. dir = " + m_filesDir + " file = " + packageName2 + "throwing IOException");
                IOException e2 = new IOException();
                throw e2;
            }
        } finally {
            if (lock != null) {
                lock.release();
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public void SetDynamicTuningPSEngineParamsPerApp(String packageName, PSEngineConfigParams params) throws InvalidParameterException, IOException {
        String packageName2 = cleanPackageName(packageName);
        if (!isValidPackageName(packageName2)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningPSEngineParamsPerApp - InvalidParameterException Invalid package name packageName " + packageName2);
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        } else if (!params.validateParam(params.mValNormal)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningPSEngineParamsPerApp - InvalidParameterException Invalid PS factor , expected 0, 2, 3 or 4, received " + params.mValNormal);
            InvalidParameterException e2 = new InvalidParameterException();
            throw e2;
        } else if (!params.validateParam(params.mValHigh)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningPSEngineParamsPerApp - InvalidParameterException Invalid PS factor , expected 0, 2, 3 or 4, received " + params.mValHigh);
            InvalidParameterException e3 = new InvalidParameterException();
            throw e3;
        } else if (!params.validateParam(params.mValUltra)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningPSEngineParamsPerApp - InvalidParameterException Invalid PS factor , expected 0, 2, 3 or 4, received " + params.mValUltra);
            InvalidParameterException e4 = new InvalidParameterException();
            throw e4;
        } else if (!params.validateParam(params.mPSMotionNormal)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningPSEngineParamsPerApp - InvalidParameterException Invalid PS Motion factor , expected 0, 2, 3 or 4, received " + params.mPSMotionNormal);
            InvalidParameterException e5 = new InvalidParameterException();
            throw e5;
        } else if (!params.validateParam(params.mPSMotionHigh)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningPSEngineParamsPerApp - InvalidParameterException Invalid PS Motion factor , expected 0, 2, 3 or 4, received " + params.mPSMotionHigh);
            InvalidParameterException e6 = new InvalidParameterException();
            throw e6;
        } else if (!params.validateParam(params.mPSMotionUltra)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningPSEngineParamsPerApp - InvalidParameterException Invalid PS Motion factor , expected 0, 2, 3 or 4, received " + params.mPSMotionUltra);
            InvalidParameterException e7 = new InvalidParameterException();
            throw e7;
        } else {
            try {
                setProperty(packageName2, "PS_NORMAL_HIGH_ULTRA", params.PsToString());
                setProperty(packageName2, "TOUCH_ENABLE_NORMAL_HIGH_ULTRA", params.TouchEnableToString());
                setProperty(packageName2, "TOUCH_PS_PARAM_NORMAL_HIGH_ULTRA", params.TouchParamToString());
            } catch (IOException e8) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningPSEngineParamsPerApp IOException on setProperty\n" + e8);
                throw e8;
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public PSEngineConfigParams GetDynamicTuningPSEngineParamsPerApp(String packageName) throws InvalidParameterException, FileNotFoundException, IOException, IllegalArgumentException {
        PSEngineConfigParams params = new PSEngineConfigParams(0, false, 0);
        String packageName2 = cleanPackageName(packageName);
        if (isValidPackageName(packageName2)) {
            try {
                String psVals = getProperty(packageName2, "PS_NORMAL_HIGH_ULTRA");
                String touchEnableVals = getProperty(packageName2, "TOUCH_ENABLE_NORMAL_HIGH_ULTRA");
                String touchPsParamVals = getProperty(packageName2, "TOUCH_PS_PARAM_NORMAL_HIGH_ULTRA");
                if (psVals == null || touchEnableVals == null || touchPsParamVals == null) {
                    Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningPSEngineParamsPerApp - IllegalArgumentException invalid or missing values");
                    IllegalArgumentException e = new IllegalArgumentException();
                    throw e;
                }
                String[] psItems = psVals.split("[,]");
                if (psItems.length != 3) {
                    Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningPSEngineParamsPerApp - IllegalArgumentException inaccurate num of values");
                    IllegalArgumentException e2 = new IllegalArgumentException();
                    throw e2;
                }
                params.mValNormal = Integer.parseInt(psItems[0]);
                params.mValHigh = Integer.parseInt(psItems[1]);
                params.mValUltra = Integer.parseInt(psItems[2]);
                String[] teItems = touchEnableVals.split("[,]");
                if (teItems.length != 3) {
                    Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningPSEngineParamsPerApp - IllegalArgumentException inaccurate num of values");
                    IllegalArgumentException e3 = new IllegalArgumentException();
                    throw e3;
                }
                int i = 0;
                for (int i2 = 3; i < i2; i2 = 3) {
                    if (!teItems[i].equals("0") && !teItems[i].equals("1")) {
                        Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningPSEngineParamsPerApp IllegalArgumentException failed on validation");
                        IllegalArgumentException e4 = new IllegalArgumentException();
                        throw e4;
                    }
                    i++;
                }
                params.mTouchEnabledNormal = teItems[0].equals("1");
                params.mTouchEnabledHigh = teItems[1].equals("1");
                params.mTouchEnabledUltra = teItems[2].equals("1");
                String[] pmItems = touchPsParamVals.split("[,]");
                if (pmItems.length != 3) {
                    Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningPSEngineParamsPerApp - IllegalArgumentException inaccurate num of values");
                    IllegalArgumentException e5 = new IllegalArgumentException();
                    throw e5;
                }
                params.mPSMotionNormal = Integer.parseInt(pmItems[0]);
                params.mPSMotionHigh = Integer.parseInt(pmItems[1]);
                params.mPSMotionUltra = Integer.parseInt(pmItems[2]);
                if (!params.Validate()) {
                    Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningPSEngineParamsPerApp IllegalArgumentException failed on validation");
                    IllegalArgumentException e6 = new IllegalArgumentException();
                    throw e6;
                }
                return params;
            } catch (FileNotFoundException e7) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningPSEngineParamsPerApp FileNotFoundException");
                throw e7;
            } catch (IOException e8) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningPSEngineParamsPerApp IOException");
                throw e8;
            }
        }
        Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningPSEngineParamsPerApp - InvalidParameterException Invalid package name packageName " + packageName2);
        InvalidParameterException e9 = new InvalidParameterException();
        throw e9;
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public void SetDynamicTuningICEEngineParamsPerApp(String packageName, ICEEngineConfigParams params) throws InvalidParameterException, IOException {
        String packageName2 = cleanPackageName(packageName);
        if (!isValidPackageName(packageName2)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningICEEngineParamsPerApp - InvalidParameterException - Invalid package name packageName " + packageName2);
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        } else if (!params.validateFactor(params.mValNormal)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningICEEngineParamsPerApp - InvalidParameterException - Invalid PS factor , expected 0, 2, 3 or 4, received " + params.mValNormal);
            InvalidParameterException e2 = new InvalidParameterException();
            throw e2;
        } else if (!params.validateFactor(params.mValHigh)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningICEEngineParamsPerApp - InvalidParameterException - Invalid PS factor , expected 0, 2, 3 or 4, received " + params.mValHigh);
            InvalidParameterException e3 = new InvalidParameterException();
            throw e3;
        } else if (!params.validateFactor(params.mValUltra)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningICEEngineParamsPerApp - InvalidParameterException - Invalid PS factor , expected 0, 2, 3 or 4, received " + params.mValUltra);
            InvalidParameterException e4 = new InvalidParameterException();
            throw e4;
        } else {
            try {
                setProperty(packageName2, "ICE_NORMAL_HIGH_ULTRA", params.ToString());
            } catch (IOException e5) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningICEEngineParamsPerApp IOException on setProperty\n" + e5);
                throw e5;
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public ICEEngineConfigParams GetDynamicTuningICEEngineParamsPerApp(String packageName) throws InvalidParameterException, FileNotFoundException, IOException, IllegalArgumentException {
        ICEEngineConfigParams params = new ICEEngineConfigParams(0, 0, 0);
        String packageName2 = cleanPackageName(packageName);
        if (!isValidPackageName(packageName2)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningICEEngineParamsPerApp - InvalidParameterException - Invalid package name packageName " + packageName2);
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            String iceVals = getProperty(packageName2, "ICE_NORMAL_HIGH_ULTRA");
            if (iceVals == null) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningICEEngineParamsPerApp - IllegalArgumentException - missing values");
                IllegalArgumentException e2 = new IllegalArgumentException();
                throw e2;
            }
            String[] items = iceVals.split("[,]");
            if (items.length != 3) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningICEEngineParamsPerApp - IllegalArgumentException inaccurate num of values");
                IllegalArgumentException e3 = new IllegalArgumentException();
                throw e3;
            }
            params.mValNormal = Integer.parseInt(items[0]);
            params.mValHigh = Integer.parseInt(items[1]);
            params.mValUltra = Integer.parseInt(items[2]);
            if (!params.Validate()) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningICEEngineParamsPerApp IllegalArgumentException failed on validation");
                IllegalArgumentException e4 = new IllegalArgumentException();
                throw e4;
            }
            return params;
        } catch (FileNotFoundException e5) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningICEEngineParamsPerApp FileNotFoundException");
            throw e5;
        } catch (IOException e6) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningICEEngineParamsPerApp IOException");
            throw e6;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public void SetDynamicTuningIRISEngineParamsPerApp(String packageName, IRISEngineConfigParams params) throws InvalidParameterException, IOException {
        String packageName2 = cleanPackageName(packageName);
        if (!isValidPackageName(packageName2)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningIRISEngineParamsPerApp -InvalidParameterException - Invalid package name packageName " + packageName2);
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        } else if (!params.validateFactor(params.mIrisGpNormal)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningIRISEngineParamsPerApp - InvalidParameterException - Invalid IRIS GP factor , expected iris filename without extention,  received " + params.mIrisGpNormal);
            InvalidParameterException e2 = new InvalidParameterException();
            throw e2;
        } else if (!params.validateFactor(params.mIrisGpHigh)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningIRISEngineParamsPerApp - InvalidParameterException - Invalid IRIS GP factor , expected iris filename without extention, received " + params.mIrisGpHigh);
            InvalidParameterException e3 = new InvalidParameterException();
            throw e3;
        } else if (!params.validateFactor(params.mIrisGpUltra)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningIRISEngineParamsPerApp - InvalidParameterException - Invalid IRIS GP factor , expected iris filename without extention, received " + params.mIrisGpUltra);
            InvalidParameterException e4 = new InvalidParameterException();
            throw e4;
        } else if (!params.validateFactor(params.mIrisFsNormal)) {
            Log.e("LUCID", "SetDynamicTuningIRISEngineParamsPerApp - InvalidParameterException - Invalid IRIS FS factor , expected iris filename without extention, received " + params.mIrisFsNormal);
            InvalidParameterException e5 = new InvalidParameterException();
            throw e5;
        } else if (!params.validateFactor(params.mIrisFsHigh)) {
            Log.e("LUCID", "SetDynamicTuningIRISEngineParamsPerApp - InvalidParameterException - Invalid IRIS FS factor , expected iris filename without extention, received " + params.mIrisFsHigh);
            InvalidParameterException e6 = new InvalidParameterException();
            throw e6;
        } else if (!params.validateFactor(params.mIrisFsUltra)) {
            Log.e("LUCID", "SetDynamicTuningIRISEngineParamsPerApp - InvalidParameterException - Invalid IRIS FS factor , expected iris filename without extention, received " + params.mIrisFsUltra);
            InvalidParameterException e7 = new InvalidParameterException();
            throw e7;
        } else {
            try {
                setProperty(packageName2, "IRIS_GP_NORMAL_HIGH_ULTRA", params.GpToString());
                setProperty(packageName2, "IRIS_FS_NORMAL_HIGH_ULTRA", params.FsToString());
            } catch (IOException e8) {
                Log.e("LUCID", "SetDynamicTuningIRISEngineParamsPerApp - IOException on setProperty\n" + e8);
                throw e8;
            }
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public IRISEngineConfigParams GetDynamicTuningIrisEngineParamsPerApp(String packageName) throws InvalidParameterException, FileNotFoundException, IOException, IllegalArgumentException {
        IRISEngineConfigParams params = new IRISEngineConfigParams("", "");
        String packageName2 = cleanPackageName(packageName);
        if (!isValidPackageName(packageName2)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningIrisEngineParamsPerApp -InvalidParameterException - Invalid package name packageName " + packageName2);
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            String irisGpVals = getProperty(packageName2, "IRIS_GP_NORMAL_HIGH_ULTRA");
            String irisFsVals = getProperty(packageName2, "IRIS_FS_NORMAL_HIGH_ULTRA");
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningIrisEngineParamsPerApp packageName = " + packageName2 + " irisGpVals = " + irisGpVals + " irisFsVals = " + irisFsVals);
            if (irisGpVals == null || irisFsVals == null) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningIrisEngineParamsPerApp -InvalidParameterException - missing values");
                IllegalArgumentException e2 = new IllegalArgumentException();
                throw e2;
            }
            String[] gpItems = irisGpVals.split("[,]");
            if (gpItems.length != 3) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningIrisEngineParamsPerApp - IllegalArgumentException inaccurate num of values pkg = " + packageName2 + " irisGpVals = " + irisGpVals + " irisFsVals = " + irisFsVals);
                IllegalArgumentException e3 = new IllegalArgumentException();
                throw e3;
            }
            params.mIrisGpNormal = gpItems[0];
            params.mIrisGpHigh = gpItems[1];
            params.mIrisGpUltra = gpItems[2];
            String[] fsItems = irisFsVals.split("[,]");
            if (fsItems.length != 3) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningIrisEngineParamsPerApp - IllegalArgumentException inaccurate num of values pkg = " + packageName2 + " irisGpVals = " + irisGpVals + " irisFsVals = " + irisFsVals);
                IllegalArgumentException e4 = new IllegalArgumentException();
                throw e4;
            }
            params.mIrisFsNormal = fsItems[0];
            params.mIrisFsHigh = fsItems[1];
            params.mIrisFsUltra = fsItems[2];
            if (!params.Validate()) {
                Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningIrisEngineParamsPerApp IllegalArgumentException failed on validation");
                IllegalArgumentException e5 = new IllegalArgumentException();
                throw e5;
            }
            return params;
        } catch (FileNotFoundException e6) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningIrisEngineParamsPerApp FileNotFoundException");
            throw e6;
        } catch (IOException e7) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningIrisEngineParamsPerApp IOException");
            throw e7;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public void SetDynamicTuningParams(String packageName, PSEngineConfigParams psParams, ICEEngineConfigParams iceParams, IRISEngineConfigParams irisParams) throws InvalidParameterException, IOException {
        String packageName2 = cleanPackageName(packageName);
        if (!isValidPackageName(packageName2)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::SetDynamicTuningParams - Invalid package name packageName " + packageName2 + " InvalidParameterException");
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            SetDynamicTuningPSEngineParamsPerApp(packageName2, psParams);
            SetDynamicTuningICEEngineParamsPerApp(packageName2, iceParams);
            SetDynamicTuningIRISEngineParamsPerApp(packageName2, irisParams);
        } catch (IOException e2) {
            throw e2;
        } catch (InvalidParameterException e3) {
            throw e3;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public DTConfigParams GetDynamicTuningParamsPerApp(String packageName) throws InvalidParameterException, FileNotFoundException, IOException, IllegalArgumentException {
        DTConfigParams params = new DTConfigParams();
        String packageName2 = cleanPackageName(packageName);
        if (!isValidPackageName(packageName2)) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningParamsPerApp - Invalid package name packageName " + packageName2 + " InvalidParameterException");
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            PSEngineConfigParams psParams = GetDynamicTuningPSEngineParamsPerApp(packageName2);
            ICEEngineConfigParams iceParams = GetDynamicTuningICEEngineParamsPerApp(packageName2);
            IRISEngineConfigParams irisParams = GetDynamicTuningIrisEngineParamsPerApp(packageName2);
            params.mPSEngineConfigParams = psParams;
            params.mICEEngineConfigParams = iceParams;
            params.mIRISEngineConfigParams = irisParams;
            return params;
        } catch (FileNotFoundException e2) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningParamsPerApp FileNotFoundException");
            throw e2;
        } catch (IOException e3) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningParamsPerApp IOException");
            throw e3;
        } catch (InvalidParameterException e4) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningParamsPerApp InvalidParameterException");
            throw e4;
        } catch (IllegalArgumentException e5) {
            Log.e("LUCID", "PowerXtendDynamicTuningAPIImpl::GetDynamicTuningParamsPerApp IllegalArgumentException");
            throw e5;
        }
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public void SetDynamicTuningIrisCurve(String data, String CurveName) throws IOException {
        File file = new File(CurveName);
        file.createNewFile();
        file.setReadable(true, false);
        FileOutputStream fop = new FileOutputStream(file);
        byte[] contentInBytes = data.getBytes();
        fop.write(contentInBytes);
        fop.flush();
        fop.close();
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public String GetAvailableIrisParams() {
        String result = "";
        String LucidParams = getDirIrisFiles(LUCID_FILES_PATH);
        if (LucidParams.equals("")) {
            LucidParams = getDirIrisFiles("/product/etc/lucid/");
        }
        String DTparams = getDirIrisFiles(m_filesDir);
        String params = LucidParams + "," + DTparams;
        params.replace(",,", ",");
        if (params.charAt(0) == ',') {
            params = params.substring(1);
        }
        List<String> list = Arrays.asList(params.split(","));
        Set<String> uniqueWords = new HashSet<>(list);
        for (String word : uniqueWords) {
            result = result + word + ",";
        }
        if (result != null && result.length() > 0 && result.charAt(result.length() - 1) == ',') {
            return result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String getDirIrisFiles(String path) {
        String curves = "";
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null || listOfFiles.length == 0) {
            return "";
        }
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String filename = listOfFiles[i].getName();
                String extention = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
                if (extention.equals(IRIS_FILES_EXTENTION)) {
                    int pos = filename.lastIndexOf(".");
                    curves = curves + filename.substring(0, pos) + ",";
                }
            }
        }
        if (curves != null && curves.length() > 0 && curves.charAt(curves.length() - 1) == ',') {
            return curves.substring(0, curves.length() - 1);
        }
        return curves;
    }

    private void initConfigFile(String packageName) throws IOException {
        try {
            setProperty(packageName, "ENABLED", "0");
            setProperty(packageName, "PS_NORMAL_HIGH_ULTRA", "0,0,0");
            setProperty(packageName, "TOUCH_ENABLE_NORMAL_HIGH_ULTRA", "0,0,0");
            setProperty(packageName, "TOUCH_PS_PARAM_NORMAL_HIGH_ULTRA", "0,0,0");
            setProperty(packageName, "ICE_NORMAL_HIGH_ULTRA", "0,0,0");
            setProperty(packageName, "IRIS_GP_NORMAL_HIGH_ULTRA", "0,0,0");
            setProperty(packageName, "IRIS_FS_NORMAL_HIGH_ULTRA", "0,0,0");
        } catch (IOException e) {
            throw e;
        }
    }

    private void setProperty(String packageName, String key, String value) throws IOException {
        OutputStream output = null;
        try {
            try {
                this.mProp = new Properties();
                File file = new File(m_filesDir, packageName + ".properties");
                if (!file.exists()) {
                    file.createNewFile();
                    file.setReadable(true, false);
                    initConfigFile(packageName);
                }
                InputStream input = new FileInputStream(file.getAbsolutePath());
                this.mProp.load(input);
                input.close();
                output = new FileOutputStream(file.getAbsolutePath());
                this.mProp.setProperty(key, value);
                this.mProp.store(output, (String) null);
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException io) {
                io.printStackTrace();
                throw io;
            }
        } catch (Throwable th) {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            throw th;
        }
    }

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    private String getProperty(String packageName, String key) throws FileNotFoundException, IOException {
        InputStream input = null;
        try {
            try {
                File file = new File(m_filesDir, packageName + ".properties");
                if (!file.exists()) {
                    FileNotFoundException e = new FileNotFoundException();
                    throw e;
                }
                InputStream input2 = new FileInputStream(file.getAbsolutePath());
                this.mProp.load(input2);
                String propertyStr = this.mProp.getProperty(key);
                try {
                    input2.close();
                    return propertyStr;
                } catch (IOException e2) {
                    throw e2;
                }
            } catch (IOException e3) {
                throw e3;
            }
        } catch (Throwable e4) {
            if (0 != 0) {
                try {
                    input.close();
                } catch (IOException e5) {
                    throw e5;
                }
            }
            throw e4;
        }
    }

    private void setMasterProperty(String key, String value) throws IOException {
        OutputStream output = null;
        try {
            try {
                this.mProp = new Properties();
                File file = new File(m_filesDir, "master.properties");
                if (!file.exists()) {
                    file.createNewFile();
                    file.setReadable(true, false);
                }
                InputStream input = new FileInputStream(file.getAbsolutePath());
                this.mProp.load(input);
                input.close();
                output = new FileOutputStream(file.getAbsolutePath());
                this.mProp.setProperty(key, value);
                this.mProp.store(output, (String) null);
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException io) {
                io.printStackTrace();
                throw io;
            }
        } catch (Throwable th) {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            throw th;
        }
    }

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    private String getMasterProperty(String key) throws FileNotFoundException, IOException {
        InputStream input = null;
        try {
            try {
                File file = new File(m_filesDir, "master.properties");
                if (!file.exists()) {
                    FileNotFoundException e = new FileNotFoundException();
                    throw e;
                }
                InputStream input2 = new FileInputStream(file.getAbsolutePath());
                this.mProp.load(input2);
                String propertyStr = this.mProp.getProperty(key);
                try {
                    input2.close();
                    return propertyStr;
                } catch (IOException e2) {
                    throw e2;
                }
            } catch (IOException e3) {
                throw e3;
            }
        } catch (Throwable e4) {
            if (0 != 0) {
                try {
                    input.close();
                } catch (IOException e5) {
                    throw e5;
                }
            }
            throw e4;
        }
    }

    private String cleanPackageName(String packageName) {
        if (packageName == null) {
            return packageName;
        }
        if (packageName.lastIndexOf(":") > -1) {
            return packageName.substring(0, packageName.lastIndexOf(":"));
        }
        return packageName;
    }

    private boolean isValidPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty() || packageName.length() > 1024) {
            return false;
        }
        char[] chars = packageName.toCharArray();
        if (!Character.isJavaIdentifierStart(chars[0])) {
            return false;
        }
        int n = chars.length;
        for (int i = 1; i < n; i++) {
            if (!Character.isJavaIdentifierPart(chars[i]) && '.' != chars[i]) {
                return false;
            }
        }
        return true;
    }

    public static String getConfigPath() {
        return m_filesDir;
    }

    @Override // com.lucid.propertiesapi.PowerXtendDynamicTuningAPI
    public void CleanDTConfiguration() throws IOException {
        File folder = new File(m_filesDir);
        File[] fList = folder.listFiles();
        if (fList == null || fList.length == 0) {
            return;
        }
        for (int i = 0; i < fList.length; i++) {
            String pes = fList[i].getName();
            if (pes.endsWith(PowerXtendInternalInterfaceImpl.IRIS_FILES_EXTENTION) || (pes.endsWith(".properties") && !pes.equals("global.properties"))) {
                File fileDelete = fList[i];
                fileDelete.delete();
            }
        }
    }
}
