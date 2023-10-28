package com.mediatek.aee;

import android.os.Process;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.dx.DexOptExtFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/* loaded from: classes.dex */
public class ExceptionLogImpl extends ExceptionLog {
    public static final byte AEE_EXCEPTION_JNI = 1;
    public static final byte AEE_WARNING_JNI = 0;
    public static final String TAG = "AES";
    private static int[] mZygotePids = null;
    private final String SEND_NON_PROTECTED_BROADCAST = "Sending non-protected broadcast";
    private final String[] protectedBroadcastFilter = {"android.intent.action.CALL_EMERGENCY", "com.debug.loggerui.ADB_CMD", "com.mediatek.log2server.EXCEPTION_HAPPEND", "com.mediatek.omacp.capability.result", "com.mediatek.autounlock", "com.mtk.autotest.heartset.stop", "com.mtk.fts.ACTION", "com.android.systemui.demo", "ATG_MQTT_MqttService.pingSender", "AUTO_SUBMIT_STATUS"};
    private final String[] FalseAlarmCases = {"Process: system_server", "Subject: LazyAlarmStore", "TerribleFailure: Removed TIME_TICK alarm", "android.util.Log.wtf", "android.util.Slog.wtf", "com.android.server.alarm.LazyAlarmStore.remove", "=====case end=====", "Process: system_server", "Subject: ActivityManager", "TerribleFailure: Background started FGS", "=====case end====="};
    private final String FILE_OBSERVER_NULL_PATH = "Unhandled exception in FileObserver com.android.server.BootReceiver";

    private static native long SFMatter(long j, long j2);

    private static native void WDTMatter(long j);

    private static native boolean getNativeExceptionPidListImpl(int[] iArr);

    private static native void report(String str, String str2, String str3, String str4, String str5, long j);

    private static native void switchFtraceImpl(int i);

    private static native void systemreportImpl(byte b, String str, String str2, String str3, String str4);

    static {
        Log.i(TAG, "load Exception Log jni");
        System.loadLibrary("mediatek_exceptionlog");
    }

    public void handle(String type, String info, String pid) {
        long lpid;
        String pkgs;
        Log.w(TAG, "Exception Log handling..." + type);
        if (!type.contains("anr") || !"enable".equals(SystemProperties.get("persist.sys.reduce.dump", "disable"))) {
            if (!type.contains("app_crash") || !"enable".equals(SystemProperties.get("persist.sys.reduce.crash", "disable"))) {
                if (type.startsWith("data_app") && !info.contains("com.android.development") && SystemProperties.getInt("persist.vendor.mtk.aee.filter", 1) == 1) {
                    Log.w(TAG, "Skipped - do not care third party apk");
                    return;
                }
                long lpid2 = 0;
                String[] splitInfo = info.split("\n+");
                Pattern procMatcher = Pattern.compile("^Process:\\s+(.*)");
                Pattern pkgMatcher = Pattern.compile("^Package:\\s+(.*)");
                int length = splitInfo.length;
                String proc = "";
                String proc2 = "";
                int i = 0;
                while (i < length) {
                    int i2 = length;
                    String s = splitInfo[i];
                    long lpid3 = lpid2;
                    Matcher m = procMatcher.matcher(s);
                    if (m.matches()) {
                        proc = m.group(1);
                    }
                    Matcher m2 = pkgMatcher.matcher(s);
                    if (m2.matches()) {
                        pkgs = proc2 + m2.group(1) + "\n";
                    } else {
                        String pkgs2 = proc2;
                        pkgs = pkgs2;
                    }
                    i++;
                    proc2 = pkgs;
                    length = i2;
                    lpid2 = lpid3;
                }
                String pkgs3 = proc2;
                long lpid4 = lpid2;
                if (pid.equals("")) {
                    lpid = lpid4;
                } else {
                    lpid = Long.parseLong(pid);
                }
                if (type.equals("system_server_wtf") && isSkipSystemWtfReport(info)) {
                    return;
                }
                if ("unknown".equals(proc) && Config.USER_CONFIG_DEFAULT_TYPE.equals(SystemProperties.get("persist.sys.adb.support", "0"))) {
                    Log.w(TAG, "Skipped - unknown exception");
                    return;
                } else {
                    report(proc, pkgs3, info, "Backtrace of all threads:\n\n", type, lpid);
                    return;
                }
            }
            Log.w(TAG, "Skipped all JE DB");
            return;
        }
        Log.w(TAG, "Skipped all anr DB");
    }

