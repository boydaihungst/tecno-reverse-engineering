package com.mediatek.anr;

import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.Printer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
/* loaded from: classes.dex */
public class MessageLogger implements Printer {
    static final int LONGER_TIME = 200;
    static final int LONGER_TIME_MESSAGE_COUNT = 20;
    static final int MESSAGE_COUNT = 20;
    private static final int MESSAGE_DUMP_SIZE_MAX = 20;
    private static final String TAG = "MessageLogger";
    public static boolean mEnableLooperLog = false;
    private static Method sGetCurrentTimeMicro = getSystemClockMethod("currentTimeMicro");
    private String MSL_Warn;
    private Method mGetMessageQueue;
    private String mLastRecord;
    private long mLastRecordDateTime;
    private long mLastRecordKernelTime;
    private CircularMessageInfoArray mLongTimeMessageHistory;
    private Field mMessageField;
    private CircularMessageInfoArray mMessageHistory;
    private Field mMessageQueueField;
    private long mMsgCnt;
    private String mName;
    private long mNonSleepLastRecordKernelTime;
    private long mProcessId;
    private int mState;
    private StringBuilder messageInfo;
    public long nonSleepWallStart;
    public long nonSleepWallTime;
    private String sInstNotCreated;
    public long wallStart;
    public long wallTime;

