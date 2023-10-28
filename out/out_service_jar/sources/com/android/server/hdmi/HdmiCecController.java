package com.android.server.hdmi;

import android.hardware.hdmi.HdmiPortInfo;
import android.hardware.tv.cec.V1_0.HotplugEvent;
import android.hardware.tv.cec.V1_0.IHdmiCec;
import android.hardware.tv.cec.V1_0.IHdmiCecCallback;
import android.hardware.tv.cec.V1_1.CecMessage;
import android.hardware.tv.cec.V1_1.IHdmiCecCallback;
import android.icu.util.IllformedLocaleException;
import android.icu.util.ULocale;
import android.os.Binder;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.hdmi.HdmiAnnotations;
import com.android.server.hdmi.HdmiCecController;
import com.android.server.hdmi.HdmiControlService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Predicate;
import libcore.util.EmptyArray;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class HdmiCecController {
    private static final int ACTION_ON_RECEIVE_MSG = 2;
    private static final int CEC_DISABLED_DROP_MSG = 4;
    private static final int CEC_DISABLED_IGNORE = 1;
    private static final int CEC_DISABLED_LOG_WARNING = 2;
    private static final byte[] EMPTY_BODY = EmptyArray.BYTE;
    protected static final int HDMI_CEC_HAL_DEATH_COOKIE = 353;
    private static final int INITIAL_HDMI_MESSAGE_HISTORY_SIZE = 250;
    private static final int INVALID_PHYSICAL_ADDRESS = 65535;
    private static final int MAX_DEDICATED_ADDRESS = 11;
    private static final int NUM_LOGICAL_ADDRESS = 16;
    private static final String TAG = "HdmiCecController";
    private Handler mControlHandler;
    private final HdmiCecAtomWriter mHdmiCecAtomWriter;
    private Handler mIoHandler;
    private final NativeWrapper mNativeWrapperImpl;
    private final HdmiControlService mService;
    private final Predicate<Integer> mRemoteDeviceAddressPredicate = new Predicate<Integer>() { // from class: com.android.server.hdmi.HdmiCecController.1
        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Predicate
        public boolean test(Integer address) {
            return !HdmiCecController.this.mService.getHdmiCecNetwork().isAllocatedLocalDeviceAddress(address.intValue());
        }
    };
    private final Predicate<Integer> mSystemAudioAddressPredicate = new Predicate<Integer>() { // from class: com.android.server.hdmi.HdmiCecController.2
        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Predicate
        public boolean test(Integer address) {
            return HdmiUtils.isEligibleAddressForDevice(5, address.intValue());
        }
    };
    private ArrayBlockingQueue<Dumpable> mMessageHistory = new ArrayBlockingQueue<>(250);
    private final Object mMessageHistoryLock = new Object();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface AllocateAddressCallback {
        void onAllocated(int i, int i2);
    }

    /* loaded from: classes.dex */
    public static abstract class Dumpable {
        protected final long mTime = System.currentTimeMillis();

        /* JADX INFO: Access modifiers changed from: package-private */
        public abstract void dump(IndentingPrintWriter indentingPrintWriter, SimpleDateFormat simpleDateFormat);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public interface NativeWrapper {
        int nativeAddLogicalAddress(int i);

        void nativeClearLogicalAddress();

        void nativeEnableAudioReturnChannel(int i, boolean z);

        int nativeGetPhysicalAddress();

        HdmiPortInfo[] nativeGetPortInfos();

        int nativeGetVendorId();

        int nativeGetVersion();

        String nativeInit();

        boolean nativeIsConnected(int i);

        int nativeSendCecCommand(int i, int i2, byte[] bArr);

        void nativeSetLanguage(String str);

        void nativeSetOption(int i, boolean z);

        void setCallback(HdmiCecCallback hdmiCecCallback);
    }

    private HdmiCecController(HdmiControlService service, NativeWrapper nativeWrapper, HdmiCecAtomWriter atomWriter) {
        this.mService = service;
        this.mNativeWrapperImpl = nativeWrapper;
        this.mHdmiCecAtomWriter = atomWriter;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecController create(HdmiControlService service, HdmiCecAtomWriter atomWriter) {
        HdmiCecController controller = createWithNativeWrapper(service, new NativeWrapperImpl11(), atomWriter);
        if (controller != null) {
            return controller;
        }
        HdmiLogger.warning("Unable to use cec@1.1", new Object[0]);
        return createWithNativeWrapper(service, new NativeWrapperImpl(), atomWriter);
    }

    static HdmiCecController createWithNativeWrapper(HdmiControlService service, NativeWrapper nativeWrapper, HdmiCecAtomWriter atomWriter) {
        HdmiCecController controller = new HdmiCecController(service, nativeWrapper, atomWriter);
        String nativePtr = nativeWrapper.nativeInit();
        if (nativePtr == null) {
            HdmiLogger.warning("Couldn't get tv.cec service.", new Object[0]);
            return null;
        }
        controller.init(nativeWrapper);
        return controller;
    }

    private void init(NativeWrapper nativeWrapper) {
        this.mIoHandler = new Handler(this.mService.getIoLooper());
        this.mControlHandler = new Handler(this.mService.getServiceLooper());
        nativeWrapper.setCallback(new HdmiCecCallback());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void allocateLogicalAddress(final int deviceType, final int preferredAddress, final AllocateAddressCallback callback) {
        assertRunOnServiceThread();
        runOnIoThread(new Runnable() { // from class: com.android.server.hdmi.HdmiCecController.3
            @Override // java.lang.Runnable
            public void run() {
                HdmiCecController.this.handleAllocateLogicalAddress(deviceType, preferredAddress, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.IoThreadOnly
    public void handleAllocateLogicalAddress(final int deviceType, int preferredAddress, final AllocateAddressCallback callback) {
        assertRunOnIoThread();
        List<Integer> logicalAddressesToPoll = new ArrayList<>();
        if (HdmiUtils.isEligibleAddressForDevice(deviceType, preferredAddress)) {
            logicalAddressesToPoll.add(Integer.valueOf(preferredAddress));
        }
        for (int i = 0; i < 16; i++) {
            if (!logicalAddressesToPoll.contains(Integer.valueOf(i)) && HdmiUtils.isEligibleAddressForDevice(deviceType, i) && HdmiUtils.isEligibleAddressForCecVersion(this.mService.getCecVersion(), i)) {
                logicalAddressesToPoll.add(Integer.valueOf(i));
            }
        }
        int logicalAddress = 15;
        Iterator<Integer> it = logicalAddressesToPoll.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Integer logicalAddressToPoll = it.next();
            boolean acked = false;
            int j = 0;
            while (true) {
                if (j < 3) {
                    if (!sendPollMessage(logicalAddressToPoll.intValue(), logicalAddressToPoll.intValue(), 1)) {
                        j++;
                    } else {
                        acked = true;
                        continue;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (!acked) {
                logicalAddress = logicalAddressToPoll.intValue();
                break;
            }
        }
        final int assignedAddress = logicalAddress;
        HdmiLogger.debug("New logical address for device [%d]: [preferred:%d, assigned:%d]", Integer.valueOf(deviceType), Integer.valueOf(preferredAddress), Integer.valueOf(assignedAddress));
        if (callback != null) {
            runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiCecController.4
                @Override // java.lang.Runnable
                public void run() {
                    callback.onAllocated(deviceType, assignedAddress);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static byte[] buildBody(int opcode, byte[] params) {
        byte[] body = new byte[params.length + 1];
        body[0] = (byte) opcode;
        System.arraycopy(params, 0, body, 1, params.length);
        return body;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiPortInfo[] getPortInfos() {
        return this.mNativeWrapperImpl.nativeGetPortInfos();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public int addLogicalAddress(int newLogicalAddress) {
        assertRunOnServiceThread();
        if (HdmiUtils.isValidAddress(newLogicalAddress)) {
            return this.mNativeWrapperImpl.nativeAddLogicalAddress(newLogicalAddress);
        }
        return 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void clearLogicalAddress() {
        assertRunOnServiceThread();
        this.mNativeWrapperImpl.nativeClearLogicalAddress();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public int getPhysicalAddress() {
        assertRunOnServiceThread();
        return this.mNativeWrapperImpl.nativeGetPhysicalAddress();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public int getVersion() {
        assertRunOnServiceThread();
        return this.mNativeWrapperImpl.nativeGetVersion();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public int getVendorId() {
        assertRunOnServiceThread();
        return this.mNativeWrapperImpl.nativeGetVendorId();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void setOption(int flag, boolean enabled) {
        assertRunOnServiceThread();
        HdmiLogger.debug("setOption: [flag:%d, enabled:%b]", Integer.valueOf(flag), Boolean.valueOf(enabled));
        this.mNativeWrapperImpl.nativeSetOption(flag, enabled);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void setLanguage(String language) {
        assertRunOnServiceThread();
        if (!isLanguage(language)) {
            return;
        }
        this.mNativeWrapperImpl.nativeSetLanguage(language);
    }

    static boolean isLanguage(String language) {
        if (language == null || language.isEmpty()) {
            return false;
        }
        ULocale.Builder builder = new ULocale.Builder();
        try {
            builder.setLanguage(language);
            return true;
        } catch (IllformedLocaleException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void enableAudioReturnChannel(int port, boolean enabled) {
        assertRunOnServiceThread();
        this.mNativeWrapperImpl.nativeEnableAudioReturnChannel(port, enabled);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public boolean isConnected(int port) {
        assertRunOnServiceThread();
        return this.mNativeWrapperImpl.nativeIsConnected(port);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void pollDevices(HdmiControlService.DevicePollingCallback callback, int sourceAddress, int pickStrategy, int retryCount) {
        assertRunOnServiceThread();
        List<Integer> pollingCandidates = pickPollCandidates(pickStrategy);
        ArrayList<Integer> allocated = new ArrayList<>();
        runDevicePolling(sourceAddress, pollingCandidates, retryCount, callback, allocated);
    }

    private List<Integer> pickPollCandidates(int pickStrategy) {
        Predicate<Integer> pickPredicate;
        int strategy = pickStrategy & 3;
        switch (strategy) {
            case 2:
                pickPredicate = this.mSystemAudioAddressPredicate;
                break;
            default:
                pickPredicate = this.mRemoteDeviceAddressPredicate;
                break;
        }
        int iterationStrategy = 196608 & pickStrategy;
        LinkedList<Integer> pollingCandidates = new LinkedList<>();
        switch (iterationStrategy) {
            case 65536:
                for (int i = 0; i <= 14; i++) {
                    if (pickPredicate.test(Integer.valueOf(i))) {
                        pollingCandidates.add(Integer.valueOf(i));
                    }
                }
                break;
            default:
                for (int i2 = 14; i2 >= 0; i2--) {
                    if (pickPredicate.test(Integer.valueOf(i2))) {
                        pollingCandidates.add(Integer.valueOf(i2));
                    }
                }
                break;
        }
        return pollingCandidates;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void runDevicePolling(final int sourceAddress, final List<Integer> candidates, final int retryCount, final HdmiControlService.DevicePollingCallback callback, final List<Integer> allocated) {
        assertRunOnServiceThread();
        if (candidates.isEmpty()) {
            if (callback != null) {
                HdmiLogger.debug("[P]:AllocatedAddress=%s", allocated.toString());
                callback.onPollingFinished(allocated);
                return;
            }
            return;
        }
        final Integer candidate = candidates.remove(0);
        runOnIoThread(new Runnable() { // from class: com.android.server.hdmi.HdmiCecController.5
            @Override // java.lang.Runnable
            public void run() {
                if (HdmiCecController.this.sendPollMessage(sourceAddress, candidate.intValue(), retryCount)) {
                    allocated.add(candidate);
                }
                HdmiCecController.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiCecController.5.1
                    @Override // java.lang.Runnable
                    public void run() {
                        HdmiCecController.this.runDevicePolling(sourceAddress, candidates, retryCount, callback, allocated);
                    }
                });
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.IoThreadOnly
    public boolean sendPollMessage(int sourceAddress, int destinationAddress, int retryCount) {
        assertRunOnIoThread();
        for (int i = 0; i < retryCount; i++) {
            int ret = this.mNativeWrapperImpl.nativeSendCecCommand(sourceAddress, destinationAddress, EMPTY_BODY);
            if (ret == 0) {
                return true;
            }
            if (ret != 1) {
                HdmiLogger.warning("Failed to send a polling message(%d->%d) with return code %d", Integer.valueOf(sourceAddress), Integer.valueOf(destinationAddress), Integer.valueOf(ret));
            }
        }
        return false;
    }

    private void assertRunOnIoThread() {
        if (Looper.myLooper() != this.mIoHandler.getLooper()) {
            throw new IllegalStateException("Should run on io thread.");
        }
    }

    private void assertRunOnServiceThread() {
        if (Looper.myLooper() != this.mControlHandler.getLooper()) {
            throw new IllegalStateException("Should run on service thread.");
        }
    }

    void runOnIoThread(Runnable runnable) {
        this.mIoHandler.post(new WorkSourceUidPreservingRunnable(runnable));
    }

    void runOnServiceThread(Runnable runnable) {
        this.mControlHandler.post(new WorkSourceUidPreservingRunnable(runnable));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void flush(final Runnable runnable) {
        assertRunOnServiceThread();
        runOnIoThread(new Runnable() { // from class: com.android.server.hdmi.HdmiCecController.6
            @Override // java.lang.Runnable
            public void run() {
                HdmiCecController.this.runOnServiceThread(runnable);
            }
        });
    }

    private boolean isAcceptableAddress(int address) {
        if (address == 15) {
            return true;
        }
        return this.mService.getHdmiCecNetwork().isAllocatedLocalDeviceAddress(address);
    }

    @HdmiAnnotations.ServiceThreadOnly
    void onReceiveCommand(HdmiCecMessage message) {
        assertRunOnServiceThread();
        if (!this.mService.isControlEnabled() && !HdmiCecMessage.isCecTransportMessage(message.getOpcode())) {
            HdmiLogger.warning("Message " + message + " received when cec disabled", new Object[0]);
        }
        if (this.mService.isAddressAllocated() && !isAcceptableAddress(message.getDestination())) {
            return;
        }
        int messageState = this.mService.handleCecCommand(message);
        if (messageState == -2) {
            maySendFeatureAbortCommand(message, 0);
        } else if (messageState != -1) {
            maySendFeatureAbortCommand(message, messageState);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void maySendFeatureAbortCommand(HdmiCecMessage message, int reason) {
        int originalOpcode;
        assertRunOnServiceThread();
        int src = message.getDestination();
        int dest = message.getSource();
        if (src == 15 || dest == 15 || (originalOpcode = message.getOpcode()) == 0) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildFeatureAbortCommand(src, dest, originalOpcode, reason));
    }

    @HdmiAnnotations.ServiceThreadOnly
    void sendCommand(HdmiCecMessage cecMessage) {
        assertRunOnServiceThread();
        sendCommand(cecMessage, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getCallingUid() {
        int workSourceUid = Binder.getCallingWorkSourceUid();
        if (workSourceUid == -1) {
            return Binder.getCallingUid();
        }
        return workSourceUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void sendCommand(final HdmiCecMessage cecMessage, final HdmiControlService.SendMessageCallback callback) {
        assertRunOnServiceThread();
        addCecMessageToHistory(false, cecMessage);
        runOnIoThread(new Runnable() { // from class: com.android.server.hdmi.HdmiCecController.7
            @Override // java.lang.Runnable
            public void run() {
                final int errorCode;
                HdmiLogger.debug("[S]:" + cecMessage, new Object[0]);
                byte[] body = HdmiCecController.buildBody(cecMessage.getOpcode(), cecMessage.getParams());
                int retransmissionCount = 0;
                while (true) {
                    errorCode = HdmiCecController.this.mNativeWrapperImpl.nativeSendCecCommand(cecMessage.getSource(), cecMessage.getDestination(), body);
                    if (errorCode == 0) {
                        break;
                    }
                    int retransmissionCount2 = retransmissionCount + 1;
                    if (retransmissionCount >= 1) {
                        break;
                    }
                    retransmissionCount = retransmissionCount2;
                }
                if (errorCode != 0) {
                    Slog.w(HdmiCecController.TAG, "Failed to send " + cecMessage + " with errorCode=" + errorCode);
                }
                HdmiCecController.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiCecController.7.1
                    @Override // java.lang.Runnable
                    public void run() {
                        HdmiCecController.this.mHdmiCecAtomWriter.messageReported(cecMessage, 2, HdmiCecController.this.getCallingUid(), errorCode);
                        if (callback != null) {
                            callback.onSendCompleted(errorCode);
                        }
                    }
                });
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void handleIncomingCecCommand(int srcAddress, int dstAddress, byte[] body) {
        assertRunOnServiceThread();
        if (body.length == 0) {
            Slog.e(TAG, "Message with empty body received.");
            return;
        }
        HdmiCecMessage command = HdmiCecMessage.build(srcAddress, dstAddress, body[0], Arrays.copyOfRange(body, 1, body.length));
        if (command.getValidationResult() != 0) {
            Slog.e(TAG, "Invalid message received: " + command);
        }
        HdmiLogger.debug("[R]:" + command, new Object[0]);
        addCecMessageToHistory(true, command);
        this.mHdmiCecAtomWriter.messageReported(command, incomingMessageDirection(srcAddress, dstAddress), getCallingUid());
        onReceiveCommand(command);
    }

    private int incomingMessageDirection(int srcAddress, int dstAddress) {
        boolean sourceIsLocal = false;
        boolean destinationIsLocal = dstAddress == 15;
        for (HdmiCecLocalDevice localDevice : this.mService.getHdmiCecNetwork().getLocalDeviceList()) {
            int logicalAddress = localDevice.getDeviceInfo().getLogicalAddress();
            if (logicalAddress == srcAddress) {
                sourceIsLocal = true;
            }
            if (logicalAddress == dstAddress) {
                destinationIsLocal = true;
            }
        }
        if (!sourceIsLocal && destinationIsLocal) {
            return 3;
        }
        if (!sourceIsLocal || !destinationIsLocal) {
            return 1;
        }
        return 4;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @HdmiAnnotations.ServiceThreadOnly
    public void handleHotplug(int port, boolean connected) {
        assertRunOnServiceThread();
        HdmiLogger.debug("Hotplug event:[port:%d, connected:%b]", Integer.valueOf(port), Boolean.valueOf(connected));
        addHotplugEventToHistory(port, connected);
        this.mService.onHotplug(port, connected);
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void addHotplugEventToHistory(int port, boolean connected) {
        assertRunOnServiceThread();
        addEventToHistory(new HotplugHistoryRecord(port, connected));
    }

    @HdmiAnnotations.ServiceThreadOnly
    private void addCecMessageToHistory(boolean isReceived, HdmiCecMessage message) {
        assertRunOnServiceThread();
        addEventToHistory(new MessageHistoryRecord(isReceived, message));
    }

    private void addEventToHistory(Dumpable event) {
        synchronized (this.mMessageHistoryLock) {
            if (!this.mMessageHistory.offer(event)) {
                this.mMessageHistory.poll();
                this.mMessageHistory.offer(event);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getMessageHistorySize() {
        int size;
        synchronized (this.mMessageHistoryLock) {
            size = this.mMessageHistory.size() + this.mMessageHistory.remainingCapacity();
        }
        return size;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setMessageHistorySize(int newSize) {
        if (newSize < 250) {
            return false;
        }
        ArrayBlockingQueue<Dumpable> newMessageHistory = new ArrayBlockingQueue<>(newSize);
        synchronized (this.mMessageHistoryLock) {
            if (newSize < this.mMessageHistory.size()) {
                for (int i = 0; i < this.mMessageHistory.size() - newSize; i++) {
                    this.mMessageHistory.poll();
                }
            }
            newMessageHistory.addAll(this.mMessageHistory);
            this.mMessageHistory = newMessageHistory;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        pw.println("CEC message history:");
        pw.increaseIndent();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Iterator<Dumpable> it = this.mMessageHistory.iterator();
        while (it.hasNext()) {
            Dumpable record = it.next();
            record.dump(pw, sdf);
        }
        pw.decreaseIndent();
    }

    /* loaded from: classes.dex */
    private static final class NativeWrapperImpl11 implements NativeWrapper, IHwBinder.DeathRecipient, IHdmiCec.getPhysicalAddressCallback {
        private HdmiCecCallback mCallback;
        private android.hardware.tv.cec.V1_1.IHdmiCec mHdmiCec;
        private final Object mLock;
        private int mPhysicalAddress;

        private NativeWrapperImpl11() {
            this.mLock = new Object();
            this.mPhysicalAddress = 65535;
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public String nativeInit() {
            if (connectToHal()) {
                return this.mHdmiCec.toString();
            }
            return null;
        }

        boolean connectToHal() {
            try {
                android.hardware.tv.cec.V1_1.IHdmiCec service = android.hardware.tv.cec.V1_1.IHdmiCec.getService(true);
                this.mHdmiCec = service;
                try {
                    service.linkToDeath(this, 353L);
                } catch (RemoteException e) {
                    HdmiLogger.error("Couldn't link to death : ", e, new Object[0]);
                }
                return true;
            } catch (RemoteException | NoSuchElementException e2) {
                HdmiLogger.error("Couldn't connect to cec@1.1", e2, new Object[0]);
                return false;
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec.getPhysicalAddressCallback
        public void onValues(int result, short addr) {
            if (result == 0) {
                synchronized (this.mLock) {
                    this.mPhysicalAddress = new Short(addr).intValue();
                }
            }
        }

        public void serviceDied(long cookie) {
            if (cookie == 353) {
                HdmiLogger.error("Service died cookie : " + cookie + "; reconnecting", new Object[0]);
                connectToHal();
                HdmiCecCallback hdmiCecCallback = this.mCallback;
                if (hdmiCecCallback != null) {
                    setCallback(hdmiCecCallback);
                }
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public void setCallback(HdmiCecCallback callback) {
            this.mCallback = callback;
            try {
                this.mHdmiCec.setCallback_1_1(new HdmiCecCallback11(callback));
            } catch (RemoteException e) {
                HdmiLogger.error("Couldn't initialise tv.cec callback : ", e, new Object[0]);
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public int nativeSendCecCommand(int srcAddress, int dstAddress, byte[] body) {
            CecMessage message = new CecMessage();
            message.initiator = srcAddress;
            message.destination = dstAddress;
            message.body = new ArrayList<>(body.length);
            for (byte b : body) {
                message.body.add(Byte.valueOf(b));
            }
            try {
                return this.mHdmiCec.sendMessage_1_1(message);
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to send CEC message : ", e, new Object[0]);
                return 3;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public int nativeAddLogicalAddress(int logicalAddress) {
            try {
                return this.mHdmiCec.addLogicalAddress_1_1(logicalAddress);
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to add a logical address : ", e, new Object[0]);
                return 2;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public void nativeClearLogicalAddress() {
            try {
                this.mHdmiCec.clearLogicalAddress();
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to clear logical address : ", e, new Object[0]);
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public int nativeGetPhysicalAddress() {
            try {
                this.mHdmiCec.getPhysicalAddress(this);
                return this.mPhysicalAddress;
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to get physical address : ", e, new Object[0]);
                return 65535;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public int nativeGetVersion() {
            try {
                return this.mHdmiCec.getCecVersion();
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to get cec version : ", e, new Object[0]);
                return 1;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public int nativeGetVendorId() {
            try {
                return this.mHdmiCec.getVendorId();
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to get vendor id : ", e, new Object[0]);
                return 1;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public HdmiPortInfo[] nativeGetPortInfos() {
            try {
                ArrayList<android.hardware.tv.cec.V1_0.HdmiPortInfo> hdmiPortInfos = this.mHdmiCec.getPortInfo();
                HdmiPortInfo[] hdmiPortInfo = new HdmiPortInfo[hdmiPortInfos.size()];
                int i = 0;
                Iterator<android.hardware.tv.cec.V1_0.HdmiPortInfo> it = hdmiPortInfos.iterator();
                while (it.hasNext()) {
                    android.hardware.tv.cec.V1_0.HdmiPortInfo portInfo = it.next();
                    hdmiPortInfo[i] = new HdmiPortInfo(portInfo.portId, portInfo.type, portInfo.physicalAddress, portInfo.cecSupported, false, portInfo.arcSupported);
                    i++;
                }
                return hdmiPortInfo;
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to get port information : ", e, new Object[0]);
                return null;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public void nativeSetOption(int flag, boolean enabled) {
            try {
                this.mHdmiCec.setOption(flag, enabled);
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to set option : ", e, new Object[0]);
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public void nativeSetLanguage(String language) {
            try {
                this.mHdmiCec.setLanguage(language);
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to set language : ", e, new Object[0]);
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public void nativeEnableAudioReturnChannel(int port, boolean flag) {
            try {
                this.mHdmiCec.enableAudioReturnChannel(port, flag);
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to enable/disable ARC : ", e, new Object[0]);
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public boolean nativeIsConnected(int port) {
            try {
                return this.mHdmiCec.isConnected(port);
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to get connection info : ", e, new Object[0]);
                return false;
            }
        }
    }

    /* loaded from: classes.dex */
    private static final class NativeWrapperImpl implements NativeWrapper, IHwBinder.DeathRecipient, IHdmiCec.getPhysicalAddressCallback {
        private HdmiCecCallback mCallback;
        private IHdmiCec mHdmiCec;
        private final Object mLock;
        private int mPhysicalAddress;

        private NativeWrapperImpl() {
            this.mLock = new Object();
            this.mPhysicalAddress = 65535;
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public String nativeInit() {
            if (connectToHal()) {
                return this.mHdmiCec.toString();
            }
            return null;
        }

        boolean connectToHal() {
            try {
                IHdmiCec service = IHdmiCec.getService(true);
                this.mHdmiCec = service;
                try {
                    service.linkToDeath(this, 353L);
                } catch (RemoteException e) {
                    HdmiLogger.error("Couldn't link to death : ", e, new Object[0]);
                }
                return true;
            } catch (RemoteException | NoSuchElementException e2) {
                HdmiLogger.error("Couldn't connect to cec@1.0", e2, new Object[0]);
                return false;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public void setCallback(HdmiCecCallback callback) {
            this.mCallback = callback;
            try {
                this.mHdmiCec.setCallback(new HdmiCecCallback10(callback));
            } catch (RemoteException e) {
                HdmiLogger.error("Couldn't initialise tv.cec callback : ", e, new Object[0]);
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public int nativeSendCecCommand(int srcAddress, int dstAddress, byte[] body) {
            android.hardware.tv.cec.V1_0.CecMessage message = new android.hardware.tv.cec.V1_0.CecMessage();
            message.initiator = srcAddress;
            message.destination = dstAddress;
            message.body = new ArrayList<>(body.length);
            for (byte b : body) {
                message.body.add(Byte.valueOf(b));
            }
            try {
                return this.mHdmiCec.sendMessage(message);
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to send CEC message : ", e, new Object[0]);
                return 3;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public int nativeAddLogicalAddress(int logicalAddress) {
            try {
                return this.mHdmiCec.addLogicalAddress(logicalAddress);
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to add a logical address : ", e, new Object[0]);
                return 2;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public void nativeClearLogicalAddress() {
            try {
                this.mHdmiCec.clearLogicalAddress();
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to clear logical address : ", e, new Object[0]);
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public int nativeGetPhysicalAddress() {
            try {
                this.mHdmiCec.getPhysicalAddress(this);
                return this.mPhysicalAddress;
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to get physical address : ", e, new Object[0]);
                return 65535;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public int nativeGetVersion() {
            try {
                return this.mHdmiCec.getCecVersion();
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to get cec version : ", e, new Object[0]);
                return 1;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public int nativeGetVendorId() {
            try {
                return this.mHdmiCec.getVendorId();
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to get vendor id : ", e, new Object[0]);
                return 1;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public HdmiPortInfo[] nativeGetPortInfos() {
            try {
                ArrayList<android.hardware.tv.cec.V1_0.HdmiPortInfo> hdmiPortInfos = this.mHdmiCec.getPortInfo();
                HdmiPortInfo[] hdmiPortInfo = new HdmiPortInfo[hdmiPortInfos.size()];
                int i = 0;
                Iterator<android.hardware.tv.cec.V1_0.HdmiPortInfo> it = hdmiPortInfos.iterator();
                while (it.hasNext()) {
                    android.hardware.tv.cec.V1_0.HdmiPortInfo portInfo = it.next();
                    hdmiPortInfo[i] = new HdmiPortInfo(portInfo.portId, portInfo.type, portInfo.physicalAddress, portInfo.cecSupported, false, portInfo.arcSupported);
                    i++;
                }
                return hdmiPortInfo;
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to get port information : ", e, new Object[0]);
                return null;
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public void nativeSetOption(int flag, boolean enabled) {
            try {
                this.mHdmiCec.setOption(flag, enabled);
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to set option : ", e, new Object[0]);
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public void nativeSetLanguage(String language) {
            try {
                this.mHdmiCec.setLanguage(language);
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to set language : ", e, new Object[0]);
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public void nativeEnableAudioReturnChannel(int port, boolean flag) {
            try {
                this.mHdmiCec.enableAudioReturnChannel(port, flag);
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to enable/disable ARC : ", e, new Object[0]);
            }
        }

        @Override // com.android.server.hdmi.HdmiCecController.NativeWrapper
        public boolean nativeIsConnected(int port) {
            try {
                return this.mHdmiCec.isConnected(port);
            } catch (RemoteException e) {
                HdmiLogger.error("Failed to get connection info : ", e, new Object[0]);
                return false;
            }
        }

        public void serviceDied(long cookie) {
            if (cookie == 353) {
                HdmiLogger.error("Service died cookie : " + cookie + "; reconnecting", new Object[0]);
                connectToHal();
                HdmiCecCallback hdmiCecCallback = this.mCallback;
                if (hdmiCecCallback != null) {
                    setCallback(hdmiCecCallback);
                }
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec.getPhysicalAddressCallback
        public void onValues(int result, short addr) {
            if (result == 0) {
                synchronized (this.mLock) {
                    this.mPhysicalAddress = new Short(addr).intValue();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class HdmiCecCallback {
        HdmiCecCallback() {
        }

        public void onCecMessage(final int initiator, final int destination, final byte[] body) {
            HdmiCecController.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiCecController$HdmiCecCallback$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    HdmiCecController.HdmiCecCallback.this.m3741xf3948724(initiator, destination, body);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onCecMessage$0$com-android-server-hdmi-HdmiCecController$HdmiCecCallback  reason: not valid java name */
        public /* synthetic */ void m3741xf3948724(int initiator, int destination, byte[] body) {
            HdmiCecController.this.handleIncomingCecCommand(initiator, destination, body);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onHotplugEvent$1$com-android-server-hdmi-HdmiCecController$HdmiCecCallback  reason: not valid java name */
        public /* synthetic */ void m3742xf8e194de(int portId, boolean connected) {
            HdmiCecController.this.handleHotplug(portId, connected);
        }

        public void onHotplugEvent(final int portId, final boolean connected) {
            HdmiCecController.this.runOnServiceThread(new Runnable() { // from class: com.android.server.hdmi.HdmiCecController$HdmiCecCallback$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    HdmiCecController.HdmiCecCallback.this.m3742xf8e194de(portId, connected);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class HdmiCecCallback10 extends IHdmiCecCallback.Stub {
        private final HdmiCecCallback mHdmiCecCallback;

        HdmiCecCallback10(HdmiCecCallback hdmiCecCallback) {
            this.mHdmiCecCallback = hdmiCecCallback;
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCecCallback
        public void onCecMessage(android.hardware.tv.cec.V1_0.CecMessage message) throws RemoteException {
            byte[] body = new byte[message.body.size()];
            for (int i = 0; i < message.body.size(); i++) {
                body[i] = message.body.get(i).byteValue();
            }
            this.mHdmiCecCallback.onCecMessage(message.initiator, message.destination, body);
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCecCallback
        public void onHotplugEvent(HotplugEvent event) throws RemoteException {
            this.mHdmiCecCallback.onHotplugEvent(event.portId, event.connected);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class HdmiCecCallback11 extends IHdmiCecCallback.Stub {
        private final HdmiCecCallback mHdmiCecCallback;

        HdmiCecCallback11(HdmiCecCallback hdmiCecCallback) {
            this.mHdmiCecCallback = hdmiCecCallback;
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCecCallback
        public void onCecMessage_1_1(CecMessage message) throws RemoteException {
            byte[] body = new byte[message.body.size()];
            for (int i = 0; i < message.body.size(); i++) {
                body[i] = message.body.get(i).byteValue();
            }
            this.mHdmiCecCallback.onCecMessage(message.initiator, message.destination, body);
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCecCallback
        public void onCecMessage(android.hardware.tv.cec.V1_0.CecMessage message) throws RemoteException {
            byte[] body = new byte[message.body.size()];
            for (int i = 0; i < message.body.size(); i++) {
                body[i] = message.body.get(i).byteValue();
            }
            this.mHdmiCecCallback.onCecMessage(message.initiator, message.destination, body);
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCecCallback
        public void onHotplugEvent(HotplugEvent event) throws RemoteException {
            this.mHdmiCecCallback.onHotplugEvent(event.portId, event.connected);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class MessageHistoryRecord extends Dumpable {
        private final boolean mIsReceived;
        private final HdmiCecMessage mMessage;

        MessageHistoryRecord(boolean isReceived, HdmiCecMessage message) {
            this.mIsReceived = isReceived;
            this.mMessage = message;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.hdmi.HdmiCecController.Dumpable
        public void dump(IndentingPrintWriter pw, SimpleDateFormat sdf) {
            pw.print(this.mIsReceived ? "[R]" : "[S]");
            pw.print(" time=");
            pw.print(sdf.format(new Date(this.mTime)));
            pw.print(" message=");
            pw.println(this.mMessage);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class HotplugHistoryRecord extends Dumpable {
        private final boolean mConnected;
        private final int mPort;

        HotplugHistoryRecord(int port, boolean connected) {
            this.mPort = port;
            this.mConnected = connected;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.hdmi.HdmiCecController.Dumpable
        public void dump(IndentingPrintWriter pw, SimpleDateFormat sdf) {
            pw.print("[H]");
            pw.print(" time=");
            pw.print(sdf.format(new Date(this.mTime)));
            pw.print(" hotplug port=");
            pw.print(this.mPort);
            pw.print(" connected=");
            pw.println(this.mConnected);
        }
    }
}
