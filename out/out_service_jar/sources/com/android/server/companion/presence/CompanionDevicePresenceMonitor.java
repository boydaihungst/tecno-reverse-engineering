package com.android.server.companion.presence;

import android.bluetooth.BluetoothAdapter;
import android.companion.AssociationInfo;
import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.server.companion.AssociationStore;
import com.android.server.companion.presence.BleCompanionDeviceScanner;
import com.android.server.companion.presence.BluetoothCompanionDeviceConnectionListener;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
/* loaded from: classes.dex */
public class CompanionDevicePresenceMonitor implements AssociationStore.OnChangeListener, BluetoothCompanionDeviceConnectionListener.Callback, BleCompanionDeviceScanner.Callback {
    static final boolean DEBUG = false;
    private static final String TAG = "CompanionDevice_PresenceMonitor";
    private final AssociationStore mAssociationStore;
    private final BleCompanionDeviceScanner mBleScanner;
    private final BluetoothCompanionDeviceConnectionListener mBtConnectionListener;
    private final Callback mCallback;
    private final Set<Integer> mConnectedBtDevices = new HashSet();
    private final Set<Integer> mNearbyBleDevices = new HashSet();
    private final Set<Integer> mReportedSelfManagedDevices = new HashSet();
    private final Set<Integer> mSimulated = new HashSet();
    private final SimulatedDevicePresenceSchedulerHelper mSchedulerHelper = new SimulatedDevicePresenceSchedulerHelper();

    /* loaded from: classes.dex */
    public interface Callback {
        void onDeviceAppeared(int i);

        void onDeviceDisappeared(int i);
    }

    public CompanionDevicePresenceMonitor(AssociationStore associationStore, Callback callback) {
        this.mAssociationStore = associationStore;
        this.mCallback = callback;
        this.mBtConnectionListener = new BluetoothCompanionDeviceConnectionListener(associationStore, this);
        this.mBleScanner = new BleCompanionDeviceScanner(associationStore, this);
    }

