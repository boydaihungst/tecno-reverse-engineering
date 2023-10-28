package com.android.server.companion.presence;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationInfo;
import android.net.MacAddress;
import android.os.Handler;
import android.os.HandlerExecutor;
import com.android.server.companion.AssociationStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
class BluetoothCompanionDeviceConnectionListener extends BluetoothAdapter.BluetoothConnectionCallback implements AssociationStore.OnChangeListener {
    private static final String TAG = "CompanionDevice_PresenceMonitor_BT";
    private final Map<MacAddress, BluetoothDevice> mAllConnectedDevices = new HashMap();
    private final AssociationStore mAssociationStore;
    private final Callback mCallback;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Callback {
        void onBluetoothCompanionDeviceConnected(int i);

        void onBluetoothCompanionDeviceDisconnected(int i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BluetoothCompanionDeviceConnectionListener(AssociationStore associationStore, Callback callback) {
        this.mAssociationStore = associationStore;
        this.mCallback = callback;
    }

    public void init(BluetoothAdapter btAdapter) {
        btAdapter.registerBluetoothConnectionCallback(new HandlerExecutor(Handler.getMain()), this);
        this.mAssociationStore.registerListener(this);
    }

    public void onDeviceConnected(BluetoothDevice device) {
        MacAddress macAddress = MacAddress.fromString(device.getAddress());
        if (this.mAllConnectedDevices.put(macAddress, device) != null) {
            return;
        }
        onDeviceConnectivityChanged(device, true);
    }

    public void onDeviceDisconnected(BluetoothDevice device, int reason) {
        MacAddress macAddress = MacAddress.fromString(device.getAddress());
        if (this.mAllConnectedDevices.remove(macAddress) == null) {
            return;
        }
        onDeviceConnectivityChanged(device, false);
    }

    private void onDeviceConnectivityChanged(BluetoothDevice device, boolean connected) {
        List<AssociationInfo> associations = this.mAssociationStore.getAssociationsByAddress(device.getAddress());
        for (AssociationInfo association : associations) {
            int id = association.getId();
            if (connected) {
                this.mCallback.onBluetoothCompanionDeviceConnected(id);
            } else {
                this.mCallback.onBluetoothCompanionDeviceDisconnected(id);
            }
        }
    }

    @Override // com.android.server.companion.AssociationStore.OnChangeListener
    public void onAssociationAdded(AssociationInfo association) {
        if (this.mAllConnectedDevices.containsKey(association.getDeviceMacAddress())) {
            this.mCallback.onBluetoothCompanionDeviceConnected(association.getId());
        }
    }

    @Override // com.android.server.companion.AssociationStore.OnChangeListener
    public void onAssociationRemoved(AssociationInfo association) {
    }

    @Override // com.android.server.companion.AssociationStore.OnChangeListener
    public void onAssociationUpdated(AssociationInfo association, boolean addressChanged) {
        if (!addressChanged) {
            return;
        }
        throw new IllegalArgumentException("Address changes are not supported.");
    }
}
