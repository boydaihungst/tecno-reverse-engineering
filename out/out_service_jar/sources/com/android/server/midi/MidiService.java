package com.android.server.midi;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import android.media.midi.IBluetoothMidiService;
import android.media.midi.IMidiDeviceListener;
import android.media.midi.IMidiDeviceOpenCallback;
import android.media.midi.IMidiDeviceServer;
import android.media.midi.IMidiManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.EventLog;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
/* loaded from: classes2.dex */
public class MidiService extends IMidiManager.Stub {
    private static final int MAX_CONNECTIONS_PER_CLIENT = 64;
    private static final int MAX_DEVICE_SERVERS_PER_UID = 16;
    private static final int MAX_LISTENERS_PER_CLIENT = 16;
    private static final String MIDI_LEGACY_STRING = "MIDI 1.0";
    private static final String MIDI_UNIVERSAL_STRING = "MIDI 2.0";
    private static final String TAG = "MidiService";
    private final BroadcastReceiver mBleMidiReceiver;
    private int mBluetoothServiceUid;
    private final Context mContext;
    private final HashSet<ParcelUuid> mNonMidiUUIDs;
    private final PackageManager mPackageManager;
    private final PackageMonitor mPackageMonitor;
    private static final UUID MIDI_SERVICE = UUID.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700");
    private static final MidiDeviceInfo[] EMPTY_DEVICE_INFO_ARRAY = new MidiDeviceInfo[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private final HashMap<IBinder, Client> mClients = new HashMap<>();
    private final HashMap<MidiDeviceInfo, Device> mDevicesByInfo = new HashMap<>();
    private final HashMap<BluetoothDevice, Device> mBluetoothDevices = new HashMap<>();
    private final HashMap<BluetoothDevice, MidiDevice> mBleMidiDeviceMap = new HashMap<>();
    private final HashMap<IBinder, Device> mDevicesByServer = new HashMap<>();
    private int mNextDeviceId = 1;
    private final Object mUsbMidiLock = new Object();
    private final HashMap<String, Integer> mUsbMidiLegacyDeviceOpenCount = new HashMap<>();
    private final HashSet<String> mUsbMidiUniversalDeviceInUse = new HashSet<>();

    /* loaded from: classes2.dex */
    public static class Lifecycle extends SystemService {
        private MidiService mMidiService;

        public Lifecycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.midi.MidiService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.midi.MidiService, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            ?? midiService = new MidiService(getContext());
            this.mMidiService = midiService;
            publishBinderService("midi", midiService);
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser user) {
            if (user.getUserIdentifier() == 0) {
                this.mMidiService.onUnlockUser();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class Client implements IBinder.DeathRecipient {
        private static final String TAG = "MidiService.Client";
        private final IBinder mToken;
        private final HashMap<IBinder, IMidiDeviceListener> mListeners = new HashMap<>();
        private final HashMap<IBinder, DeviceConnection> mDeviceConnections = new HashMap<>();
        private final int mUid = Binder.getCallingUid();
        private final int mPid = Binder.getCallingPid();

        public Client(IBinder token) {
            this.mToken = token;
        }

        public int getUid() {
            return this.mUid;
        }

        public void addListener(IMidiDeviceListener listener) {
            if (this.mListeners.size() >= 16) {
                throw new SecurityException("too many MIDI listeners for UID = " + this.mUid);
            }
            this.mListeners.put(listener.asBinder(), listener);
        }

        public void removeListener(IMidiDeviceListener listener) {
            this.mListeners.remove(listener.asBinder());
            if (this.mListeners.size() == 0 && this.mDeviceConnections.size() == 0) {
                close();
            }
        }

        public void addDeviceConnection(Device device, IMidiDeviceOpenCallback callback) {
            Log.d(TAG, "addDeviceConnection() device:" + device);
            if (this.mDeviceConnections.size() >= 64) {
                Log.i(TAG, "too many MIDI connections for UID = " + this.mUid);
                throw new SecurityException("too many MIDI connections for UID = " + this.mUid);
            }
            DeviceConnection connection = new DeviceConnection(device, this, callback);
            this.mDeviceConnections.put(connection.getToken(), connection);
            device.addDeviceConnection(connection);
        }

        public void removeDeviceConnection(IBinder token) {
            DeviceConnection connection = this.mDeviceConnections.remove(token);
            if (connection != null) {
                connection.getDevice().removeDeviceConnection(connection);
            }
            if (this.mListeners.size() == 0 && this.mDeviceConnections.size() == 0) {
                close();
            }
        }

        public void removeDeviceConnection(DeviceConnection connection) {
            this.mDeviceConnections.remove(connection.getToken());
            if (this.mListeners.size() == 0 && this.mDeviceConnections.size() == 0) {
                close();
            }
        }

        public void deviceAdded(Device device) {
            if (device.isUidAllowed(this.mUid)) {
                MidiDeviceInfo deviceInfo = device.getDeviceInfo();
                try {
                    for (IMidiDeviceListener listener : this.mListeners.values()) {
                        listener.onDeviceAdded(deviceInfo);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "remote exception", e);
                }
            }
        }

        public void deviceRemoved(Device device) {
            if (device.isUidAllowed(this.mUid)) {
                MidiDeviceInfo deviceInfo = device.getDeviceInfo();
                try {
                    for (IMidiDeviceListener listener : this.mListeners.values()) {
                        listener.onDeviceRemoved(deviceInfo);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "remote exception", e);
                }
            }
        }

        public void deviceStatusChanged(Device device, MidiDeviceStatus status) {
            if (device.isUidAllowed(this.mUid)) {
                try {
                    for (IMidiDeviceListener listener : this.mListeners.values()) {
                        listener.onDeviceStatusChanged(status);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "remote exception", e);
                }
            }
        }

        private void close() {
            synchronized (MidiService.this.mClients) {
                MidiService.this.mClients.remove(this.mToken);
                this.mToken.unlinkToDeath(this, 0);
            }
            for (DeviceConnection connection : this.mDeviceConnections.values()) {
                connection.getDevice().removeDeviceConnection(connection);
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Log.d(TAG, "Client died: " + this);
            close();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("Client: UID: ");
            sb.append(this.mUid);
            sb.append(" PID: ");
            sb.append(this.mPid);
            sb.append(" listener count: ");
            sb.append(this.mListeners.size());
            sb.append(" Device Connections:");
            for (DeviceConnection connection : this.mDeviceConnections.values()) {
                sb.append(" <device ");
                sb.append(connection.getDevice().getDeviceInfo().getId());
                sb.append(">");
            }
            return sb.toString();
        }
    }

    private Client getClient(IBinder token) {
        Client client;
        synchronized (this.mClients) {
            client = this.mClients.get(token);
            if (client == null) {
                client = new Client(token);
                try {
                    token.linkToDeath(client, 0);
                    this.mClients.put(token, client);
                } catch (RemoteException e) {
                    return null;
                }
            }
        }
        return client;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class Device implements IBinder.DeathRecipient {
        private static final String TAG = "MidiService.Device";
        private final BluetoothDevice mBluetoothDevice;
        private final ArrayList<DeviceConnection> mDeviceConnections;
        private MidiDeviceInfo mDeviceInfo;
        private MidiDeviceStatus mDeviceStatus;
        private IMidiDeviceServer mServer;
        private ServiceConnection mServiceConnection;
        private final ServiceInfo mServiceInfo;
        private final int mUid;

        public Device(IMidiDeviceServer server, MidiDeviceInfo deviceInfo, ServiceInfo serviceInfo, int uid) {
            this.mDeviceConnections = new ArrayList<>();
            this.mDeviceInfo = deviceInfo;
            this.mServiceInfo = serviceInfo;
            this.mUid = uid;
            this.mBluetoothDevice = (BluetoothDevice) deviceInfo.getProperties().getParcelable("bluetooth_device");
            setDeviceServer(server);
        }

        public Device(BluetoothDevice bluetoothDevice) {
            this.mDeviceConnections = new ArrayList<>();
            this.mBluetoothDevice = bluetoothDevice;
            this.mServiceInfo = null;
            this.mUid = MidiService.this.mBluetoothServiceUid;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setDeviceServer(IMidiDeviceServer server) {
            Log.i(TAG, "setDeviceServer()");
            if (server != null) {
                if (this.mServer != null) {
                    Log.e(TAG, "mServer already set in setDeviceServer");
                    return;
                }
                IBinder binder = server.asBinder();
                try {
                    binder.linkToDeath(this, 0);
                    this.mServer = server;
                    MidiService.this.mDevicesByServer.put(binder, this);
                } catch (RemoteException e) {
                    this.mServer = null;
                    return;
                }
            } else if (this.mServer != null) {
                server = this.mServer;
                this.mServer = null;
                IBinder binder2 = server.asBinder();
                MidiService.this.mDevicesByServer.remove(binder2);
                this.mDeviceStatus = null;
                try {
                    server.closeDevice();
                    binder2.unlinkToDeath(this, 0);
                } catch (RemoteException e2) {
                }
            }
            ArrayList<DeviceConnection> arrayList = this.mDeviceConnections;
            if (arrayList != null) {
                synchronized (arrayList) {
                    Iterator<DeviceConnection> it = this.mDeviceConnections.iterator();
                    while (it.hasNext()) {
                        DeviceConnection connection = it.next();
                        connection.notifyClient(server);
                    }
                }
            }
        }

        public MidiDeviceInfo getDeviceInfo() {
            return this.mDeviceInfo;
        }

        public void setDeviceInfo(MidiDeviceInfo deviceInfo) {
            this.mDeviceInfo = deviceInfo;
        }

        public MidiDeviceStatus getDeviceStatus() {
            return this.mDeviceStatus;
        }

        public void setDeviceStatus(MidiDeviceStatus status) {
            this.mDeviceStatus = status;
        }

        public IMidiDeviceServer getDeviceServer() {
            return this.mServer;
        }

        public ServiceInfo getServiceInfo() {
            return this.mServiceInfo;
        }

        public String getPackageName() {
            ServiceInfo serviceInfo = this.mServiceInfo;
            if (serviceInfo == null) {
                return null;
            }
            return serviceInfo.packageName;
        }

        public int getUid() {
            return this.mUid;
        }

        public boolean isUidAllowed(int uid) {
            return !this.mDeviceInfo.isPrivate() || this.mUid == uid;
        }

        public void addDeviceConnection(DeviceConnection connection) {
            Intent intent;
            Log.d(TAG, "addDeviceConnection() [A] connection:" + connection);
            synchronized (this.mDeviceConnections) {
                Log.d(TAG, "  mServer:" + this.mServer);
                if (this.mServer != null) {
                    Log.i(TAG, "++++ A");
                    this.mDeviceConnections.add(connection);
                    connection.notifyClient(this.mServer);
                } else if (this.mServiceConnection == null && (this.mServiceInfo != null || this.mBluetoothDevice != null)) {
                    Log.i(TAG, "++++ B");
                    this.mDeviceConnections.add(connection);
                    this.mServiceConnection = new ServiceConnection() { // from class: com.android.server.midi.MidiService.Device.1
                        @Override // android.content.ServiceConnection
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            Log.i(Device.TAG, "++++ onServiceConnected() mBluetoothDevice:" + Device.this.mBluetoothDevice);
                            IMidiDeviceServer server = null;
                            if (Device.this.mBluetoothDevice != null) {
                                IBluetoothMidiService mBluetoothMidiService = IBluetoothMidiService.Stub.asInterface(service);
                                Log.i(Device.TAG, "++++ mBluetoothMidiService:" + mBluetoothMidiService);
                                if (mBluetoothMidiService != null) {
                                    try {
                                        IBinder deviceBinder = mBluetoothMidiService.addBluetoothDevice(Device.this.mBluetoothDevice);
                                        server = IMidiDeviceServer.Stub.asInterface(deviceBinder);
                                    } catch (RemoteException e) {
                                        Log.e(Device.TAG, "Could not call addBluetoothDevice()", e);
                                    } catch (NullPointerException e2) {
                                        Log.e(Device.TAG, "Could not call addBluetoothDevice()", e2);
                                    }
                                }
                            } else {
                                server = IMidiDeviceServer.Stub.asInterface(service);
                            }
                            Device.this.setDeviceServer(server);
                        }

                        @Override // android.content.ServiceConnection
                        public void onServiceDisconnected(ComponentName name) {
                            Device.this.setDeviceServer(null);
                            Device.this.mServiceConnection = null;
                        }
                    };
                    if (this.mBluetoothDevice != null) {
                        intent = new Intent("android.media.midi.BluetoothMidiService");
                        intent.setComponent(new ComponentName("com.android.bluetoothmidiservice", "com.android.bluetoothmidiservice.BluetoothMidiService"));
                    } else {
                        intent = new Intent("android.media.midi.MidiDeviceService");
                        intent.setComponent(new ComponentName(this.mServiceInfo.packageName, this.mServiceInfo.name));
                    }
                    if (!MidiService.this.mContext.bindService(intent, this.mServiceConnection, 1)) {
                        Log.e(TAG, "Unable to bind service: " + intent);
                        setDeviceServer(null);
                        this.mServiceConnection = null;
                    }
                } else {
                    Log.e(TAG, "No way to connect to device in addDeviceConnection");
                    connection.notifyClient(null);
                }
            }
        }

        public void removeDeviceConnection(DeviceConnection connection) {
            synchronized (MidiService.this.mDevicesByInfo) {
                synchronized (this.mDeviceConnections) {
                    this.mDeviceConnections.remove(connection);
                    if (connection.getDevice().getDeviceInfo().getType() == 1) {
                        synchronized (MidiService.this.mUsbMidiLock) {
                            MidiService.this.removeUsbMidiDeviceLocked(connection.getDevice().getDeviceInfo());
                        }
                    }
                    if (this.mDeviceConnections.size() == 0 && this.mServiceConnection != null) {
                        MidiService.this.mContext.unbindService(this.mServiceConnection);
                        this.mServiceConnection = null;
                        if (this.mBluetoothDevice != null) {
                            closeLocked();
                        } else {
                            setDeviceServer(null);
                        }
                    }
                }
            }
        }

        public void closeLocked() {
            synchronized (this.mDeviceConnections) {
                Iterator<DeviceConnection> it = this.mDeviceConnections.iterator();
                while (it.hasNext()) {
                    DeviceConnection connection = it.next();
                    if (connection.getDevice().getDeviceInfo().getType() == 1) {
                        synchronized (MidiService.this.mUsbMidiLock) {
                            MidiService.this.removeUsbMidiDeviceLocked(connection.getDevice().getDeviceInfo());
                        }
                    }
                    connection.getClient().removeDeviceConnection(connection);
                }
                this.mDeviceConnections.clear();
            }
            setDeviceServer(null);
            if (this.mServiceInfo == null) {
                MidiService.this.removeDeviceLocked(this);
            } else {
                this.mDeviceStatus = new MidiDeviceStatus(this.mDeviceInfo);
            }
            if (this.mBluetoothDevice != null) {
                MidiService.this.mBluetoothDevices.remove(this.mBluetoothDevice);
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Log.d(TAG, "Device died: " + this);
            synchronized (MidiService.this.mDevicesByInfo) {
                closeLocked();
            }
        }

        public String toString() {
            return "Device Info: " + this.mDeviceInfo + " Status: " + this.mDeviceStatus + " UID: " + this.mUid + " DeviceConnection count: " + this.mDeviceConnections.size() + " mServiceConnection: " + this.mServiceConnection;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class DeviceConnection {
        private static final String TAG = "MidiService.DeviceConnection";
        private IMidiDeviceOpenCallback mCallback;
        private final Client mClient;
        private final Device mDevice;
        private final IBinder mToken = new Binder();

        public DeviceConnection(Device device, Client client, IMidiDeviceOpenCallback callback) {
            this.mDevice = device;
            this.mClient = client;
            this.mCallback = callback;
        }

        public Device getDevice() {
            return this.mDevice;
        }

        public Client getClient() {
            return this.mClient;
        }

        public IBinder getToken() {
            return this.mToken;
        }

        public void notifyClient(IMidiDeviceServer deviceServer) {
            IBinder iBinder;
            Log.d(TAG, "notifyClient");
            IMidiDeviceOpenCallback iMidiDeviceOpenCallback = this.mCallback;
            if (iMidiDeviceOpenCallback != null) {
                if (deviceServer == null) {
                    iBinder = null;
                } else {
                    try {
                        iBinder = this.mToken;
                    } catch (RemoteException e) {
                    }
                }
                iMidiDeviceOpenCallback.onDeviceOpened(deviceServer, iBinder);
                this.mCallback = null;
            }
        }

        public String toString() {
            Device device = this.mDevice;
            return (device == null || device.getDeviceInfo() == null) ? "null" : "" + this.mDevice.getDeviceInfo().getId();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isBLEMIDIDevice(BluetoothDevice btDevice) {
        ParcelUuid[] uuids = btDevice.getUuids();
        if (uuids != null) {
            for (ParcelUuid uuid : uuids) {
                if (uuid.getUuid().equals(MIDI_SERVICE)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void dumpIntentExtras(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Intent: " + action);
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Log.d(TAG, "  " + key + " : " + (bundle.get(key) != null ? bundle.get(key) : "NULL"));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isBleTransport(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return false;
        }
        boolean isBle = bundle.getInt("android.bluetooth.device.extra.TRANSPORT", 0) == 2;
        return isBle;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpUuids(BluetoothDevice btDevice) {
        ParcelUuid[] uuidParcels = btDevice.getUuids();
        Log.d(TAG, "dumpUuids(" + btDevice + ") numParcels:" + (uuidParcels != null ? uuidParcels.length : 0));
        if (uuidParcels == null) {
            Log.d(TAG, "No UUID Parcels");
            return;
        }
        for (ParcelUuid parcel : uuidParcels) {
            UUID uuid = parcel.getUuid();
            Log.d(TAG, " uuid:" + uuid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasNonMidiUuids(BluetoothDevice btDevice) {
        ParcelUuid[] uuidParcels = btDevice.getUuids();
        if (uuidParcels != null) {
            for (ParcelUuid parcel : uuidParcels) {
                if (this.mNonMidiUUIDs.contains(parcel)) {
                    return true;
                }
            }
        }
        return false;
    }

    public MidiService(Context context) {
        HashSet<ParcelUuid> hashSet = new HashSet<>();
        this.mNonMidiUUIDs = hashSet;
        this.mPackageMonitor = new PackageMonitor() { // from class: com.android.server.midi.MidiService.1
            public void onPackageAdded(String packageName, int uid) {
                MidiService.this.addPackageDeviceServers(packageName);
            }

            public void onPackageModified(String packageName) {
                MidiService.this.removePackageDeviceServers(packageName);
                MidiService.this.addPackageDeviceServers(packageName);
            }

            public void onPackageRemoved(String packageName, int uid) {
                MidiService.this.removePackageDeviceServers(packageName);
            }
        };
        this.mBleMidiReceiver = new BroadcastReceiver() { // from class: com.android.server.midi.MidiService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action == null) {
                    Log.w(MidiService.TAG, "MidiService, action is null");
                    return;
                }
                char c = 65535;
                switch (action.hashCode()) {
                    case -377527494:
                        if (action.equals("android.bluetooth.device.action.UUID")) {
                            c = 3;
                            break;
                        }
                        break;
                    case -301431627:
                        if (action.equals("android.bluetooth.device.action.ACL_CONNECTED")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1821585647:
                        if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED")) {
                            c = 1;
                            break;
                        }
                        break;
                    case 2116862345:
                        if (action.equals("android.bluetooth.device.action.BOND_STATE_CHANGED")) {
                            c = 2;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        Log.d(MidiService.TAG, "ACTION_ACL_CONNECTED");
                        MidiService.dumpIntentExtras(intent);
                        if (!MidiService.isBleTransport(intent)) {
                            Log.i(MidiService.TAG, "No BLE transport - NOT MIDI");
                            return;
                        }
                        Log.d(MidiService.TAG, "BLE Device");
                        BluetoothDevice btDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                        MidiService.this.dumpUuids(btDevice);
                        if (MidiService.this.hasNonMidiUuids(btDevice)) {
                            Log.d(MidiService.TAG, "Non-MIDI service UUIDs found. NOT MIDI");
                            return;
                        }
                        Log.d(MidiService.TAG, "Potential MIDI Device.");
                        MidiService.this.openBluetoothDevice(btDevice);
                        return;
                    case 1:
                        Log.d(MidiService.TAG, "ACTION_ACL_DISCONNECTED");
                        BluetoothDevice btDevice2 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                        if (MidiService.this.isBLEMIDIDevice(btDevice2)) {
                            MidiService.this.closeBluetoothDevice(btDevice2);
                            return;
                        }
                        return;
                    case 2:
                    case 3:
                        Log.d(MidiService.TAG, "ACTION_UUID");
                        BluetoothDevice btDevice3 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                        MidiService.this.dumpUuids(btDevice3);
                        if (MidiService.this.isBLEMIDIDevice(btDevice3)) {
                            Log.d(MidiService.TAG, "BT MIDI DEVICE");
                            MidiService.this.openBluetoothDevice(btDevice3);
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mBluetoothServiceUid = -1;
        hashSet.add(BluetoothUuid.A2DP_SINK);
        hashSet.add(BluetoothUuid.A2DP_SOURCE);
        hashSet.add(BluetoothUuid.ADV_AUDIO_DIST);
        hashSet.add(BluetoothUuid.AVRCP_CONTROLLER);
        hashSet.add(BluetoothUuid.HFP);
        hashSet.add(BluetoothUuid.HSP);
        hashSet.add(BluetoothUuid.HID);
        hashSet.add(BluetoothUuid.LE_AUDIO);
        hashSet.add(BluetoothUuid.HOGP);
        hashSet.add(BluetoothUuid.HEARING_AID);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUnlockUser() {
        PackageInfo info;
        this.mPackageMonitor.register(this.mContext, (Looper) null, true);
        Intent intent = new Intent("android.media.midi.MidiDeviceService");
        List<ResolveInfo> resolveInfos = this.mPackageManager.queryIntentServices(intent, 128);
        if (resolveInfos != null) {
            int count = resolveInfos.size();
            for (int i = 0; i < count; i++) {
                ServiceInfo serviceInfo = resolveInfos.get(i).serviceInfo;
                if (serviceInfo != null) {
                    addPackageDeviceServer(serviceInfo);
                }
            }
        }
        try {
            info = this.mPackageManager.getPackageInfo("com.android.bluetoothmidiservice", 0);
        } catch (PackageManager.NameNotFoundException e) {
            info = null;
        }
        if (info != null && info.applicationInfo != null) {
            this.mBluetoothServiceUid = info.applicationInfo.uid;
        } else {
            this.mBluetoothServiceUid = -1;
        }
    }

    public void registerListener(IBinder token, IMidiDeviceListener listener) {
        Client client = getClient(token);
        if (client == null) {
            return;
        }
        client.addListener(listener);
        updateStickyDeviceStatus(client.mUid, listener);
    }

    public void unregisterListener(IBinder token, IMidiDeviceListener listener) {
        Client client = getClient(token);
        if (client == null) {
            return;
        }
        client.removeListener(listener);
    }

    private void updateStickyDeviceStatus(int uid, IMidiDeviceListener listener) {
        synchronized (this.mDevicesByInfo) {
            for (Device device : this.mDevicesByInfo.values()) {
                if (device.isUidAllowed(uid)) {
                    try {
                        MidiDeviceStatus status = device.getDeviceStatus();
                        if (status != null) {
                            listener.onDeviceStatusChanged(status);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote exception", e);
                    }
                }
            }
        }
    }

    public MidiDeviceInfo[] getDevices() {
        return getDevicesForTransport(1);
    }

    public MidiDeviceInfo[] getDevicesForTransport(int transport) {
        ArrayList<MidiDeviceInfo> deviceInfos = new ArrayList<>();
        int uid = Binder.getCallingUid();
        synchronized (this.mDevicesByInfo) {
            for (Device device : this.mDevicesByInfo.values()) {
                if (device.isUidAllowed(uid)) {
                    if (transport == 2) {
                        if (device.getDeviceInfo().getDefaultProtocol() != -1) {
                            deviceInfos.add(device.getDeviceInfo());
                        }
                    } else if (transport == 1 && device.getDeviceInfo().getDefaultProtocol() == -1) {
                        deviceInfos.add(device.getDeviceInfo());
                    }
                }
            }
        }
        return (MidiDeviceInfo[]) deviceInfos.toArray(EMPTY_DEVICE_INFO_ARRAY);
    }

    public void openDevice(IBinder token, MidiDeviceInfo deviceInfo, IMidiDeviceOpenCallback callback) {
        Device device;
        Client client = getClient(token);
        Log.d(TAG, "openDevice() client:" + client);
        if (client == null) {
            return;
        }
        synchronized (this.mDevicesByInfo) {
            device = this.mDevicesByInfo.get(deviceInfo);
            Log.d(TAG, "  device:" + device);
            if (device == null) {
                throw new IllegalArgumentException("device does not exist: " + deviceInfo);
            }
            if (!device.isUidAllowed(Binder.getCallingUid())) {
                throw new SecurityException("Attempt to open private device with wrong UID");
            }
        }
        if (deviceInfo.getType() == 1) {
            synchronized (this.mUsbMidiLock) {
                if (isUsbMidiDeviceInUseLocked(deviceInfo)) {
                    throw new IllegalArgumentException("device already in use: " + deviceInfo);
                }
                addUsbMidiDeviceLocked(deviceInfo);
            }
        }
        long identity = Binder.clearCallingIdentity();
        try {
            Log.i(TAG, "addDeviceConnection() [B] device:" + device);
            client.addDeviceConnection(device, callback);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openBluetoothDevice(final BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "openBluetoothDevice() device: " + bluetoothDevice);
        MidiManager midiManager = (MidiManager) this.mContext.getSystemService(MidiManager.class);
        midiManager.openBluetoothDevice(bluetoothDevice, new MidiManager.OnDeviceOpenedListener() { // from class: com.android.server.midi.MidiService.3
            @Override // android.media.midi.MidiManager.OnDeviceOpenedListener
            public void onDeviceOpened(MidiDevice device) {
                synchronized (MidiService.this.mBleMidiDeviceMap) {
                    Log.i(MidiService.TAG, "onDeviceOpened() device:" + device);
                    MidiService.this.mBleMidiDeviceMap.put(bluetoothDevice, device);
                }
            }
        }, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void closeBluetoothDevice(BluetoothDevice bluetoothDevice) {
        MidiDevice midiDevice;
        Log.d(TAG, "closeBluetoothDevice() device: " + bluetoothDevice);
        synchronized (this.mBleMidiDeviceMap) {
            midiDevice = this.mBleMidiDeviceMap.remove(bluetoothDevice);
        }
        if (midiDevice != null) {
            try {
                midiDevice.close();
            } catch (IOException ex) {
                Log.e(TAG, "Exception closing BLE-MIDI device" + ex);
            }
        }
    }

    public void openBluetoothDevice(IBinder token, BluetoothDevice bluetoothDevice, IMidiDeviceOpenCallback callback) {
        Device device;
        Log.d(TAG, "openBluetoothDevice()");
        Client client = getClient(token);
        if (client == null) {
            return;
        }
        Log.i(TAG, "alloc device...");
        synchronized (this.mDevicesByInfo) {
            device = this.mBluetoothDevices.get(bluetoothDevice);
            if (device == null) {
                device = new Device(bluetoothDevice);
                this.mBluetoothDevices.put(bluetoothDevice, device);
            }
        }
        Log.i(TAG, "device: " + device);
        long identity = Binder.clearCallingIdentity();
        try {
            Log.i(TAG, "addDeviceConnection() [C] device:" + device);
            client.addDeviceConnection(device, callback);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void closeDevice(IBinder clientToken, IBinder deviceToken) {
        Client client = getClient(clientToken);
        if (client == null) {
            return;
        }
        client.removeDeviceConnection(deviceToken);
    }

    public MidiDeviceInfo registerDeviceServer(IMidiDeviceServer server, int numInputPorts, int numOutputPorts, String[] inputPortNames, String[] outputPortNames, Bundle properties, int type, int defaultProtocol) {
        MidiDeviceInfo addDeviceLocked;
        int uid = Binder.getCallingUid();
        if (type == 1 && uid != 1000) {
            throw new SecurityException("only system can create USB devices");
        }
        if (type == 3 && uid != this.mBluetoothServiceUid) {
            throw new SecurityException("only MidiBluetoothService can create Bluetooth devices");
        }
        synchronized (this.mDevicesByInfo) {
            addDeviceLocked = addDeviceLocked(type, numInputPorts, numOutputPorts, inputPortNames, outputPortNames, properties, server, null, false, uid, defaultProtocol);
        }
        return addDeviceLocked;
    }

    public void unregisterDeviceServer(IMidiDeviceServer server) {
        synchronized (this.mDevicesByInfo) {
            Device device = this.mDevicesByServer.get(server.asBinder());
            if (device != null) {
                device.closeLocked();
            }
        }
    }

    public MidiDeviceInfo getServiceDeviceInfo(String packageName, String className) {
        int uid = Binder.getCallingUid();
        synchronized (this.mDevicesByInfo) {
            for (Device device : this.mDevicesByInfo.values()) {
                ServiceInfo serviceInfo = device.getServiceInfo();
                if (serviceInfo != null && packageName.equals(serviceInfo.packageName) && className.equals(serviceInfo.name)) {
                    if (device.isUidAllowed(uid)) {
                        return device.getDeviceInfo();
                    }
                    EventLog.writeEvent(1397638484, "185796676", -1, "");
                    return null;
                }
            }
            return null;
        }
    }

    public MidiDeviceStatus getDeviceStatus(MidiDeviceInfo deviceInfo) {
        Device device = this.mDevicesByInfo.get(deviceInfo);
        if (device == null) {
            throw new IllegalArgumentException("no such device for " + deviceInfo);
        }
        int uid = Binder.getCallingUid();
        if (device.isUidAllowed(uid)) {
            return device.getDeviceStatus();
        }
        Log.e(TAG, "getDeviceStatus() invalid UID = " + uid);
        EventLog.writeEvent(1397638484, "203549963", Integer.valueOf(uid), "getDeviceStatus: invalid uid");
        return null;
    }

    public void setDeviceStatus(IMidiDeviceServer server, MidiDeviceStatus status) {
        Device device = this.mDevicesByServer.get(server.asBinder());
        if (device != null) {
            if (Binder.getCallingUid() != device.getUid()) {
                throw new SecurityException("setDeviceStatus() caller UID " + Binder.getCallingUid() + " does not match device's UID " + device.getUid());
            }
            device.setDeviceStatus(status);
            notifyDeviceStatusChanged(device, status);
        }
    }

    private void notifyDeviceStatusChanged(Device device, MidiDeviceStatus status) {
        synchronized (this.mClients) {
            for (Client c : this.mClients.values()) {
                c.deviceStatusChanged(device, status);
            }
        }
    }

    private MidiDeviceInfo addDeviceLocked(int type, int numInputPorts, int numOutputPorts, String[] inputPortNames, String[] outputPortNames, Bundle properties, IMidiDeviceServer server, ServiceInfo serviceInfo, boolean isPrivate, int uid, int defaultProtocol) {
        BluetoothDevice bluetoothDevice;
        Device device;
        int deviceCountForApp = 0;
        for (Device device2 : this.mDevicesByInfo.values()) {
            if (device2.getUid() == uid) {
                deviceCountForApp++;
            }
        }
        if (deviceCountForApp < 16) {
            int id = this.mNextDeviceId;
            this.mNextDeviceId = id + 1;
            MidiDeviceInfo deviceInfo = new MidiDeviceInfo(type, id, numInputPorts, numOutputPorts, inputPortNames, outputPortNames, properties, isPrivate, defaultProtocol);
            if (server != null) {
                try {
                    server.setDeviceInfo(deviceInfo);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in setDeviceInfo()");
                    return null;
                }
            }
            Device device3 = null;
            if (type != 3) {
                bluetoothDevice = null;
            } else {
                BluetoothDevice bluetoothDevice2 = (BluetoothDevice) properties.getParcelable("bluetooth_device");
                Device device4 = this.mBluetoothDevices.get(bluetoothDevice2);
                device3 = device4;
                if (device3 != null) {
                    device3.setDeviceInfo(deviceInfo);
                }
                bluetoothDevice = bluetoothDevice2;
            }
            if (device3 != null) {
                device = device3;
            } else {
                Device device5 = new Device(server, deviceInfo, serviceInfo, uid);
                device = device5;
            }
            this.mDevicesByInfo.put(deviceInfo, device);
            if (bluetoothDevice != null) {
                this.mBluetoothDevices.put(bluetoothDevice, device);
            }
            synchronized (this.mClients) {
                for (Client c : this.mClients.values()) {
                    c.deviceAdded(device);
                }
            }
            return deviceInfo;
        }
        throw new SecurityException("too many MIDI devices already created for UID = " + uid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeDeviceLocked(Device device) {
        IMidiDeviceServer server = device.getDeviceServer();
        if (server != null) {
            this.mDevicesByServer.remove(server.asBinder());
        }
        this.mDevicesByInfo.remove(device.getDeviceInfo());
        synchronized (this.mClients) {
            for (Client c : this.mClients.values()) {
                c.deviceRemoved(device);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addPackageDeviceServers(String packageName) {
        try {
            PackageInfo info = this.mPackageManager.getPackageInfo(packageName, 132);
            ServiceInfo[] services = info.services;
            if (services == null) {
                return;
            }
            for (ServiceInfo serviceInfo : services) {
                addPackageDeviceServer(serviceInfo);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "handlePackageUpdate could not find package " + packageName, e);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1360=6] */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:108:? -> B:83:0x0214). Please submit an issue!!! */
    private void addPackageDeviceServer(ServiceInfo serviceInfo) {
        ArrayList<String> outputPortNames;
        ArrayList<String> inputPortNames;
        ArrayList<String> outputPortNames2;
        ArrayList<String> inputPortNames2;
        HashMap<MidiDeviceInfo, Device> hashMap;
        XmlResourceParser parser = null;
        try {
            try {
                XmlResourceParser parser2 = serviceInfo.loadXmlMetaData(this.mPackageManager, "android.media.midi.MidiDeviceService");
                if (parser2 == null) {
                    if (parser2 != null) {
                        parser2.close();
                        return;
                    }
                    return;
                }
                try {
                    if (!"android.permission.BIND_MIDI_DEVICE_SERVICE".equals(serviceInfo.permission)) {
                        Log.w(TAG, "Skipping MIDI device service " + serviceInfo.packageName + ": it does not require the permission android.permission.BIND_MIDI_DEVICE_SERVICE");
                        if (parser2 != null) {
                            parser2.close();
                            return;
                        }
                        return;
                    }
                    ArrayList<String> inputPortNames3 = new ArrayList<>();
                    ArrayList<String> outputPortNames3 = new ArrayList<>();
                    Bundle properties = null;
                    int numInputPorts = 0;
                    int numOutputPorts = 0;
                    boolean isPrivate = false;
                    while (true) {
                        int eventType = parser2.next();
                        if (eventType == 1) {
                            break;
                        }
                        if (eventType == 2) {
                            String tagName = parser2.getName();
                            if ("device".equals(tagName)) {
                                if (properties != null) {
                                    Log.w(TAG, "nested <device> elements in metadata for " + serviceInfo.packageName);
                                    outputPortNames2 = outputPortNames3;
                                    inputPortNames2 = inputPortNames3;
                                    outputPortNames3 = outputPortNames2;
                                    inputPortNames3 = inputPortNames2;
                                } else {
                                    Bundle properties2 = new Bundle();
                                    properties2.putParcelable("service_info", serviceInfo);
                                    numInputPorts = 0;
                                    numOutputPorts = 0;
                                    int count = parser2.getAttributeCount();
                                    isPrivate = false;
                                    for (int i = 0; i < count; i++) {
                                        String name = parser2.getAttributeName(i);
                                        String value = parser2.getAttributeValue(i);
                                        if ("private".equals(name)) {
                                            isPrivate = "true".equals(value);
                                        } else {
                                            properties2.putString(name, value);
                                        }
                                    }
                                    properties = properties2;
                                }
                            } else if ("input-port".equals(tagName)) {
                                if (properties == null) {
                                    Log.w(TAG, "<input-port> outside of <device> in metadata for " + serviceInfo.packageName);
                                    outputPortNames2 = outputPortNames3;
                                    inputPortNames2 = inputPortNames3;
                                    outputPortNames3 = outputPortNames2;
                                    inputPortNames3 = inputPortNames2;
                                } else {
                                    numInputPorts++;
                                    String portName = null;
                                    int count2 = parser2.getAttributeCount();
                                    int i2 = 0;
                                    while (true) {
                                        if (i2 >= count2) {
                                            break;
                                        }
                                        String name2 = parser2.getAttributeName(i2);
                                        String value2 = parser2.getAttributeValue(i2);
                                        if ("name".equals(name2)) {
                                            portName = value2;
                                            break;
                                        }
                                        i2++;
                                    }
                                    inputPortNames3.add(portName);
                                }
                            } else if ("output-port".equals(tagName)) {
                                if (properties == null) {
                                    Log.w(TAG, "<output-port> outside of <device> in metadata for " + serviceInfo.packageName);
                                    outputPortNames2 = outputPortNames3;
                                    inputPortNames2 = inputPortNames3;
                                    outputPortNames3 = outputPortNames2;
                                    inputPortNames3 = inputPortNames2;
                                } else {
                                    numOutputPorts++;
                                    String portName2 = null;
                                    int count3 = parser2.getAttributeCount();
                                    int i3 = 0;
                                    while (true) {
                                        if (i3 >= count3) {
                                            break;
                                        }
                                        String name3 = parser2.getAttributeName(i3);
                                        String value3 = parser2.getAttributeValue(i3);
                                        if ("name".equals(name3)) {
                                            portName2 = value3;
                                            break;
                                        }
                                        i3++;
                                    }
                                    outputPortNames3.add(portName2);
                                }
                            }
                            outputPortNames = outputPortNames3;
                            inputPortNames = inputPortNames3;
                        } else if (eventType != 3) {
                            outputPortNames = outputPortNames3;
                            inputPortNames = inputPortNames3;
                        } else if (!"device".equals(parser2.getName())) {
                            outputPortNames = outputPortNames3;
                            inputPortNames = inputPortNames3;
                        } else if (properties != null) {
                            if (numInputPorts == 0 && numOutputPorts == 0) {
                                Log.w(TAG, "<device> with no ports in metadata for " + serviceInfo.packageName);
                                outputPortNames2 = outputPortNames3;
                                inputPortNames2 = inputPortNames3;
                            } else {
                                try {
                                    ApplicationInfo appInfo = this.mPackageManager.getApplicationInfo(serviceInfo.packageName, 0);
                                    int uid = appInfo.uid;
                                    HashMap<MidiDeviceInfo, Device> hashMap2 = this.mDevicesByInfo;
                                    synchronized (hashMap2) {
                                        try {
                                            String[] strArr = EMPTY_STRING_ARRAY;
                                            hashMap = hashMap2;
                                            outputPortNames = outputPortNames3;
                                            inputPortNames = inputPortNames3;
                                            try {
                                                addDeviceLocked(2, numInputPorts, numOutputPorts, (String[]) inputPortNames3.toArray(strArr), (String[]) outputPortNames3.toArray(strArr), properties, null, serviceInfo, isPrivate, uid, -1);
                                                inputPortNames.clear();
                                                outputPortNames.clear();
                                                properties = null;
                                            } catch (Throwable th) {
                                                th = th;
                                                throw th;
                                            }
                                        } catch (Throwable th2) {
                                            th = th2;
                                            hashMap = hashMap2;
                                            throw th;
                                        }
                                    }
                                } catch (PackageManager.NameNotFoundException e) {
                                    outputPortNames2 = outputPortNames3;
                                    inputPortNames2 = inputPortNames3;
                                    Log.e(TAG, "could not fetch ApplicationInfo for " + serviceInfo.packageName);
                                }
                            }
                            outputPortNames3 = outputPortNames2;
                            inputPortNames3 = inputPortNames2;
                        } else {
                            outputPortNames = outputPortNames3;
                            inputPortNames = inputPortNames3;
                        }
                        outputPortNames3 = outputPortNames;
                        inputPortNames3 = inputPortNames;
                    }
                    if (parser2 != null) {
                        parser2.close();
                    }
                } catch (Exception e2) {
                    e = e2;
                    parser = parser2;
                    Log.w(TAG, "Unable to load component info " + serviceInfo.toString(), e);
                    if (parser != null) {
                        parser.close();
                    }
                } catch (Throwable th3) {
                    th = th3;
                    parser = parser2;
                    if (parser != null) {
                        parser.close();
                    }
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        } catch (Exception e3) {
            e = e3;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removePackageDeviceServers(String packageName) {
        synchronized (this.mDevicesByInfo) {
            Iterator<Device> iterator = this.mDevicesByInfo.values().iterator();
            while (iterator.hasNext()) {
                Device device = iterator.next();
                if (packageName.equals(device.getPackageName())) {
                    iterator.remove();
                    removeDeviceLocked(device);
                }
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, writer)) {
            IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
            pw.println("MIDI Manager State:");
            pw.increaseIndent();
            pw.println("Devices:");
            pw.increaseIndent();
            synchronized (this.mDevicesByInfo) {
                for (Device device : this.mDevicesByInfo.values()) {
                    pw.println(device.toString());
                }
            }
            pw.decreaseIndent();
            pw.println("Clients:");
            pw.increaseIndent();
            synchronized (this.mClients) {
                for (Client client : this.mClients.values()) {
                    pw.println(client.toString());
                }
            }
            pw.decreaseIndent();
        }
    }

    private boolean isUsbMidiDeviceInUseLocked(MidiDeviceInfo info) {
        String name = info.getProperties().getString("name");
        if (name.length() < MIDI_LEGACY_STRING.length()) {
            return false;
        }
        String deviceName = extractUsbDeviceName(name);
        String tagName = extractUsbDeviceTag(name);
        Log.i(TAG, "Checking " + deviceName + " " + tagName);
        if (this.mUsbMidiUniversalDeviceInUse.contains(deviceName)) {
            return true;
        }
        return tagName.equals(MIDI_UNIVERSAL_STRING) && this.mUsbMidiLegacyDeviceOpenCount.containsKey(deviceName);
    }

    void addUsbMidiDeviceLocked(MidiDeviceInfo info) {
        String name = info.getProperties().getString("name");
        if (name.length() < MIDI_LEGACY_STRING.length()) {
            return;
        }
        String deviceName = extractUsbDeviceName(name);
        String tagName = extractUsbDeviceTag(name);
        Log.i(TAG, "Adding " + deviceName + " " + tagName);
        if (tagName.equals(MIDI_UNIVERSAL_STRING)) {
            this.mUsbMidiUniversalDeviceInUse.add(deviceName);
        } else if (tagName.equals(MIDI_LEGACY_STRING)) {
            int count = this.mUsbMidiLegacyDeviceOpenCount.getOrDefault(deviceName, 0).intValue() + 1;
            this.mUsbMidiLegacyDeviceOpenCount.put(deviceName, Integer.valueOf(count));
        }
    }

    void removeUsbMidiDeviceLocked(MidiDeviceInfo info) {
        String name = info.getProperties().getString("name");
        if (name.length() < MIDI_LEGACY_STRING.length()) {
            return;
        }
        String deviceName = extractUsbDeviceName(name);
        String tagName = extractUsbDeviceTag(name);
        Log.i(TAG, "Removing " + deviceName + " " + tagName);
        if (tagName.equals(MIDI_UNIVERSAL_STRING)) {
            this.mUsbMidiUniversalDeviceInUse.remove(deviceName);
        } else if (tagName.equals(MIDI_LEGACY_STRING) && this.mUsbMidiLegacyDeviceOpenCount.containsKey(deviceName)) {
            int count = this.mUsbMidiLegacyDeviceOpenCount.get(deviceName).intValue();
            if (count > 1) {
                this.mUsbMidiLegacyDeviceOpenCount.put(deviceName, Integer.valueOf(count - 1));
            } else {
                this.mUsbMidiLegacyDeviceOpenCount.remove(deviceName);
            }
        }
    }

    String extractUsbDeviceName(String propertyName) {
        return propertyName.substring(0, propertyName.length() - MIDI_LEGACY_STRING.length());
    }

    String extractUsbDeviceTag(String propertyName) {
        return propertyName.substring(propertyName.length() - MIDI_LEGACY_STRING.length());
    }
}
