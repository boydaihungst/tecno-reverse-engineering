package com.android.internal.protolog;

import com.android.internal.protolog.BaseProtoLogImpl;
import com.android.internal.protolog.common.IProtoLogGroup;
import java.io.File;
import java.io.PrintWriter;
/* loaded from: classes4.dex */
public class ProtoLogImpl extends BaseProtoLogImpl {
    private static final int BUFFER_CAPACITY = 1048576;
    private static final String LOG_FILENAME = "/data/misc/wmtrace/wm_log.winscope";
    private static final String VIEWER_CONFIG_FILENAME = "/system/etc/protolog.conf.json.gz";
    private static ProtoLogImpl sServiceInstance = null;

    static {
        addLogGroupEnum(ProtoLogGroup.values());
    }

    public static void d(IProtoLogGroup group, int messageHash, int paramsMask, String messageString, Object... args) {
        getSingleInstance().log(BaseProtoLogImpl.LogLevel.DEBUG, group, messageHash, paramsMask, messageString, args);
    }

    public static void v(IProtoLogGroup group, int messageHash, int paramsMask, String messageString, Object... args) {
        getSingleInstance().log(BaseProtoLogImpl.LogLevel.VERBOSE, group, messageHash, paramsMask, messageString, args);
    }

    public static void i(IProtoLogGroup group, int messageHash, int paramsMask, String messageString, Object... args) {
        getSingleInstance().log(BaseProtoLogImpl.LogLevel.INFO, group, messageHash, paramsMask, messageString, args);
    }

    public static void w(IProtoLogGroup group, int messageHash, int paramsMask, String messageString, Object... args) {
        getSingleInstance().log(BaseProtoLogImpl.LogLevel.WARN, group, messageHash, paramsMask, messageString, args);
    }

    public static void e(IProtoLogGroup group, int messageHash, int paramsMask, String messageString, Object... args) {
        getSingleInstance().log(BaseProtoLogImpl.LogLevel.ERROR, group, messageHash, paramsMask, messageString, args);
    }

    public static void wtf(IProtoLogGroup group, int messageHash, int paramsMask, String messageString, Object... args) {
        getSingleInstance().log(BaseProtoLogImpl.LogLevel.WTF, group, messageHash, paramsMask, messageString, args);
    }

    public static boolean isEnabled(IProtoLogGroup group) {
        return group.isLogToLogcat() || (group.isLogToProto() && getSingleInstance().isProtoEnabled());
    }

    public static synchronized ProtoLogImpl getSingleInstance() {
        ProtoLogImpl protoLogImpl;
        synchronized (ProtoLogImpl.class) {
            if (sServiceInstance == null) {
                sServiceInstance = new ProtoLogImpl(new File(LOG_FILENAME), 1048576, new ProtoLogViewerConfigReader());
            }
            protoLogImpl = sServiceInstance;
        }
        return protoLogImpl;
    }

    public static synchronized void setSingleInstance(ProtoLogImpl instance) {
        synchronized (ProtoLogImpl.class) {
            sServiceInstance = instance;
        }
    }

    public ProtoLogImpl(File logFile, int bufferCapacity, ProtoLogViewerConfigReader viewConfigReader) {
        super(logFile, VIEWER_CONFIG_FILENAME, bufferCapacity, viewConfigReader);
    }

    public void openWmLogcat(boolean open) {
        ProtoLogGroup[] values;
        for (ProtoLogGroup group : ProtoLogGroup.values()) {
            if (!ProtoLogGroup.TEST_GROUP.equals(group)) {
                group.setLogToLogcat(open);
            }
        }
    }

    public void enableText(PrintWriter pw, boolean enable) {
        super.enableText(pw);
        openWmLogcat(enable);
    }
}
