package com.android.server.usb;

import android.util.Log;
import com.android.internal.util.dump.DualDumpOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
/* loaded from: classes2.dex */
public class UsbDeviceLogger {
    private final int mMemSize;
    private final String mTitle;
    private final Object mLock = new Object();
    private final LinkedList<Event> mEvents = new LinkedList<>();

    /* loaded from: classes2.dex */
    public static abstract class Event {
        private static final SimpleDateFormat sFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
        private final Calendar mCalendar = Calendar.getInstance();

        public abstract String eventToString();

        Event() {
        }

        public String toString() {
            Calendar calendar = this.mCalendar;
            return String.format("%tm-%td %tH:%tM:%tS.%tL", calendar, calendar, calendar, calendar, calendar, calendar) + " " + eventToString();
        }

        public Event printLog(String tag) {
            Log.i(tag, eventToString());
            return this;
        }
    }

    /* loaded from: classes2.dex */
    public static class StringEvent extends Event {
        private final String mMsg;

        public StringEvent(String msg) {
            this.mMsg = msg;
        }

        @Override // com.android.server.usb.UsbDeviceLogger.Event
        public String eventToString() {
            return this.mMsg;
        }
    }

    public UsbDeviceLogger(int size, String title) {
        this.mMemSize = size;
        this.mTitle = title;
    }

    public synchronized void log(Event evt) {
        synchronized (this.mLock) {
            if (this.mEvents.size() >= this.mMemSize) {
                this.mEvents.removeFirst();
            }
            this.mEvents.add(evt);
        }
    }

    public synchronized void dump(DualDumpOutputStream dump, long id) {
        dump.write("USB Event Log", id, this.mTitle);
        Iterator<Event> it = this.mEvents.iterator();
        while (it.hasNext()) {
            Event evt = it.next();
            dump.write("USB Event", id, evt.toString());
        }
    }
}
