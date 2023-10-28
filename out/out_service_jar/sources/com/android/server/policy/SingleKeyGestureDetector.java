package com.android.server.policy;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import com.transsion.tne.TNEService;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes2.dex */
public final class SingleKeyGestureDetector {
    private static final boolean DEBUG;
    public static final int KEY_LONGPRESS = 2;
    public static final int KEY_VERYLONGPRESS = 4;
    private static final int MSG_KEY_DELAYED_PRESS = 2;
    private static final int MSG_KEY_LONG_PRESS = 0;
    private static final int MSG_KEY_VERY_LONG_PRESS = 1;
    private static final int MSG_KEY_VERY_VERY_LONG_PRESS = 3;
    static final long MULTI_PRESS_TIMEOUT;
    private static final String TAG = "SingleKeyGesture";
    static long sDefaultLongPressTimeout;
    static long sDefaultVeryLongPressTimeout;
    private int mKeyPressCounter;
    private boolean mBeganFromNonInteractive = false;
    private final ArrayList<SingleKeyRule> mRules = new ArrayList<>();
    private SingleKeyRule mActiveRule = null;
    private int mDownKeyCode = 0;
    private volatile boolean mHandledByLongPress = false;
    private long mLastDownTime = 0;
    private final long OS_SHUTDOWN_MESSAGE_DELAY = 1800;
    private final long START_TNE_MESSAGE_DELAY = 5000;
    private final Handler mHandler = new KeyHandler();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface KeyGestureFlag {
    }

