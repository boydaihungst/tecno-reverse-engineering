package com.android.server.usb;

import android.content.ComponentName;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.dump.DumpUtils;
import com.android.server.app.GameManagerService;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.usb.descriptors.UsbDescriptorParser;
import com.android.server.usb.descriptors.UsbDeviceDescriptor;
import com.android.server.usb.descriptors.UsbInterfaceDescriptor;
import com.android.server.usb.descriptors.report.TextReportCanvas;
import com.android.server.usb.descriptors.tree.UsbDescriptorsTree;
import defpackage.CompanionAppsPermissions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public class UsbHostManager {
    private static final boolean DEBUG = false;
    private static final int LINUX_FOUNDATION_VID = 7531;
    private static final int MAX_CONNECT_RECORDS = 32;
    private static final int MAX_UNIQUE_CODE_GENERATION_ATTEMPTS = 10;
    private static final String TAG = UsbHostManager.class.getSimpleName();
    static final SimpleDateFormat sFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
    private final Context mContext;
    private UsbProfileGroupSettingsManager mCurrentSettings;
    private final boolean mHasMidiFeature;
    private final String[] mHostDenyList;
    private ConnectionRecord mLastConnect;
    private int mNumConnects;
    private final UsbPermissionManager mPermissionManager;
    private final UsbAlsaManager mUsbAlsaManager;
    private ComponentName mUsbDeviceConnectionHandler;
    private final Object mLock = new Object();
    private final HashMap<String, UsbDevice> mDevices = new HashMap<>();
    private Object mSettingsLock = new Object();
    private Object mHandlerLock = new Object();
    private final LinkedList<ConnectionRecord> mConnections = new LinkedList<>();
    private final ArrayMap<String, ConnectionRecord> mConnected = new ArrayMap<>();
    private final HashMap<String, ArrayList<UsbDirectMidiDevice>> mMidiDevices = new HashMap<>();
    private final HashSet<String> mMidiUniqueCodes = new HashSet<>();
    private final Random mRandom = new Random();

    /* JADX INFO: Access modifiers changed from: private */
    public native void monitorUsbHostBus();

    private native ParcelFileDescriptor nativeOpenDevice(String str);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class ConnectionRecord {
        static final int CONNECT = 0;
        static final int CONNECT_BADDEVICE = 2;
        static final int CONNECT_BADPARSE = 1;
        static final int DISCONNECT = -1;
        private static final int kDumpBytesPerLine = 16;
        final byte[] mDescriptors;
        String mDeviceAddress;
        final int mMode;
        long mTimestamp = System.currentTimeMillis();

        ConnectionRecord(String deviceAddress, int mode, byte[] descriptors) {
            this.mDeviceAddress = deviceAddress;
            this.mMode = mode;
            this.mDescriptors = descriptors;
        }

        private String formatTime() {
            return new StringBuilder(UsbHostManager.sFormat.format(new Date(this.mTimestamp))).toString();
        }

        void dump(DualDumpOutputStream dump, String idName, long id) {
            long token = dump.start(idName, id);
            dump.write("device_address", (long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.mDeviceAddress);
            dump.write(GameManagerService.GamePackageConfiguration.GameModeConfiguration.MODE_KEY, (long) CompanionMessage.TYPE, this.mMode);
            dump.write(WatchlistLoggingHandler.WatchlistEventKeys.TIMESTAMP, 1112396529667L, this.mTimestamp);
            if (this.mMode != -1) {
                UsbDescriptorParser parser = new UsbDescriptorParser(this.mDeviceAddress, this.mDescriptors);
                UsbDeviceDescriptor deviceDescriptor = parser.getDeviceDescriptor();
                dump.write("manufacturer", 1120986464260L, deviceDescriptor.getVendorID());
                dump.write("product", 1120986464261L, deviceDescriptor.getProductID());
                long isHeadSetToken = dump.start("is_headset", 1146756268038L);
                dump.write("in", 1133871366145L, parser.isInputHeadset());
                dump.write("out", 1133871366146L, parser.isOutputHeadset());
                dump.end(isHeadSetToken);
            }
            dump.end(token);
        }

        void dumpShort(IndentingPrintWriter pw) {
            if (this.mMode != -1) {
                pw.println(formatTime() + " Connect " + this.mDeviceAddress + " mode:" + this.mMode);
                UsbDescriptorParser parser = new UsbDescriptorParser(this.mDeviceAddress, this.mDescriptors);
                UsbDeviceDescriptor deviceDescriptor = parser.getDeviceDescriptor();
                pw.println("manfacturer:0x" + Integer.toHexString(deviceDescriptor.getVendorID()) + " product:" + Integer.toHexString(deviceDescriptor.getProductID()));
                pw.println("isHeadset[in: " + parser.isInputHeadset() + " , out: " + parser.isOutputHeadset() + "], isDock: " + parser.isDock());
                return;
            }
            pw.println(formatTime() + " Disconnect " + this.mDeviceAddress);
        }

        void dumpTree(IndentingPrintWriter pw) {
            if (this.mMode != -1) {
                pw.println(formatTime() + " Connect " + this.mDeviceAddress + " mode:" + this.mMode);
                UsbDescriptorParser parser = new UsbDescriptorParser(this.mDeviceAddress, this.mDescriptors);
                StringBuilder stringBuilder = new StringBuilder();
                UsbDescriptorsTree descriptorTree = new UsbDescriptorsTree();
                descriptorTree.parse(parser);
                descriptorTree.report(new TextReportCanvas(parser, stringBuilder));
                stringBuilder.append("isHeadset[in: " + parser.isInputHeadset() + " , out: " + parser.isOutputHeadset() + "], isDock: " + parser.isDock());
                pw.println(stringBuilder.toString());
                return;
            }
            pw.println(formatTime() + " Disconnect " + this.mDeviceAddress);
        }

        void dumpList(IndentingPrintWriter pw) {
            if (this.mMode != -1) {
                pw.println(formatTime() + " Connect " + this.mDeviceAddress + " mode:" + this.mMode);
                UsbDescriptorParser parser = new UsbDescriptorParser(this.mDeviceAddress, this.mDescriptors);
                StringBuilder stringBuilder = new StringBuilder();
                TextReportCanvas canvas = new TextReportCanvas(parser, stringBuilder);
                Iterator<UsbDescriptor> it = parser.getDescriptors().iterator();
                while (it.hasNext()) {
                    UsbDescriptor descriptor = it.next();
                    descriptor.report(canvas);
                }
                pw.println(stringBuilder.toString());
                pw.println("isHeadset[in: " + parser.isInputHeadset() + " , out: " + parser.isOutputHeadset() + "], isDock: " + parser.isDock());
                return;
            }
            pw.println(formatTime() + " Disconnect " + this.mDeviceAddress);
        }

        void dumpRaw(IndentingPrintWriter pw) {
            if (this.mMode != -1) {
                pw.println(formatTime() + " Connect " + this.mDeviceAddress + " mode:" + this.mMode);
                int length = this.mDescriptors.length;
                pw.println("Raw Descriptors " + length + " bytes");
                int dataOffset = 0;
                for (int line = 0; line < length / 16; line++) {
                    StringBuilder sb = new StringBuilder();
                    int offset = 0;
                    while (offset < 16) {
                        sb.append("0x").append(String.format("0x%02X", Byte.valueOf(this.mDescriptors[dataOffset]))).append(" ");
                        offset++;
                        dataOffset++;
                    }
                    pw.println(sb.toString());
                }
                StringBuilder sb2 = new StringBuilder();
                while (dataOffset < length) {
                    sb2.append("0x").append(String.format("0x%02X", Byte.valueOf(this.mDescriptors[dataOffset]))).append(" ");
                    dataOffset++;
                }
                pw.println(sb2.toString());
                return;
            }
            pw.println(formatTime() + " Disconnect " + this.mDeviceAddress);
        }
    }

    public UsbHostManager(Context context, UsbAlsaManager alsaManager, UsbPermissionManager permissionManager) {
        this.mContext = context;
        this.mHostDenyList = context.getResources().getStringArray(17236150);
        this.mUsbAlsaManager = alsaManager;
        this.mPermissionManager = permissionManager;
        String deviceConnectionHandler = context.getResources().getString(17039879);
        if (!TextUtils.isEmpty(deviceConnectionHandler)) {
            setUsbDeviceConnectionHandler(ComponentName.unflattenFromString(deviceConnectionHandler));
        }
        this.mHasMidiFeature = context.getPackageManager().hasSystemFeature("android.software.midi");
    }

    public void setCurrentUserSettings(UsbProfileGroupSettingsManager settings) {
        synchronized (this.mSettingsLock) {
            this.mCurrentSettings = settings;
        }
    }

    private UsbProfileGroupSettingsManager getCurrentUserSettings() {
        UsbProfileGroupSettingsManager usbProfileGroupSettingsManager;
        synchronized (this.mSettingsLock) {
            usbProfileGroupSettingsManager = this.mCurrentSettings;
        }
        return usbProfileGroupSettingsManager;
    }

    public void setUsbDeviceConnectionHandler(ComponentName usbDeviceConnectionHandler) {
        synchronized (this.mHandlerLock) {
            this.mUsbDeviceConnectionHandler = usbDeviceConnectionHandler;
        }
    }

    private ComponentName getUsbDeviceConnectionHandler() {
        ComponentName componentName;
        synchronized (this.mHandlerLock) {
            componentName = this.mUsbDeviceConnectionHandler;
        }
        return componentName;
    }

    private boolean isDenyListed(String deviceAddress) {
        int count = this.mHostDenyList.length;
        for (int i = 0; i < count; i++) {
            if (deviceAddress.startsWith(this.mHostDenyList[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean isDenyListed(int clazz, int subClass) {
        if (clazz == 9) {
            return true;
        }
        return clazz == 3 && subClass == 1;
    }

    private void addConnectionRecord(String deviceAddress, int mode, byte[] rawDescriptors) {
        this.mNumConnects++;
        while (this.mConnections.size() >= 32) {
            this.mConnections.removeFirst();
        }
        ConnectionRecord rec = new ConnectionRecord(deviceAddress, mode, rawDescriptors);
        this.mConnections.add(rec);
        if (mode != -1) {
            this.mLastConnect = rec;
        }
        if (mode == 0) {
            this.mConnected.put(deviceAddress, rec);
        } else if (mode == -1) {
            this.mConnected.remove(deviceAddress);
        }
    }

    private void logUsbDevice(UsbDescriptorParser descriptorParser) {
        int vid = 0;
        int pid = 0;
        String mfg = "<unknown>";
        String product = "<unknown>";
        String version = "<unknown>";
        String serial = "<unknown>";
        UsbDeviceDescriptor deviceDescriptor = descriptorParser.getDeviceDescriptor();
        if (deviceDescriptor != null) {
            vid = deviceDescriptor.getVendorID();
            pid = deviceDescriptor.getProductID();
            mfg = deviceDescriptor.getMfgString(descriptorParser);
            product = deviceDescriptor.getProductString(descriptorParser);
            version = deviceDescriptor.getDeviceReleaseString();
            serial = deviceDescriptor.getSerialString(descriptorParser);
        }
        if (vid == LINUX_FOUNDATION_VID) {
            return;
        }
        boolean hasAudio = descriptorParser.hasAudioInterface();
        boolean hasHid = descriptorParser.hasHIDInterface();
        boolean hasStorage = descriptorParser.hasStorageInterface();
        String attachedString = "USB device attached: " + String.format("vidpid %04x:%04x", Integer.valueOf(vid), Integer.valueOf(pid));
        Slog.d(TAG, (attachedString + String.format(" mfg/product/ver/serial %s/%s/%s/%s", mfg, product, version, serial)) + String.format(" hasAudio/HID/Storage: %b/%b/%b", Boolean.valueOf(hasAudio), Boolean.valueOf(hasHid), Boolean.valueOf(hasStorage)));
    }

    private boolean usbDeviceAdded(String deviceAddress, int deviceClass, int deviceSubclass, byte[] descriptors) {
        if (isDenyListed(deviceAddress) || isDenyListed(deviceClass, deviceSubclass)) {
            return false;
        }
        UsbDescriptorParser parser = new UsbDescriptorParser(deviceAddress, descriptors);
        if (deviceClass != 0 || checkUsbInterfacesDenyListed(parser)) {
            logUsbDevice(parser);
            synchronized (this.mLock) {
                if (this.mDevices.get(deviceAddress) != null) {
                    Slog.w(TAG, "device already on mDevices list: " + deviceAddress);
                    return false;
                }
                UsbDevice.Builder newDeviceBuilder = parser.toAndroidUsbDeviceBuilder();
                if (newDeviceBuilder == null) {
                    Slog.e(TAG, "Couldn't create UsbDevice object.");
                    addConnectionRecord(deviceAddress, 2, parser.getRawDescriptors());
                } else {
                    UsbSerialReader serialNumberReader = new UsbSerialReader(this.mContext, this.mPermissionManager, newDeviceBuilder.serialNumber);
                    UsbDevice newDevice = newDeviceBuilder.build(serialNumberReader);
                    serialNumberReader.setDevice(newDevice);
                    this.mDevices.put(deviceAddress, newDevice);
                    String str = TAG;
                    Slog.d(str, "Added device " + newDevice);
                    ComponentName usbDeviceConnectionHandler = getUsbDeviceConnectionHandler();
                    if (usbDeviceConnectionHandler == null) {
                        getCurrentUserSettings().deviceAttached(newDevice);
                    } else {
                        getCurrentUserSettings().deviceAttachedForFixedHandler(newDevice, usbDeviceConnectionHandler);
                    }
                    this.mUsbAlsaManager.usbDeviceAdded(deviceAddress, newDevice, parser);
                    if (this.mHasMidiFeature) {
                        String uniqueUsbDeviceIdentifier = generateNewUsbDeviceIdentifier();
                        ArrayList<UsbDirectMidiDevice> midiDevices = new ArrayList<>();
                        if (parser.containsUniversalMidiDeviceEndpoint()) {
                            UsbDirectMidiDevice midiDevice = UsbDirectMidiDevice.create(this.mContext, newDevice, parser, true, uniqueUsbDeviceIdentifier);
                            if (midiDevice != null) {
                                midiDevices.add(midiDevice);
                            } else {
                                Slog.e(str, "Universal Midi Device is null.");
                            }
                        }
                        if (parser.containsLegacyMidiDeviceEndpoint()) {
                            UsbDirectMidiDevice midiDevice2 = UsbDirectMidiDevice.create(this.mContext, newDevice, parser, false, uniqueUsbDeviceIdentifier);
                            if (midiDevice2 != null) {
                                midiDevices.add(midiDevice2);
                            } else {
                                Slog.e(str, "Legacy Midi Device is null.");
                            }
                        }
                        if (!midiDevices.isEmpty()) {
                            this.mMidiDevices.put(deviceAddress, midiDevices);
                        }
                    }
                    addConnectionRecord(deviceAddress, 0, parser.getRawDescriptors());
                    FrameworkStatsLog.write(77, newDevice.getVendorId(), newDevice.getProductId(), parser.hasAudioInterface(), parser.hasHIDInterface(), parser.hasStorageInterface(), 1, 0L);
                }
                return true;
            }
        }
        return false;
    }

    private void usbDeviceRemoved(String deviceAddress) {
        synchronized (this.mLock) {
            UsbDevice device = this.mDevices.remove(deviceAddress);
            if (device != null) {
                Slog.d(TAG, "Removed device at " + deviceAddress + ": " + device.getProductName());
                this.mUsbAlsaManager.usbDeviceRemoved(deviceAddress);
                this.mPermissionManager.usbDeviceRemoved(device);
                ArrayList<UsbDirectMidiDevice> midiDevices = this.mMidiDevices.remove(deviceAddress);
                if (midiDevices != null) {
                    Iterator<UsbDirectMidiDevice> it = midiDevices.iterator();
                    while (it.hasNext()) {
                        UsbDirectMidiDevice midiDevice = it.next();
                        if (midiDevice != null) {
                            IoUtils.closeQuietly(midiDevice);
                        }
                    }
                    Slog.i(TAG, "USB MIDI Devices Removed: " + deviceAddress);
                }
                getCurrentUserSettings().usbDeviceRemoved(device);
                ConnectionRecord current = this.mConnected.get(deviceAddress);
                addConnectionRecord(deviceAddress, -1, null);
                if (current != null) {
                    UsbDescriptorParser parser = new UsbDescriptorParser(deviceAddress, current.mDescriptors);
                    FrameworkStatsLog.write(77, device.getVendorId(), device.getProductId(), parser.hasAudioInterface(), parser.hasHIDInterface(), parser.hasStorageInterface(), 0, System.currentTimeMillis() - current.mTimestamp);
                }
            } else {
                Slog.d(TAG, "Removed device at " + deviceAddress + " was already gone");
            }
        }
    }

    public void systemReady() {
        synchronized (this.mLock) {
            Runnable runnable = new Runnable() { // from class: com.android.server.usb.UsbHostManager$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    UsbHostManager.this.monitorUsbHostBus();
                }
            };
            new Thread(null, runnable, "UsbService host thread").start();
        }
    }

    public void getDeviceList(Bundle devices) {
        synchronized (this.mLock) {
            for (String name : this.mDevices.keySet()) {
                devices.putParcelable(name, this.mDevices.get(name));
            }
        }
    }

    public ParcelFileDescriptor openDevice(String deviceAddress, UsbUserPermissionManager permissions, String packageName, int pid, int uid) {
        ParcelFileDescriptor nativeOpenDevice;
        synchronized (this.mLock) {
            if (isDenyListed(deviceAddress)) {
                throw new SecurityException("USB device is on a restricted bus");
            }
            UsbDevice device = this.mDevices.get(deviceAddress);
            if (device == null) {
                throw new IllegalArgumentException("device " + deviceAddress + " does not exist or is restricted");
            }
            permissions.checkPermission(device, packageName, pid, uid);
            nativeOpenDevice = nativeOpenDevice(deviceAddress);
        }
        return nativeOpenDevice;
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        synchronized (this.mHandlerLock) {
            ComponentName componentName = this.mUsbDeviceConnectionHandler;
            if (componentName != null) {
                DumpUtils.writeComponentName(dump, "default_usb_host_connection_handler", 1146756268033L, componentName);
            }
        }
        synchronized (this.mLock) {
            for (String name : this.mDevices.keySet()) {
                com.android.internal.usb.DumpUtils.writeDevice(dump, "devices", 2246267895810L, this.mDevices.get(name));
            }
            dump.write("num_connects", 1120986464259L, this.mNumConnects);
            Iterator<ConnectionRecord> it = this.mConnections.iterator();
            while (it.hasNext()) {
                ConnectionRecord rec = it.next();
                rec.dump(dump, "connections", 2246267895812L);
            }
            for (ArrayList<UsbDirectMidiDevice> directMidiDevices : this.mMidiDevices.values()) {
                Iterator<UsbDirectMidiDevice> it2 = directMidiDevices.iterator();
                while (it2.hasNext()) {
                    UsbDirectMidiDevice directMidiDevice = it2.next();
                    directMidiDevice.dump(dump, "midi_devices", 2246267895813L);
                }
            }
        }
        dump.end(token);
    }

    public void dumpDescriptors(IndentingPrintWriter pw, String[] args) {
        if (this.mLastConnect != null) {
            pw.println("Last Connected USB Device:");
            if (args.length <= 1 || args[1].equals("-dump-short")) {
                this.mLastConnect.dumpShort(pw);
                return;
            } else if (args[1].equals("-dump-tree")) {
                this.mLastConnect.dumpTree(pw);
                return;
            } else if (args[1].equals("-dump-list")) {
                this.mLastConnect.dumpList(pw);
                return;
            } else if (args[1].equals("-dump-raw")) {
                this.mLastConnect.dumpRaw(pw);
                return;
            } else {
                return;
            }
        }
        pw.println("No USB Devices have been connected.");
    }

    private boolean checkUsbInterfacesDenyListed(UsbDescriptorParser parser) {
        boolean shouldIgnoreDevice = false;
        Iterator<UsbDescriptor> it = parser.getDescriptors().iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor instanceof UsbInterfaceDescriptor) {
                UsbInterfaceDescriptor iface = (UsbInterfaceDescriptor) descriptor;
                shouldIgnoreDevice = isDenyListed(iface.getUsbClass(), iface.getUsbSubclass());
                if (!shouldIgnoreDevice) {
                    break;
                }
            }
        }
        if (shouldIgnoreDevice) {
            return false;
        }
        return true;
    }

    private String generateNewUsbDeviceIdentifier() {
        String code;
        int numberOfAttempts = 0;
        do {
            if (numberOfAttempts > 10) {
                Slog.w(TAG, "MIDI unique code array resetting");
                this.mMidiUniqueCodes.clear();
                numberOfAttempts = 0;
            }
            code = "";
            for (int i = 0; i < 3; i++) {
                code = code + this.mRandom.nextInt(10);
            }
            numberOfAttempts++;
        } while (this.mMidiUniqueCodes.contains(code));
        this.mMidiUniqueCodes.add(code);
        return code;
    }
}