    public void init(Context context) {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            this.mBtConnectionListener.init(btAdapter);
            this.mBleScanner.init(context, btAdapter);
        } else {
            Log.w(TAG, "BluetoothAdapter is NOT available.");
        }
        this.mAssociationStore.registerListener(this);
    }

    public boolean isDevicePresent(int associationId) {
        return this.mReportedSelfManagedDevices.contains(Integer.valueOf(associationId)) || this.mConnectedBtDevices.contains(Integer.valueOf(associationId)) || this.mNearbyBleDevices.contains(Integer.valueOf(associationId)) || this.mSimulated.contains(Integer.valueOf(associationId));
    }

    public void onSelfManagedDeviceConnected(int associationId) {
        onDevicePresent(this.mReportedSelfManagedDevices, associationId, "application-reported");
    }

    public void onSelfManagedDeviceDisconnected(int associationId) {
        onDeviceGone(this.mReportedSelfManagedDevices, associationId, "application-reported");
    }

    public void onSelfManagedDeviceReporterBinderDied(int associationId) {
        onDeviceGone(this.mReportedSelfManagedDevices, associationId, "application-reported");
    }

    @Override // com.android.server.companion.presence.BluetoothCompanionDeviceConnectionListener.Callback
    public void onBluetoothCompanionDeviceConnected(int associationId) {
        onDevicePresent(this.mConnectedBtDevices, associationId, "bt");
    }

    @Override // com.android.server.companion.presence.BluetoothCompanionDeviceConnectionListener.Callback
    public void onBluetoothCompanionDeviceDisconnected(int associationId) {
        onDeviceGone(this.mConnectedBtDevices, associationId, "bt");
    }

    @Override // com.android.server.companion.presence.BleCompanionDeviceScanner.Callback
    public void onBleCompanionDeviceFound(int associationId) {
        onDevicePresent(this.mNearbyBleDevices, associationId, "ble");
    }

    @Override // com.android.server.companion.presence.BleCompanionDeviceScanner.Callback
    public void onBleCompanionDeviceLost(int associationId) {
        onDeviceGone(this.mNearbyBleDevices, associationId, "ble");
    }

    public void simulateDeviceAppeared(int associationId) {
        enforceCallerShellOrRoot();
        enforceAssociationExists(associationId);
        onDevicePresent(this.mSimulated, associationId, "simulated");
        this.mSchedulerHelper.scheduleOnDeviceGoneCallForSimulatedDevicePresence(associationId);
    }

    public void simulateDeviceDisappeared(int associationId) {
        enforceCallerShellOrRoot();
        enforceAssociationExists(associationId);
        this.mSchedulerHelper.unscheduleOnDeviceGoneCallForSimulatedDevicePresence(associationId);
        onDeviceGone(this.mSimulated, associationId, "simulated");
    }

    private void enforceAssociationExists(int associationId) {
        if (this.mAssociationStore.getAssociationById(associationId) == null) {
            throw new IllegalArgumentException("Association with id " + associationId + " does not exist.");
        }
    }

    private void onDevicePresent(Set<Integer> presentDevicesForSource, int newDeviceAssociationId, String sourceLoggingTag) {
        boolean alreadyPresent = isDevicePresent(newDeviceAssociationId);
        presentDevicesForSource.add(Integer.valueOf(newDeviceAssociationId));
        if (alreadyPresent) {
            return;
        }
        this.mCallback.onDeviceAppeared(newDeviceAssociationId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDeviceGone(Set<Integer> presentDevicesForSource, int goneDeviceAssociationId, String sourceLoggingTag) {
        boolean removed = presentDevicesForSource.remove(Integer.valueOf(goneDeviceAssociationId));
        if (!removed) {
            return;
        }
        boolean stillPresent = isDevicePresent(goneDeviceAssociationId);
        if (stillPresent) {
            return;
        }
        this.mCallback.onDeviceDisappeared(goneDeviceAssociationId);
    }

    @Override // com.android.server.companion.AssociationStore.OnChangeListener
    public void onAssociationRemoved(AssociationInfo association) {
        int id = association.getId();
        this.mConnectedBtDevices.remove(Integer.valueOf(id));
        this.mNearbyBleDevices.remove(Integer.valueOf(id));
        this.mReportedSelfManagedDevices.remove(Integer.valueOf(id));
    }

    private static void enforceCallerShellOrRoot() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 2000 || callingUid == 0) {
            return;
        }
        throw new SecurityException("Caller is neither Shell nor Root");
    }

    public void dump(PrintWriter out) {
        out.append("Companion Device Present: ");
        if (this.mConnectedBtDevices.isEmpty() && this.mNearbyBleDevices.isEmpty() && this.mReportedSelfManagedDevices.isEmpty()) {
            out.append("<empty>\n");
            return;
        }
        out.append("\n");
        out.append("  Connected Bluetooth Devices: ");
        if (this.mConnectedBtDevices.isEmpty()) {
            out.append("<empty>\n");
        } else {
            out.append("\n");
            for (Integer num : this.mConnectedBtDevices) {
                int associationId = num.intValue();
                AssociationInfo a = this.mAssociationStore.getAssociationById(associationId);
                out.append("    ").append((CharSequence) a.toShortString()).append('\n');
            }
        }
        out.append("  Nearby BLE Devices: ");
        if (this.mNearbyBleDevices.isEmpty()) {
            out.append("<empty>\n");
        } else {
            out.append("\n");
            for (Integer num2 : this.mNearbyBleDevices) {
                int associationId2 = num2.intValue();
                AssociationInfo a2 = this.mAssociationStore.getAssociationById(associationId2);
                out.append("    ").append((CharSequence) a2.toShortString()).append('\n');
            }
        }
        out.append("  Self-Reported Devices: ");
        if (this.mReportedSelfManagedDevices.isEmpty()) {
            out.append("<empty>\n");
            return;
        }
        out.append("\n");
        for (Integer num3 : this.mReportedSelfManagedDevices) {
            int associationId3 = num3.intValue();
            AssociationInfo a3 = this.mAssociationStore.getAssociationById(associationId3);
            out.append("    ").append((CharSequence) a3.toShortString()).append('\n');
        }
    }

    /* loaded from: classes.dex */
    private class SimulatedDevicePresenceSchedulerHelper extends Handler {
        SimulatedDevicePresenceSchedulerHelper() {
            super(Looper.getMainLooper());
        }

        void scheduleOnDeviceGoneCallForSimulatedDevicePresence(int associationId) {
            if (hasMessages(associationId)) {
                removeMessages(associationId);
            }
            sendEmptyMessageDelayed(associationId, 60000L);
        }

        void unscheduleOnDeviceGoneCallForSimulatedDevicePresence(int associationId) {
            removeMessages(associationId);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int associationId = msg.what;
            CompanionDevicePresenceMonitor companionDevicePresenceMonitor = CompanionDevicePresenceMonitor.this;
            companionDevicePresenceMonitor.onDeviceGone(companionDevicePresenceMonitor.mSimulated, associationId, "simulated");
        }
    }
}
