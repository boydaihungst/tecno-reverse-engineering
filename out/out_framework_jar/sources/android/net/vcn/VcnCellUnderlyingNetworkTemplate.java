package android.net.vcn;

import android.os.PersistableBundle;
import android.util.ArraySet;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.vcn.repackaged.util.PersistableBundleUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes2.dex */
public final class VcnCellUnderlyingNetworkTemplate extends VcnUnderlyingNetworkTemplate {
    private static final String ALLOWED_NETWORK_PLMN_IDS_KEY = "mAllowedNetworkPlmnIds";
    private static final String ALLOWED_SPECIFIC_CARRIER_IDS_KEY = "mAllowedSpecificCarrierIds";
    private static final int DEFAULT_OPPORTUNISTIC_MATCH_CRITERIA = 0;
    private static final int DEFAULT_ROAMING_MATCH_CRITERIA = 0;
    private static final String OPPORTUNISTIC_MATCH_KEY = "mOpportunisticMatchCriteria";
    private static final String ROAMING_MATCH_KEY = "mRoamingMatchCriteria";
    private final Set<String> mAllowedNetworkPlmnIds;
    private final Set<Integer> mAllowedSpecificCarrierIds;
    private final int mOpportunisticMatchCriteria;
    private final int mRoamingMatchCriteria;

