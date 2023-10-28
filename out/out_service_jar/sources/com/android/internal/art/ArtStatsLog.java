package com.android.internal.art;

import android.util.StatsEvent;
import android.util.StatsLog;
/* loaded from: classes.dex */
public class ArtStatsLog {
    public static final byte ANNOTATION_ID_EXCLUSIVE_STATE = 4;
    public static final byte ANNOTATION_ID_IS_UID = 1;
    public static final byte ANNOTATION_ID_PRIMARY_FIELD = 3;
    public static final byte ANNOTATION_ID_PRIMARY_FIELD_FIRST_UID = 5;
    public static final byte ANNOTATION_ID_STATE_NESTED = 8;
    public static final byte ANNOTATION_ID_TRIGGER_STATE_RESET = 7;
    public static final byte ANNOTATION_ID_TRUNCATE_TIMESTAMP = 2;
    public static final int ART_DATUM_REPORTED = 332;
    public static final int ART_DATUM_REPORTED__APK_TYPE__ART_APK_TYPE_BASE = 1;
    public static final int ART_DATUM_REPORTED__APK_TYPE__ART_APK_TYPE_SPLIT = 2;
    public static final int ART_DATUM_REPORTED__APK_TYPE__ART_APK_TYPE_UNKNOWN = 0;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_AB_OTA = 7;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_BG_DEXOPT = 6;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_BOOT = 4;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_BOOT_AFTER_OTA = 17;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_CMDLINE = 19;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_ERROR = 1;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_FIRST_BOOT = 3;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_INACTIVE = 8;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_INSTALL = 5;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_INSTALL_BULK = 13;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_INSTALL_BULK_DOWNGRADED = 15;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_INSTALL_BULK_SECONDARY = 14;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_INSTALL_BULK_SECONDARY_DOWNGRADED = 16;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_INSTALL_FAST = 12;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_INSTALL_WITH_DEX_METADATA = 10;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_POST_BOOT = 11;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_PREBUILT = 18;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_SHARED = 9;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_UNKNOWN = 2;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_UNSPECIFIED = 0;
    public static final int ART_DATUM_REPORTED__COMPILATION_REASON__ART_COMPILATION_REASON_VDEX = 20;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_ASSUMED_VERIFIED = 3;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_ERROR = 1;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_EVERYTHING = 12;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_EVERYTHING_PROFILE = 11;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_EXTRACT = 4;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_FAKE_RUN_FROM_APK = 13;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_FAKE_RUN_FROM_APK_FALLBACK = 14;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_FAKE_RUN_FROM_VDEX_FALLBACK = 15;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_QUICKEN = 6;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_SPACE = 8;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_SPACE_PROFILE = 7;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_SPEED = 10;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_SPEED_PROFILE = 9;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_UNKNOWN = 2;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_UNSPECIFIED = 0;
    public static final int ART_DATUM_REPORTED__COMPILE_FILTER__ART_COMPILATION_FILTER_VERIFY = 5;
    public static final int ART_DATUM_REPORTED__DEX_METADATA_TYPE__ART_DEX_METADATA_TYPE_ERROR = 5;
    public static final int ART_DATUM_REPORTED__DEX_METADATA_TYPE__ART_DEX_METADATA_TYPE_NONE = 4;
    public static final int ART_DATUM_REPORTED__DEX_METADATA_TYPE__ART_DEX_METADATA_TYPE_PROFILE = 1;
    public static final int ART_DATUM_REPORTED__DEX_METADATA_TYPE__ART_DEX_METADATA_TYPE_PROFILE_AND_VDEX = 3;
    public static final int ART_DATUM_REPORTED__DEX_METADATA_TYPE__ART_DEX_METADATA_TYPE_UNKNOWN = 0;
    public static final int ART_DATUM_REPORTED__DEX_METADATA_TYPE__ART_DEX_METADATA_TYPE_VDEX = 2;
    public static final int ART_DATUM_REPORTED__ISA__ART_ISA_ARM = 1;
    public static final int ART_DATUM_REPORTED__ISA__ART_ISA_ARM64 = 2;
    public static final int ART_DATUM_REPORTED__ISA__ART_ISA_MIPS = 5;
    public static final int ART_DATUM_REPORTED__ISA__ART_ISA_MIPS64 = 6;
    public static final int ART_DATUM_REPORTED__ISA__ART_ISA_UNKNOWN = 0;
    public static final int ART_DATUM_REPORTED__ISA__ART_ISA_X86 = 3;
    public static final int ART_DATUM_REPORTED__ISA__ART_ISA_X86_64 = 4;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_AOT_COMPILE_TIME = 7;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_CLASS_LOADING_TIME_COUNTER_MICROS = 9;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_CLASS_VERIFICATION_COUNT = 16;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_CLASS_VERIFICATION_TIME_COUNTER_MICROS = 8;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_DEX2OAT_DEX_CODE_COUNTER_BYTES = 11;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_DEX2OAT_FAST_VERIFY_TIME_COUNTER_MILLIS = 14;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_DEX2OAT_RESOLVE_METHODS_AND_FIELDS_TIME_COUNTER_MILLIS = 15;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_DEX2OAT_RESULT_CODE = 10;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_DEX2OAT_TOTAL_TIME_COUNTER_MILLIS = 12;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_DEX2OAT_VERIFY_DEX_FILE_TIME_COUNTER_MILLIS = 13;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_FULL_HEAP_COLLECTION_COUNT = 5;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_FULL_HEAP_COLLECTION_THROUGHPUT_AVG_MB_PER_SEC = 25;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_FULL_HEAP_COLLECTION_THROUGHPUT_HISTO_MB_PER_SEC = 20;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_FULL_HEAP_COLLECTION_TIME_HISTO_MILLIS = 4;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_FULL_HEAP_TRACING_THROUGHPUT_AVG_MB_PER_SEC = 27;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_FULL_HEAP_TRACING_THROUGHPUT_HISTO_MB_PER_SEC = 23;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_TOTAL_BYTES_ALLOCATED = 17;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_TOTAL_COLLECTION_TIME_MS = 28;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_TOTAL_METADATA_SIZE_BYTES = 18;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_WORLD_STOP_TIME_AVG_MICROS = 1;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_YOUNG_GENERATION_COLLECTION_COUNT = 3;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_YOUNG_GENERATION_COLLECTION_THROUGHPUT_AVG_MB_PER_SEC = 24;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_YOUNG_GENERATION_COLLECTION_THROUGHPUT_HISTO_MB_PER_SEC = 19;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_YOUNG_GENERATION_COLLECTION_TIME_HISTO_MILLIS = 2;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_YOUNG_GENERATION_TRACING_THROUGHPUT_AVG_MB_PER_SEC = 26;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_GC_YOUNG_GENERATION_TRACING_THROUGHPUT_HISTO_MB_PER_SEC = 22;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_INVALID = 0;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_JIT_METHOD_COMPILE_COUNT = 21;
    public static final int ART_DATUM_REPORTED__KIND__ART_DATUM_JIT_METHOD_COMPILE_TIME_MICROS = 6;
    public static final int ART_DATUM_REPORTED__THREAD_TYPE__ART_THREAD_BACKGROUND = 2;
    public static final int ART_DATUM_REPORTED__THREAD_TYPE__ART_THREAD_MAIN = 1;
    public static final int ART_DATUM_REPORTED__THREAD_TYPE__ART_THREAD_UNKNOWN = 0;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED = 467;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_APP_STANDBY = 12;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_BACKGROUND_RESTRICTION = 11;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_CANCELLED_BY_APP = 1;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW = 5;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_CONSTRAINT_CHARGING = 6;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_CONSTRAINT_CONNECTIVITY = 7;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_CONSTRAINT_DEVICE_IDLE = 8;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW = 9;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_DEVICE_STATE = 4;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_PREEMPT = 2;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_QUOTA = 10;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_SYSTEM_PROCESSING = 14;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_TIMEOUT = 3;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_UNDEFINED = 0;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__CANCELLATION_REASON__STOP_REASON_USER = 13;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__STATUS__STATUS_ABORT_BATTERY = 5;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__STATUS__STATUS_ABORT_BY_CANCELLATION = 2;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__STATUS__STATUS_ABORT_NO_SPACE_LEFT = 3;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__STATUS__STATUS_ABORT_THERMAL = 4;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__STATUS__STATUS_JOB_FINISHED = 1;
    public static final int BACKGROUND_DEXOPT_JOB_ENDED__STATUS__STATUS_UNKNOWN = 0;
    public static final int EARLY_BOOT_COMP_OS_ARTIFACTS_CHECK_REPORTED = 419;
    public static final int ISOLATED_COMPILATION_ENDED = 458;
    public static final int ISOLATED_COMPILATION_ENDED__COMPILATION_RESULT__RESULT_COMPILATION_FAILED = 5;
    public static final int ISOLATED_COMPILATION_ENDED__COMPILATION_RESULT__RESULT_COMPOSD_DIED = 7;
    public static final int ISOLATED_COMPILATION_ENDED__COMPILATION_RESULT__RESULT_FAILED_TO_START = 3;
    public static final int ISOLATED_COMPILATION_ENDED__COMPILATION_RESULT__RESULT_JOB_CANCELED = 4;
    public static final int ISOLATED_COMPILATION_ENDED__COMPILATION_RESULT__RESULT_SUCCESS = 1;
    public static final int ISOLATED_COMPILATION_ENDED__COMPILATION_RESULT__RESULT_UNEXPECTED_COMPILATION_RESULT = 6;
    public static final int ISOLATED_COMPILATION_ENDED__COMPILATION_RESULT__RESULT_UNKNOWN = 0;
    public static final int ISOLATED_COMPILATION_ENDED__COMPILATION_RESULT__RESULT_UNKNOWN_FAILURE = 2;
    public static final int ISOLATED_COMPILATION_SCHEDULED = 457;
    public static final int ISOLATED_COMPILATION_SCHEDULED__SCHEDULING_RESULT__SCHEDULING_FAILURE = 1;
    public static final int ISOLATED_COMPILATION_SCHEDULED__SCHEDULING_RESULT__SCHEDULING_RESULT_UNKNOWN = 0;
    public static final int ISOLATED_COMPILATION_SCHEDULED__SCHEDULING_RESULT__SCHEDULING_SUCCESS = 2;
    public static final int ODREFRESH_REPORTED = 366;
    public static final int ODREFRESH_REPORTED__STAGE_REACHED__STAGE_CHECK = 10;
    public static final int ODREFRESH_REPORTED__STAGE_REACHED__STAGE_COMPLETE = 60;
    public static final int ODREFRESH_REPORTED__STAGE_REACHED__STAGE_PREPARATION = 20;
    public static final int ODREFRESH_REPORTED__STAGE_REACHED__STAGE_PRIMARY_BOOT_CLASSPATH = 30;
    public static final int ODREFRESH_REPORTED__STAGE_REACHED__STAGE_SECONDARY_BOOT_CLASSPATH = 40;
    public static final int ODREFRESH_REPORTED__STAGE_REACHED__STAGE_SYSTEM_SERVER_CLASSPATH = 50;
    public static final int ODREFRESH_REPORTED__STAGE_REACHED__STAGE_UNKNOWN = 0;
    public static final int ODREFRESH_REPORTED__STATUS__STATUS_DEX2OAT_ERROR = 4;
    public static final int ODREFRESH_REPORTED__STATUS__STATUS_INSTALL_FAILED = 7;
    public static final int ODREFRESH_REPORTED__STATUS__STATUS_IO_ERROR = 3;
    public static final int ODREFRESH_REPORTED__STATUS__STATUS_NO_SPACE = 2;
    public static final int ODREFRESH_REPORTED__STATUS__STATUS_OK = 1;
    public static final int ODREFRESH_REPORTED__STATUS__STATUS_STAGING_FAILED = 6;
    public static final int ODREFRESH_REPORTED__STATUS__STATUS_TIME_LIMIT_EXCEEDED = 5;
    public static final int ODREFRESH_REPORTED__STATUS__STATUS_UNKNOWN = 0;
    public static final int ODREFRESH_REPORTED__TRIGGER__TRIGGER_APEX_VERSION_MISMATCH = 1;
    public static final int ODREFRESH_REPORTED__TRIGGER__TRIGGER_DEX_FILES_CHANGED = 2;
    public static final int ODREFRESH_REPORTED__TRIGGER__TRIGGER_MISSING_ARTIFACTS = 3;
    public static final int ODREFRESH_REPORTED__TRIGGER__TRIGGER_UNKNOWN = 0;

