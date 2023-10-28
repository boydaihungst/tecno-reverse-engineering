package android.net.util;

import android.text.TextUtils;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.StringJoiner;
/* loaded from: classes.dex */
public class SharedLog {
    private static final String COMPONENT_DELIMITER = ".";
    private static final int DEFAULT_MAX_RECORDS = 500;
    private final String mComponent;
    private final LocalLog mLocalLog;
    private final String mTag;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public enum Category {
        NONE,
        ERROR,
        MARK,
        WARN
    }

    public SharedLog(String tag) {
        this(500, tag);
    }

    public SharedLog(int maxRecords, String tag) {
        this(new LocalLog(maxRecords), tag, tag);
    }

    private SharedLog(LocalLog localLog, String tag, String component) {
        this.mLocalLog = localLog;
        this.mTag = tag;
        this.mComponent = component;
    }

    public String getTag() {
        return this.mTag;
    }

    public SharedLog forSubComponent(String component) {
        if (!isRootLogInstance()) {
            component = this.mComponent + COMPONENT_DELIMITER + component;
        }
        return new SharedLog(this.mLocalLog, this.mTag, component);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mLocalLog.dump(writer);
    }

    public void reverseDump(PrintWriter writer) {
        this.mLocalLog.reverseDump(writer);
    }

    public void e(Exception e) {
        Log.e(this.mTag, record(Category.ERROR, e.toString()));
    }

    public void e(String msg) {
        Log.e(this.mTag, record(Category.ERROR, msg));
    }

    public void e(String msg, Throwable exception) {
        if (exception == null) {
            e(msg);
        } else {
            Log.e(this.mTag, record(Category.ERROR, msg + ": " + exception.getMessage()), exception);
        }
    }

    public void i(String msg) {
        Log.i(this.mTag, record(Category.NONE, msg));
    }

    public void w(String msg) {
        Log.w(this.mTag, record(Category.WARN, msg));
    }

    public void log(String msg) {
        record(Category.NONE, msg);
    }

    public void logf(String fmt, Object... args) {
        log(String.format(fmt, args));
    }

    public void mark(String msg) {
        record(Category.MARK, msg);
    }

    private String record(Category category, String msg) {
        String entry = logLine(category, msg);
        this.mLocalLog.append(entry);
        return entry;
    }

    private String logLine(Category category, String msg) {
        StringJoiner sj = new StringJoiner(" ");
        if (!isRootLogInstance()) {
            sj.add("[" + this.mComponent + "]");
        }
        if (category != Category.NONE) {
            sj.add(category.toString());
        }
        return sj.add(msg).toString();
    }

    private boolean isRootLogInstance() {
        return TextUtils.isEmpty(this.mComponent) || this.mComponent.equals(this.mTag);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class LocalLog {
        private final Deque<String> mLog;
        private final int mMaxLines;

        LocalLog(int maxLines) {
            int max = Math.max(0, maxLines);
            this.mMaxLines = max;
            this.mLog = new ArrayDeque(max);
        }

        synchronized void append(String logLine) {
            if (this.mMaxLines <= 0) {
                return;
            }
            while (this.mLog.size() >= this.mMaxLines) {
                this.mLog.remove();
            }
            this.mLog.add(LocalDateTime.now() + " - " + logLine);
        }

        synchronized void dump(PrintWriter pw) {
            for (String s : this.mLog) {
                pw.println(s);
            }
        }

        synchronized void reverseDump(PrintWriter pw) {
            Iterator<String> itr = this.mLog.descendingIterator();
            while (itr.hasNext()) {
                pw.println(itr.next());
            }
        }
    }
}
