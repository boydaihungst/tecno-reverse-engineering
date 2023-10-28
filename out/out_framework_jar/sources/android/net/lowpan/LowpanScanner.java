package android.net.lowpan;

import android.net.lowpan.ILowpanEnergyScanCallback;
import android.net.lowpan.ILowpanNetScanCallback;
import android.net.lowpan.LowpanScanner;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;
/* loaded from: classes2.dex */
public class LowpanScanner {
    private static final String TAG = LowpanScanner.class.getSimpleName();
    private ILowpanInterface mBinder;
    private Callback mCallback = null;
    private Handler mHandler = null;
    private ArrayList<Integer> mChannelMask = null;
    private int mTxPower = Integer.MAX_VALUE;

    /* loaded from: classes2.dex */
    public static abstract class Callback {
        public void onNetScanBeacon(LowpanBeaconInfo beacon) {
        }

        public void onEnergyScanResult(LowpanEnergyScanResult result) {
        }

        public void onScanFinished() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LowpanScanner(ILowpanInterface binder) {
        this.mBinder = binder;
    }

    public synchronized void setCallback(Callback cb, Handler handler) {
        this.mCallback = cb;
        this.mHandler = handler;
    }

    public void setCallback(Callback cb) {
        setCallback(cb, null);
    }

    public void setChannelMask(Collection<Integer> mask) {
        if (mask == null) {
            this.mChannelMask = null;
            return;
        }
        ArrayList<Integer> arrayList = this.mChannelMask;
        if (arrayList == null) {
            this.mChannelMask = new ArrayList<>();
        } else {
            arrayList.clear();
        }
        this.mChannelMask.addAll(mask);
    }

    public Collection<Integer> getChannelMask() {
        return (Collection) this.mChannelMask.clone();
    }

    public void addChannel(int channel) {
        if (this.mChannelMask == null) {
            this.mChannelMask = new ArrayList<>();
        }
        this.mChannelMask.add(Integer.valueOf(channel));
    }

    public void setTxPower(int txPower) {
        this.mTxPower = txPower;
    }

    public int getTxPower() {
        return this.mTxPower;
    }

    private Map<String, Object> createScanOptionMap() {
        Map<String, Object> map = new HashMap<>();
        if (this.mChannelMask != null) {
            LowpanProperties.KEY_CHANNEL_MASK.putInMap(map, this.mChannelMask.stream().mapToInt(new ToIntFunction() { // from class: android.net.lowpan.LowpanScanner$$ExternalSyntheticLambda0
                @Override // java.util.function.ToIntFunction
                public final int applyAsInt(Object obj) {
                    int intValue;
                    intValue = ((Integer) obj).intValue();
                    return intValue;
                }
            }).toArray());
        }
        if (this.mTxPower != Integer.MAX_VALUE) {
            LowpanProperties.KEY_MAX_TX_POWER.putInMap(map, Integer.valueOf(this.mTxPower));
        }
        return map;
    }

    public void startNetScan() throws LowpanException {
        Map<String, Object> map = createScanOptionMap();
        ILowpanNetScanCallback binderListener = new AnonymousClass1();
        try {
            this.mBinder.startNetScan(map, binderListener);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    /* renamed from: android.net.lowpan.LowpanScanner$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass1 extends ILowpanNetScanCallback.Stub {
        AnonymousClass1() {
        }

        @Override // android.net.lowpan.ILowpanNetScanCallback
        public void onNetScanBeacon(final LowpanBeaconInfo beaconInfo) {
            final Callback callback;
            Handler handler;
            synchronized (LowpanScanner.this) {
                callback = LowpanScanner.this.mCallback;
                handler = LowpanScanner.this.mHandler;
            }
            if (callback == null) {
                return;
            }
            Runnable runnable = new Runnable() { // from class: android.net.lowpan.LowpanScanner$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanScanner.Callback.this.onNetScanBeacon(beaconInfo);
                }
            };
            if (handler != null) {
                handler.post(runnable);
            } else {
                runnable.run();
            }
        }

        @Override // android.net.lowpan.ILowpanNetScanCallback
        public void onNetScanFinished() {
            final Callback callback;
            Handler handler;
            synchronized (LowpanScanner.this) {
                callback = LowpanScanner.this.mCallback;
                handler = LowpanScanner.this.mHandler;
            }
            if (callback == null) {
                return;
            }
            Runnable runnable = new Runnable() { // from class: android.net.lowpan.LowpanScanner$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanScanner.Callback.this.onScanFinished();
                }
            };
            if (handler != null) {
                handler.post(runnable);
            } else {
                runnable.run();
            }
        }
    }

    public void stopNetScan() {
        try {
            this.mBinder.stopNetScan();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public void startEnergyScan() throws LowpanException {
        Map<String, Object> map = createScanOptionMap();
        ILowpanEnergyScanCallback binderListener = new AnonymousClass2();
        try {
            this.mBinder.startEnergyScan(map, binderListener);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    /* renamed from: android.net.lowpan.LowpanScanner$2  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass2 extends ILowpanEnergyScanCallback.Stub {
        AnonymousClass2() {
        }

        @Override // android.net.lowpan.ILowpanEnergyScanCallback
        public void onEnergyScanResult(final int channel, final int rssi) {
            final Callback callback = LowpanScanner.this.mCallback;
            Handler handler = LowpanScanner.this.mHandler;
            if (callback == null) {
                return;
            }
            Runnable runnable = new Runnable() { // from class: android.net.lowpan.LowpanScanner$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanScanner.AnonymousClass2.lambda$onEnergyScanResult$0(LowpanScanner.Callback.this, channel, rssi);
                }
            };
            if (handler != null) {
                handler.post(runnable);
            } else {
                runnable.run();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$onEnergyScanResult$0(Callback callback, int channel, int rssi) {
            if (callback != null) {
                LowpanEnergyScanResult result = new LowpanEnergyScanResult();
                result.setChannel(channel);
                result.setMaxRssi(rssi);
                callback.onEnergyScanResult(result);
            }
        }

        @Override // android.net.lowpan.ILowpanEnergyScanCallback
        public void onEnergyScanFinished() {
            final Callback callback = LowpanScanner.this.mCallback;
            Handler handler = LowpanScanner.this.mHandler;
            if (callback == null) {
                return;
            }
            Runnable runnable = new Runnable() { // from class: android.net.lowpan.LowpanScanner$2$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanScanner.Callback.this.onScanFinished();
                }
            };
            if (handler != null) {
                handler.post(runnable);
            } else {
                runnable.run();
            }
        }
    }

    public void stopEnergyScan() {
        try {
            this.mBinder.stopEnergyScan();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }
}
