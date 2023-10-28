package com.android.server.pm;

import android.os.Trace;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.pm.parsing.PackageParser2;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import java.io.File;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ParallelPackageParser {
    private static final int MAX_THREADS = 4;
    private static final int QUEUE_CAPACITY = 30;
    private final ExecutorService mExecutorService;
    private final List<File> mFrameworkSplits;
    private volatile String mInterruptedInThread;
    private final PackageParser2 mPackageParser;
    private final BlockingQueue<ParseResult> mQueue;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ExecutorService makeExecutorService() {
        return ConcurrentUtils.newFixedThreadPool(4, "package-parsing-thread", -2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ParallelPackageParser(PackageParser2 packageParser, ExecutorService executorService) {
        this(packageParser, executorService, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ParallelPackageParser(PackageParser2 packageParser, ExecutorService executorService, List<File> frameworkSplits) {
        this.mQueue = new ArrayBlockingQueue(30);
        this.mPackageParser = packageParser;
        this.mExecutorService = executorService;
        this.mFrameworkSplits = frameworkSplits;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class ParseResult {
        ParsedPackage parsedPackage;
        File scanFile;
        Throwable throwable;

        ParseResult() {
        }

        public String toString() {
            return "ParseResult{parsedPackage=" + this.parsedPackage + ", scanFile=" + this.scanFile + ", throwable=" + this.throwable + '}';
        }
    }

    public ParseResult take() {
        try {
            if (this.mInterruptedInThread != null) {
                throw new InterruptedException("Interrupted in " + this.mInterruptedInThread);
            }
            return this.mQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public void submit(final File scanFile, final int parseFlags) {
        this.mExecutorService.submit(new Runnable() { // from class: com.android.server.pm.ParallelPackageParser$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ParallelPackageParser.this.m5591lambda$submit$0$comandroidserverpmParallelPackageParser(scanFile, parseFlags);
            }
        });
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE, MOVE_EXCEPTION, IPUT, INVOKE, MOVE_EXCEPTION] complete} */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$submit$0$com-android-server-pm-ParallelPackageParser  reason: not valid java name */
    public /* synthetic */ void m5591lambda$submit$0$comandroidserverpmParallelPackageParser(File scanFile, int parseFlags) {
        ParseResult pr = new ParseResult();
        Trace.traceBegin(262144L, "parallel parsePackage [" + scanFile + "]");
        try {
            try {
                pr.scanFile = scanFile;
                pr.parsedPackage = parsePackage(scanFile, parseFlags);
            } finally {
                try {
                    this.mQueue.put(pr);
                } finally {
                }
            }
            this.mQueue.put(pr);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.mInterruptedInThread = Thread.currentThread().getName();
        }
    }

    protected ParsedPackage parsePackage(File scanFile, int parseFlags) throws PackageManagerException {
        return this.mPackageParser.parsePackage(scanFile, parseFlags, true, this.mFrameworkSplits);
    }
}
