package com.android.server.tare;

import android.util.IndentingPrintWriter;
import android.util.Log;
import com.android.server.tare.Ledger;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public class Analyst {
    private static final boolean DEBUG;
    private static final int NUM_PERIODS_TO_RETAIN = 8;
    private static final String TAG;
    private int mPeriodIndex = 0;
    private final Report[] mReports = new Report[8];

    static {
        String str = "TARE-" + Analyst.class.getSimpleName();
        TAG = str;
        DEBUG = InternalResourceService.DEBUG || Log.isLoggable(str, 3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class Report {
        public int cumulativeBatteryDischarge = 0;
        public int currentBatteryLevel = 0;
        public long cumulativeProfit = 0;
        public int numProfitableActions = 0;
        public long cumulativeLoss = 0;
        public int numUnprofitableActions = 0;
        public long cumulativeRewards = 0;
        public int numRewards = 0;
        public long cumulativePositiveRegulations = 0;
        public int numPositiveRegulations = 0;
        public long cumulativeNegativeRegulations = 0;
        public int numNegativeRegulations = 0;

        /* JADX INFO: Access modifiers changed from: private */
        public void clear() {
            this.cumulativeBatteryDischarge = 0;
            this.currentBatteryLevel = 0;
            this.cumulativeProfit = 0L;
            this.numProfitableActions = 0;
            this.cumulativeLoss = 0L;
            this.numUnprofitableActions = 0;
            this.cumulativeRewards = 0L;
            this.numRewards = 0;
            this.cumulativePositiveRegulations = 0L;
            this.numPositiveRegulations = 0;
            this.cumulativeNegativeRegulations = 0L;
            this.numNegativeRegulations = 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<Report> getReports() {
        List<Report> list = new ArrayList<>(8);
        for (int i = 1; i <= 8; i++) {
            int idx = (this.mPeriodIndex + i) % 8;
            Report report = this.mReports[idx];
            if (report != null) {
                list.add(report);
            }
        }
        return list;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadReports(List<Report> reports) {
        int numReports = reports.size();
        this.mPeriodIndex = Math.max(0, numReports - 1);
        for (int i = 0; i < 8; i++) {
            if (i < numReports) {
                this.mReports[i] = reports.get(i);
            } else {
                this.mReports[i] = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteBatteryLevelChange(int newBatteryLevel) {
        Report report;
        if (newBatteryLevel == 100 && (report = this.mReports[this.mPeriodIndex]) != null && report.currentBatteryLevel < newBatteryLevel) {
            int i = (this.mPeriodIndex + 1) % 8;
            this.mPeriodIndex = i;
            Report[] reportArr = this.mReports;
            if (reportArr[i] != null) {
                Report report2 = reportArr[i];
                report2.clear();
                report2.currentBatteryLevel = newBatteryLevel;
                return;
            }
        }
        Report[] reportArr2 = this.mReports;
        int i2 = this.mPeriodIndex;
        if (reportArr2[i2] == null) {
            Report report3 = new Report();
            this.mReports[this.mPeriodIndex] = report3;
            report3.currentBatteryLevel = newBatteryLevel;
            return;
        }
        Report report4 = reportArr2[i2];
        if (newBatteryLevel < report4.currentBatteryLevel) {
            report4.cumulativeBatteryDischarge += report4.currentBatteryLevel - newBatteryLevel;
        }
        report4.currentBatteryLevel = newBatteryLevel;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteTransaction(Ledger.Transaction transaction) {
        Report[] reportArr = this.mReports;
        int i = this.mPeriodIndex;
        if (reportArr[i] == null) {
            reportArr[i] = new Report();
        }
        Report report = this.mReports[this.mPeriodIndex];
        switch (EconomicPolicy.getEventType(transaction.eventId)) {
            case Integer.MIN_VALUE:
                if (transaction.delta != 0) {
                    report.cumulativeRewards += transaction.delta;
                    report.numRewards++;
                    return;
                }
                return;
            case 0:
                if (transaction.delta > 0) {
                    report.cumulativePositiveRegulations += transaction.delta;
                    report.numPositiveRegulations++;
                    return;
                } else if (transaction.delta < 0) {
                    report.cumulativeNegativeRegulations -= transaction.delta;
                    report.numNegativeRegulations++;
                    return;
                } else {
                    return;
                }
            case 1073741824:
                if ((-transaction.delta) > transaction.ctp) {
                    report.cumulativeProfit += (-transaction.delta) - transaction.ctp;
                    report.numProfitableActions++;
                    return;
                } else if ((-transaction.delta) < transaction.ctp) {
                    report.cumulativeLoss += transaction.ctp + transaction.delta;
                    report.numUnprofitableActions++;
                    return;
                } else {
                    return;
                }
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void tearDown() {
        int i = 0;
        while (true) {
            Report[] reportArr = this.mReports;
            if (i < reportArr.length) {
                reportArr[i] = null;
                i++;
            } else {
                this.mPeriodIndex = 0;
                return;
            }
        }
    }

    private String padStringWithSpaces(String text, int targetLength) {
        int padding = Math.max(2, targetLength - text.length()) >>> 1;
        return " ".repeat(padding) + text + " ".repeat(padding);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        String perDischarge;
        Report report;
        int i;
        String perDischarge2;
        String perDischarge3;
        String perDischarge4;
        String perDischarge5;
        String str;
        String perDischarge6;
        pw.println("Reports:");
        pw.increaseIndent();
        pw.print("      Total Discharge");
        int i2 = 47;
        pw.print(padStringWithSpaces("Profit (avg/action : avg/discharge)", 47));
        pw.print(padStringWithSpaces("Loss (avg/action : avg/discharge)", 47));
        pw.print(padStringWithSpaces("Rewards (avg/reward : avg/discharge)", 47));
        pw.print(padStringWithSpaces("+Regs (avg/reg : avg/discharge)", 47));
        pw.print(padStringWithSpaces("-Regs (avg/reg : avg/discharge)", 47));
        pw.println();
        int r = 0;
        while (r < 8) {
            int idx = ((this.mPeriodIndex - r) + 8) % 8;
            Report report2 = this.mReports[idx];
            if (report2 == null) {
                i = i2;
            } else {
                pw.print("t-");
                pw.print(r);
                pw.print(":  ");
                pw.print(padStringWithSpaces(Integer.toString(report2.cumulativeBatteryDischarge), 15));
                if (report2.numProfitableActions > 0) {
                    if (report2.cumulativeBatteryDischarge > 0) {
                        str = "N/A";
                        perDischarge6 = TareUtils.cakeToString(report2.cumulativeProfit / report2.cumulativeBatteryDischarge);
                    } else {
                        str = "N/A";
                        perDischarge6 = str;
                    }
                    pw.print(padStringWithSpaces(String.format("%s (%s : %s)", TareUtils.cakeToString(report2.cumulativeProfit), TareUtils.cakeToString(report2.cumulativeProfit / report2.numProfitableActions), perDischarge6), i2));
                    perDischarge = str;
                } else {
                    perDischarge = "N/A";
                    pw.print(padStringWithSpaces(perDischarge, i2));
                }
                if (report2.numUnprofitableActions > 0) {
                    if (report2.cumulativeBatteryDischarge > 0) {
                        perDischarge5 = TareUtils.cakeToString(report2.cumulativeLoss / report2.cumulativeBatteryDischarge);
                    } else {
                        perDischarge5 = perDischarge;
                    }
                    report = report2;
                    pw.print(padStringWithSpaces(String.format("%s (%s : %s)", TareUtils.cakeToString(report2.cumulativeLoss), TareUtils.cakeToString(report2.cumulativeLoss / report2.numUnprofitableActions), perDischarge5), 47));
                } else {
                    report = report2;
                    pw.print(padStringWithSpaces(perDischarge, i2));
                }
                Report report3 = report;
                if (report3.numRewards <= 0) {
                    pw.print(padStringWithSpaces(perDischarge, 47));
                } else {
                    if (report3.cumulativeBatteryDischarge > 0) {
                        perDischarge4 = TareUtils.cakeToString(report3.cumulativeRewards / report3.cumulativeBatteryDischarge);
                    } else {
                        perDischarge4 = perDischarge;
                    }
                    pw.print(padStringWithSpaces(String.format("%s (%s : %s)", TareUtils.cakeToString(report3.cumulativeRewards), TareUtils.cakeToString(report3.cumulativeRewards / report3.numRewards), perDischarge4), 47));
                }
                if (report3.numPositiveRegulations <= 0) {
                    pw.print(padStringWithSpaces(perDischarge, 47));
                } else {
                    if (report3.cumulativeBatteryDischarge > 0) {
                        perDischarge3 = TareUtils.cakeToString(report3.cumulativePositiveRegulations / report3.cumulativeBatteryDischarge);
                    } else {
                        perDischarge3 = perDischarge;
                    }
                    pw.print(padStringWithSpaces(String.format("%s (%s : %s)", TareUtils.cakeToString(report3.cumulativePositiveRegulations), TareUtils.cakeToString(report3.cumulativePositiveRegulations / report3.numPositiveRegulations), perDischarge3), 47));
                }
                if (report3.numNegativeRegulations > 0) {
                    if (report3.cumulativeBatteryDischarge > 0) {
                        perDischarge2 = TareUtils.cakeToString(report3.cumulativeNegativeRegulations / report3.cumulativeBatteryDischarge);
                    } else {
                        perDischarge2 = perDischarge;
                    }
                    i = 47;
                    pw.print(padStringWithSpaces(String.format("%s (%s : %s)", TareUtils.cakeToString(report3.cumulativeNegativeRegulations), TareUtils.cakeToString(report3.cumulativeNegativeRegulations / report3.numNegativeRegulations), perDischarge2), 47));
                } else {
                    i = 47;
                    pw.print(padStringWithSpaces(perDischarge, 47));
                }
                pw.println();
            }
            r++;
            i2 = i;
        }
        pw.decreaseIndent();
    }
}
