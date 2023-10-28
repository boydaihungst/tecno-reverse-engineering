package com.mediatek.cpuloading;

import android.os.StrictMode;
import android.os.UEventObserver;
import com.mediatek.boostfwk.utils.Config;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
/* loaded from: classes.dex */
public class CpuLoadingObserver {
    private static final String BACKGROUND_CPUS_PATH = "/dev/cpuset/background/cpus";
    private static final String OVER_THRESHOLD = "/proc/cpu_loading/overThrhld";
    private static final String POLLING_ON_OFF = "/proc/cpu_loading/onoff";
    private static final String POLLING_TIME_SECOND = "/proc/cpu_loading/poltime_secs";
    private static final String SPECIFY_32BIT_CPUS_PATH = "sys/devices/system/cpu/aarch32_el0";
    private static final String SPECIFY_CPUS = "/proc/cpu_loading/specify_cpus";
    private static final String SPECIFY_OVER_THRESHOLD = "/proc/cpu_loading/specify_overThrhld";
    private static final String UEVENT_PATH = "DEVPATH=/devices/virtual/misc/cpu_loading";
    private Observer mObserver;
    private String mSpecifyCpus;
    private static final String TAG = CpuLoadingObserver.class.getSimpleName();
    private static int DEFAULT_THRESHOLD = 85;
    private static int DEFAULT_WINDOW = 10;
    private static int DEFAULT_SPECIFY_THRESHOLD = 85;
    private static int SPECIFY_RELEASE_TARGET = 15;
    private int mThreshold = DEFAULT_THRESHOLD;
    private int mWindow = DEFAULT_WINDOW;
    private int mSpecifyThreshold = DEFAULT_SPECIFY_THRESHOLD;
    private MyUEventObserver mUEventObserver = new MyUEventObserver();

    /* loaded from: classes.dex */
    public interface Observer {
        void onHighCpuLoading(int i);
    }

    public CpuLoadingObserver() {
        this.mSpecifyCpus = "";
        this.mSpecifyCpus = readSpecifyCpus();
    }

    public void setObserver(Observer observer) {
        this.mObserver = observer;
    }

    public void startObserving() {
        writeFile(POLLING_ON_OFF, Config.USER_CONFIG_DEFAULT_TYPE);
        writeFile(OVER_THRESHOLD, String.valueOf(this.mThreshold));
        writeFile(POLLING_TIME_SECOND, String.valueOf(this.mWindow));
        if (!this.mSpecifyCpus.equals("")) {
            writeFile(SPECIFY_CPUS, this.mSpecifyCpus);
            writeFile(SPECIFY_OVER_THRESHOLD, String.valueOf(this.mSpecifyThreshold));
        }
        this.mUEventObserver.startObserving(UEVENT_PATH);
    }

    private String readSpecifyCpus() {
        String specifyCpus = readFile(SPECIFY_32BIT_CPUS_PATH);
        if (specifyCpus.equals("")) {
            specifyCpus = readFile(BACKGROUND_CPUS_PATH);
        }
        if (specifyCpus.equals("")) {
            return "";
        }
        String[] arr = specifyCpus.split("-");
        if (arr.length == 2) {
            return arr[1].trim() + arr[0].trim();
        }
        return specifyCpus;
    }

    public void stopObserving() {
        writeFile(POLLING_ON_OFF, "0");
        this.mUEventObserver.stopObserving();
        this.mObserver = null;
    }

    private void writeFile(String filePath, String value) {
        if (filePath == null) {
            return;
        }
        File file = new File(filePath);
        FileOutputStream out = null;
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        StrictMode.allowThreadDiskWrites();
        try {
            try {
                out = new FileOutputStream(file);
                out.write(value.getBytes());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e2) {
                        ioe = e2;
                        ioe.printStackTrace();
                        StrictMode.setThreadPolicy(oldPolicy);
                    }
                }
            }
            try {
                out.close();
            } catch (IOException e3) {
                ioe = e3;
                ioe.printStackTrace();
                StrictMode.setThreadPolicy(oldPolicy);
            }
            StrictMode.setThreadPolicy(oldPolicy);
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            StrictMode.setThreadPolicy(oldPolicy);
            throw th;
        }
    }

    private String readFile(String filePath) {
        if (filePath == null) {
            return "";
        }
        File file = new File(filePath);
        FileInputStream in = null;
        try {
            try {
                in = new FileInputStream(file);
                InputStreamReader input = new InputStreamReader(in, "UTF-8");
                StringWriter output = new StringWriter();
                char[] buffer = new char[1024];
                long count = 0;
                while (true) {
                    int n = input.read(buffer);
                    if (-1 == n) {
                        break;
                    }
                    output.write(buffer, 0, n);
                    count += n;
                }
                String stringWriter = output.toString();
                try {
                    in.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                return stringWriter;
            } catch (IOException e) {
                e.printStackTrace();
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ioe2) {
                        ioe2.printStackTrace();
                    }
                }
                return "";
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe3) {
                    ioe3.printStackTrace();
                }
            }
            throw th;
        }
    }

    /* loaded from: classes.dex */
    private class MyUEventObserver extends UEventObserver {
        private MyUEventObserver() {
        }

        public void onUEvent(UEventObserver.UEvent event) {
            String over = event.get("over");
            String specify_over = event.get("specify_over");
            if (over != null && over.equals(Config.USER_CONFIG_DEFAULT_TYPE)) {
                CpuLoadingObserver.this.mObserver.onHighCpuLoading(-1);
            } else if (specify_over != null && specify_over.equals(Config.USER_CONFIG_DEFAULT_TYPE)) {
                CpuLoadingObserver.this.mObserver.onHighCpuLoading(CpuLoadingObserver.SPECIFY_RELEASE_TARGET);
            }
        }
    }
}