    public static void write(int code, boolean arg1, boolean arg2, boolean arg3) {
        StatsEvent.Builder builder = StatsEvent.newBuilder();
        builder.setAtomId(code);
        builder.writeBoolean(arg1);
        builder.writeBoolean(arg2);
        builder.writeBoolean(arg3);
        builder.usePooledBuffer();
        StatsLog.write(builder.build());
    }

    public static void write(int code, int arg1) {
        StatsEvent.Builder builder = StatsEvent.newBuilder();
        builder.setAtomId(code);
        builder.writeInt(arg1);
        builder.usePooledBuffer();
        StatsLog.write(builder.build());
    }

    public static void write(int code, int arg1, int arg2, long arg3, long arg4) {
        StatsEvent.Builder builder = StatsEvent.newBuilder();
        builder.setAtomId(code);
        builder.writeInt(arg1);
        builder.writeInt(arg2);
        builder.writeLong(arg3);
        builder.writeLong(arg4);
        builder.usePooledBuffer();
        StatsLog.write(builder.build());
    }

    public static void write(int code, long arg1, int arg2) {
        StatsEvent.Builder builder = StatsEvent.newBuilder();
        builder.setAtomId(code);
        builder.writeLong(arg1);
        builder.writeInt(arg2);
        builder.usePooledBuffer();
        StatsLog.write(builder.build());
    }