    private VcnCellUnderlyingNetworkTemplate(int meteredMatchCriteria, int minEntryUpstreamBandwidthKbps, int minExitUpstreamBandwidthKbps, int minEntryDownstreamBandwidthKbps, int minExitDownstreamBandwidthKbps, Set<String> allowedNetworkPlmnIds, Set<Integer> allowedSpecificCarrierIds, int roamingMatchCriteria, int opportunisticMatchCriteria) {
        super(2, meteredMatchCriteria, minEntryUpstreamBandwidthKbps, minExitUpstreamBandwidthKbps, minEntryDownstreamBandwidthKbps, minExitDownstreamBandwidthKbps);
        this.mAllowedNetworkPlmnIds = new ArraySet(allowedNetworkPlmnIds);
        this.mAllowedSpecificCarrierIds = new ArraySet(allowedSpecificCarrierIds);
        this.mRoamingMatchCriteria = roamingMatchCriteria;
        this.mOpportunisticMatchCriteria = opportunisticMatchCriteria;
        validate();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.net.vcn.VcnUnderlyingNetworkTemplate
    public void validate() {
        super.validate();
        validatePlmnIds(this.mAllowedNetworkPlmnIds);
        Objects.requireNonNull(this.mAllowedSpecificCarrierIds, "matchingCarrierIds is null");
        validateMatchCriteria(this.mRoamingMatchCriteria, ROAMING_MATCH_KEY);
        validateMatchCriteria(this.mOpportunisticMatchCriteria, OPPORTUNISTIC_MATCH_KEY);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:5:0x000f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static void validatePlmnIds(Set<String> matchingOperatorPlmnIds) {
        Objects.requireNonNull(matchingOperatorPlmnIds, "matchingOperatorPlmnIds is null");
        for (String id : matchingOperatorPlmnIds) {
            if ((id.length() != 5 && id.length() != 6) || !id.matches("[0-9]+")) {
                throw new IllegalArgumentException("Found invalid PLMN ID: " + id);
            }
            while (r0.hasNext()) {
            }
        }
    }

    public static VcnCellUnderlyingNetworkTemplate fromPersistableBundle(PersistableBundle in) {
        Objects.requireNonNull(in, "PersistableBundle is null");
        int meteredMatchCriteria = in.getInt("mMeteredMatchCriteria");
        int minEntryUpstreamBandwidthKbps = in.getInt("mMinEntryUpstreamBandwidthKbps", 0);
        int minExitUpstreamBandwidthKbps = in.getInt("mMinExitUpstreamBandwidthKbps", 0);
        int minEntryDownstreamBandwidthKbps = in.getInt("mMinEntryDownstreamBandwidthKbps", 0);
        int minExitDownstreamBandwidthKbps = in.getInt("mMinExitDownstreamBandwidthKbps", 0);
        PersistableBundle plmnIdsBundle = in.getPersistableBundle(ALLOWED_NETWORK_PLMN_IDS_KEY);
        Objects.requireNonNull(plmnIdsBundle, "plmnIdsBundle is null");
        Set<String> allowedNetworkPlmnIds = new ArraySet<>(PersistableBundleUtils.toList(plmnIdsBundle, PersistableBundleUtils.STRING_DESERIALIZER));
        PersistableBundle specificCarrierIdsBundle = in.getPersistableBundle(ALLOWED_SPECIFIC_CARRIER_IDS_KEY);
        Objects.requireNonNull(specificCarrierIdsBundle, "specificCarrierIdsBundle is null");
        Set<Integer> allowedSpecificCarrierIds = new ArraySet<>(PersistableBundleUtils.toList(specificCarrierIdsBundle, PersistableBundleUtils.INTEGER_DESERIALIZER));
        int roamingMatchCriteria = in.getInt(ROAMING_MATCH_KEY);
        int opportunisticMatchCriteria = in.getInt(OPPORTUNISTIC_MATCH_KEY);
        return new VcnCellUnderlyingNetworkTemplate(meteredMatchCriteria, minEntryUpstreamBandwidthKbps, minExitUpstreamBandwidthKbps, minEntryDownstreamBandwidthKbps, minExitDownstreamBandwidthKbps, allowedNetworkPlmnIds, allowedSpecificCarrierIds, roamingMatchCriteria, opportunisticMatchCriteria);
    }

    @Override // android.net.vcn.VcnUnderlyingNetworkTemplate
    public PersistableBundle toPersistableBundle() {
        PersistableBundle result = super.toPersistableBundle();
        PersistableBundle plmnIdsBundle = PersistableBundleUtils.fromList(new ArrayList(this.mAllowedNetworkPlmnIds), PersistableBundleUtils.STRING_SERIALIZER);
        result.putPersistableBundle(ALLOWED_NETWORK_PLMN_IDS_KEY, plmnIdsBundle);
        PersistableBundle specificCarrierIdsBundle = PersistableBundleUtils.fromList(new ArrayList(this.mAllowedSpecificCarrierIds), PersistableBundleUtils.INTEGER_SERIALIZER);
        result.putPersistableBundle(ALLOWED_SPECIFIC_CARRIER_IDS_KEY, specificCarrierIdsBundle);
        result.putInt(ROAMING_MATCH_KEY, this.mRoamingMatchCriteria);
        result.putInt(OPPORTUNISTIC_MATCH_KEY, this.mOpportunisticMatchCriteria);
        return result;
    }

    public Set<String> getOperatorPlmnIds() {
        return Collections.unmodifiableSet(this.mAllowedNetworkPlmnIds);
    }

    public Set<Integer> getSimSpecificCarrierIds() {
        return Collections.unmodifiableSet(this.mAllowedSpecificCarrierIds);
    }

    public int getRoaming() {
        return this.mRoamingMatchCriteria;
    }

    public int getOpportunistic() {
        return this.mOpportunisticMatchCriteria;
    }

    @Override // android.net.vcn.VcnUnderlyingNetworkTemplate
    public int hashCode() {
        return Objects.hash(Integer.valueOf(super.hashCode()), this.mAllowedNetworkPlmnIds, this.mAllowedSpecificCarrierIds, Integer.valueOf(this.mRoamingMatchCriteria), Integer.valueOf(this.mOpportunisticMatchCriteria));
    }

    @Override // android.net.vcn.VcnUnderlyingNetworkTemplate
    public boolean equals(Object other) {
        if (super.equals(other) && (other instanceof VcnCellUnderlyingNetworkTemplate)) {
            VcnCellUnderlyingNetworkTemplate rhs = (VcnCellUnderlyingNetworkTemplate) other;
            return Objects.equals(this.mAllowedNetworkPlmnIds, rhs.mAllowedNetworkPlmnIds) && Objects.equals(this.mAllowedSpecificCarrierIds, rhs.mAllowedSpecificCarrierIds) && this.mRoamingMatchCriteria == rhs.mRoamingMatchCriteria && this.mOpportunisticMatchCriteria == rhs.mOpportunisticMatchCriteria;
        }
        return false;
    }

    @Override // android.net.vcn.VcnUnderlyingNetworkTemplate
    void dumpTransportSpecificFields(IndentingPrintWriter pw) {
        if (!this.mAllowedNetworkPlmnIds.isEmpty()) {
            pw.println("mAllowedNetworkPlmnIds: " + this.mAllowedNetworkPlmnIds);
        }
        if (!this.mAllowedNetworkPlmnIds.isEmpty()) {
            pw.println("mAllowedSpecificCarrierIds: " + this.mAllowedSpecificCarrierIds);
        }
        if (this.mRoamingMatchCriteria != 0) {
            pw.println("mRoamingMatchCriteria: " + getMatchCriteriaString(this.mRoamingMatchCriteria));
        }
        if (this.mOpportunisticMatchCriteria != 0) {
            pw.println("mOpportunisticMatchCriteria: " + getMatchCriteriaString(this.mOpportunisticMatchCriteria));
        }
    }

    /* loaded from: classes2.dex */
    public static final class Builder {
        private int mMeteredMatchCriteria = 0;
        private final Set<String> mAllowedNetworkPlmnIds = new ArraySet();
        private final Set<Integer> mAllowedSpecificCarrierIds = new ArraySet();
        private int mRoamingMatchCriteria = 0;
        private int mOpportunisticMatchCriteria = 0;
        private int mMinEntryUpstreamBandwidthKbps = 0;
        private int mMinExitUpstreamBandwidthKbps = 0;
        private int mMinEntryDownstreamBandwidthKbps = 0;
        private int mMinExitDownstreamBandwidthKbps = 0;

        public Builder setMetered(int matchCriteria) {
            VcnUnderlyingNetworkTemplate.validateMatchCriteria(matchCriteria, "setMetered");
            this.mMeteredMatchCriteria = matchCriteria;
            return this;
        }

        public Builder setOperatorPlmnIds(Set<String> operatorPlmnIds) {
            VcnCellUnderlyingNetworkTemplate.validatePlmnIds(operatorPlmnIds);
            this.mAllowedNetworkPlmnIds.clear();
            this.mAllowedNetworkPlmnIds.addAll(operatorPlmnIds);
            return this;
        }

        public Builder setSimSpecificCarrierIds(Set<Integer> simSpecificCarrierIds) {
            Objects.requireNonNull(simSpecificCarrierIds, "simSpecificCarrierIds is null");
            this.mAllowedSpecificCarrierIds.clear();
            this.mAllowedSpecificCarrierIds.addAll(simSpecificCarrierIds);
            return this;
        }

        public Builder setRoaming(int matchCriteria) {
            VcnUnderlyingNetworkTemplate.validateMatchCriteria(matchCriteria, "setRoaming");
            this.mRoamingMatchCriteria = matchCriteria;
            return this;
        }

        public Builder setOpportunistic(int matchCriteria) {
            VcnUnderlyingNetworkTemplate.validateMatchCriteria(matchCriteria, "setOpportunistic");
            this.mOpportunisticMatchCriteria = matchCriteria;
            return this;
        }

        public Builder setMinUpstreamBandwidthKbps(int minEntryUpstreamBandwidthKbps, int minExitUpstreamBandwidthKbps) {
            VcnUnderlyingNetworkTemplate.validateMinBandwidthKbps(minEntryUpstreamBandwidthKbps, minExitUpstreamBandwidthKbps);
            this.mMinEntryUpstreamBandwidthKbps = minEntryUpstreamBandwidthKbps;
            this.mMinExitUpstreamBandwidthKbps = minExitUpstreamBandwidthKbps;
            return this;
        }

        public Builder setMinDownstreamBandwidthKbps(int minEntryDownstreamBandwidthKbps, int minExitDownstreamBandwidthKbps) {
            VcnUnderlyingNetworkTemplate.validateMinBandwidthKbps(minEntryDownstreamBandwidthKbps, minExitDownstreamBandwidthKbps);
            this.mMinEntryDownstreamBandwidthKbps = minEntryDownstreamBandwidthKbps;
            this.mMinExitDownstreamBandwidthKbps = minExitDownstreamBandwidthKbps;
            return this;
        }

        public VcnCellUnderlyingNetworkTemplate build() {
            return new VcnCellUnderlyingNetworkTemplate(this.mMeteredMatchCriteria, this.mMinEntryUpstreamBandwidthKbps, this.mMinExitUpstreamBandwidthKbps, this.mMinEntryDownstreamBandwidthKbps, this.mMinExitDownstreamBandwidthKbps, this.mAllowedNetworkPlmnIds, this.mAllowedSpecificCarrierIds, this.mRoamingMatchCriteria, this.mOpportunisticMatchCriteria);
        }
    }
}