    static {
        DEBUG = PhoneWindowManager.DEBUG_INPUT || "1".equals(SystemProperties.get("persist.sys.adb.support", "0"));
        MULTI_PRESS_TIMEOUT = ViewConfiguration.getMultiPressTimeout();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static abstract class SingleKeyRule {
        private final int mKeyCode;
        private final int mSupportedGestures;

        abstract void onPress(long j);

        /* JADX INFO: Access modifiers changed from: package-private */
        public SingleKeyRule(int keyCode, int supportedGestures) {
            this.mKeyCode = keyCode;
            this.mSupportedGestures = supportedGestures;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean shouldInterceptKey(int keyCode) {
            return keyCode == this.mKeyCode;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean supportLongPress() {
            return (this.mSupportedGestures & 2) != 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean supportVeryLongPress() {
            return (this.mSupportedGestures & 4) != 0;
        }

        int getMaxMultiPressCount() {
            return 1;
        }

        void onMultiPress(long downTime, int count) {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public long getLongPressTimeoutMs() {
            return SingleKeyGestureDetector.sDefaultLongPressTimeout;
        }

        void onLongPress(long eventTime) {
        }

        long getVeryLongPressTimeoutMs() {
            return SingleKeyGestureDetector.sDefaultVeryLongPressTimeout;
        }

        void onVeryLongPress(long eventTime) {
        }

        public String toString() {
            return "KeyCode=" + KeyEvent.keyCodeToString(this.mKeyCode) + ", LongPress=" + supportLongPress() + ", VeryLongPress=" + supportVeryLongPress() + ", MaxMultiPressCount=" + getMaxMultiPressCount();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SingleKeyGestureDetector get(Context context) {
        SingleKeyGestureDetector detector = new SingleKeyGestureDetector();
        sDefaultLongPressTimeout = context.getResources().getInteger(17694839);
        sDefaultVeryLongPressTimeout = context.getResources().getInteger(17694969);
        return detector;
    }

    private SingleKeyGestureDetector() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addRule(SingleKeyRule rule) {
        this.mRules.add(rule);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void interceptKey(KeyEvent event, boolean interactive) {
        if (event.getAction() == 0) {
            int i = this.mDownKeyCode;
            if (i == 0 || i != event.getKeyCode()) {
                this.mBeganFromNonInteractive = !interactive;
            }
            interceptKeyDown(event);
            return;
        }
        interceptKeyUp(event);
    }

    private void interceptKeyDown(KeyEvent event) {
        SingleKeyRule singleKeyRule;
        int keyCode = event.getKeyCode();
        int i = this.mDownKeyCode;
        if (i == keyCode) {
            if (this.mActiveRule != null && (event.getFlags() & 128) != 0 && this.mActiveRule.supportLongPress() && !this.mHandledByLongPress) {
                if (DEBUG) {
                    Log.i(TAG, "Long press key " + KeyEvent.keyCodeToString(keyCode));
                }
                this.mHandledByLongPress = true;
                this.mHandler.removeMessages(0);
                this.mHandler.removeMessages(1);
                this.mHandler.removeMessages(3);
                Message msg = this.mHandler.obtainMessage(0, this.mActiveRule.mKeyCode, 0, this.mActiveRule);
                msg.setAsynchronous(true);
                this.mHandler.sendMessage(msg);
                return;
            }
            return;
        }
        if (i != 0 || ((singleKeyRule = this.mActiveRule) != null && !singleKeyRule.shouldInterceptKey(keyCode))) {
            if (DEBUG) {
                Log.i(TAG, "Press another key " + KeyEvent.keyCodeToString(keyCode));
            }
            reset();
        }
        this.mDownKeyCode = keyCode;
        if (this.mActiveRule == null) {
            int count = this.mRules.size();
            int index = 0;
            while (true) {
                if (index >= count) {
                    break;
                }
                SingleKeyRule rule = this.mRules.get(index);
                boolean z = DEBUG;
                if (z) {
                    Log.i(TAG, "before intercept key by rule " + rule);
                }
                if (!rule.shouldInterceptKey(keyCode)) {
                    index++;
                } else {
                    if (z) {
                        Log.i(TAG, "Intercept key by rule " + rule);
                    }
                    this.mActiveRule = rule;
                }
            }
            this.mLastDownTime = 0L;
        }
        if (this.mActiveRule == null) {
            return;
        }
        long keyDownInterval = event.getDownTime() - this.mLastDownTime;
        this.mLastDownTime = event.getDownTime();
        if (keyDownInterval >= MULTI_PRESS_TIMEOUT) {
            this.mKeyPressCounter = 1;
        } else {
            this.mKeyPressCounter++;
        }
        Log.i(TAG, "press mKeyPressCounter = " + this.mKeyPressCounter);
        if (this.mKeyPressCounter == 1) {
            if (this.mActiveRule.supportLongPress()) {
                Message msg2 = this.mHandler.obtainMessage(0, keyCode, 0, this.mActiveRule);
                msg2.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(msg2, 1800L);
            }
            if (this.mActiveRule.supportVeryLongPress()) {
                Message msg3 = this.mHandler.obtainMessage(1, keyCode, 0, this.mActiveRule);
                msg3.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(msg3, this.mActiveRule.getVeryLongPressTimeoutMs());
            }
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(3, keyCode, 0, this.mActiveRule), 5000L);
            return;
        }
        this.mHandler.removeMessages(0);
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(3);
        if (this.mActiveRule.getMaxMultiPressCount() > 1 && this.mKeyPressCounter == this.mActiveRule.getMaxMultiPressCount()) {
            if (DEBUG) {
                Log.i(TAG, "Trigger multi press " + this.mActiveRule.toString() + " for it reached the max count " + this.mKeyPressCounter);
            }
            Message msg4 = this.mHandler.obtainMessage(2, keyCode, this.mKeyPressCounter, this.mActiveRule);
            msg4.setAsynchronous(true);
            this.mHandler.sendMessage(msg4);
        }
    }

    private boolean interceptKeyUp(KeyEvent event) {
        this.mHandler.removeMessages(0);
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(3);
        this.mDownKeyCode = 0;
        boolean z = DEBUG;
        if (z) {
            Log.i(TAG, "interceptKeyUp mActiveRule:" + this.mActiveRule + " mHandledByLongPress:" + this.mHandledByLongPress);
        }
        if (this.mActiveRule == null) {
            return false;
        }
        if (this.mHandledByLongPress) {
            this.mHandledByLongPress = false;
            this.mKeyPressCounter = 0;
            this.mActiveRule = null;
            return true;
        } else if (event.getKeyCode() == this.mActiveRule.mKeyCode) {
            if (this.mActiveRule.getMaxMultiPressCount() == 1) {
                if (z) {
                    Log.i(TAG, "press key " + KeyEvent.keyCodeToString(event.getKeyCode()));
                }
                Message msg = this.mHandler.obtainMessage(2, this.mActiveRule.mKeyCode, 1, this.mActiveRule);
                msg.setAsynchronous(true);
                this.mHandler.sendMessage(msg);
                this.mActiveRule = null;
                return true;
            }
            if (this.mKeyPressCounter < this.mActiveRule.getMaxMultiPressCount()) {
                Message msg2 = this.mHandler.obtainMessage(2, this.mActiveRule.mKeyCode, this.mKeyPressCounter, this.mActiveRule);
                msg2.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(msg2, MULTI_PRESS_TIMEOUT);
            }
            return true;
        } else {
            reset();
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getKeyPressCounter(int keyCode) {
        SingleKeyRule singleKeyRule = this.mActiveRule;
        if (singleKeyRule != null && singleKeyRule.mKeyCode == keyCode) {
            return this.mKeyPressCounter;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reset() {
        if (this.mActiveRule != null) {
            if (this.mDownKeyCode != 0) {
                this.mHandler.removeMessages(0);
                this.mHandler.removeMessages(1);
                this.mHandler.removeMessages(3);
            }
            if (this.mKeyPressCounter > 0) {
                this.mHandler.removeMessages(2);
                this.mKeyPressCounter = 0;
            }
            this.mActiveRule = null;
            if (DEBUG) {
                Slog.d(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        this.mHandledByLongPress = false;
        this.mDownKeyCode = 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyIntercepted(int keyCode) {
        SingleKeyRule singleKeyRule = this.mActiveRule;
        return singleKeyRule != null && singleKeyRule.shouldInterceptKey(keyCode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean beganFromNonInteractive() {
        return this.mBeganFromNonInteractive;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + "SingleKey rules:");
        Iterator<SingleKeyRule> it = this.mRules.iterator();
        while (it.hasNext()) {
            SingleKeyRule rule = it.next();
            pw.println(prefix + "  " + rule);
        }
    }

    /* loaded from: classes2.dex */
    private class KeyHandler extends Handler {
        KeyHandler() {
            super(Looper.getMainLooper());
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            SingleKeyRule rule = (SingleKeyRule) msg.obj;
            if (rule == null) {
                Log.wtf(SingleKeyGestureDetector.TAG, "No active rule.");
                return;
            }
            int keyCode = msg.arg1;
            int pressCount = msg.arg2;
            switch (msg.what) {
                case 0:
                    if (SingleKeyGestureDetector.DEBUG) {
                        Log.i(SingleKeyGestureDetector.TAG, "Detect long press " + KeyEvent.keyCodeToString(keyCode));
                    }
                    SingleKeyGestureDetector.this.mHandledByLongPress = true;
                    rule.onLongPress(SingleKeyGestureDetector.this.mLastDownTime);
                    return;
                case 1:
                    if (SingleKeyGestureDetector.DEBUG) {
                        Log.i(SingleKeyGestureDetector.TAG, "Detect very long press " + KeyEvent.keyCodeToString(keyCode));
                    }
                    SingleKeyGestureDetector.this.mHandledByLongPress = true;
                    rule.onVeryLongPress(SingleKeyGestureDetector.this.mLastDownTime);
                    return;
                case 2:
                    if (SingleKeyGestureDetector.DEBUG) {
                        Log.i(SingleKeyGestureDetector.TAG, "Detect press " + KeyEvent.keyCodeToString(keyCode) + ", count " + pressCount);
                    }
                    if (pressCount == 1) {
                        rule.onPress(SingleKeyGestureDetector.this.mLastDownTime);
                        return;
                    } else {
                        rule.onMultiPress(SingleKeyGestureDetector.this.mLastDownTime, pressCount);
                        return;
                    }
                case 3:
                    if (SingleKeyGestureDetector.DEBUG) {
                        Log.i(SingleKeyGestureDetector.TAG, "Detect very very long press " + KeyEvent.keyCodeToString(keyCode) + ", count " + pressCount + ",startTNE 0x007a002a");
                    }
                    if ("1".equals(SystemProperties.get("persist.sys.adb.support", "0"))) {
                        TNEService tnev = new TNEService();
                        tnev.startTNE("0x007a002a", 1073741824L, Process.myPid(), "");
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }
}
