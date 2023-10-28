package com.android.server.connectivity;

import android.content.Context;
import android.net.ConnectivityMetricsEvent;
import android.net.IIpConnectivityMetrics;
import android.net.INetdEventCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkStack;
import android.net.metrics.ApfProgramEvent;
import android.os.Binder;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import com.android.internal.util.RingBuffer;
import com.android.internal.util.TokenBucket;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.connectivity.metrics.nano.IpConnectivityLogClass;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
/* loaded from: classes.dex */
public final class IpConnectivityMetrics extends SystemService {
    private static final boolean DBG = false;
    private static final int DEFAULT_BUFFER_SIZE = 2000;
    private static final int DEFAULT_LOG_SIZE = 500;
    private static final int ERROR_RATE_LIMITED = -1;
    private static final int MAXIMUM_BUFFER_SIZE = 20000;
    private static final int MAXIMUM_CONNECT_LATENCY_RECORDS = 20000;
    private static final int NYC = 0;
    private static final int NYC_MR1 = 1;
    private static final int NYC_MR2 = 2;
    private static final String SERVICE_NAME = "connmetrics";
    public static final int VERSION = 2;
    public final Impl impl;
    private final ArrayMap<Class<?>, TokenBucket> mBuckets;
    private ArrayList<ConnectivityMetricsEvent> mBuffer;
    private int mCapacity;
    private final ToIntFunction<Context> mCapacityGetter;
    final DefaultNetworkMetrics mDefaultNetworkMetrics;
    private int mDropped;
    private final RingBuffer<ConnectivityMetricsEvent> mEventLog;
    private final Object mLock;
    NetdEventListenerService mNetdListener;
    private static final String TAG = IpConnectivityMetrics.class.getSimpleName();
    private static final ToIntFunction<Context> READ_BUFFER_SIZE = new ToIntFunction() { // from class: com.android.server.connectivity.IpConnectivityMetrics$$ExternalSyntheticLambda1
        @Override // java.util.function.ToIntFunction
        public final int applyAsInt(Object obj) {
            return IpConnectivityMetrics.lambda$static$1((Context) obj);
        }
    };

    /* loaded from: classes.dex */
    public interface Logger {
        DefaultNetworkMetrics defaultNetworkMetrics();
    }

    public IpConnectivityMetrics(Context ctx, ToIntFunction<Context> capacityGetter) {
        super(ctx);
        this.mLock = new Object();
        this.impl = new Impl();
        this.mEventLog = new RingBuffer<>(ConnectivityMetricsEvent.class, 500);
        this.mBuckets = makeRateLimitingBuckets();
        this.mDefaultNetworkMetrics = new DefaultNetworkMetrics();
        this.mCapacityGetter = capacityGetter;
        initBuffer();
    }

