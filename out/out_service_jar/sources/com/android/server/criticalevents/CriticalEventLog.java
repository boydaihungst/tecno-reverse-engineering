package com.android.server.criticalevents;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Slog;
import com.android.framework.protobuf.nano.MessageNanoPrinter;
import com.android.internal.util.RingBuffer;
import com.android.server.criticalevents.nano.CriticalEventLogProto;
import com.android.server.criticalevents.nano.CriticalEventLogStorageProto;
import com.android.server.criticalevents.nano.CriticalEventProto;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
/* loaded from: classes.dex */
public class CriticalEventLog {
    private static final int AID_SYSTEM = 1000;
    static final String FILENAME = "critical_event_log.pb";
    private static final String TAG = CriticalEventLog.class.getSimpleName();
    private static CriticalEventLog sInstance;
    private final ThreadSafeRingBuffer<CriticalEventProto> mEvents;
    private final Handler mHandler;
    private long mLastSaveAttemptMs;
    private final boolean mLoadAndSaveImmediately;
    private final File mLogFile;
    private final long mMinTimeBetweenSavesMs;
    private final Runnable mSaveRunnable;
    private final int mWindowMs;

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public interface ILogLoader {
        void load(File file, ThreadSafeRingBuffer<CriticalEventProto> threadSafeRingBuffer);
    }

