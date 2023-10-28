package com.lucid.propertiesapi;

import android.util.Log;
import com.android.server.slice.SliceClientPermissions;
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
public class ConfigData {
    private static final String mPropFileName = "global.properties";
    private FileLock lock;
    private String mConfigFile = "";
    private InputStream mInput;
    private OutputStream mOutput;
    private Properties mProp;

    public ConfigData() throws IOException {
        try {
            init();
        } catch (IOException e) {
            throw e;
        }
    }

    public void init() throws IOException {
        this.mProp = new Properties();
        this.mOutput = null;
        String configDir = PowerXtendControlAPIImpl.getConfigPath();
        this.mConfigFile = configDir + SliceClientPermissions.SliceAuthority.DELIMITER + mPropFileName;
        if (configDir != null) {
            File f = new File(configDir);
            if (!f.isDirectory()) {
                File directory = new File(configDir);
                directory.mkdirs();
                boolean rc1 = directory.setReadable(true, false);
                boolean rc2 = directory.setWritable(true, false);
                boolean rc3 = directory.setExecutable(true, false);
                if (!rc1) {
                    Log.e("LUCID", "failed to setReadable to " + configDir);
                }
                if (!rc2) {
                    Log.e("LUCID", "failed to setWritable to " + configDir);
                }
                if (!rc3) {
                    Log.e("LUCID", "failed to setExecutable " + configDir);
                }
            }
            if (!f.isDirectory()) {
                Log.e("LUCID", "DB folder could not be created Override mechanism is switched off, .");
            }
        } else {
            Log.e("LUCID", "DB folder id NULL Override mechanism is switched off.");
        }
        File file = new File(this.mConfigFile);
        if (!file.exists()) {
            file.createNewFile();
            file.setReadable(true, false);
            boolean rc = file.setWritable(true, false);
            if (rc) {
                Log.e("LUCID", "set writable success  " + this.mConfigFile);
            } else {
                Log.e("LUCID", "set writable fail  " + this.mConfigFile);
            }
            try {
                initConfigFile();
                return;
            } catch (IOException e) {
                throw e;
            }
        }
        Log.e("LUCID", "ConfigData init - config file exists " + this.mConfigFile);
    }

