package com.lucid.propertiesapi;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileLock;
import java.security.InvalidParameterException;
import java.util.Properties;
/* loaded from: classes2.dex */
public class InternalConfigData {
    private final String c_file = "internal_global.properties";
    private FileLock lock;
    private Context mContext;
    private String mFilename;
    private InputStream mInput;
    private OutputStream mOutput;
    private Properties mProp;

    public InternalConfigData(Context c) throws IOException {
        init(c);
    }

    public void init(Context c) throws IOException {
        this.mContext = c;
        this.mProp = new Properties();
        this.mOutput = null;
        this.mFilename = this.mContext.getFilesDir().getPath() + "/internal_global.properties";
        File file = new File(this.mFilename);
        try {
            if (!file.exists()) {
                file.createNewFile();
                file.setReadable(true, false);
                initConfigFile();
            }
        } catch (IOException e) {
            throw e;
        }
    }

    private void initConfigFile() throws IOException {
        try {
            setProperty("ON", "0");
            setProperty("TOUCH_ENABLED", "0");
            setProperty("TOUCH_PS_PARAM", "0");
            setProperty("PS_ENGINE", "0");
            setProperty("ICE_ENGINE", "0");
            setProperty("IRIS_GP_ENGINE", "0");
            setProperty("IRIS_FS_ENGINE", "0");
            setProperty("LOG_LEVEL", "0");
            setProperty("EXCLUSION_LIST", "");
        } catch (IOException e) {
            throw e;
        }
    }

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    private void setProperty(String key, String value) throws IOException {
        try {
            try {
                FileInputStream fileInputStream = new FileInputStream(this.mFilename);
                this.mInput = fileInputStream;
                this.mProp.load(fileInputStream);
                this.mInput.close();
                this.mOutput = new FileOutputStream(this.mFilename);
                this.mProp.setProperty(key, value);
                this.mProp.store(this.mOutput, (String) null);
                OutputStream outputStream = this.mOutput;
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
            } catch (IOException io) {
                io.printStackTrace();
                throw io;
            }
        } catch (Throwable e2) {
            OutputStream outputStream2 = this.mOutput;
            if (outputStream2 != null) {
                try {
                    outputStream2.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                    throw e3;
                }
            }
            throw e2;
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IGET]}, finally: {[IGET, INVOKE, MOVE_EXCEPTION, INVOKE, INVOKE, MOVE_EXCEPTION, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [122=4] */
    private String getProperty(String key) {
        this.mInput = null;
        try {
            try {
                FileInputStream fileInputStream = new FileInputStream(this.mFilename);
                this.mInput = fileInputStream;
                this.mProp.load(fileInputStream);
                String property = this.mProp.getProperty(key);
                InputStream inputStream = this.mInput;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return property;
            } catch (IOException ex) {
                ex.printStackTrace();
                InputStream inputStream2 = this.mInput;
                if (inputStream2 != null) {
                    try {
                        inputStream2.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
                return null;
            }
        } catch (Throwable th) {
            InputStream inputStream3 = this.mInput;
            if (inputStream3 != null) {
                try {
                    inputStream3.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            throw th;
        }
    }

    public void SetControlPanelStatus(Boolean status) throws IOException {
        int on = 0;
        if (status.booleanValue()) {
            on = 1;
        }
        try {
            setProperty("ON", Integer.toString(on));
        } catch (IOException e) {
            throw e;
        }
    }

    public boolean GetControlPanelStatus() throws InvalidParameterException {
        String paramStr = getProperty("ON");
        if (paramStr == null) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        int param = Integer.parseInt(paramStr);
        if (param != 0 && param != 1) {
            InvalidParameterException e2 = new InvalidParameterException();
            throw e2;
        }
        boolean rc = param != 0;
        return rc;
    }

    public void SetPSEngineOverrideParameter(int param) throws IOException {
        try {
            setProperty("PS_ENGINE", Integer.toString(param));
        } catch (IOException e) {
            throw e;
        }
    }

    public int GetPSEngineOverrideParameter() throws InvalidParameterException {
        String paramStr = getProperty("PS_ENGINE");
        if (paramStr == null) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        int param = Integer.parseInt(paramStr);
        if (param < 0 || param > 4 || param == 1) {
            InvalidParameterException e2 = new InvalidParameterException();
            throw e2;
        }
        return param;
    }

    public void SetICEEngineOverrideParameter(int param) throws IOException {
        try {
            setProperty("ICE_ENGINE", Integer.toString(param));
        } catch (IOException e) {
            throw e;
        }
    }

    public int GetICEEngineOverrideParameter() throws InvalidParameterException {
        String paramStr = getProperty("ICE_ENGINE");
        if (paramStr == null) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        int param = Integer.parseInt(paramStr);
        if (param < 0 || param > 99) {
            InvalidParameterException e2 = new InvalidParameterException();
            throw e2;
        }
        return param;
    }

    public void SetIrisEngineOverrideGPParameter(String param) throws IOException {
        try {
            setProperty("IRIS_GP_ENGINE", param);
        } catch (IOException e) {
            throw e;
        }
    }

    public String GetIrisEngineOverrideGPParameter() throws InvalidParameterException {
        String paramStr = getProperty("IRIS_GP_ENGINE");
        if (paramStr == null) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        return paramStr;
    }

    public void SetIrisEngineOverrideFSParameter(String param) throws IOException {
        try {
            setProperty("IRIS_FS_ENGINE", param);
        } catch (IOException e) {
            throw e;
        }
    }

    public String GetIrisEngineOverrideFSParameter() throws InvalidParameterException {
        String paramStr = getProperty("IRIS_FS_ENGINE");
        if (paramStr == null) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        File f = new File("/product/etc/lucid/" + paramStr + PowerXtendInternalInterfaceImpl.IRIS_FILES_EXTENTION);
        if (!f.exists() || f.isDirectory() || paramStr.equals("0") || paramStr.equals("No Save")) {
            InvalidParameterException e2 = new InvalidParameterException();
            throw e2;
        }
        return paramStr;
    }

    public void SetPowerXtendTouchToOn(int psMotion) throws IOException {
        try {
            setProperty("TOUCH_ENABLED", "1");
            setProperty("TOUCH_PS_PARAM", Integer.toString(psMotion));
        } catch (IOException e) {
            throw e;
        }
    }

    public void SetPowerXtendTouchToOff() throws IOException {
        try {
            setProperty("TOUCH_ENABLED", "0");
        } catch (IOException e) {
            throw e;
        }
    }

    public int GetPowerXtendTouchEnabled() throws InvalidParameterException {
        String paramStr = getProperty("TOUCH_ENABLED");
        if (paramStr == null) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        int param = Integer.parseInt(paramStr);
        if (param != 0 && param != 1) {
            InvalidParameterException e2 = new InvalidParameterException();
            throw e2;
        }
        return param;
    }

    public int GetPowerXtendTouchPsParam() throws InvalidParameterException {
        String paramStr = getProperty("TOUCH_PS_PARAM");
        if (paramStr == null) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        int param = Integer.parseInt(paramStr);
        if (param < 0 || param > 4 || param == 1) {
            InvalidParameterException e2 = new InvalidParameterException();
            throw e2;
        }
        return param;
    }

    public void SetLogLevel(int logLevel) throws IOException {
        try {
            setProperty("LOG_LEVEL", Integer.toString(logLevel));
        } catch (IOException e) {
            throw e;
        }
    }

    public int GetLogLevel() throws InvalidParameterException {
        String paramStr = getProperty("LOG_LEVEL");
        if (paramStr == null) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        int param = Integer.parseInt(paramStr);
        if (param < 0 || param > 2) {
            InvalidParameterException e2 = new InvalidParameterException();
            throw e2;
        }
        return param;
    }

    public void SetExclusionList(String list) throws IOException {
        try {
            setProperty("EXCLUSION_LIST", list);
        } catch (IOException e) {
            throw e;
        }
    }

    public String GetExclusionList() {
        String list = getProperty("EXCLUSION_LIST");
        return list;
    }
}
