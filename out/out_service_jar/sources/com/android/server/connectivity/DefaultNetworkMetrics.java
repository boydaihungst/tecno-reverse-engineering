package com.android.server.connectivity;

import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.metrics.DefaultNetworkEvent;
import android.os.SystemClock;
import com.android.internal.util.BitUtils;
import com.android.internal.util.RingBuffer;
import com.android.server.connectivity.metrics.nano.IpConnectivityLogClass;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class DefaultNetworkMetrics {
    private static final int ROLLING_LOG_SIZE = 64;
    public final long creationTimeMs;
    private DefaultNetworkEvent mCurrentDefaultNetwork;
    private final List<DefaultNetworkEvent> mEvents;
    private final RingBuffer<DefaultNetworkEvent> mEventsLog;
    private boolean mIsCurrentlyValid;
    private int mLastTransports;
    private long mLastValidationTimeMs;

    public DefaultNetworkMetrics() {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        this.creationTimeMs = elapsedRealtime;
        this.mEvents = new ArrayList();
        this.mEventsLog = new RingBuffer<>(DefaultNetworkEvent.class, 64);
        newDefaultNetwork(elapsedRealtime, null, 0, false, null, null);
    }

    public synchronized void listEvents(PrintWriter pw) {
        DefaultNetworkEvent[] defaultNetworkEventArr;
        pw.println("default network events:");
        long localTimeMs = System.currentTimeMillis();
        long timeMs = SystemClock.elapsedRealtime();
        for (DefaultNetworkEvent ev : (DefaultNetworkEvent[]) this.mEventsLog.toArray()) {
            printEvent(localTimeMs, pw, ev);
        }
        this.mCurrentDefaultNetwork.updateDuration(timeMs);
        if (this.mIsCurrentlyValid) {
            updateValidationTime(timeMs);
            this.mLastValidationTimeMs = timeMs;
        }
        printEvent(localTimeMs, pw, this.mCurrentDefaultNetwork);
    }

    public synchronized List<IpConnectivityLogClass.IpConnectivityEvent> listEventsAsProto() {
        List<IpConnectivityLogClass.IpConnectivityEvent> list;
        DefaultNetworkEvent[] defaultNetworkEventArr;
        list = new ArrayList<>();
        for (DefaultNetworkEvent ev : (DefaultNetworkEvent[]) this.mEventsLog.toArray()) {
            list.add(IpConnectivityEventBuilder.toProto(ev));
        }
        return list;
    }

    public synchronized void flushEvents(List<IpConnectivityLogClass.IpConnectivityEvent> out) {
        for (DefaultNetworkEvent ev : this.mEvents) {
            out.add(IpConnectivityEventBuilder.toProto(ev));
        }
        this.mEvents.clear();
    }

    public synchronized void logDefaultNetworkValidity(long timeMs, boolean isValid) {
        if (!isValid) {
            try {
                if (this.mIsCurrentlyValid) {
                    this.mIsCurrentlyValid = false;
                    updateValidationTime(timeMs);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        if (isValid && !this.mIsCurrentlyValid) {
            this.mIsCurrentlyValid = true;
            this.mLastValidationTimeMs = timeMs;
        }
    }

    private void updateValidationTime(long timeMs) {
        this.mCurrentDefaultNetwork.validatedMs += timeMs - this.mLastValidationTimeMs;
    }

    public synchronized void logDefaultNetworkEvent(long timeMs, Network defaultNetwork, int score, boolean validated, LinkProperties lp, NetworkCapabilities nc, Network previousDefaultNetwork, int previousScore, LinkProperties previousLp, NetworkCapabilities previousNc) {
        logCurrentDefaultNetwork(timeMs, previousDefaultNetwork, previousScore, previousLp, previousNc);
        newDefaultNetwork(timeMs, defaultNetwork, score, validated, lp, nc);
    }

    private void logCurrentDefaultNetwork(long timeMs, Network network, int score, LinkProperties lp, NetworkCapabilities nc) {
        if (this.mIsCurrentlyValid) {
            updateValidationTime(timeMs);
        }
        DefaultNetworkEvent ev = this.mCurrentDefaultNetwork;
        ev.updateDuration(timeMs);
        ev.previousTransports = this.mLastTransports;
        if (network != null) {
            fillLinkInfo(ev, network, lp, nc);
            ev.finalScore = score;
        }
        if (ev.transports != 0) {
            this.mLastTransports = ev.transports;
        }
        this.mEvents.add(ev);
        this.mEventsLog.append(ev);
    }

    private void newDefaultNetwork(long timeMs, Network network, int score, boolean validated, LinkProperties lp, NetworkCapabilities nc) {
        DefaultNetworkEvent ev = new DefaultNetworkEvent(timeMs);
        ev.durationMs = timeMs;
        if (network != null) {
            fillLinkInfo(ev, network, lp, nc);
            ev.initialScore = score;
            if (validated) {
                this.mIsCurrentlyValid = true;
                this.mLastValidationTimeMs = timeMs;
            }
        } else {
            this.mIsCurrentlyValid = false;
        }
        this.mCurrentDefaultNetwork = ev;
    }

    private static void fillLinkInfo(DefaultNetworkEvent ev, Network network, LinkProperties lp, NetworkCapabilities nc) {
        ev.netId = network.getNetId();
        ev.transports = (int) (ev.transports | BitUtils.packBits(nc.getTransportTypes()));
        boolean z = true;
        ev.ipv4 |= lp.hasIpv4Address() && lp.hasIpv4DefaultRoute();
        boolean z2 = ev.ipv6;
        if (!lp.hasGlobalIpv6Address() || !lp.hasIpv6DefaultRoute()) {
            z = false;
        }
        ev.ipv6 = z2 | z;
    }

    private static void printEvent(long localTimeMs, PrintWriter pw, DefaultNetworkEvent ev) {
        long localCreationTimeMs = localTimeMs - ev.durationMs;
        pw.println(String.format("%tT.%tL: %s", Long.valueOf(localCreationTimeMs), Long.valueOf(localCreationTimeMs), ev));
    }
}