    CriticalEventLog(String logDir, int capacity, int windowMs, long minTimeBetweenSavesMs, boolean loadAndSaveImmediately, final ILogLoader logLoader) {
        this.mLastSaveAttemptMs = 0L;
        this.mSaveRunnable = new Runnable() { // from class: com.android.server.criticalevents.CriticalEventLog$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CriticalEventLog.this.saveLogToFileNow();
            }
        };
        this.mLogFile = Paths.get(logDir, FILENAME).toFile();
        this.mWindowMs = windowMs;
        this.mMinTimeBetweenSavesMs = minTimeBetweenSavesMs;
        this.mLoadAndSaveImmediately = loadAndSaveImmediately;
        this.mEvents = new ThreadSafeRingBuffer<>(CriticalEventProto.class, capacity);
        HandlerThread thread = new HandlerThread("CriticalEventLogIO");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        this.mHandler = handler;
        Runnable loadEvents = new Runnable() { // from class: com.android.server.criticalevents.CriticalEventLog$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                CriticalEventLog.this.m2918lambda$new$0$comandroidservercriticaleventsCriticalEventLog(logLoader);
            }
        };
        if (!loadAndSaveImmediately) {
            handler.post(loadEvents);
        } else {
            loadEvents.run();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-criticalevents-CriticalEventLog  reason: not valid java name */
    public /* synthetic */ void m2918lambda$new$0$comandroidservercriticaleventsCriticalEventLog(ILogLoader logLoader) {
        logLoader.load(this.mLogFile, this.mEvents);
    }

    private CriticalEventLog() {
        this("/data/misc/critical-events", 20, (int) Duration.ofMinutes(5L).toMillis(), Duration.ofSeconds(2L).toMillis(), false, new LogLoader());
    }

    public static CriticalEventLog getInstance() {
        if (sInstance == null) {
            sInstance = new CriticalEventLog();
        }
        return sInstance;
    }

    public static void init() {
        getInstance();
    }

    protected long getWallTimeMillis() {
        return System.currentTimeMillis();
    }

    public void logWatchdog(String subject, UUID uuid) {
        CriticalEventProto.Watchdog watchdog = new CriticalEventProto.Watchdog();
        watchdog.subject = subject;
        watchdog.uuid = uuid.toString();
        CriticalEventProto event = new CriticalEventProto();
        event.setWatchdog(watchdog);
        log(event);
    }

    public void logHalfWatchdog(String subject) {
        CriticalEventProto.HalfWatchdog halfWatchdog = new CriticalEventProto.HalfWatchdog();
        halfWatchdog.subject = subject;
        CriticalEventProto event = new CriticalEventProto();
        event.setHalfWatchdog(halfWatchdog);
        log(event);
    }

    public void logAnr(String subject, int processClassEnum, String processName, int uid, int pid) {
        CriticalEventProto.AppNotResponding anr = new CriticalEventProto.AppNotResponding();
        anr.subject = subject;
        anr.processClass = processClassEnum;
        anr.process = processName;
        anr.uid = uid;
        anr.pid = pid;
        CriticalEventProto event = new CriticalEventProto();
        event.setAnr(anr);
        log(event);
    }

    public void logJavaCrash(String exceptionClass, int processClassEnum, String processName, int uid, int pid) {
        CriticalEventProto.JavaCrash crash = new CriticalEventProto.JavaCrash();
        crash.exceptionClass = exceptionClass;
        crash.processClass = processClassEnum;
        crash.process = processName;
        crash.uid = uid;
        crash.pid = pid;
        CriticalEventProto event = new CriticalEventProto();
        event.setJavaCrash(crash);
        log(event);
    }

    public void logNativeCrash(int processClassEnum, String processName, int uid, int pid) {
        CriticalEventProto.NativeCrash crash = new CriticalEventProto.NativeCrash();
        crash.processClass = processClassEnum;
        crash.process = processName;
        crash.uid = uid;
        crash.pid = pid;
        CriticalEventProto event = new CriticalEventProto();
        event.setNativeCrash(crash);
        log(event);
    }

    private void log(CriticalEventProto event) {
        event.timestampMs = getWallTimeMillis();
        appendAndSave(event);
    }

    void appendAndSave(CriticalEventProto event) {
        this.mEvents.append(event);
        saveLogToFile();
    }

    public String logLinesForSystemServerTraceFile() {
        return logLinesForTraceFile(3, "AID_SYSTEM", 1000);
    }

    public String logLinesForTraceFile(int traceProcessClassEnum, String traceProcessName, int traceUid) {
        CriticalEventLogProto outputLogProto = getOutputLogProto(traceProcessClassEnum, traceProcessName, traceUid);
        return "--- CriticalEventLog ---\n" + MessageNanoPrinter.print(outputLogProto) + '\n';
    }

    protected CriticalEventLogProto getOutputLogProto(int traceProcessClassEnum, String traceProcessName, int traceUid) {
        CriticalEventLogProto log = new CriticalEventLogProto();
        log.timestampMs = getWallTimeMillis();
        log.windowMs = this.mWindowMs;
        log.capacity = this.mEvents.capacity();
        CriticalEventProto[] events = recentEventsWithMinTimestamp(log.timestampMs - this.mWindowMs);
        LogSanitizer sanitizer = new LogSanitizer(traceProcessClassEnum, traceProcessName, traceUid);
        for (int i = 0; i < events.length; i++) {
            events[i] = sanitizer.process(events[i]);
        }
        log.events = events;
        return log;
    }

    private CriticalEventProto[] recentEventsWithMinTimestamp(long minTimestampMs) {
        CriticalEventProto[] allEvents = this.mEvents.toArray();
        for (int i = 0; i < allEvents.length; i++) {
            if (allEvents[i].timestampMs >= minTimestampMs) {
                return (CriticalEventProto[]) Arrays.copyOfRange(allEvents, i, allEvents.length);
            }
        }
        return new CriticalEventProto[0];
    }

    private void saveLogToFile() {
        if (this.mLoadAndSaveImmediately) {
            saveLogToFileNow();
        } else if (!this.mHandler.hasCallbacks(this.mSaveRunnable) && !this.mHandler.postDelayed(this.mSaveRunnable, saveDelayMs())) {
            Slog.w(TAG, "Error scheduling save");
        }
    }

    protected long saveDelayMs() {
        long nowMs = getWallTimeMillis();
        return Math.max(0L, (this.mLastSaveAttemptMs + this.mMinTimeBetweenSavesMs) - nowMs);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void saveLogToFileNow() {
        this.mLastSaveAttemptMs = getWallTimeMillis();
        File logDir = this.mLogFile.getParentFile();
        if (!logDir.exists() && !logDir.mkdir()) {
            Slog.e(TAG, "Error creating log directory: " + logDir.getPath());
            return;
        }
        if (!this.mLogFile.exists()) {
            try {
                this.mLogFile.createNewFile();
            } catch (IOException e) {
                Slog.e(TAG, "Error creating log file", e);
                return;
            }
        }
        CriticalEventLogStorageProto logProto = new CriticalEventLogStorageProto();
        logProto.events = this.mEvents.toArray();
        byte[] bytes = CriticalEventLogStorageProto.toByteArray(logProto);
        try {
            FileOutputStream stream = new FileOutputStream(this.mLogFile, false);
            stream.write(bytes);
            stream.close();
        } catch (IOException e2) {
            Slog.e(TAG, "Error saving log to disk.", e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static class ThreadSafeRingBuffer<T> {
        private final RingBuffer<T> mBuffer;
        private final int mCapacity;

        ThreadSafeRingBuffer(Class<T> clazz, int capacity) {
            this.mCapacity = capacity;
            this.mBuffer = new RingBuffer<>(clazz, capacity);
        }

        synchronized void append(T t) {
            this.mBuffer.append(t);
        }

        synchronized T[] toArray() {
            return (T[]) this.mBuffer.toArray();
        }

        int capacity() {
            return this.mCapacity;
        }
    }

    /* loaded from: classes.dex */
    static class LogLoader implements ILogLoader {
        LogLoader() {
        }

        @Override // com.android.server.criticalevents.CriticalEventLog.ILogLoader
        public void load(File logFile, ThreadSafeRingBuffer<CriticalEventProto> buffer) {
            CriticalEventProto[] criticalEventProtoArr;
            for (CriticalEventProto event : loadLogFromFile(logFile).events) {
                buffer.append(event);
            }
        }

        private static CriticalEventLogStorageProto loadLogFromFile(File logFile) {
            if (!logFile.exists()) {
                Slog.i(CriticalEventLog.TAG, "No log found, returning empty log proto.");
                return new CriticalEventLogStorageProto();
            }
            try {
                return CriticalEventLogStorageProto.parseFrom(Files.readAllBytes(logFile.toPath()));
            } catch (IOException e) {
                Slog.e(CriticalEventLog.TAG, "Error reading log from disk.", e);
                return new CriticalEventLogStorageProto();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class LogSanitizer {
        int mTraceProcessClassEnum;
        String mTraceProcessName;
        int mTraceUid;

        LogSanitizer(int traceProcessClassEnum, String traceProcessName, int traceUid) {
            this.mTraceProcessClassEnum = traceProcessClassEnum;
            this.mTraceProcessName = traceProcessName;
            this.mTraceUid = traceUid;
        }

        CriticalEventProto process(CriticalEventProto event) {
            if (event.hasAnr()) {
                CriticalEventProto.AppNotResponding anr = event.getAnr();
                if (shouldSanitize(anr.processClass, anr.process, anr.uid)) {
                    return sanitizeAnr(event);
                }
            } else if (event.hasJavaCrash()) {
                CriticalEventProto.JavaCrash crash = event.getJavaCrash();
                if (shouldSanitize(crash.processClass, crash.process, crash.uid)) {
                    return sanitizeJavaCrash(event);
                }
            } else if (event.hasNativeCrash()) {
                CriticalEventProto.NativeCrash crash2 = event.getNativeCrash();
                if (shouldSanitize(crash2.processClass, crash2.process, crash2.uid)) {
                    return sanitizeNativeCrash(event);
                }
            }
            return event;
        }

        private boolean shouldSanitize(int processClassEnum, String processName, int uid) {
            boolean sameApp = processName != null && processName.equals(this.mTraceProcessName) && this.mTraceUid == uid;
            return processClassEnum == 1 && this.mTraceProcessClassEnum == 1 && !sameApp;
        }

        private static CriticalEventProto sanitizeAnr(CriticalEventProto base) {
            CriticalEventProto.AppNotResponding anr = new CriticalEventProto.AppNotResponding();
            anr.processClass = base.getAnr().processClass;
            anr.uid = base.getAnr().uid;
            anr.pid = base.getAnr().pid;
            CriticalEventProto sanitized = sanitizeCriticalEventProto(base);
            sanitized.setAnr(anr);
            return sanitized;
        }

        private static CriticalEventProto sanitizeJavaCrash(CriticalEventProto base) {
            CriticalEventProto.JavaCrash crash = new CriticalEventProto.JavaCrash();
            crash.processClass = base.getJavaCrash().processClass;
            crash.uid = base.getJavaCrash().uid;
            crash.pid = base.getJavaCrash().pid;
            CriticalEventProto sanitized = sanitizeCriticalEventProto(base);
            sanitized.setJavaCrash(crash);
            return sanitized;
        }

        private static CriticalEventProto sanitizeNativeCrash(CriticalEventProto base) {
            CriticalEventProto.NativeCrash crash = new CriticalEventProto.NativeCrash();
            crash.processClass = base.getNativeCrash().processClass;
            crash.uid = base.getNativeCrash().uid;
            crash.pid = base.getNativeCrash().pid;
            CriticalEventProto sanitized = sanitizeCriticalEventProto(base);
            sanitized.setNativeCrash(crash);
            return sanitized;
        }

        private static CriticalEventProto sanitizeCriticalEventProto(CriticalEventProto base) {
            CriticalEventProto sanitized = new CriticalEventProto();
            sanitized.timestampMs = base.timestampMs;
            return sanitized;
        }
    }
}