    public IpConnectivityMetrics(Context ctx) {
        this(ctx, READ_BUFFER_SIZE);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mNetdListener = new NetdEventListenerService(getContext());
            publishBinderService(SERVICE_NAME, this.impl);
            publishBinderService(NetdEventListenerService.SERVICE_NAME, this.mNetdListener);
            LocalServices.addService(Logger.class, new LoggerImpl());
        }
    }

    public int bufferCapacity() {
        return this.mCapacityGetter.applyAsInt(getContext());
    }

    private void initBuffer() {
        synchronized (this.mLock) {
            this.mDropped = 0;
            this.mCapacity = bufferCapacity();
            this.mBuffer = new ArrayList<>(this.mCapacity);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int append(ConnectivityMetricsEvent event) {
        synchronized (this.mLock) {
            this.mEventLog.append(event);
            int left = this.mCapacity - this.mBuffer.size();
            if (event == null) {
                return left;
            }
            if (isRateLimited(event)) {
                return -1;
            }
            if (left == 0) {
                this.mDropped++;
                return 0;
            }
            this.mBuffer.add(event);
            return left - 1;
        }
    }

    private boolean isRateLimited(ConnectivityMetricsEvent event) {
        TokenBucket tb = this.mBuckets.get(event.data.getClass());
        return (tb == null || tb.get()) ? false : true;
    }

    private String flushEncodedOutput() {
        ArrayList<ConnectivityMetricsEvent> events;
        int dropped;
        synchronized (this.mLock) {
            events = this.mBuffer;
            dropped = this.mDropped;
            initBuffer();
        }
        List<IpConnectivityLogClass.IpConnectivityEvent> protoEvents = IpConnectivityEventBuilder.toProto(events);
        this.mDefaultNetworkMetrics.flushEvents(protoEvents);
        NetdEventListenerService netdEventListenerService = this.mNetdListener;
        if (netdEventListenerService != null) {
            netdEventListenerService.flushStatistics(protoEvents);
        }
        try {
            byte[] data = IpConnectivityEventBuilder.serialize(dropped, protoEvents);
            return Base64.encodeToString(data, 0);
        } catch (IOException e) {
            Log.e(TAG, "could not serialize events", e);
            return "";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cmdFlush(PrintWriter pw) {
        pw.print(flushEncodedOutput());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cmdList(PrintWriter pw) {
        pw.println("metrics events:");
        List<ConnectivityMetricsEvent> events = getEvents();
        for (ConnectivityMetricsEvent ev : events) {
            pw.println(ev.toString());
        }
        pw.println("");
        NetdEventListenerService netdEventListenerService = this.mNetdListener;
        if (netdEventListenerService != null) {
            netdEventListenerService.list(pw);
        }
        pw.println("");
        this.mDefaultNetworkMetrics.listEvents(pw);
    }

    private List<IpConnectivityLogClass.IpConnectivityEvent> listEventsAsProtos() {
        List<IpConnectivityLogClass.IpConnectivityEvent> events = IpConnectivityEventBuilder.toProto(getEvents());
        NetdEventListenerService netdEventListenerService = this.mNetdListener;
        if (netdEventListenerService != null) {
            events.addAll(netdEventListenerService.listAsProtos());
        }
        events.addAll(this.mDefaultNetworkMetrics.listEventsAsProto());
        return events;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cmdListAsTextProto(final PrintWriter pw) {
        listEventsAsProtos().forEach(new Consumer() { // from class: com.android.server.connectivity.IpConnectivityMetrics$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                pw.print(((IpConnectivityLogClass.IpConnectivityEvent) obj).toString());
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cmdListAsBinaryProto(OutputStream out) {
        int dropped;
        synchronized (this.mLock) {
            dropped = this.mDropped;
        }
        try {
            byte[] data = IpConnectivityEventBuilder.serialize(dropped, listEventsAsProtos());
            out.write(data);
            out.flush();
        } catch (IOException e) {
            Log.e(TAG, "could not serialize events", e);
        }
    }

    private List<ConnectivityMetricsEvent> getEvents() {
        List<ConnectivityMetricsEvent> asList;
        synchronized (this.mLock) {
            asList = Arrays.asList((ConnectivityMetricsEvent[]) this.mEventLog.toArray());
        }
        return asList;
    }

    /* loaded from: classes.dex */
    public final class Impl extends IIpConnectivityMetrics.Stub {
        static final String CMD_DEFAULT = "";
        static final String CMD_FLUSH = "flush";
        static final String CMD_LIST = "list";
        static final String CMD_PROTO = "proto";
        static final String CMD_PROTO_BIN = "--proto";

        public Impl() {
        }

        public int logEvent(ConnectivityMetricsEvent event) {
            NetworkStack.checkNetworkStackPermission(IpConnectivityMetrics.this.getContext());
            return IpConnectivityMetrics.this.append(event);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:13:0x0026, code lost:
            if (r0.equals(com.android.server.connectivity.IpConnectivityMetrics.Impl.CMD_FLUSH) != false) goto L9;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            enforceDumpPermission();
            char c = 0;
            String cmd = args.length > 0 ? args[0] : "";
            switch (cmd.hashCode()) {
                case -1616754616:
                    if (cmd.equals("--proto")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 3322014:
                    if (cmd.equals(CMD_LIST)) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 97532676:
                    break;
                case 106940904:
                    if (cmd.equals(CMD_PROTO)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    IpConnectivityMetrics.this.cmdFlush(pw);
                    return;
                case 1:
                    IpConnectivityMetrics.this.cmdListAsTextProto(pw);
                    return;
                case 2:
                    IpConnectivityMetrics.this.cmdListAsBinaryProto(new FileOutputStream(fd));
                    return;
                default:
                    IpConnectivityMetrics.this.cmdList(pw);
                    return;
            }
        }

        private void enforceDumpPermission() {
            enforcePermission("android.permission.DUMP");
        }

        private void enforcePermission(String what) {
            IpConnectivityMetrics.this.getContext().enforceCallingOrSelfPermission(what, "IpConnectivityMetrics");
        }

        private void enforceNetdEventListeningPermission() {
            int uid = Binder.getCallingUid();
            if (uid != 1000 && uid != 1001) {
                throw new SecurityException(String.format("Uid %d has no permission to listen for netd events.", Integer.valueOf(uid)));
            }
        }

        public boolean addNetdEventCallback(int callerType, INetdEventCallback callback) {
            enforceNetdEventListeningPermission();
            if (IpConnectivityMetrics.this.mNetdListener == null) {
                return false;
            }
            return IpConnectivityMetrics.this.mNetdListener.addNetdEventCallback(callerType, callback);
        }

        public boolean removeNetdEventCallback(int callerType) {
            enforceNetdEventListeningPermission();
            if (IpConnectivityMetrics.this.mNetdListener == null) {
                return true;
            }
            return IpConnectivityMetrics.this.mNetdListener.removeNetdEventCallback(callerType);
        }

        public void logDefaultNetworkValidity(boolean valid) {
            NetworkStack.checkNetworkStackPermission(IpConnectivityMetrics.this.getContext());
            IpConnectivityMetrics.this.mDefaultNetworkMetrics.logDefaultNetworkValidity(SystemClock.elapsedRealtime(), valid);
        }

        public void logDefaultNetworkEvent(Network defaultNetwork, int score, boolean validated, LinkProperties lp, NetworkCapabilities nc, Network previousDefaultNetwork, int previousScore, LinkProperties previousLp, NetworkCapabilities previousNc) {
            NetworkStack.checkNetworkStackPermission(IpConnectivityMetrics.this.getContext());
            long timeMs = SystemClock.elapsedRealtime();
            IpConnectivityMetrics.this.mDefaultNetworkMetrics.logDefaultNetworkEvent(timeMs, defaultNetwork, score, validated, lp, nc, previousDefaultNetwork, previousScore, previousLp, previousNc);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$static$1(Context ctx) {
        int size = Settings.Global.getInt(ctx.getContentResolver(), "connectivity_metrics_buffer_size", 2000);
        if (size <= 0) {
            return 2000;
        }
        return Math.min(size, 20000);
    }

    private static ArrayMap<Class<?>, TokenBucket> makeRateLimitingBuckets() {
        ArrayMap<Class<?>, TokenBucket> map = new ArrayMap<>();
        map.put(ApfProgramEvent.class, new TokenBucket(60000, 50));
        return map;
    }

    /* loaded from: classes.dex */
    private class LoggerImpl implements Logger {
        private LoggerImpl() {
        }

        @Override // com.android.server.connectivity.IpConnectivityMetrics.Logger
        public DefaultNetworkMetrics defaultNetworkMetrics() {
            return IpConnectivityMetrics.this.mDefaultNetworkMetrics;
        }
    }
}
