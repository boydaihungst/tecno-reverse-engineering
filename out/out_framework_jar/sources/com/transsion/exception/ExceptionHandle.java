package com.transsion.exception;

import android.app.ActivityManager;
import android.media.AudioSystem;
import android.os.Debug;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telecom.TelecomManager;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
/* loaded from: classes4.dex */
public class ExceptionHandle {
    private static final int[] CMDLINE_OUT = {4096};
    public static final long FLAG_TNE_USER_ANR_TRACE = 64;
    public static final long FLAG_TNE_USER_BINDER = 128;
    public static final long FLAG_TNE_USER_BLOCKIO = 32768;
    public static final long FLAG_TNE_USER_LAST_KMSG = 2048;
    public static final long FLAG_TNE_USER_LOG_CRASH = 16;
    public static final long FLAG_TNE_USER_LOG_EVENT = 4;
    public static final long FLAG_TNE_USER_LOG_KERNEL = 32;
    public static final long FLAG_TNE_USER_LOG_MAIN_SYS = 2;
    public static final long FLAG_TNE_USER_LOG_RADIO = 8;
    public static final long FLAG_TNE_USER_MAPS = 256;
    public static final long FLAG_TNE_USER_MMSTAT = 65536;
    public static final long FLAG_TNE_USER_NATIVE_TRACE = 1024;
    public static final long FLAG_TNE_USER_PROC_MEMINFO = 8192;
    public static final long FLAG_TNE_USER_PROPERTY = 262144;
    public static final long FLAG_TNE_USER_PS = 4096;
    public static final long FLAG_TNE_USER_SIGNAL_TRACE = 512;
    public static final long FLAG_TNE_USER_ZONE_INFO = 16384;
    private static final String TAG = "ExceptionHandle";

    public static native int nativeHprofStrip(String str);

    private String getProcessName(int pid) {
        String[] cmdline = new String[1];
        if (!Process.readProcFile("/proc/" + pid + "/cmdline", CMDLINE_OUT, cmdline, null, null)) {
            return "";
        }
        return cmdline[0];
    }

    /* JADX WARN: Removed duplicated region for block: B:37:0x0134  */
    /* JADX WARN: Removed duplicated region for block: B:58:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void handleException(boolean isSystem, Throwable tr) {
        long flag;
        int pid;
        StackTraceElement[] stacks = tr.getStackTrace();
        for (StackTraceElement s : stacks) {
            Log.i(TAG, "at " + s);
        }
        if ("1".equals(SystemProperties.get("persist.sys.adb.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS))) {
            try {
                int sysPid = SystemProperties.getInt("sys.transsion.lowmem", 0);
                if (sysPid > 0) {
                    SystemProperties.set("sys.transsion.lowmem", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS);
                    ActivityManager.getService().startTNE("0x007a0011", 256L, sysPid, "");
                    SystemClock.sleep(5000L);
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "start tne error " + e);
            }
        }
        try {
            int pid2 = Process.myPid();
            String procName = getProcessName(pid2);
            if (tr.getClass().getName().equals("java.lang.OutOfMemoryError")) {
                long flag2 = 262166 | 4352;
                if (!"1".equals(SystemProperties.get("persist.sys.fans.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS))) {
                    flag = flag2;
                } else {
                    String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date(System.currentTimeMillis()));
                    String path = String.format("/data/hprof/SYSTEM_OOM_%d_%s.hprof", Integer.valueOf(Process.myPid()), date);
                    nativeHprofStrip(path);
                    Debug.dumpHprofData(path);
                    ActivityManager.getService().startTNE("0x007a0012", 512L, pid2, path);
                    return;
                }
            } else {
                flag = 262166;
            }
            if (isSystem) {
                if (!tr.getClass().getName().equals("java.lang.IllegalStateException")) {
                    pid = pid2;
                } else if (tr.getMessage() == null || !tr.getMessage().contains("/data/TNE/TRAN_ALL_ALARMS_INFO")) {
                    pid = pid2;
                } else {
                    Log.d(TAG, "dump alarms file to TNE");
                    ActivityManager.getService().startTNE("0xffffff00", flag, pid2, "/data/TNE/TRAN_ALL_ALARMS_INFO");
                    if (!tr.getClass().getName().equals("java.lang.OutOfMemoryError")) {
                        try {
                            Thread.sleep(TelecomManager.VERY_SHORT_CALL_TIME_MS);
                            return;
                        } catch (InterruptedException e2) {
                            e2.printStackTrace();
                            return;
                        }
                    }
                    return;
                }
                ActivityManager.getService().startTNE("0xffffff00", flag, pid, procName);
                if (!tr.getClass().getName().equals("java.lang.OutOfMemoryError")) {
                }
            }
        } catch (RemoteException e1) {
            Log.e(TAG, "e1=" + e1);
        } catch (Exception e22) {
            Log.e(TAG, "e2=" + e22);
        }
    }
}
