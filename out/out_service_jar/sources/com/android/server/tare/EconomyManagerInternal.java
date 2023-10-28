package com.android.server.tare;

import com.android.server.tare.EconomyManagerInternal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;
/* loaded from: classes2.dex */
public interface EconomyManagerInternal {

    /* loaded from: classes2.dex */
    public interface AffordabilityChangeListener {
        void onAffordabilityChanged(int i, String str, ActionBill actionBill, boolean z);
    }

    /* loaded from: classes2.dex */
    public interface TareStateChangeListener {
        void onTareEnabledStateChanged(boolean z);
    }

    boolean canPayFor(int i, String str, ActionBill actionBill);

    long getMaxDurationMs(int i, String str, ActionBill actionBill);

    boolean isEnabled();

    void noteInstantaneousEvent(int i, String str, int i2, String str2);

    void noteOngoingEventStarted(int i, String str, int i2, String str2);

    void noteOngoingEventStopped(int i, String str, int i2, String str2);

    void registerAffordabilityChangeListener(int i, String str, AffordabilityChangeListener affordabilityChangeListener, ActionBill actionBill);

    void registerTareStateChangeListener(TareStateChangeListener tareStateChangeListener);

    void unregisterAffordabilityChangeListener(int i, String str, AffordabilityChangeListener affordabilityChangeListener, ActionBill actionBill);

    void unregisterTareStateChangeListener(TareStateChangeListener tareStateChangeListener);

    /* loaded from: classes2.dex */
    public static final class AnticipatedAction {
        public final int actionId;
        private final int mHashCode;
        public final int numInstantaneousCalls;
        public final long ongoingDurationMs;

        public AnticipatedAction(int actionId, int numInstantaneousCalls, long ongoingDurationMs) {
            this.actionId = actionId;
            this.numInstantaneousCalls = numInstantaneousCalls;
            this.ongoingDurationMs = ongoingDurationMs;
            int hash = (0 * 31) + actionId;
            this.mHashCode = (((hash * 31) + numInstantaneousCalls) * 31) + ((int) ((ongoingDurationMs >>> 32) ^ ongoingDurationMs));
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AnticipatedAction that = (AnticipatedAction) o;
            if (this.actionId == that.actionId && this.numInstantaneousCalls == that.numInstantaneousCalls && this.ongoingDurationMs == that.ongoingDurationMs) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return this.mHashCode;
        }
    }

    /* loaded from: classes2.dex */
    public static final class ActionBill {
        private static final Comparator<AnticipatedAction> sAnticipatedActionComparator = Comparator.comparingInt(new ToIntFunction() { // from class: com.android.server.tare.EconomyManagerInternal$ActionBill$$ExternalSyntheticLambda0
            @Override // java.util.function.ToIntFunction
            public final int applyAsInt(Object obj) {
                int i;
                i = ((EconomyManagerInternal.AnticipatedAction) obj).actionId;
                return i;
            }
        });
        private final List<AnticipatedAction> mAnticipatedActions;
        private final int mHashCode;

        public ActionBill(List<AnticipatedAction> anticipatedActions) {
            List<AnticipatedAction> actions = new ArrayList<>(anticipatedActions);
            actions.sort(sAnticipatedActionComparator);
            this.mAnticipatedActions = Collections.unmodifiableList(actions);
            int hash = 0;
            for (int i = 0; i < this.mAnticipatedActions.size(); i++) {
                hash = (hash * 31) + this.mAnticipatedActions.get(i).hashCode();
            }
            this.mHashCode = hash;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public List<AnticipatedAction> getAnticipatedActions() {
            return this.mAnticipatedActions;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ActionBill that = (ActionBill) o;
            return this.mAnticipatedActions.equals(that.mAnticipatedActions);
        }

        public int hashCode() {
            return this.mHashCode;
        }
    }
}
