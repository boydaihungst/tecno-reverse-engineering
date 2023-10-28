package com.android.server.companion.presence;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.companion.AssociationInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import com.android.server.companion.AssociationStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class BleCompanionDeviceScanner implements AssociationStore.OnChangeListener {
    private static final int NOTIFY_DEVICE_LOST_DELAY = 120000;
    private static final ScanSettings SCAN_SETTINGS = new ScanSettings.Builder().setCallbackType(6).setScanMode(0).build();
    private static final String TAG = "CompanionDevice_PresenceMonitor_BLE";
    private final AssociationStore mAssociationStore;
    private BluetoothLeScanner mBleScanner;
    private BluetoothAdapter mBtAdapter;
    private final Callback mCallback;
    private boolean mScanning = false;
    private final ScanCallback mScanCallback = new ScanCallback() { // from class: com.android.server.companion.presence.BleCompanionDeviceScanner.2
        @Override // android.bluetooth.le.ScanCallback
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            switch (callbackType) {
                case 2:
                    if (BleCompanionDeviceScanner.this.mMainThreadHandler.hasNotifyDeviceLostMessages(device)) {
                        BleCompanionDeviceScanner.this.mMainThreadHandler.removeNotifyDeviceLostMessages(device);
                        return;
                    } else {
                        BleCompanionDeviceScanner.this.notifyDeviceFound(device);
                        return;
                    }
                case 3:
                default:
                    Slog.wtf(BleCompanionDeviceScanner.TAG, "Unexpected callback " + BleCompanionDeviceScanner.nameForBleScanCallbackType(callbackType));
                    return;
                case 4:
                    BleCompanionDeviceScanner.this.mMainThreadHandler.sendNotifyDeviceLostDelayedMessage(device);
                    return;
            }
        }

        @Override // android.bluetooth.le.ScanCallback
        public void onScanFailed(int errorCode) {
            BleCompanionDeviceScanner.this.mScanning = false;
        }
    };
    private final MainThreadHandler mMainThreadHandler = new MainThreadHandler();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Callback {
        void onBleCompanionDeviceFound(int i);

        void onBleCompanionDeviceLost(int i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BleCompanionDeviceScanner(AssociationStore associationStore, Callback callback) {
        this.mAssociationStore = associationStore;
        this.mCallback = callback;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(Context context, BluetoothAdapter btAdapter) {
        if (this.mBtAdapter != null) {
            throw new IllegalStateException(getClass().getSimpleName() + " is already initialized");
        }
        this.mBtAdapter = (BluetoothAdapter) Objects.requireNonNull(btAdapter);
        checkBleState();
        registerBluetoothStateBroadcastReceiver(context);
        this.mAssociationStore.registerListener(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void restartScan() {
        enforceInitialized();
        if (this.mBleScanner == null) {
            return;
        }
        stopScanIfNeeded();
        startScan();
    }

    @Override // com.android.server.companion.AssociationStore.OnChangeListener
    public void onAssociationChanged(int changeType, AssociationInfo association) {
        if (Looper.getMainLooper().isCurrentThread()) {
            restartScan();
        } else {
            this.mMainThreadHandler.post(new Runnable() { // from class: com.android.server.companion.presence.BleCompanionDeviceScanner$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BleCompanionDeviceScanner.this.restartScan();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkBleState() {
        enforceInitialized();
        boolean bleAvailable = this.mBtAdapter.isLeEnabled();
        if (!bleAvailable || this.mBleScanner == null) {
            if (!bleAvailable && this.mBleScanner == null) {
                return;
            }
            if (bleAvailable) {
                BluetoothLeScanner bluetoothLeScanner = this.mBtAdapter.getBluetoothLeScanner();
                this.mBleScanner = bluetoothLeScanner;
                if (bluetoothLeScanner == null) {
                    return;
                }
                startScan();
                return;
            }
            stopScanIfNeeded();
            this.mBleScanner = null;
        }
    }

    private void startScan() {
        String macAddress;
        enforceInitialized();
        if (this.mScanning) {
            throw new IllegalStateException("Scan is already in progress.");
        }
        if (this.mBleScanner == null) {
            throw new IllegalStateException("BLE is not available.");
        }
        Set<String> macAddresses = new HashSet<>();
        for (AssociationInfo association : this.mAssociationStore.getAssociations()) {
            if (association.isNotifyOnDeviceNearby() && (macAddress = association.getDeviceMacAddressAsString()) != null) {
                macAddresses.add(macAddress);
            }
        }
        if (macAddresses.isEmpty()) {
            return;
        }
        List<ScanFilter> filters = new ArrayList<>(macAddresses.size());
        for (String macAddress2 : macAddresses) {
            ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(macAddress2).build();
            filters.add(filter);
        }
        this.mBleScanner.startScan(filters, SCAN_SETTINGS, this.mScanCallback);
        this.mScanning = true;
    }

    private void stopScanIfNeeded() {
        enforceInitialized();
        if (!this.mScanning) {
            return;
        }
        if (this.mBtAdapter.isLeEnabled()) {
            try {
                this.mBleScanner.stopScan(this.mScanCallback);
            } catch (RuntimeException e) {
                Slog.w(TAG, "Exception while stopping BLE scanning", e);
            }
        }
        this.mScanning = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyDeviceFound(BluetoothDevice device) {
        List<AssociationInfo> associations = this.mAssociationStore.getAssociationsByAddress(device.getAddress());
        for (AssociationInfo association : associations) {
            this.mCallback.onBleCompanionDeviceFound(association.getId());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyDeviceLost(BluetoothDevice device) {
        List<AssociationInfo> associations = this.mAssociationStore.getAssociationsByAddress(device.getAddress());
        for (AssociationInfo association : associations) {
            this.mCallback.onBleCompanionDeviceLost(association.getId());
        }
    }

    private void registerBluetoothStateBroadcastReceiver(Context context) {
        BroadcastReceiver receiver = new BroadcastReceiver() { // from class: com.android.server.companion.presence.BleCompanionDeviceScanner.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                intent.getIntExtra("android.bluetooth.adapter.extra.PREVIOUS_STATE", -1);
                intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1);
                BleCompanionDeviceScanner.this.checkBleState();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.BLE_STATE_CHANGED");
        context.registerReceiver(receiver, filter);
    }

    private void enforceInitialized() {
        if (this.mBtAdapter == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " is not initialized");
        }
    }

    /* loaded from: classes.dex */
    private class MainThreadHandler extends Handler {
        private static final int NOTIFY_DEVICE_LOST = 1;

        MainThreadHandler() {
            super(Looper.getMainLooper());
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            if (message.what != 1) {
                return;
            }
            BluetoothDevice device = (BluetoothDevice) message.obj;
            BleCompanionDeviceScanner.this.notifyDeviceLost(device);
        }

        void sendNotifyDeviceLostDelayedMessage(BluetoothDevice device) {
            Message message = obtainMessage(1, device);
            sendMessageDelayed(message, 120000L);
        }

        boolean hasNotifyDeviceLostMessages(BluetoothDevice device) {
            return hasEqualMessages(1, device);
        }

        void removeNotifyDeviceLostMessages(BluetoothDevice device) {
            removeEqualMessages(1, device);
        }
    }

    private static String nameForBtState(int state) {
        return BluetoothAdapter.nameForState(state) + "(" + state + ")";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String nameForBleScanCallbackType(int callbackType) {
        String name;
        switch (callbackType) {
            case 1:
                name = "ALL_MATCHES";
                break;
            case 2:
                name = "FIRST_MATCH";
                break;
            case 3:
            default:
                name = "Unknown";
                break;
            case 4:
                name = "MATCH_LOST";
                break;
        }
        return name + "(" + callbackType + ")";
    }

    private static String nameForBleScanErrorCode(int errorCode) {
        String name;
        switch (errorCode) {
            case 1:
                name = "ALREADY_STARTED";
                break;
            case 2:
                name = "APPLICATION_REGISTRATION_FAILED";
                break;
            case 3:
                name = "INTERNAL_ERROR";
                break;
            case 4:
                name = "FEATURE_UNSUPPORTED";
                break;
            case 5:
                name = "OUT_OF_HARDWARE_RESOURCES";
                break;
            case 6:
                name = "SCANNING_TOO_FREQUENTLY";
                break;
            default:
                name = "Unknown";
                break;
        }
        return name + "(" + errorCode + ")";
    }
}
