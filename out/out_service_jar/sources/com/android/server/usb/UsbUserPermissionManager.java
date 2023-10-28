package com.android.server.usb;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorPrivacyManagerInternal;
import android.hardware.usb.AccessoryFilter;
import android.hardware.usb.DeviceFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.LocalServices;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import com.android.server.slice.SliceClientPermissions;
import defpackage.CompanionAppsPermissions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class UsbUserPermissionManager {
    private static final boolean DEBUG = false;
    private static final int SNET_EVENT_LOG_ID = 1397638484;
    private static final String TAG = UsbUserPermissionManager.class.getSimpleName();
    private final Context mContext;
    private final boolean mDisablePermissionDialogs;
    private boolean mIsCopyPermissionsScheduled;
    private final Object mLock;
    private final AtomicFile mPermissionsFile;
    private final SensorPrivacyManagerInternal mSensorPrivacyMgrInternal;
    private final UsbUserSettingsManager mUsbUserSettingsManager;
    private final UserHandle mUser;
    private final ArrayMap<String, SparseBooleanArray> mDevicePermissionMap = new ArrayMap<>();
    private final ArrayMap<UsbAccessory, SparseBooleanArray> mAccessoryPermissionMap = new ArrayMap<>();
    private final ArrayMap<DeviceFilter, SparseBooleanArray> mDevicePersistentPermissionMap = new ArrayMap<>();
    private final ArrayMap<AccessoryFilter, SparseBooleanArray> mAccessoryPersistentPermissionMap = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbUserPermissionManager(Context context, UsbUserSettingsManager usbUserSettingsManager) {
        Object obj = new Object();
        this.mLock = obj;
        this.mContext = context;
        UserHandle user = context.getUser();
        this.mUser = user;
        this.mUsbUserSettingsManager = usbUserSettingsManager;
        this.mSensorPrivacyMgrInternal = (SensorPrivacyManagerInternal) LocalServices.getService(SensorPrivacyManagerInternal.class);
        this.mDisablePermissionDialogs = context.getResources().getBoolean(17891599);
        this.mPermissionsFile = new AtomicFile(new File(Environment.getUserSystemDirectory(user.getIdentifier()), "usb_permissions.xml"), "usb-permissions");
        synchronized (obj) {
            readPermissionsLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAccessoryPermissions(UsbAccessory accessory) {
        synchronized (this.mLock) {
            this.mAccessoryPermissionMap.remove(accessory);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeDevicePermissions(UsbDevice device) {
        synchronized (this.mLock) {
            this.mDevicePermissionMap.remove(device.getDeviceName());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void grantDevicePermission(UsbDevice device, int uid) {
        synchronized (this.mLock) {
            String deviceName = device.getDeviceName();
            SparseBooleanArray uidList = this.mDevicePermissionMap.get(deviceName);
            if (uidList == null) {
                uidList = new SparseBooleanArray(1);
                this.mDevicePermissionMap.put(deviceName, uidList);
            }
            uidList.put(uid, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void grantAccessoryPermission(UsbAccessory accessory, int uid) {
        synchronized (this.mLock) {
            SparseBooleanArray uidList = this.mAccessoryPermissionMap.get(accessory);
            if (uidList == null) {
                uidList = new SparseBooleanArray(1);
                this.mAccessoryPermissionMap.put(accessory, uidList);
            }
            uidList.put(uid, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasPermission(UsbDevice device, String packageName, int pid, int uid) {
        int idx;
        if (device.getHasVideoCapture()) {
            boolean isCameraPrivacyEnabled = this.mSensorPrivacyMgrInternal.isSensorPrivacyEnabled(UserHandle.getUserId(uid), 2);
            if (isCameraPrivacyEnabled || !isCameraPermissionGranted(packageName, pid, uid)) {
                return false;
            }
        }
        boolean isCameraPrivacyEnabled2 = device.getHasAudioCapture();
        if (isCameraPrivacyEnabled2 && this.mSensorPrivacyMgrInternal.isSensorPrivacyEnabled(UserHandle.getUserId(uid), 1)) {
            return false;
        }
        synchronized (this.mLock) {
            if (uid != 1000) {
                if (!this.mDisablePermissionDialogs) {
                    DeviceFilter filter = new DeviceFilter(device);
                    SparseBooleanArray permissionsForDevice = this.mDevicePersistentPermissionMap.get(filter);
                    if (permissionsForDevice != null && (idx = permissionsForDevice.indexOfKey(uid)) >= 0) {
                        return permissionsForDevice.valueAt(idx);
                    }
                    SparseBooleanArray uidList = this.mDevicePermissionMap.get(device.getDeviceName());
                    if (uidList == null) {
                        return false;
                    }
                    return uidList.get(uid);
                }
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasPermission(UsbAccessory accessory, int pid, int uid) {
        int idx;
        synchronized (this.mLock) {
            if (uid != 1000) {
                if (!this.mDisablePermissionDialogs && this.mContext.checkPermission("android.permission.MANAGE_USB", pid, uid) != 0) {
                    AccessoryFilter filter = new AccessoryFilter(accessory);
                    SparseBooleanArray permissionsForAccessory = this.mAccessoryPersistentPermissionMap.get(filter);
                    if (permissionsForAccessory != null && (idx = permissionsForAccessory.indexOfKey(uid)) >= 0) {
                        return permissionsForAccessory.valueAt(idx);
                    }
                    SparseBooleanArray uidList = this.mAccessoryPermissionMap.get(accessory);
                    if (uidList == null) {
                        return false;
                    }
                    return uidList.get(uid);
                }
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDevicePersistentPermission(UsbDevice device, int uid, boolean isGranted) {
        boolean isChanged;
        DeviceFilter filter = new DeviceFilter(device);
        synchronized (this.mLock) {
            SparseBooleanArray permissionsForDevice = this.mDevicePersistentPermissionMap.get(filter);
            if (permissionsForDevice == null) {
                permissionsForDevice = new SparseBooleanArray();
                this.mDevicePersistentPermissionMap.put(filter, permissionsForDevice);
            }
            int idx = permissionsForDevice.indexOfKey(uid);
            if (idx >= 0) {
                isChanged = permissionsForDevice.valueAt(idx) != isGranted;
                permissionsForDevice.setValueAt(idx, isGranted);
            } else {
                isChanged = true;
                permissionsForDevice.put(uid, isGranted);
            }
            if (isChanged) {
                scheduleWritePermissionsLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAccessoryPersistentPermission(UsbAccessory accessory, int uid, boolean isGranted) {
        boolean isChanged;
        AccessoryFilter filter = new AccessoryFilter(accessory);
        synchronized (this.mLock) {
            SparseBooleanArray permissionsForAccessory = this.mAccessoryPersistentPermissionMap.get(filter);
            if (permissionsForAccessory == null) {
                permissionsForAccessory = new SparseBooleanArray();
                this.mAccessoryPersistentPermissionMap.put(filter, permissionsForAccessory);
            }
            int idx = permissionsForAccessory.indexOfKey(uid);
            if (idx >= 0) {
                isChanged = permissionsForAccessory.valueAt(idx) != isGranted;
                permissionsForAccessory.setValueAt(idx, isGranted);
            } else {
                isChanged = true;
                permissionsForAccessory.put(uid, isGranted);
            }
            if (isChanged) {
                scheduleWritePermissionsLocked();
            }
        }
    }

    private void readPermission(XmlPullParser parser) throws XmlPullParserException, IOException {
        try {
            int uid = XmlUtils.readIntAttribute(parser, WatchlistLoggingHandler.WatchlistEventKeys.UID);
            String isGrantedString = parser.getAttributeValue(null, "granted");
            if (isGrantedString == null || (!isGrantedString.equals(Boolean.TRUE.toString()) && !isGrantedString.equals(Boolean.FALSE.toString()))) {
                Slog.e(TAG, "error reading usb permission granted state");
                XmlUtils.skipCurrentTag(parser);
                return;
            }
            boolean isGranted = isGrantedString.equals(Boolean.TRUE.toString());
            XmlUtils.nextElement(parser);
            if ("usb-device".equals(parser.getName())) {
                DeviceFilter filter = DeviceFilter.read(parser);
                int idx = this.mDevicePersistentPermissionMap.indexOfKey(filter);
                if (idx >= 0) {
                    this.mDevicePersistentPermissionMap.valueAt(idx).put(uid, isGranted);
                    return;
                }
                SparseBooleanArray permissionsForDevice = new SparseBooleanArray();
                this.mDevicePersistentPermissionMap.put(filter, permissionsForDevice);
                permissionsForDevice.put(uid, isGranted);
            } else if ("usb-accessory".equals(parser.getName())) {
                AccessoryFilter filter2 = AccessoryFilter.read(parser);
                int idx2 = this.mAccessoryPersistentPermissionMap.indexOfKey(filter2);
                if (idx2 >= 0) {
                    this.mAccessoryPersistentPermissionMap.valueAt(idx2).put(uid, isGranted);
                    return;
                }
                SparseBooleanArray permissionsForAccessory = new SparseBooleanArray();
                this.mAccessoryPersistentPermissionMap.put(filter2, permissionsForAccessory);
                permissionsForAccessory.put(uid, isGranted);
            }
        } catch (NumberFormatException e) {
            Slog.e(TAG, "error reading usb permission uid", e);
            XmlUtils.skipCurrentTag(parser);
        }
    }

    private void readPermissionsLocked() {
        this.mDevicePersistentPermissionMap.clear();
        this.mAccessoryPersistentPermissionMap.clear();
        try {
            FileInputStream in = this.mPermissionsFile.openRead();
            try {
                TypedXmlPullParser parser = Xml.resolvePullParser(in);
                XmlUtils.nextElement(parser);
                while (parser.getEventType() != 1) {
                    String tagName = parser.getName();
                    if (ParsingPackageUtils.TAG_PERMISSION.equals(tagName)) {
                        readPermission(parser);
                    } else {
                        XmlUtils.nextElement(parser);
                    }
                }
                if (in != null) {
                    in.close();
                }
            } catch (Throwable th) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e) {
        } catch (Exception e2) {
            Slog.e(TAG, "error reading usb permissions file, deleting to start fresh", e2);
            this.mPermissionsFile.delete();
        }
    }

    private void scheduleWritePermissionsLocked() {
        if (this.mIsCopyPermissionsScheduled) {
            return;
        }
        this.mIsCopyPermissionsScheduled = true;
        AsyncTask.execute(new Runnable() { // from class: com.android.server.usb.UsbUserPermissionManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                UsbUserPermissionManager.this.m7403x9cbd6c3();
            }
        });
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [507=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:51:0x0184 A[Catch: all -> 0x018b, TryCatch #4 {all -> 0x018b, blocks: (B:29:0x00e5, B:49:0x017b, B:51:0x0184, B:52:0x0189, B:55:0x018c, B:34:0x0113, B:37:0x0122, B:39:0x0128, B:40:0x015b, B:41:0x015e), top: B:60:0x00ab }] */
    /* renamed from: lambda$scheduleWritePermissionsLocked$0$com-android-server-usb-UsbUserPermissionManager  reason: not valid java name */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public /* synthetic */ void m7403x9cbd6c3() {
        int numDevices;
        DeviceFilter[] devices;
        int[][] uidsForDevices;
        boolean[][] grantedValuesForDevices;
        int numAccessories;
        AccessoryFilter[] accessories;
        int[][] uidsForAccessories;
        synchronized (this.mLock) {
            numDevices = this.mDevicePersistentPermissionMap.size();
            devices = new DeviceFilter[numDevices];
            uidsForDevices = new int[numDevices];
            grantedValuesForDevices = new boolean[numDevices];
            for (int deviceIdx = 0; deviceIdx < numDevices; deviceIdx++) {
                devices[deviceIdx] = new DeviceFilter(this.mDevicePersistentPermissionMap.keyAt(deviceIdx));
                SparseBooleanArray permissions = this.mDevicePersistentPermissionMap.valueAt(deviceIdx);
                int numPermissions = permissions.size();
                uidsForDevices[deviceIdx] = new int[numPermissions];
                grantedValuesForDevices[deviceIdx] = new boolean[numPermissions];
                for (int permissionIdx = 0; permissionIdx < numPermissions; permissionIdx++) {
                    uidsForDevices[deviceIdx][permissionIdx] = permissions.keyAt(permissionIdx);
                    grantedValuesForDevices[deviceIdx][permissionIdx] = permissions.valueAt(permissionIdx);
                }
            }
            numAccessories = this.mAccessoryPersistentPermissionMap.size();
            accessories = new AccessoryFilter[numAccessories];
            uidsForAccessories = new int[numAccessories];
            boolean[][] grantedValuesForAccessories = new boolean[numAccessories];
            for (int accessoryIdx = 0; accessoryIdx < numAccessories; accessoryIdx++) {
                accessories[accessoryIdx] = new AccessoryFilter(this.mAccessoryPersistentPermissionMap.keyAt(accessoryIdx));
                SparseBooleanArray permissions2 = this.mAccessoryPersistentPermissionMap.valueAt(accessoryIdx);
                int numPermissions2 = permissions2.size();
                uidsForAccessories[accessoryIdx] = new int[numPermissions2];
                grantedValuesForAccessories[accessoryIdx] = new boolean[numPermissions2];
                for (int permissionIdx2 = 0; permissionIdx2 < numPermissions2; permissionIdx2++) {
                    uidsForAccessories[accessoryIdx][permissionIdx2] = permissions2.keyAt(permissionIdx2);
                    grantedValuesForAccessories[accessoryIdx][permissionIdx2] = permissions2.valueAt(permissionIdx2);
                }
            }
            this.mIsCopyPermissionsScheduled = false;
        }
        synchronized (this.mPermissionsFile) {
            FileOutputStream out = null;
            try {
                try {
                    out = this.mPermissionsFile.startWrite();
                    TypedXmlSerializer serializer = Xml.resolveSerializer(out);
                    serializer.startDocument((String) null, true);
                    serializer.startTag((String) null, "permissions");
                    for (int i = 0; i < numDevices; i++) {
                        int numPermissions3 = uidsForDevices[i].length;
                        int j = 0;
                        while (j < numPermissions3) {
                            int numDevices2 = numDevices;
                            try {
                                serializer.startTag((String) null, ParsingPackageUtils.TAG_PERMISSION);
                                int[][] uidsForDevices2 = uidsForDevices;
                                try {
                                    serializer.attribute((String) null, WatchlistLoggingHandler.WatchlistEventKeys.UID, Integer.toString(uidsForDevices[i][j]));
                                    serializer.attribute((String) null, "granted", Boolean.toString(grantedValuesForDevices[i][j]));
                                    devices[i].write(serializer);
                                    serializer.endTag((String) null, ParsingPackageUtils.TAG_PERMISSION);
                                    j++;
                                    numDevices = numDevices2;
                                    uidsForDevices = uidsForDevices2;
                                } catch (IOException e) {
                                    e = e;
                                    Slog.e(TAG, "Failed to write permissions", e);
                                    if (out != null) {
                                        this.mPermissionsFile.failWrite(out);
                                    }
                                }
                            } catch (IOException e2) {
                                e = e2;
                                Slog.e(TAG, "Failed to write permissions", e);
                                if (out != null) {
                                }
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                    }
                    for (int i2 = 0; i2 < numAccessories; i2++) {
                        int numPermissions4 = uidsForAccessories[i2].length;
                        for (int j2 = 0; j2 < numPermissions4; j2++) {
                            serializer.startTag((String) null, ParsingPackageUtils.TAG_PERMISSION);
                            serializer.attribute((String) null, WatchlistLoggingHandler.WatchlistEventKeys.UID, Integer.toString(uidsForAccessories[i2][j2]));
                            serializer.attribute((String) null, "granted", Boolean.toString(grantedValuesForDevices[i2][j2]));
                            accessories[i2].write(serializer);
                            serializer.endTag((String) null, ParsingPackageUtils.TAG_PERMISSION);
                        }
                    }
                    serializer.endTag((String) null, "permissions");
                    serializer.endDocument();
                    this.mPermissionsFile.finishWrite(out);
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (IOException e3) {
                e = e3;
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    void requestPermissionDialog(UsbDevice device, UsbAccessory accessory, boolean canBeDefault, String packageName, int uid, Context userContext, PendingIntent pi) {
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                Intent intent = new Intent();
                if (device != null) {
                    intent.putExtra("device", device);
                } else {
                    intent.putExtra("accessory", accessory);
                }
                intent.putExtra("android.intent.extra.INTENT", pi);
                intent.putExtra("android.intent.extra.UID", uid);
                intent.putExtra("android.hardware.usb.extra.CAN_BE_DEFAULT", canBeDefault);
                intent.putExtra("android.hardware.usb.extra.PACKAGE", packageName);
                intent.setComponent(ComponentName.unflattenFromString(userContext.getResources().getString(17040054)));
                intent.addFlags(268435456);
                userContext.startActivityAsUser(intent, this.mUser);
            } catch (ActivityNotFoundException e) {
                Slog.e(TAG, "unable to start UsbPermissionActivity");
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:46:0x01f5 -> B:47:0x01f6). Please submit an issue!!! */
    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long j;
        int numMappings;
        int mappingsIdx;
        long j2;
        long token;
        long token2 = dump.start(idName, id);
        synchronized (this.mLock) {
            try {
                dump.write("user_id", (long) CompanionMessage.MESSAGE_ID, this.mUser.getIdentifier());
                int numMappings2 = this.mDevicePermissionMap.size();
                int mappingsIdx2 = 0;
                while (true) {
                    j = CompanionAppsPermissions.AppPermissions.PACKAGE_NAME;
                    if (mappingsIdx2 >= numMappings2) {
                        break;
                    }
                    String deviceName = this.mDevicePermissionMap.keyAt(mappingsIdx2);
                    long devicePermissionToken = dump.start("device_permissions", 2246267895810L);
                    dump.write("device_name", (long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, deviceName);
                    SparseBooleanArray uidList = this.mDevicePermissionMap.valueAt(mappingsIdx2);
                    int numUids = uidList.size();
                    int uidsIdx = 0;
                    while (uidsIdx < numUids) {
                        dump.write("uids", 2220498092034L, uidList.keyAt(uidsIdx));
                        uidsIdx++;
                        uidList = uidList;
                    }
                    dump.end(devicePermissionToken);
                    mappingsIdx2++;
                }
                int numMappings3 = this.mAccessoryPermissionMap.size();
                int mappingsIdx3 = 0;
                while (mappingsIdx3 < numMappings3) {
                    UsbAccessory accessory = this.mAccessoryPermissionMap.keyAt(mappingsIdx3);
                    long accessoryPermissionToken = dump.start("accessory_permissions", 2246267895811L);
                    dump.write("accessory_description", j, accessory.getDescription());
                    SparseBooleanArray uidList2 = this.mAccessoryPermissionMap.valueAt(mappingsIdx3);
                    int uidsIdx2 = 0;
                    for (int numUids2 = uidList2.size(); uidsIdx2 < numUids2; numUids2 = numUids2) {
                        dump.write("uids", 2220498092034L, uidList2.keyAt(uidsIdx2));
                        uidsIdx2++;
                    }
                    dump.end(accessoryPermissionToken);
                    mappingsIdx3++;
                    j = CompanionAppsPermissions.AppPermissions.PACKAGE_NAME;
                }
                numMappings = this.mDevicePersistentPermissionMap.size();
                mappingsIdx = 0;
            } catch (Throwable th) {
                th = th;
            }
            while (true) {
                j2 = 1146756268033L;
                if (mappingsIdx >= numMappings) {
                    break;
                }
                try {
                    DeviceFilter filter = this.mDevicePersistentPermissionMap.keyAt(mappingsIdx);
                    long devicePermissionToken2 = dump.start("device_persistent_permissions", 2246267895812L);
                    filter.dump(dump, "device", 1146756268033L);
                    SparseBooleanArray permissions = this.mDevicePersistentPermissionMap.valueAt(mappingsIdx);
                    int numPermissions = permissions.size();
                    int permissionsIdx = 0;
                    while (permissionsIdx < numPermissions) {
                        long uidPermissionToken = dump.start("uid_permission", 2246267895810L);
                        int numMappings4 = numMappings;
                        int numMappings5 = permissions.keyAt(permissionsIdx);
                        int numPermissions2 = numPermissions;
                        DeviceFilter filter2 = filter;
                        dump.write(WatchlistLoggingHandler.WatchlistEventKeys.UID, (long) CompanionMessage.MESSAGE_ID, numMappings5);
                        token = token2;
                        try {
                            dump.write("is_granted", 1133871366146L, permissions.valueAt(permissionsIdx));
                            dump.end(uidPermissionToken);
                            permissionsIdx++;
                            numMappings = numMappings4;
                            filter = filter2;
                            numPermissions = numPermissions2;
                            token2 = token;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    int numMappings6 = numMappings;
                    long token3 = token2;
                    dump.end(devicePermissionToken2);
                    mappingsIdx++;
                    numMappings = numMappings6;
                    token2 = token3;
                } catch (Throwable th3) {
                    th = th3;
                }
                th = th2;
                throw th;
            }
            token = token2;
            try {
                int numMappings7 = this.mAccessoryPersistentPermissionMap.size();
                int mappingsIdx4 = 0;
                while (mappingsIdx4 < numMappings7) {
                    long accessoryPermissionToken2 = dump.start("accessory_persistent_permissions", 2246267895813L);
                    this.mAccessoryPersistentPermissionMap.keyAt(mappingsIdx4).dump(dump, "accessory", j2);
                    SparseBooleanArray permissions2 = this.mAccessoryPersistentPermissionMap.valueAt(mappingsIdx4);
                    int numPermissions3 = permissions2.size();
                    for (int permissionsIdx2 = 0; permissionsIdx2 < numPermissions3; permissionsIdx2++) {
                        long uidPermissionToken2 = dump.start("uid_permission", 2246267895810L);
                        dump.write(WatchlistLoggingHandler.WatchlistEventKeys.UID, (long) CompanionMessage.MESSAGE_ID, permissions2.keyAt(permissionsIdx2));
                        dump.write("is_granted", 1133871366146L, permissions2.valueAt(permissionsIdx2));
                        dump.end(uidPermissionToken2);
                    }
                    dump.end(accessoryPermissionToken2);
                    mappingsIdx4++;
                    j2 = 1146756268033L;
                }
                dump.end(token);
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    private boolean isCameraPermissionGranted(String packageName, int pid, int uid) {
        try {
            ApplicationInfo aInfo = this.mContext.getPackageManager().getApplicationInfo(packageName, 0);
            if (aInfo.uid != uid) {
                Slog.i(TAG, "Package " + packageName + " does not match caller's uid " + uid);
                return false;
            }
            int targetSdkVersion = aInfo.targetSdkVersion;
            if (targetSdkVersion >= 28) {
                int allowed = this.mContext.checkPermission("android.permission.CAMERA", pid, uid);
                if (-1 == allowed) {
                    Slog.i(TAG, "Camera permission required for USB video class devices");
                    return false;
                }
                return true;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.i(TAG, "Package not found, likely due to invalid package name!");
            return false;
        }
    }

    public void checkPermission(UsbDevice device, String packageName, int pid, int uid) {
        if (!hasPermission(device, packageName, pid, uid)) {
            throw new SecurityException("User has not given " + uid + SliceClientPermissions.SliceAuthority.DELIMITER + packageName + " permission to access device " + device.getDeviceName());
        }
    }

    public void checkPermission(UsbAccessory accessory, int pid, int uid) {
        if (!hasPermission(accessory, pid, uid)) {
            throw new SecurityException("User has not given " + uid + " permission to accessory " + accessory);
        }
    }

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    private void requestPermissionDialog(UsbDevice device, UsbAccessory accessory, boolean canBeDefault, String packageName, PendingIntent pi, int uid) {
        boolean throwException = false;
        try {
            ApplicationInfo aInfo = this.mContext.getPackageManager().getApplicationInfo(packageName, 0);
            if (aInfo.uid != uid) {
                Slog.w(TAG, "package " + packageName + " does not match caller's uid " + uid);
                EventLog.writeEvent((int) SNET_EVENT_LOG_ID, "180104273", -1, "");
                throwException = true;
            }
            if (throwException) {
                throw new IllegalArgumentException("package " + packageName + " not found");
            }
        } catch (PackageManager.NameNotFoundException e) {
            throwException = true;
            if (1 != 0) {
                throw new IllegalArgumentException("package " + packageName + " not found");
            }
        } catch (Throwable th) {
            if (0 != 0) {
                throw new IllegalArgumentException("package " + packageName + " not found");
            }
            throw th;
        }
        requestPermissionDialog(device, accessory, canBeDefault, packageName, uid, this.mContext, pi);
    }

    public void requestPermission(UsbDevice device, String packageName, PendingIntent pi, int pid, int uid) {
        Intent intent = new Intent();
        if (hasPermission(device, packageName, pid, uid)) {
            intent.putExtra("device", device);
            intent.putExtra(ParsingPackageUtils.TAG_PERMISSION, true);
            try {
                pi.send(this.mContext, 0, intent);
            } catch (PendingIntent.CanceledException e) {
            }
        } else if (device.getHasVideoCapture() && !isCameraPermissionGranted(packageName, pid, uid)) {
            intent.putExtra("device", device);
            intent.putExtra(ParsingPackageUtils.TAG_PERMISSION, false);
            try {
                pi.send(this.mContext, 0, intent);
            } catch (PendingIntent.CanceledException e2) {
            }
        } else {
            requestPermissionDialog(device, null, this.mUsbUserSettingsManager.canBeDefault(device, packageName), packageName, pi, uid);
        }
    }

    public void requestPermission(UsbAccessory accessory, String packageName, PendingIntent pi, int pid, int uid) {
        if (hasPermission(accessory, pid, uid)) {
            Intent intent = new Intent();
            intent.putExtra("accessory", accessory);
            intent.putExtra(ParsingPackageUtils.TAG_PERMISSION, true);
            try {
                pi.send(this.mContext, 0, intent);
                return;
            } catch (PendingIntent.CanceledException e) {
                return;
            }
        }
        requestPermissionDialog(null, accessory, this.mUsbUserSettingsManager.canBeDefault(accessory, packageName), packageName, pi, uid);
    }
}