    public void systemreport(byte Type, String Module, String Msg, String Path) {
        String Backtrace = getThreadStackTrace();
        systemreportImpl(Type, Module, Backtrace, Msg, Path);
    }

    public boolean getNativeExceptionPidList(int[] pidList) {
        return getNativeExceptionPidListImpl(pidList);
    }

    public void switchFtrace(int config) {
        if (config == 3) {
            DexOptExtFactory.getInstance().makeDexOpExt().notifySpeedUp();
        }
        switchFtraceImpl(config);
    }

    public boolean isJavaProcess(int pid) {
        int[] iArr;
        if (pid <= 0) {
            return false;
        }
        if (mZygotePids == null) {
            String[] commands = {"zygote64", "zygote"};
            mZygotePids = Process.getPidsForCommands(commands);
        }
        if (mZygotePids != null) {
            int parentPid = Process.getParentPid(pid);
            for (int zygotePid : mZygotePids) {
                if (parentPid == zygotePid) {
                    return true;
                }
            }
        }
        Log.w(TAG, "pid: " + pid + " is not a Java process");
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [232=4, 234=5] */
    /* JADX WARN: Removed duplicated region for block: B:121:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:122:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:69:0x0122 A[Catch: IOException -> 0x0101, TRY_ENTER, TRY_LEAVE, TryCatch #4 {IOException -> 0x0101, blocks: (B:69:0x0122, B:76:0x0133, B:50:0x00fd), top: B:88:0x000b }] */
    /* JADX WARN: Removed duplicated region for block: B:76:0x0133 A[Catch: IOException -> 0x0101, TRY_ENTER, TRY_LEAVE, TryCatch #4 {IOException -> 0x0101, blocks: (B:69:0x0122, B:76:0x0133, B:50:0x00fd), top: B:88:0x000b }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void readTransactionInfoFromFile(int pid, ArrayList<Integer> binderList) {
        File file;
        Pattern pattern = Pattern.compile("outgoing transaction.+from.+to (\\d+):.+code.+");
        BufferedReader br = null;
        try {
            try {
                try {
                    String filepath = "/sys/kernel/debug/binder/proc/" + Integer.toString(pid);
                    file = new File(filepath);
                    try {
                    } catch (FileNotFoundException e) {
                        e = e;
                        Log.e(TAG, "FileNotFoundException", e);
                        if (br == null) {
                        }
                    } catch (IOException e2) {
                        e = e2;
                        Log.e(TAG, "IOException when gettting Binder. ", e);
                        if (br == null) {
                        }
                    } catch (Throwable th) {
                        th = th;
                        Throwable th2 = th;
                        if (0 != 0) {
                            try {
                                br.close();
                            } catch (IOException ioe) {
                                Log.e(TAG, "IOException when close buffer reader:", ioe);
                            }
                        }
                        throw th2;
                    }
                } catch (IOException ioe2) {
                    Log.e(TAG, "IOException when close buffer reader:", ioe2);
                    return;
                }
            } catch (FileNotFoundException e3) {
                e = e3;
            } catch (IOException e4) {
                e = e4;
            } catch (Throwable th3) {
                th = th3;
            }
            if (!file.exists()) {
                Log.w(TAG, "Filepath isn't exist: /d/binder/proc/" + pid);
                if (0 != 0) {
                    try {
                        br.close();
                        return;
                    } catch (IOException ioe3) {
                        Log.e(TAG, "IOException when close buffer reader:", ioe3);
                        return;
                    }
                }
                return;
            }
            ArrayMap<Integer, Integer> rcvPidMap = new ArrayMap<>();
            br = new BufferedReader(new FileReader(file));
            boolean contextBinder = false;
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                String line2 = line.trim();
                if (contextBinder) {
                    if (line2.contains("outgoing transaction")) {
                        Matcher matcher = pattern.matcher(line2);
                        if (matcher.find()) {
                            Integer rcv_pid = Integer.valueOf(matcher.group(1));
                            int index = rcvPidMap.indexOfKey(rcv_pid);
                            if (index >= 0) {
                                int value = rcvPidMap.valueAt(index).intValue();
                                rcvPidMap.setValueAt(index, Integer.valueOf(value + 1));
                            } else {
                                rcvPidMap.put(rcv_pid, 1);
                            }
                        }
                    } else if (line2.startsWith("node")) {
                        break;
                    }
                } else if ("context binder".equals(line2)) {
                    contextBinder = true;
                }
            }
            for (Map.Entry<Integer, Integer> entry : rcvPidMap.entrySet()) {
                if (entry.getValue().intValue() > 10) {
                    try {
                        binderList.add(entry.getKey());
                    } catch (FileNotFoundException e5) {
                        e = e5;
                        Log.e(TAG, "FileNotFoundException", e);
                        if (br == null) {
                            br.close();
                        }
                        return;
                    } catch (IOException e6) {
                        e = e6;
                        Log.e(TAG, "IOException when gettting Binder. ", e);
                        if (br == null) {
                            br.close();
                        }
                        return;
                    }
                }
            }
            br.close();
        } catch (Throwable th4) {
            th = th4;
        }
    }

    private static String getThreadStackTrace() {
        Writer traces = new StringWriter();
        try {
            Thread th = Thread.currentThread();
            StackTraceElement[] st = th.getStackTrace();
            traces.write("\"" + th.getName() + "\" " + (th.isDaemon() ? "daemon" : "") + " prio=" + th.getPriority() + " Thread id=" + th.getId() + " " + th.getState() + "\n");
            for (StackTraceElement line : st) {
                traces.write("\t" + line + "\n");
            }
            traces.write("\n");
            String ret_trace = traces.toString();
            return ret_trace;
        } catch (IOException e) {
            return "IOException";
        } catch (OutOfMemoryError e2) {
            return "java.lang.OutOfMemoryError";
        }
    }

    private static String getAllThreadStackTraces() {
        Map<Thread, StackTraceElement[]> st = Thread.getAllStackTraces();
        Writer traces = new StringWriter();
        try {
            for (Map.Entry<Thread, StackTraceElement[]> e : st.entrySet()) {
                StackTraceElement[] el = e.getValue();
                Thread th = e.getKey();
                traces.write("\"" + th.getName() + "\" " + (th.isDaemon() ? "daemon" : "") + " prio=" + th.getPriority() + " Thread id=" + th.getId() + " " + th.getState() + "\n");
                for (StackTraceElement line : el) {
                    traces.write("\t" + line + "\n");
                }
                traces.write("\n");
            }
            String ret_traces = traces.toString();
            return ret_traces;
        } catch (IOException e2) {
            return "IOException";
        } catch (OutOfMemoryError e3) {
            return "java.lang.OutOfMemoryError";
        }
    }

    public void WDTMatterJava(long lParam) {
        WDTMatter(lParam);
    }

    public long SFMatterJava(long setorget, long lParam) {
        return SFMatter(setorget, lParam);
    }

    private boolean isSkipSystemWtfReport(String info) {
        return isSkipReportFromProtectedBroadcast(info) || isSkipReportFromNullFilePath(info) || isSkipFalseAlarmCases(info);
    }

    private boolean isSkipReportFromProtectedBroadcast(String info) {
        if (info.contains("Sending non-protected broadcast")) {
            int i = 0;
            while (true) {
                String[] strArr = this.protectedBroadcastFilter;
                if (i < strArr.length) {
                    if (!info.contains(strArr[i])) {
                        i++;
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private boolean isSkipFalseAlarmCases(String info) {
        boolean case_match = true;
        int i = 0;
        while (true) {
            String[] strArr = this.FalseAlarmCases;
            if (i < strArr.length) {
                if (!strArr[i].equals("=====case end=====")) {
                    if (!info.contains(this.FalseAlarmCases[i])) {
                        case_match = false;
                    }
                } else if (case_match) {
                    return true;
                } else {
                    case_match = true;
                }
                i++;
            } else {
                return false;
            }
        }
    }

    private boolean isSkipReportFromNullFilePath(String info) {
        if (info.contains("Unhandled exception in FileObserver com.android.server.BootReceiver")) {
            return true;
        }
        return false;
    }
}