    private static Method getSystemClockMethod(String func) {
        try {
            Class<?> systemClock = Class.forName("android.os.SystemClock");
            return systemClock.getDeclaredMethod(func, new Class[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private Method getLooperMethod(String func) {
        try {
            Class<?> looper = Class.forName("android.os.Looper");
            return looper.getDeclaredMethod(func, new Class[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private Field getMessageQueueField(String var) {
        try {
            Class<?> messageQueue = Class.forName("android.os.MessageQueue");
            Field field = messageQueue.getDeclaredField(var);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            return null;
        }
    }

    private Field getMessageField(String var) {
        try {
            Class<?> message = Class.forName("android.os.Message");
            Field field = message.getDeclaredField(var);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            return null;
        }
    }

    public MessageLogger() {
        this.mLastRecord = null;
        this.mState = 0;
        this.mMsgCnt = 0L;
        this.mName = null;
        this.MSL_Warn = "MSL Waraning:";
        this.sInstNotCreated = this.MSL_Warn + "!!! MessageLoggerInstance might not be created !!!\n";
        this.mGetMessageQueue = getLooperMethod("getQueue");
        this.mMessageQueueField = getMessageQueueField("mMessages");
        this.mMessageField = getMessageField("next");
        init();
    }

    public MessageLogger(boolean mValue) {
        this.mLastRecord = null;
        this.mState = 0;
        this.mMsgCnt = 0L;
        this.mName = null;
        this.MSL_Warn = "MSL Waraning:";
        this.sInstNotCreated = this.MSL_Warn + "!!! MessageLoggerInstance might not be created !!!\n";
        this.mGetMessageQueue = getLooperMethod("getQueue");
        this.mMessageQueueField = getMessageQueueField("mMessages");
        this.mMessageField = getMessageField("next");
        mEnableLooperLog = mValue;
        init();
    }

    public MessageLogger(boolean mValue, String Name) {
        this.mLastRecord = null;
        this.mState = 0;
        this.mMsgCnt = 0L;
        this.mName = null;
        this.MSL_Warn = "MSL Waraning:";
        this.sInstNotCreated = this.MSL_Warn + "!!! MessageLoggerInstance might not be created !!!\n";
        this.mGetMessageQueue = getLooperMethod("getQueue");
        this.mMessageQueueField = getMessageQueueField("mMessages");
        this.mMessageField = getMessageField("next");
        this.mName = Name;
        mEnableLooperLog = mValue;
        init();
    }

    private void init() {
        this.mMessageHistory = new CircularMessageInfoArray(20);
        this.mLongTimeMessageHistory = new CircularMessageInfoArray(20);
        this.messageInfo = new StringBuilder(20480);
        this.mProcessId = Process.myPid();
    }

    @Override // android.util.Printer
    public void println(String s) {
        synchronized (this) {
            this.mState++;
            this.mMsgCnt++;
            this.mLastRecordKernelTime = SystemClock.elapsedRealtime();
            this.mNonSleepLastRecordKernelTime = SystemClock.uptimeMillis();
            try {
                Method method = sGetCurrentTimeMicro;
                if (method != null) {
                    this.mLastRecordDateTime = ((Long) method.invoke(null, new Object[0])).longValue();
                }
            } catch (Exception e) {
            }
            if (this.mState == 1) {
                MessageInfo msgInfo = this.mMessageHistory.add();
                msgInfo.init();
                msgInfo.startDispatch = s;
                msgInfo.msgIdStart = this.mMsgCnt;
                msgInfo.startTimeElapsed = this.mLastRecordDateTime;
                msgInfo.startTimeUp = this.mNonSleepLastRecordKernelTime;
            } else {
                this.mState = 0;
                MessageInfo msgInfo2 = this.mMessageHistory.getLast();
                msgInfo2.finishDispatch = s;
                msgInfo2.msgIdFinish = this.mMsgCnt;
                msgInfo2.durationElapsed = this.mLastRecordDateTime - msgInfo2.startTimeElapsed;
                msgInfo2.durationUp = this.mNonSleepLastRecordKernelTime - msgInfo2.startTimeUp;
                this.wallTime = msgInfo2.durationElapsed;
                if (msgInfo2.durationElapsed >= 200000) {
                    MessageInfo longMsgInfo = this.mLongTimeMessageHistory.add();
                    longMsgInfo.copy(msgInfo2);
                }
            }
            if (mEnableLooperLog) {
                if (this.mState == 1) {
                    Log.d(TAG, "Debugging_MessageLogger: " + s + " start");
                } else {
                    Log.d(TAG, "Debugging_MessageLogger: " + s + " spent " + (this.wallTime / 1000) + "ms");
                }
            }
        }
    }

    public void setInitStr(String str_tmp) {
        StringBuilder sb = this.messageInfo;
        sb.delete(0, sb.length());
        this.messageInfo.append(str_tmp);
    }

    private void log(String info) {
        this.messageInfo.append(info).append("\n");
    }

    public void dumpMessageQueue() {
        try {
            Looper looper = Looper.getMainLooper();
            if (looper == null) {
                log(this.MSL_Warn + "!!! Current MainLooper is Null !!!");
            } else {
                MessageQueue messageQueue = (MessageQueue) this.mGetMessageQueue.invoke(looper, new Object[0]);
                if (messageQueue == null) {
                    log(this.MSL_Warn + "!!! Current MainLooper's MsgQueue is Null !!!");
                } else {
                    dumpMessageQueueImpl(messageQueue);
                }
            }
        } catch (Exception e) {
        }
        log(String.format(this.MSL_Warn + "!!! Calling thread from PID:%d's TID:%d(%s),Thread's type is %s!!!", Integer.valueOf(Process.myPid()), Long.valueOf(Thread.currentThread().getId()), Thread.currentThread().getName(), Thread.currentThread().getClass().getName()));
        StackTraceElement[] stkTrace = Thread.currentThread().getStackTrace();
        log(String.format(this.MSL_Warn + "!!! get StackTrace: !!!", new Object[0]));
        for (int index = 0; index < stkTrace.length; index++) {
            log(String.format(this.MSL_Warn + "File:%s's Linenumber:%d, Class:%s, Method:%s", stkTrace[index].getFileName(), Integer.valueOf(stkTrace[index].getLineNumber()), stkTrace[index].getClassName(), stkTrace[index].getMethodName()));
        }
    }

    public void dumpMessageQueueImpl(MessageQueue messageQueue) throws Exception {
        synchronized (messageQueue) {
            Message mMessages = null;
            Field field = this.mMessageQueueField;
            if (field != null) {
                mMessages = (Message) field.get(messageQueue);
            }
            if (mMessages != null) {
                log("Dump first 20 messages in Queue: ");
                Message message = mMessages;
                int count = 0;
                while (message != null) {
                    count++;
                    if (count <= 20) {
                        log("Dump Message in Queue (" + count + "): " + message);
                    }
                    message = (Message) this.mMessageField.get(message);
                }
                log("Total Message Count: " + count);
            } else {
                log("mMessages is null");
            }
        }
    }

    public void dumpMessageHistory() {
        synchronized (this) {
            log(">>> Entering MessageLogger.dump. to Dump MSG HISTORY <<<");
            CircularMessageInfoArray circularMessageInfoArray = this.mMessageHistory;
            if (circularMessageInfoArray != null && circularMessageInfoArray.size() != 0) {
                log("MSG HISTORY IN MAIN THREAD:");
                log("Current kernel time : " + SystemClock.uptimeMillis() + "ms PID=" + this.mProcessId);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                int msgIdx = this.mMessageHistory.size() - 1;
                if (this.mState == 1) {
                    Date date = new Date(this.mLastRecordDateTime / 1000);
                    long spent = SystemClock.elapsedRealtime() - this.mLastRecordKernelTime;
                    long nonSleepSpent = SystemClock.uptimeMillis() - this.mNonSleepLastRecordKernelTime;
                    MessageInfo msgInfo = this.mMessageHistory.getLast();
                    log("Last record : Msg#:" + msgInfo.msgIdStart + " " + msgInfo.startDispatch);
                    log("Last record dispatching elapsedTime:" + spent + " ms/upTime:" + nonSleepSpent + " ms");
                    log("Last record dispatching time : " + simpleDateFormat.format(date));
                    msgIdx--;
                }
                while (msgIdx >= 0) {
                    MessageInfo info = this.mMessageHistory.get(msgIdx);
                    if (info != null) {
                        Date date2 = new Date(info.startTimeElapsed / 1000);
                        log("Msg#:" + info.msgIdFinish + " " + info.finishDispatch + " elapsedTime:" + (info.durationElapsed / 1000) + " ms/upTime:" + info.durationUp + " ms");
                        log("Msg#:" + info.msgIdStart + " " + info.startDispatch + " from " + simpleDateFormat.format(date2));
                    }
                    msgIdx--;
                }
                log("=== Finish Dumping MSG HISTORY===");
                log("=== LONGER MSG HISTORY IN MAIN THREAD ===");
                for (int msgIdx2 = this.mLongTimeMessageHistory.size() - 1; msgIdx2 >= 0; msgIdx2--) {
                    MessageInfo info2 = this.mLongTimeMessageHistory.get(msgIdx2);
                    if (info2 != null) {
                        Date date3 = new Date(info2.startTimeElapsed / 1000);
                        log("Msg#:" + info2.msgIdStart + " " + info2.startDispatch + " from " + simpleDateFormat.format(date3) + " elapsedTime:" + (info2.durationElapsed / 1000) + " ms/upTime:" + info2.durationUp + "ms");
                    }
                }
                log("=== Finish Dumping LONGER MSG HISTORY===");
                try {
                    dumpMessageQueue();
                    AnrManagerNative.getDefault().informMessageDump(new String(this.messageInfo.toString()), Process.myPid());
                    StringBuilder sb = this.messageInfo;
                    sb.delete(0, sb.length());
                } catch (RemoteException ex) {
                    Log.d(TAG, "informMessageDump exception " + ex);
                }
                return;
            }
            log(this.sInstNotCreated);
            dumpMessageQueue();
            try {
                AnrManagerNative.getDefault().informMessageDump(this.messageInfo.toString(), Process.myPid());
            } catch (RemoteException ex2) {
                Log.d(TAG, "informMessageDump exception " + ex2);
            }
        }
    }

    /* loaded from: classes.dex */
    public class MessageInfo {
        public long durationElapsed;
        public long durationUp;
        public String finishDispatch;
        public long msgIdFinish;
        public long msgIdStart;
        public String startDispatch;
        public long startTimeElapsed;
        public long startTimeUp;

        public MessageInfo() {
            init();
        }

        public void init() {
            this.startDispatch = null;
            this.finishDispatch = null;
            this.msgIdStart = -1L;
            this.msgIdFinish = -1L;
            this.startTimeUp = 0L;
            this.durationUp = -1L;
            this.startTimeElapsed = 0L;
            this.durationElapsed = -1L;
        }

        public void copy(MessageInfo info) {
            this.startDispatch = info.startDispatch;
            this.finishDispatch = info.finishDispatch;
            this.msgIdStart = info.msgIdStart;
            this.msgIdFinish = info.msgIdFinish;
            this.startTimeUp = info.startTimeUp;
            this.durationUp = info.durationUp;
            this.startTimeElapsed = info.startTimeElapsed;
            this.durationElapsed = info.durationElapsed;
        }
    }

    /* loaded from: classes.dex */
    public class CircularMessageInfoArray {
        private MessageInfo[] mElem;
        private int mHead;
        private MessageInfo mLastElem;
        private int mSize;
        private int mTail;

        public CircularMessageInfoArray(int size) {
            int capacity = size + 1;
            this.mElem = new MessageInfo[capacity];
            for (int i = 0; i < capacity; i++) {
                this.mElem[i] = new MessageInfo();
            }
            this.mHead = 0;
            this.mTail = 0;
            this.mLastElem = null;
            this.mSize = capacity;
        }

        public boolean empty() {
            return this.mHead == this.mTail || this.mElem == null;
        }

        public boolean full() {
            int i = this.mTail;
            int i2 = this.mHead;
            return i == i2 + (-1) || i - i2 == this.mSize - 1;
        }

        public int size() {
            int i = this.mTail;
            int i2 = this.mHead;
            if (i - i2 >= 0) {
                return i - i2;
            }
            return (this.mSize + i) - i2;
        }

        private MessageInfo getLocked(int n) {
            int i = this.mHead;
            int i2 = i + n;
            int i3 = this.mSize;
            if (i2 <= i3 - 1) {
                return this.mElem[i + n];
            }
            return this.mElem[(i + n) - i3];
        }

        public synchronized MessageInfo get(int n) {
            if (n >= 0) {
                if (n < size()) {
                    return getLocked(n);
                }
            }
            return null;
        }

        public synchronized MessageInfo getLast() {
            return this.mLastElem;
        }

        public synchronized MessageInfo add() {
            MessageInfo messageInfo;
            if (full()) {
                int i = this.mHead + 1;
                this.mHead = i;
                if (i == this.mSize) {
                    this.mHead = 0;
                }
            }
            MessageInfo[] messageInfoArr = this.mElem;
            int i2 = this.mTail;
            messageInfo = messageInfoArr[i2];
            this.mLastElem = messageInfo;
            int i3 = i2 + 1;
            this.mTail = i3;
            if (i3 == this.mSize) {
                this.mTail = 0;
            }
            return messageInfo;
        }
    }
}
