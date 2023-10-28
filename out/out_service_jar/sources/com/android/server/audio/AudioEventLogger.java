package com.android.server.audio;

import android.util.Log;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
/* loaded from: classes.dex */
public class AudioEventLogger {
    private final LinkedList<Event> mEvents = new LinkedList<>();
    private final int mMemSize;
    private final String mTitle;

    /* loaded from: classes.dex */
    public static abstract class Event {
        public static final int ALOGE = 1;
        public static final int ALOGI = 0;
        public static final int ALOGV = 3;
        public static final int ALOGW = 2;
        private static final SimpleDateFormat sFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
        private final long mTimestamp = System.currentTimeMillis();

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        public @interface LogType {
        }

        public abstract String eventToString();

        public String toString() {
            return sFormat.format(new Date(this.mTimestamp)) + " " + eventToString();
        }

        public Event printLog(String tag) {
            return printLog(0, tag);
        }

        public Event printLog(int type, String tag) {
            switch (type) {
                case 0:
                    Log.i(tag, eventToString());
                    break;
                case 1:
                    Log.e(tag, eventToString());
                    break;
                case 2:
                    Log.w(tag, eventToString());
                    break;
                default:
                    Log.v(tag, eventToString());
                    break;
            }
            return this;
        }
    }

    /* loaded from: classes.dex */
    public static class StringEvent extends Event {
        private final String mMsg;

        public StringEvent(String msg) {
            this.mMsg = msg;
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            return this.mMsg;
        }
    }

    public AudioEventLogger(int size, String title) {
        this.mMemSize = size;
        this.mTitle = title;
    }

    public synchronized void log(Event evt) {
        if (this.mEvents.size() >= this.mMemSize) {
            this.mEvents.removeFirst();
        }
        this.mEvents.add(evt);
    }

    public synchronized void loglogi(String msg, String tag) {
        Event event = new StringEvent(msg);
        log(event.printLog(tag));
    }

    public synchronized void loglog(String msg, int logType, String tag) {
        Event event = new StringEvent(msg);
        log(event.printLog(logType, tag));
    }

    public synchronized void dump(PrintWriter pw) {
        pw.println("Audio event log: " + this.mTitle);
        Iterator<Event> it = this.mEvents.iterator();
        while (it.hasNext()) {
            Event evt = it.next();
            pw.println(evt.toString());
        }
    }
}
