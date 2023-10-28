package com.android.server.utils;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
/* loaded from: classes2.dex */
public final class PriorityDump {
    public static final String PRIORITY_ARG = "--dump-priority";
    public static final String PRIORITY_ARG_CRITICAL = "CRITICAL";
    public static final String PRIORITY_ARG_HIGH = "HIGH";
    public static final String PRIORITY_ARG_NORMAL = "NORMAL";
    private static final int PRIORITY_TYPE_CRITICAL = 1;
    private static final int PRIORITY_TYPE_HIGH = 2;
    private static final int PRIORITY_TYPE_INVALID = 0;
    private static final int PRIORITY_TYPE_NORMAL = 3;
    public static final String PROTO_ARG = "--proto";

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    private @interface PriorityType {
    }

    private PriorityDump() {
        throw new UnsupportedOperationException();
    }

    public static void dump(PriorityDumper dumper, FileDescriptor fd, PrintWriter pw, String[] args) {
        boolean asProto = false;
        int priority = 0;
        if (args == null) {
            dumper.dump(fd, pw, args, false);
            return;
        }
        String[] strippedArgs = new String[args.length];
        int strippedCount = 0;
        int argIndex = 0;
        while (argIndex < args.length) {
            if (args[argIndex].equals("--proto")) {
                asProto = true;
            } else if (args[argIndex].equals(PRIORITY_ARG)) {
                if (argIndex + 1 < args.length) {
                    argIndex++;
                    priority = getPriorityType(args[argIndex]);
                }
            } else {
                strippedArgs[strippedCount] = args[argIndex];
                strippedCount++;
            }
            argIndex++;
        }
        int argIndex2 = args.length;
        if (strippedCount < argIndex2) {
            strippedArgs = (String[]) Arrays.copyOf(strippedArgs, strippedCount);
        }
        switch (priority) {
            case 1:
                dumper.dumpCritical(fd, pw, strippedArgs, asProto);
                return;
            case 2:
                dumper.dumpHigh(fd, pw, strippedArgs, asProto);
                return;
            case 3:
                dumper.dumpNormal(fd, pw, strippedArgs, asProto);
                return;
            default:
                dumper.dump(fd, pw, strippedArgs, asProto);
                return;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static int getPriorityType(String arg) {
        char c;
        switch (arg.hashCode()) {
            case -1986416409:
                if (arg.equals(PRIORITY_ARG_NORMAL)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1560189025:
                if (arg.equals(PRIORITY_ARG_CRITICAL)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 2217378:
                if (arg.equals(PRIORITY_ARG_HIGH)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            default:
                return 0;
        }
    }

    /* loaded from: classes2.dex */
    public interface PriorityDumper {
        default void dumpCritical(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
        }

        default void dumpHigh(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
        }

        default void dumpNormal(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
        }

        default void dump(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
            dumpCritical(fd, pw, args, asProto);
            dumpHigh(fd, pw, args, asProto);
            dumpNormal(fd, pw, args, asProto);
        }
    }
}