    public static void write(int code, long arg1, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8, int arg9) {
        StatsEvent.Builder builder = StatsEvent.newBuilder();
        builder.setAtomId(code);
        builder.writeLong(arg1);
        builder.writeInt(arg2);
        builder.writeInt(arg3);
        builder.writeInt(arg4);
        builder.writeInt(arg5);
        builder.writeInt(arg6);
        builder.writeInt(arg7);
        builder.writeInt(arg8);
        builder.writeInt(arg9);
        builder.usePooledBuffer();
        StatsLog.write(builder.build());
    }

    public static void write(int code, long arg1, int arg2, int arg3, int arg4, long arg5, int arg6, int arg7, long arg8, int arg9, int arg10, int arg11) {
        StatsEvent.Builder builder = StatsEvent.newBuilder();
        builder.setAtomId(code);
        builder.writeLong(arg1);
        builder.writeInt(arg2);
        if (332 == code) {
            builder.addBooleanAnnotation((byte) 1, true);
        }
        builder.writeInt(arg3);
        builder.writeInt(arg4);
        builder.writeLong(arg5);
        builder.writeInt(arg6);
        builder.writeInt(arg7);
        builder.writeLong(arg8);
        builder.writeInt(arg9);
        builder.writeInt(arg10);
        builder.writeInt(arg11);
        builder.usePooledBuffer();
        StatsLog.write(builder.build());
    }
}
