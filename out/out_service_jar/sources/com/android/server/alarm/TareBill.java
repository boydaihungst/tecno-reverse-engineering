package com.android.server.alarm;

import com.android.server.tare.AlarmManagerEconomicPolicy;
import com.android.server.tare.EconomyManagerInternal;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class TareBill {
    static final EconomyManagerInternal.ActionBill ALARM_CLOCK = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(AlarmManagerEconomicPolicy.ACTION_ALARM_CLOCK, 1, 0)));
    static final EconomyManagerInternal.ActionBill NONWAKEUP_INEXACT_ALARM = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_INEXACT, 1, 0)));
    static final EconomyManagerInternal.ActionBill NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE_ALARM = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE, 1, 0)));
    static final EconomyManagerInternal.ActionBill NONWAKEUP_EXACT_ALARM = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_EXACT, 1, 0)));
    static final EconomyManagerInternal.ActionBill NONWAKEUP_EXACT_ALLOW_WHILE_IDLE_ALARM = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_EXACT_ALLOW_WHILE_IDLE, 1, 0)));
    static final EconomyManagerInternal.ActionBill WAKEUP_INEXACT_ALARM = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(AlarmManagerEconomicPolicy.ACTION_ALARM_WAKEUP_INEXACT, 1, 0)));
    static final EconomyManagerInternal.ActionBill WAKEUP_INEXACT_ALLOW_WHILE_IDLE_ALARM = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(AlarmManagerEconomicPolicy.ACTION_ALARM_WAKEUP_INEXACT_ALLOW_WHILE_IDLE, 1, 0)));
    static final EconomyManagerInternal.ActionBill WAKEUP_EXACT_ALARM = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(1073741825, 1, 0)));
    static final EconomyManagerInternal.ActionBill WAKEUP_EXACT_ALLOW_WHILE_IDLE_ALARM = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(1073741824, 1, 0)));

    /* JADX INFO: Access modifiers changed from: package-private */
    public static EconomyManagerInternal.ActionBill getAppropriateBill(Alarm alarm) {
        if (alarm.alarmClock != null) {
            return ALARM_CLOCK;
        }
        boolean allowWhileIdle = (alarm.flags & 12) != 0;
        boolean isExact = alarm.windowLength == 0;
        if (alarm.wakeup) {
            if (isExact) {
                if (allowWhileIdle) {
                    return WAKEUP_EXACT_ALLOW_WHILE_IDLE_ALARM;
                }
                return WAKEUP_EXACT_ALARM;
            } else if (allowWhileIdle) {
                return WAKEUP_INEXACT_ALLOW_WHILE_IDLE_ALARM;
            } else {
                return WAKEUP_INEXACT_ALARM;
            }
        } else if (isExact) {
            if (allowWhileIdle) {
                return NONWAKEUP_EXACT_ALLOW_WHILE_IDLE_ALARM;
            }
            return NONWAKEUP_EXACT_ALARM;
        } else if (allowWhileIdle) {
            return NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE_ALARM;
        } else {
            return NONWAKEUP_INEXACT_ALARM;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String getName(EconomyManagerInternal.ActionBill bill) {
        if (bill.equals(ALARM_CLOCK)) {
            return "ALARM_CLOCK_BILL";
        }
        if (bill.equals(NONWAKEUP_INEXACT_ALARM)) {
            return "NONWAKEUP_INEXACT_ALARM_BILL";
        }
        if (bill.equals(NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE_ALARM)) {
            return "NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE_ALARM_BILL";
        }
        if (bill.equals(NONWAKEUP_EXACT_ALARM)) {
            return "NONWAKEUP_EXACT_ALARM_BILL";
        }
        if (bill.equals(NONWAKEUP_EXACT_ALLOW_WHILE_IDLE_ALARM)) {
            return "NONWAKEUP_EXACT_ALLOW_WHILE_IDLE_ALARM_BILL";
        }
        if (bill.equals(WAKEUP_INEXACT_ALARM)) {
            return "WAKEUP_INEXACT_ALARM_BILL";
        }
        if (bill.equals(WAKEUP_INEXACT_ALLOW_WHILE_IDLE_ALARM)) {
            return "WAKEUP_INEXACT_ALLOW_WHILE_IDLE_ALARM_BILL";
        }
        if (bill.equals(WAKEUP_EXACT_ALARM)) {
            return "WAKEUP_EXACT_ALARM_BILL";
        }
        if (bill.equals(WAKEUP_EXACT_ALLOW_WHILE_IDLE_ALARM)) {
            return "WAKEUP_EXACT_ALLOW_WHILE_IDLE_ALARM_BILL";
        }
        return "UNKNOWN_BILL (" + bill.toString() + ")";
    }

    private TareBill() {
    }
}
