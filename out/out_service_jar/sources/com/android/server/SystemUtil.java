package com.android.server;

import android.app.ActivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Slog;
/* loaded from: classes.dex */
public class SystemUtil {
    private static final int BINDER = 6000;
    private static final int DELAY = 7000;
    private static final int NO_BINDER = 5000;
    private static final String TAG = "SystemUtil";
    final KillProcessHandler killProcessHandler;
    private Looper mLooper;
    private String mPackageName;

    public SystemUtil(Looper looper) {
        this.mLooper = looper;
        this.killProcessHandler = new KillProcessHandler(this.mLooper);
    }

    public boolean isSpecialPackage(String packageName) {
        return "com.android.networkstack.process".equals(packageName) || "com.android.phone".equals(packageName) || "system_server".equals(packageName);
    }

    public void sendNoBinderMessage(int pid, String packageName, String methodName) {
        Message msg = new Message();
        msg.what = 5000;
        Bundle bundle = new Bundle();
        bundle.putString("mPackageName", packageName);
        bundle.putString("mCallMethodName", methodName);
        bundle.putInt("pid", pid);
        msg.setData(bundle);
        this.killProcessHandler.sendMessage(msg);
    }

    public void sendBinderMessage() {
        Message msg = new Message();
        msg.what = BINDER;
        this.killProcessHandler.sendMessage(msg);
    }

    /* loaded from: classes.dex */
    private final class KillProcessHandler extends Handler {
        public KillProcessHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 5000:
                    Message mymsg = new Message();
                    mymsg.what = SystemUtil.DELAY;
                    mymsg.setData(msg.getData());
                    SystemUtil.this.killProcessHandler.sendMessageDelayed(mymsg, 40000L);
                    return;
                case SystemUtil.BINDER /* 6000 */:
                    SystemUtil.this.killProcessHandler.removeMessages(SystemUtil.DELAY);
                    return;
                case SystemUtil.DELAY /* 7000 */:
                    String pacakgeName = msg.getData().getString("mPackageName");
                    String callMethodName = msg.getData().getString("mCallMethodName");
                    int pid = msg.getData().getInt("pid");
                    if (SystemUtil.this.isSpecialPackage(pacakgeName) || Process.myPid() == pid) {
                        Slog.d(SystemUtil.TAG, SystemUtil.this.mPackageName + " can't kill, " + callMethodName);
                        return;
                    }
                    Slog.d(SystemUtil.TAG, "killProcess pid = " + pid + " because " + callMethodName + ", catch TNE");
                    try {
                        ActivityManager.getService().startTNE("0x007a0034", 512L, Process.myPid(), "");
                    } catch (Exception e) {
                        Slog.d(SystemUtil.TAG, "killProcess,but catchtne error");
                    }
                    Process.killProcess(pid);
                    return;
                default:
                    return;
            }
        }
    }
}
