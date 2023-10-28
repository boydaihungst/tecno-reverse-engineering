package com.android.server.vcn.routeselection;

import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.vcn.VcnUnderlyingNetworkTemplate;
import android.os.ParcelUuid;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.vcn.TelephonySubscriptionTracker;
import com.android.server.vcn.VcnContext;
import com.android.server.vcn.util.PersistableBundleUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
public class UnderlyingNetworkRecord {
    private static final int PRIORITY_CLASS_INVALID = Integer.MAX_VALUE;
    public final boolean isBlocked;
    public final LinkProperties linkProperties;
    private int mPriorityClass = Integer.MAX_VALUE;
    public final Network network;
    public final NetworkCapabilities networkCapabilities;

    public UnderlyingNetworkRecord(Network network, NetworkCapabilities networkCapabilities, LinkProperties linkProperties, boolean isBlocked) {
        this.network = network;
        this.networkCapabilities = networkCapabilities;
        this.linkProperties = linkProperties;
        this.isBlocked = isBlocked;
    }

    private int getOrCalculatePriorityClass(VcnContext vcnContext, List<VcnUnderlyingNetworkTemplate> underlyingNetworkTemplates, ParcelUuid subscriptionGroup, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, UnderlyingNetworkRecord currentlySelected, PersistableBundleUtils.PersistableBundleWrapper carrierConfig) {
        if (this.mPriorityClass == Integer.MAX_VALUE) {
            this.mPriorityClass = NetworkPriorityClassifier.calculatePriorityClass(vcnContext, this, underlyingNetworkTemplates, subscriptionGroup, snapshot, currentlySelected, carrierConfig);
        }
        return this.mPriorityClass;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPriorityClass() {
        return this.mPriorityClass;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof UnderlyingNetworkRecord) {
            UnderlyingNetworkRecord that = (UnderlyingNetworkRecord) o;
            return this.network.equals(that.network) && this.networkCapabilities.equals(that.networkCapabilities) && this.linkProperties.equals(that.linkProperties) && this.isBlocked == that.isBlocked;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.network, this.networkCapabilities, this.linkProperties, Boolean.valueOf(this.isBlocked));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Comparator<UnderlyingNetworkRecord> getComparator(final VcnContext vcnContext, final List<VcnUnderlyingNetworkTemplate> underlyingNetworkTemplates, final ParcelUuid subscriptionGroup, final TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, final UnderlyingNetworkRecord currentlySelected, final PersistableBundleUtils.PersistableBundleWrapper carrierConfig) {
        return new Comparator() { // from class: com.android.server.vcn.routeselection.UnderlyingNetworkRecord$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return UnderlyingNetworkRecord.lambda$getComparator$0(VcnContext.this, underlyingNetworkTemplates, subscriptionGroup, snapshot, currentlySelected, carrierConfig, (UnderlyingNetworkRecord) obj, (UnderlyingNetworkRecord) obj2);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$getComparator$0(VcnContext vcnContext, List underlyingNetworkTemplates, ParcelUuid subscriptionGroup, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, UnderlyingNetworkRecord currentlySelected, PersistableBundleUtils.PersistableBundleWrapper carrierConfig, UnderlyingNetworkRecord left, UnderlyingNetworkRecord right) {
        int leftIndex = left.getOrCalculatePriorityClass(vcnContext, underlyingNetworkTemplates, subscriptionGroup, snapshot, currentlySelected, carrierConfig);
        int rightIndex = right.getOrCalculatePriorityClass(vcnContext, underlyingNetworkTemplates, subscriptionGroup, snapshot, currentlySelected, carrierConfig);
        if (leftIndex == rightIndex) {
            if (isSelected(left, currentlySelected)) {
                return -1;
            }
            if (isSelected(left, currentlySelected)) {
                return 1;
            }
        }
        return Integer.compare(leftIndex, rightIndex);
    }

    private static boolean isSelected(UnderlyingNetworkRecord recordToCheck, UnderlyingNetworkRecord currentlySelected) {
        if (currentlySelected == null || currentlySelected.network != recordToCheck.network) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(VcnContext vcnContext, IndentingPrintWriter pw, List<VcnUnderlyingNetworkTemplate> underlyingNetworkTemplates, ParcelUuid subscriptionGroup, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, UnderlyingNetworkRecord currentlySelected, PersistableBundleUtils.PersistableBundleWrapper carrierConfig) {
        pw.println("UnderlyingNetworkRecord:");
        pw.increaseIndent();
        int priorityIndex = getOrCalculatePriorityClass(vcnContext, underlyingNetworkTemplates, subscriptionGroup, snapshot, currentlySelected, carrierConfig);
        pw.println("Priority index: " + priorityIndex);
        pw.println("mNetwork: " + this.network);
        pw.println("mNetworkCapabilities: " + this.networkCapabilities);
        pw.println("mLinkProperties: " + this.linkProperties);
        pw.decreaseIndent();
    }

    /* loaded from: classes2.dex */
    static class Builder {
        private UnderlyingNetworkRecord mCached;
        boolean mIsBlocked;
        private LinkProperties mLinkProperties;
        private final Network mNetwork;
        private NetworkCapabilities mNetworkCapabilities;
        boolean mWasIsBlockedSet;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(Network network) {
            this.mNetwork = network;
        }

        Network getNetwork() {
            return this.mNetwork;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void setNetworkCapabilities(NetworkCapabilities networkCapabilities) {
            this.mNetworkCapabilities = networkCapabilities;
            this.mCached = null;
        }

        NetworkCapabilities getNetworkCapabilities() {
            return this.mNetworkCapabilities;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void setLinkProperties(LinkProperties linkProperties) {
            this.mLinkProperties = linkProperties;
            this.mCached = null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void setIsBlocked(boolean isBlocked) {
            this.mIsBlocked = isBlocked;
            this.mWasIsBlockedSet = true;
            this.mCached = null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean isValid() {
            return (this.mNetworkCapabilities == null || this.mLinkProperties == null || !this.mWasIsBlockedSet) ? false : true;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public UnderlyingNetworkRecord build() {
            if (!isValid()) {
                throw new IllegalArgumentException("Called build before UnderlyingNetworkRecord was valid");
            }
            if (this.mCached == null) {
                this.mCached = new UnderlyingNetworkRecord(this.mNetwork, this.mNetworkCapabilities, this.mLinkProperties, this.mIsBlocked);
            }
            return this.mCached;
        }
    }
}