    private void initConfigFile() throws IOException {
        try {
            setProperty("MODE", "0");
            setProperty("STATE", "0");
            setProperty("LOG_LEVEL", "0");
            setProperty("BATTERY_LEVEL_TRIGGERS", "-1,-1,-1");
        } catch (IOException e) {
            throw e;
        }
    }

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    private void setProperty(String key, String value) throws IOException {
        try {
            try {
                FileInputStream fileInputStream = new FileInputStream(this.mConfigFile);
                this.mInput = fileInputStream;
                this.mProp.load(fileInputStream);
                this.mInput.close();
                this.mOutput = new FileOutputStream(this.mConfigFile);
                this.mProp.setProperty(key, value);
                this.mProp.store(this.mOutput, (String) null);
                OutputStream outputStream = this.mOutput;
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        throw e;
                    }
                }
            } catch (IOException io) {
                throw io;
            }
        } catch (Throwable e2) {
            OutputStream outputStream2 = this.mOutput;
            if (outputStream2 != null) {
                try {
                    outputStream2.close();
                } catch (IOException e3) {
                    throw e3;
                }
            }
            throw e2;
        }
    }

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    private String getProperty(String key) throws IOException {
        this.mInput = null;
        try {
            try {
                FileInputStream fileInputStream = new FileInputStream(this.mConfigFile);
                this.mInput = fileInputStream;
                this.mProp.load(fileInputStream);
                String property = this.mProp.getProperty(key);
                InputStream inputStream = this.mInput;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        throw e;
                    }
                }
                return property;
            } catch (IOException e2) {
                throw e2;
            }
        } catch (Throwable e3) {
            InputStream inputStream2 = this.mInput;
            if (inputStream2 != null) {
                try {
                    inputStream2.close();
                } catch (IOException e4) {
                    throw e4;
                }
            }
            throw e3;
        }
    }

    private void initDefault() throws IOException {
    }

    public void setAutoMode(int mode) throws IOException {
        if (mode < 0 || mode > 1) {
            IllegalArgumentException e = new IllegalArgumentException();
            throw e;
        }
        try {
            setProperty("MODE", Integer.toString(mode));
        } catch (IOException e2) {
            throw e2;
        }
    }

    public void setParam(int state) throws IllegalArgumentException, IOException {
        if ((state < 2 || state > 4) && state != 0) {
            IllegalArgumentException e = new IllegalArgumentException();
            throw e;
        }
        try {
            setProperty("STATE", Integer.toString(state));
        } catch (IOException e2) {
            throw e2;
        }
    }

    public Triggers getTriggers() throws IOException {
        try {
            String paramStr = getProperty("BATTERY_LEVEL_TRIGGERS");
            if (paramStr == null) {
                InvalidParameterException e = new InvalidParameterException();
                throw e;
            }
            String[] tokens = paramStr.split(",");
            if (tokens.length != 3) {
                InvalidParameterException e2 = new InvalidParameterException();
                throw e2;
            }
            int i = 0;
            int[] ary = new int[tokens.length];
            int length = tokens.length;
            int i2 = 0;
            while (i2 < length) {
                String token = tokens[i2];
                ary[i] = Integer.parseInt(token);
                i2++;
                i++;
            }
            Triggers triggers = new Triggers(ary[0], ary[1], ary[2]);
            if (triggers.normal > 100 || triggers.normal < 0 || triggers.high > 100 || triggers.high < 0 || triggers.high > triggers.normal || triggers.ultra > 100 || triggers.ultra < 0 || triggers.ultra > triggers.high) {
                InvalidParameterException e3 = new InvalidParameterException();
                throw e3;
            }
            return triggers;
        } catch (IOException e4) {
            throw e4;
        }
    }

    public int getParam() throws IOException, InvalidParameterException {
        try {
            String paramStr = getProperty("STATE");
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
        } catch (IOException e3) {
            throw e3;
        }
    }

    public int getAutoMode() throws IOException, InvalidParameterException {
        try {
            String paramStr = getProperty("MODE");
            if (paramStr == null) {
                InvalidParameterException e = new InvalidParameterException();
                throw e;
            }
            int param = Integer.parseInt(paramStr);
            if (param < 0 || param > 1) {
                InvalidParameterException e2 = new InvalidParameterException();
                throw e2;
            }
            return param;
        } catch (IOException e3) {
            throw e3;
        }
    }

    public void setLogLevel(int logLevel) throws IOException {
        try {
            setProperty("LOG_LEVEL", Integer.toString(logLevel));
        } catch (IOException e) {
            throw e;
        }
    }

    public int getLogLevel() throws IOException {
        try {
            String loglevelStr = getProperty("LOG_LEVEL");
            if (loglevelStr == null) {
                InvalidParameterException e = new InvalidParameterException();
                throw e;
            }
            int param = Integer.parseInt(loglevelStr);
            return param;
        } catch (IOException e2) {
            throw e2;
        }
    }

    public void setTriggers(Triggers triggers) throws InvalidParameterException, IOException {
        if (triggers.normal > 100 || triggers.normal < 0 || triggers.high > 100 || triggers.high < 0 || triggers.high > triggers.normal || triggers.ultra > 100 || triggers.ultra < 0 || triggers.ultra > triggers.high) {
            InvalidParameterException e = new InvalidParameterException();
            throw e;
        }
        try {
            setProperty("BATTERY_LEVEL_TRIGGERS", Integer.toString(triggers.normal) + "," + Integer.toString(triggers.high) + "," + Integer.toString(triggers.ultra));
        } catch (IOException e2) {
            throw e2;
        }
    }

    public void resetTriggers() throws IOException {
        try {
            setProperty("BATTERY_LEVEL_TRIGGERS", "-1,-1,-1");
        } catch (IOException e) {
            throw e;
        }
    }

    public void cleanGlobalConfiguration() throws IOException {
        String configDir = PowerXtendControlAPIImpl.getConfigPath();
        this.mConfigFile = configDir + SliceClientPermissions.SliceAuthority.DELIMITER + mPropFileName;
        File file = new File(this.mConfigFile);
        if (file.delete()) {
            System.out.println(file.getName() + " is deleted!");
        } else {
            System.out.println("Delete operation is failed.");
        }
    }
}
