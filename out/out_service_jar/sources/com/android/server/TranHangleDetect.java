package com.android.server;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Printer;
/* loaded from: classes.dex */
public class TranHangleDetect {
    private static final int START_MONITOR = 1000;
    private static final String TAG = "TranLoopMonitor";
    private static final long TIME_BLOCK = 2000;
    private HandlerThread logThread;
    private Looper mLooper;
    private PrintHandler mPrintHandler;
    private final String START = ">>>>> Dispatching";
    private final String END = "<<<<< Finished";

    public TranHangleDetect() {
        HandlerThread handlerThread = new HandlerThread("TranLoopLog");
        this.logThread = handlerThread;
        handlerThread.start();
        this.mPrintHandler = new PrintHandler(this.logThread.getLooper());
    }

    public void start(Looper looper) {
        this.mLooper = looper;
        looper.setMessageLogging(new Printer() { // from class: com.android.server.TranHangleDetect.1
            @Override // android.util.Printer
            public void println(String msgString) {
                if (msgString.startsWith(">>>>> Dispatching")) {
                    TranHangleDetect.this.startMonitor(msgString);
                }
                if (msgString.startsWith("<<<<< Finished")) {
                    TranHangleDetect.this.removeMonitor();
                }
            }
        });
    }

    public void startMonitor(String msgString) {
        Message msg = new Message();
        msg.what = 1000;
        msg.obj = msgString;
        this.mPrintHandler.sendMessageDelayed(msg, TIME_BLOCK);
    }

    public void removeMonitor() {
        this.mPrintHandler.removeMessages(1000);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class PrintHandler extends Handler {
        public PrintHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1000:
                    StringBuilder sb = new StringBuilder();
                    StackTraceElement[] stackTrace = TranHangleDetect.this.mLooper.getThread().getStackTrace();
                    for (StackTraceElement s : stackTrace) {
                        sb.append(s.toString());
                        sb.append("\n");
                    }
                    Log.e(TranHangleDetect.TAG, "Thread " + TranHangleDetect.this.mLooper.getThread().getName() + ", msg " + msg.obj + " need check!");
                    Log.e(TranHangleDetect.TAG, sb.toString());
                    return;
                default:
                    return;
            }
        }
    }
}
