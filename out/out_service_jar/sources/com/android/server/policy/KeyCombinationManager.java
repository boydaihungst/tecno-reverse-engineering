package com.android.server.policy;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseLongArray;
import android.view.KeyEvent;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.policy.KeyCombinationManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class KeyCombinationManager {
    private static final long COMBINE_KEY_DELAY_MILLIS = 150;
    private static final String TAG = "KeyCombinationManager";
    private final Handler mHandler;
    private TwoKeysCombinationRule mTriggeredRule;
    private final SparseLongArray mDownTimes = new SparseLongArray(2);
    private final ArrayList<TwoKeysCombinationRule> mRules = new ArrayList<>();
    private final Object mLock = new Object();
    private final ArrayList<TwoKeysCombinationRule> mActiveRules = new ArrayList<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static abstract class TwoKeysCombinationRule {
        private int mKeyCode1;
        private int mKeyCode2;

        /* JADX INFO: Access modifiers changed from: package-private */
        public abstract void cancel();

        /* JADX INFO: Access modifiers changed from: package-private */
        public abstract void execute();

        /* JADX INFO: Access modifiers changed from: package-private */
        public TwoKeysCombinationRule(int keyCode1, int keyCode2) {
            this.mKeyCode1 = keyCode1;
            this.mKeyCode2 = keyCode2;
        }

        boolean preCondition() {
            return true;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean shouldInterceptKey(int keyCode) {
            return preCondition() && (keyCode == this.mKeyCode1 || keyCode == this.mKeyCode2);
        }

        boolean shouldInterceptKeys(SparseLongArray downTimes) {
            long now = SystemClock.uptimeMillis();
            if (downTimes.get(this.mKeyCode1) > 0 && downTimes.get(this.mKeyCode2) > 0 && now <= downTimes.get(this.mKeyCode1) + 150 && now <= downTimes.get(this.mKeyCode2) + 150) {
                return true;
            }
            return false;
        }

        long getKeyInterceptDelayMs() {
            return 150L;
        }

        public String toString() {
            return KeyEvent.keyCodeToString(this.mKeyCode1) + " + " + KeyEvent.keyCodeToString(this.mKeyCode2);
        }
    }

    public KeyCombinationManager(Handler handler) {
        this.mHandler = handler;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addRule(TwoKeysCombinationRule rule) {
        this.mRules.add(rule);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean interceptKey(KeyEvent event, boolean interactive) {
        boolean interceptKeyLocked;
        synchronized (this.mLock) {
            interceptKeyLocked = interceptKeyLocked(event, interactive);
        }
        return interceptKeyLocked;
    }

    private boolean interceptKeyLocked(KeyEvent event, boolean interactive) {
        boolean down = event.getAction() == 0;
        final int keyCode = event.getKeyCode();
        int count = this.mActiveRules.size();
        final long eventTime = event.getEventTime();
        if (interactive && down) {
            if (this.mDownTimes.size() > 0) {
                if (count > 0 && eventTime > this.mDownTimes.valueAt(0) + 150) {
                    forAllRules(this.mActiveRules, new Consumer() { // from class: com.android.server.policy.KeyCombinationManager$$ExternalSyntheticLambda3
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((KeyCombinationManager.TwoKeysCombinationRule) obj).cancel();
                        }
                    });
                    this.mActiveRules.clear();
                    return false;
                } else if (count == 0) {
                    return false;
                }
            }
            if (this.mDownTimes.get(keyCode) != 0) {
                return false;
            }
            forAllRules(this.mRules, new Consumer() { // from class: com.android.server.policy.KeyCombinationManager$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    KeyCombinationManager.this.m5879xb3a384f(keyCode, eventTime, (KeyCombinationManager.TwoKeysCombinationRule) obj);
                }
            });
            if (this.mDownTimes.size() == 1) {
                this.mTriggeredRule = null;
                forAllRules(this.mRules, new Consumer() { // from class: com.android.server.policy.KeyCombinationManager$$ExternalSyntheticLambda5
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        KeyCombinationManager.this.m5880xac3d250(keyCode, (KeyCombinationManager.TwoKeysCombinationRule) obj);
                    }
                });
            } else if (this.mTriggeredRule != null) {
                return true;
            } else {
                forAllActiveRules(new ToBooleanFunction() { // from class: com.android.server.policy.KeyCombinationManager$$ExternalSyntheticLambda6
                    public final boolean apply(Object obj) {
                        return KeyCombinationManager.this.m5881xa4d6c51((KeyCombinationManager.TwoKeysCombinationRule) obj);
                    }
                });
                this.mActiveRules.clear();
                TwoKeysCombinationRule twoKeysCombinationRule = this.mTriggeredRule;
                if (twoKeysCombinationRule != null) {
                    this.mActiveRules.add(twoKeysCombinationRule);
                    return true;
                }
            }
        } else {
            this.mDownTimes.delete(keyCode);
            for (int index = count - 1; index >= 0; index--) {
                final TwoKeysCombinationRule rule = this.mActiveRules.get(index);
                if (rule.shouldInterceptKey(keyCode)) {
                    Handler handler = this.mHandler;
                    Objects.requireNonNull(rule);
                    handler.post(new Runnable() { // from class: com.android.server.policy.KeyCombinationManager$$ExternalSyntheticLambda7
                        @Override // java.lang.Runnable
                        public final void run() {
                            KeyCombinationManager.TwoKeysCombinationRule.this.cancel();
                        }
                    });
                    this.mActiveRules.remove(index);
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$interceptKeyLocked$1$com-android-server-policy-KeyCombinationManager  reason: not valid java name */
    public /* synthetic */ void m5879xb3a384f(int keyCode, long eventTime, TwoKeysCombinationRule rule) {
        if (rule.shouldInterceptKey(keyCode)) {
            this.mDownTimes.put(keyCode, eventTime);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$interceptKeyLocked$2$com-android-server-policy-KeyCombinationManager  reason: not valid java name */
    public /* synthetic */ void m5880xac3d250(int keyCode, TwoKeysCombinationRule rule) {
        if (rule.shouldInterceptKey(keyCode)) {
            this.mActiveRules.add(rule);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$interceptKeyLocked$3$com-android-server-policy-KeyCombinationManager  reason: not valid java name */
    public /* synthetic */ boolean m5881xa4d6c51(final TwoKeysCombinationRule rule) {
        if (!rule.shouldInterceptKeys(this.mDownTimes)) {
            return false;
        }
        Log.v(TAG, "Performing combination rule : " + rule);
        Handler handler = this.mHandler;
        Objects.requireNonNull(rule);
        handler.post(new Runnable() { // from class: com.android.server.policy.KeyCombinationManager$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                KeyCombinationManager.TwoKeysCombinationRule.this.execute();
            }
        });
        this.mTriggeredRule = rule;
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getKeyInterceptTimeout(int keyCode) {
        synchronized (this.mLock) {
            if (this.mDownTimes.get(keyCode) == 0) {
                return 0L;
            }
            long delayMs = 0;
            Iterator<TwoKeysCombinationRule> it = this.mActiveRules.iterator();
            while (it.hasNext()) {
                TwoKeysCombinationRule rule = it.next();
                if (rule.shouldInterceptKey(keyCode)) {
                    delayMs = Math.max(delayMs, rule.getKeyInterceptDelayMs());
                }
            }
            return this.mDownTimes.get(keyCode) + Math.min(delayMs, 150L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyConsumed(KeyEvent event) {
        synchronized (this.mLock) {
            boolean z = false;
            if ((event.getFlags() & 1024) != 0) {
                return false;
            }
            TwoKeysCombinationRule twoKeysCombinationRule = this.mTriggeredRule;
            if (twoKeysCombinationRule != null && twoKeysCombinationRule.shouldInterceptKey(event.getKeyCode())) {
                z = true;
            }
            return z;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPowerKeyIntercepted() {
        synchronized (this.mLock) {
            boolean z = false;
            if (forAllActiveRules(new ToBooleanFunction() { // from class: com.android.server.policy.KeyCombinationManager$$ExternalSyntheticLambda0
                public final boolean apply(Object obj) {
                    boolean shouldInterceptKey;
                    shouldInterceptKey = ((KeyCombinationManager.TwoKeysCombinationRule) obj).shouldInterceptKey(26);
                    return shouldInterceptKey;
                }
            })) {
                if (this.mDownTimes.size() > 1 || this.mDownTimes.get(26) == 0) {
                    z = true;
                }
                return z;
            }
            return false;
        }
    }

    private void forAllRules(ArrayList<TwoKeysCombinationRule> rules, Consumer<TwoKeysCombinationRule> callback) {
        int count = rules.size();
        for (int index = 0; index < count; index++) {
            TwoKeysCombinationRule rule = rules.get(index);
            callback.accept(rule);
        }
    }

    private boolean forAllActiveRules(ToBooleanFunction<TwoKeysCombinationRule> callback) {
        int count = this.mActiveRules.size();
        for (int index = 0; index < count; index++) {
            TwoKeysCombinationRule rule = this.mActiveRules.get(index);
            if (callback.apply(rule)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(final String prefix, final PrintWriter pw) {
        pw.println(prefix + "KeyCombination rules:");
        forAllRules(this.mRules, new Consumer() { // from class: com.android.server.policy.KeyCombinationManager$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                pw.println(prefix + "  " + ((KeyCombinationManager.TwoKeysCombinationRule) obj));
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reset() {
        if (this.mDownTimes.size() > 0) {
            forAllRules(this.mActiveRules, new Consumer() { // from class: com.android.server.policy.KeyCombinationManager$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((KeyCombinationManager.TwoKeysCombinationRule) obj).cancel();
                }
            });
            this.mActiveRules.clear();
            this.mDownTimes.clear();
        }
    }
}
