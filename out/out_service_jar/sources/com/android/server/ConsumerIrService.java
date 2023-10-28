package com.android.server;

import android.content.Context;
import android.hardware.IConsumerIrService;
import android.hardware.ir.ConsumerIrFreqRange;
import android.hardware.ir.IConsumerIr;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
/* loaded from: classes.dex */
public class ConsumerIrService extends IConsumerIrService.Stub {
    private static final int MAX_XMIT_TIME = 2000000;
    private static final String TAG = "ConsumerIrService";
    private final Context mContext;
    private final boolean mHasNativeHal;
    private final PowerManager.WakeLock mWakeLock;
    private final Object mHalLock = new Object();
    private IConsumerIr mAidlService = null;

    private static native boolean getHidlHalService();

    private static native int[] halGetCarrierFrequencies();

    private static native int halTransmit(int i, int[] iArr);

    /* JADX INFO: Access modifiers changed from: package-private */
    public ConsumerIrService(Context context) {
        this.mContext = context;
        PowerManager pm = (PowerManager) context.getSystemService("power");
        PowerManager.WakeLock newWakeLock = pm.newWakeLock(1, TAG);
        this.mWakeLock = newWakeLock;
        newWakeLock.setReferenceCounted(true);
        boolean halService = getHalService();
        this.mHasNativeHal = halService;
        if (context.getPackageManager().hasSystemFeature("android.hardware.consumerir")) {
            if (!halService) {
                throw new RuntimeException("FEATURE_CONSUMER_IR present, but no IR HAL loaded!");
            }
        } else if (halService) {
            throw new RuntimeException("IR HAL present, but FEATURE_CONSUMER_IR is not set!");
        }
    }

    public boolean hasIrEmitter() {
        return this.mHasNativeHal;
    }

    private boolean getHalService() {
        String fqName = IConsumerIr.DESCRIPTOR + "/default";
        IConsumerIr asInterface = IConsumerIr.Stub.asInterface(ServiceManager.waitForDeclaredService(fqName));
        this.mAidlService = asInterface;
        if (asInterface != null) {
            return true;
        }
        return getHidlHalService();
    }

    private void throwIfNoIrEmitter() {
        if (!this.mHasNativeHal) {
            throw new UnsupportedOperationException("IR emitter not available");
        }
    }

    public void transmit(String packageName, int carrierFrequency, int[] pattern) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.TRANSMIT_IR") != 0) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }
        long totalXmitTime = 0;
        for (int slice : pattern) {
            if (slice <= 0) {
                throw new IllegalArgumentException("Non-positive IR slice");
            }
            totalXmitTime += slice;
        }
        if (totalXmitTime > 2000000) {
            throw new IllegalArgumentException("IR pattern too long");
        }
        throwIfNoIrEmitter();
        synchronized (this.mHalLock) {
            IConsumerIr iConsumerIr = this.mAidlService;
            if (iConsumerIr != null) {
                try {
                    iConsumerIr.transmit(carrierFrequency, pattern);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error transmitting frequency: " + carrierFrequency);
                }
            } else {
                int err = halTransmit(carrierFrequency, pattern);
                if (err < 0) {
                    Slog.e(TAG, "Error transmitting: " + err);
                }
            }
        }
    }

    public int[] getCarrierFrequencies() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.TRANSMIT_IR") != 0) {
            throw new SecurityException("Requires TRANSMIT_IR permission");
        }
        throwIfNoIrEmitter();
        synchronized (this.mHalLock) {
            IConsumerIr iConsumerIr = this.mAidlService;
            if (iConsumerIr != null) {
                try {
                    ConsumerIrFreqRange[] output = iConsumerIr.getCarrierFreqs();
                    if (output.length <= 0) {
                        Slog.e(TAG, "Error getting carrier frequencies.");
                    }
                    int[] result = new int[output.length * 2];
                    for (int i = 0; i < output.length; i++) {
                        result[i * 2] = output[i].minHz;
                        result[(i * 2) + 1] = output[i].maxHz;
                    }
                    return result;
                } catch (RemoteException e) {
                    return null;
                }
            }
            return halGetCarrierFrequencies();
        }
    }
}
