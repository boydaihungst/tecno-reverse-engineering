package com.android.server.tare;

import android.util.IndentingPrintWriter;
import android.util.SparseLongArray;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class Ledger {
    private final SparseLongArray mCumulativeDeltaPerReason;
    private long mCurrentBalance;
    private long mEarliestSumTime;
    private final List<Transaction> mTransactions;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Transaction {
        public final long ctp;
        public final long delta;
        public final long endTimeMs;
        public final int eventId;
        public final long startTimeMs;
        public final String tag;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Transaction(long startTimeMs, long endTimeMs, int eventId, String tag, long delta, long ctp) {
            this.startTimeMs = startTimeMs;
            this.endTimeMs = endTimeMs;
            this.eventId = eventId;
            this.tag = tag;
            this.delta = delta;
            this.ctp = ctp;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Ledger() {
        this.mCurrentBalance = 0L;
        this.mTransactions = new ArrayList();
        this.mCumulativeDeltaPerReason = new SparseLongArray();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Ledger(long currentBalance, List<Transaction> transactions) {
        this.mCurrentBalance = 0L;
        ArrayList arrayList = new ArrayList();
        this.mTransactions = arrayList;
        this.mCumulativeDeltaPerReason = new SparseLongArray();
        this.mCurrentBalance = currentBalance;
        arrayList.addAll(transactions);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getCurrentBalance() {
        return this.mCurrentBalance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Transaction getEarliestTransaction() {
        if (this.mTransactions.size() > 0) {
            return this.mTransactions.get(0);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<Transaction> getTransactions() {
        return this.mTransactions;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void recordTransaction(Transaction transaction) {
        this.mTransactions.add(transaction);
        this.mCurrentBalance += transaction.delta;
        long sum = this.mCumulativeDeltaPerReason.get(transaction.eventId);
        this.mCumulativeDeltaPerReason.put(transaction.eventId, transaction.delta + sum);
        this.mEarliestSumTime = Math.min(this.mEarliestSumTime, transaction.startTimeMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long get24HourSum(int eventId, long now) {
        long j;
        long windowStartTime = now - 86400000;
        if (this.mEarliestSumTime < windowStartTime) {
            this.mCumulativeDeltaPerReason.clear();
            for (int i = this.mTransactions.size() - 1; i >= 0; i--) {
                Transaction transaction = this.mTransactions.get(i);
                if (transaction.endTimeMs <= windowStartTime) {
                    break;
                }
                long sum = this.mCumulativeDeltaPerReason.get(transaction.eventId);
                if (transaction.startTimeMs >= windowStartTime) {
                    j = transaction.delta;
                } else {
                    j = ((long) (((transaction.endTimeMs - windowStartTime) * 1.0d) * transaction.delta)) / (transaction.endTimeMs - transaction.startTimeMs);
                }
                this.mCumulativeDeltaPerReason.put(transaction.eventId, sum + j);
            }
            this.mEarliestSumTime = windowStartTime;
        }
        return this.mCumulativeDeltaPerReason.get(eventId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeOldTransactions(long minAgeMs) {
        long cutoff = TareUtils.getCurrentTimeMillis() - minAgeMs;
        while (this.mTransactions.size() > 0 && this.mTransactions.get(0).endTimeMs <= cutoff) {
            this.mTransactions.remove(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw, int numRecentTransactions) {
        pw.print("Current balance", TareUtils.cakeToString(getCurrentBalance())).println();
        int size = this.mTransactions.size();
        for (int i = Math.max(0, size - numRecentTransactions); i < size; i++) {
            Transaction transaction = this.mTransactions.get(i);
            TareUtils.dumpTime(pw, transaction.startTimeMs);
            pw.print("--");
            TareUtils.dumpTime(pw, transaction.endTimeMs);
            pw.print(": ");
            pw.print(EconomicPolicy.eventToString(transaction.eventId));
            if (transaction.tag != null) {
                pw.print("(");
                pw.print(transaction.tag);
                pw.print(")");
            }
            pw.print(" --> ");
            pw.print(TareUtils.cakeToString(transaction.delta));
            pw.print(" (ctp=");
            pw.print(TareUtils.cakeToString(transaction.ctp));
            pw.println(")");
        }
    }
}
