package com.android.server.os;
/* loaded from: classes4.dex */
public final class TombstoneProtos {
    public static final int ARM32 = 0;
    public static final int ARM64 = 1;
    public static final int X86 = 2;
    public static final int X86_64 = 3;

    /* loaded from: classes4.dex */
    public final class Tombstone {
        public static final long ABORT_MESSAGE = 1138166333454L;
        public static final long ARCH = 1159641169921L;
        public static final long BUILD_FINGERPRINT = 1138166333442L;
        public static final long CAUSES = 2246267895823L;
        public static final long COMMAND_LINE = 2237677961225L;
        public static final long LOG_BUFFERS = 2246267895826L;
        public static final long MEMORY_MAPPINGS = 2246267895825L;
        public static final long OPEN_FDS = 2246267895827L;
        public static final long PID = 1155346202629L;
        public static final long PROCESS_UPTIME = 1155346202644L;
        public static final long REVISION = 1138166333443L;
        public static final long SELINUX_LABEL = 1138166333448L;
        public static final long SIGNAL_INFO = 1146756268042L;
        public static final long THREADS = 2246267895824L;
        public static final long TID = 1155346202630L;
        public static final long TIMESTAMP = 1138166333444L;
        public static final long UID = 1155346202631L;

        public Tombstone() {
        }

        /* loaded from: classes4.dex */
        public final class ThreadsEntry {
            public static final long KEY = 1155346202625L;
            public static final long VALUE = 1146756268034L;

            public ThreadsEntry() {
            }
        }
    }

    /* loaded from: classes4.dex */
    public final class Signal {
        public static final long CODE = 1120986464259L;
        public static final long CODE_NAME = 1138166333444L;
        public static final long FAULT_ADDRESS = 1116691496969L;
        public static final long FAULT_ADJACENT_METADATA = 1146756268042L;
        public static final long HAS_FAULT_ADDRESS = 1133871366152L;
        public static final long HAS_SENDER = 1133871366149L;
        public static final long NAME = 1138166333442L;
        public static final long NUMBER = 1120986464257L;
        public static final long SENDER_PID = 1120986464263L;
        public static final long SENDER_UID = 1120986464262L;

        public Signal() {
        }
    }

    /* loaded from: classes4.dex */
    public final class HeapObject {
        public static final long ADDRESS = 1116691496961L;
        public static final long ALLOCATION_BACKTRACE = 2246267895812L;
        public static final long ALLOCATION_TID = 1116691496963L;
        public static final long DEALLOCATION_BACKTRACE = 2246267895814L;
        public static final long DEALLOCATION_TID = 1116691496965L;
        public static final long SIZE = 1116691496962L;

        public HeapObject() {
        }
    }

    /* loaded from: classes4.dex */
    public final class MemoryError {
        public static final int BUFFER_OVERFLOW = 4;
        public static final int BUFFER_UNDERFLOW = 5;
        public static final int DOUBLE_FREE = 2;
        public static final int GWP_ASAN = 0;
        public static final long HEAP = 1146756268035L;
        public static final int INVALID_FREE = 3;
        public static final int SCUDO = 1;
        public static final long TOOL = 1159641169921L;
        public static final long TYPE = 1159641169922L;
        public static final int UNKNOWN = 0;
        public static final int USE_AFTER_FREE = 1;

        public MemoryError() {
        }
    }

    /* loaded from: classes4.dex */
    public final class Cause {
        public static final long HUMAN_READABLE = 1138166333441L;
        public static final long MEMORY_ERROR = 1146756268034L;

        public Cause() {
        }
    }

    /* loaded from: classes4.dex */
    public final class Register {
        public static final long NAME = 1138166333441L;
        public static final long U64 = 1116691496962L;

        public Register() {
        }
    }

    /* loaded from: classes4.dex */
    public final class Thread {
        public static final long BACKTRACE_NOTE = 2237677961223L;
        public static final long CURRENT_BACKTRACE = 2246267895812L;
        public static final long ID = 1120986464257L;
        public static final long MEMORY_DUMP = 2246267895813L;
        public static final long NAME = 1138166333442L;
        public static final long PAC_ENABLED_KEYS = 1112396529672L;
        public static final long REGISTERS = 2246267895811L;
        public static final long TAGGED_ADDR_CTRL = 1112396529670L;
        public static final long UNREADABLE_ELF_FILES = 2237677961225L;

        public Thread() {
        }
    }

    /* loaded from: classes4.dex */
    public final class BacktraceFrame {
        public static final long BUILD_ID = 1138166333448L;
        public static final long FILE_MAP_OFFSET = 1116691496967L;
        public static final long FILE_NAME = 1138166333446L;
        public static final long FUNCTION_NAME = 1138166333444L;
        public static final long FUNCTION_OFFSET = 1116691496965L;
        public static final long PC = 1116691496962L;
        public static final long REL_PC = 1116691496961L;
        public static final long SP = 1116691496963L;

        public BacktraceFrame() {
        }
    }

    /* loaded from: classes4.dex */
    public final class ArmMTEMetadata {
        public static final long MEMORY_TAGS = 1151051235329L;

        public ArmMTEMetadata() {
        }
    }

    /* loaded from: classes4.dex */
    public final class MemoryDump {
        public static final long ARM_MTE_METADATA = 1146756268038L;
        public static final long BEGIN_ADDRESS = 1116691496963L;
        public static final long MAPPING_NAME = 1138166333442L;
        public static final long MEMORY = 1151051235332L;
        public static final long REGISTER_NAME = 1138166333441L;

        public MemoryDump() {
        }
    }

    /* loaded from: classes4.dex */
    public final class MemoryMapping {
        public static final long BEGIN_ADDRESS = 1116691496961L;
        public static final long BUILD_ID = 1138166333448L;
        public static final long END_ADDRESS = 1116691496962L;
        public static final long EXECUTE = 1133871366150L;
        public static final long LOAD_BIAS = 1116691496969L;
        public static final long MAPPING_NAME = 1138166333447L;
        public static final long OFFSET = 1116691496963L;
        public static final long READ = 1133871366148L;
        public static final long WRITE = 1133871366149L;

        public MemoryMapping() {
        }
    }

    /* loaded from: classes4.dex */
    public final class FD {
        public static final long FD = 1120986464257L;
        public static final long OWNER = 1138166333443L;
        public static final long PATH = 1138166333442L;
        public static final long TAG = 1116691496964L;

        public FD() {
        }
    }

    /* loaded from: classes4.dex */
    public final class LogBuffer {
        public static final long LOGS = 2246267895810L;
        public static final long NAME = 1138166333441L;

        public LogBuffer() {
        }
    }

    /* loaded from: classes4.dex */
    public final class LogMessage {
        public static final long MESSAGE = 1138166333446L;
        public static final long PID = 1155346202626L;
        public static final long PRIORITY = 1155346202628L;
        public static final long TAG = 1138166333445L;
        public static final long TID = 1155346202627L;
        public static final long TIMESTAMP = 1138166333441L;

        public LogMessage() {
        }
    }
}
