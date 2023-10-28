package com.android.server.usb;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.hardware.usb.AccessoryFilter;
import android.hardware.usb.DeviceFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.dump.DumpUtils;
import com.android.server.am.HostingRecord;
import com.android.server.pm.PackageManagerService;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class UsbUserSettingsManager {
    private static final boolean DEBUG = false;
    private static final String TAG = UsbUserSettingsManager.class.getSimpleName();
    private final Object mLock = new Object();
    private final PackageManager mPackageManager;
    private final UserHandle mUser;
    private final Context mUserContext;

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbUserSettingsManager(Context context, UserHandle user) {
        try {
            Context createPackageContextAsUser = context.createPackageContextAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, user);
            this.mUserContext = createPackageContextAsUser;
            this.mPackageManager = createPackageContextAsUser.getPackageManager();
            this.mUser = user;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Missing android package");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<ResolveInfo> queryIntentActivities(Intent intent) {
        return this.mPackageManager.queryIntentActivitiesAsUser(intent, 128, this.mUser.getIdentifier());
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [122=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canBeDefault(UsbDevice device, String packageName) {
        ActivityInfo[] activities = getPackageActivities(packageName);
        if (activities != null) {
            for (ActivityInfo activityInfo : activities) {
                try {
                    XmlResourceParser parser = activityInfo.loadXmlMetaData(this.mPackageManager, "android.hardware.usb.action.USB_DEVICE_ATTACHED");
                    if (parser != null) {
                        XmlUtils.nextElement(parser);
                        while (parser.getEventType() != 1) {
                            if ("usb-device".equals(parser.getName())) {
                                DeviceFilter filter = DeviceFilter.read(parser);
                                if (filter.matches(device)) {
                                    if (parser != null) {
                                        parser.close();
                                    }
                                    return true;
                                }
                            }
                            XmlUtils.nextElement(parser);
                        }
                        if (parser != null) {
                            parser.close();
                        }
                    } else if (parser != null) {
                        parser.close();
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Unable to load component info " + activityInfo.toString(), e);
                }
            }
            return false;
        }
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [164=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canBeDefault(UsbAccessory accessory, String packageName) {
        ActivityInfo[] activities = getPackageActivities(packageName);
        if (activities != null) {
            for (ActivityInfo activityInfo : activities) {
                try {
                    XmlResourceParser parser = activityInfo.loadXmlMetaData(this.mPackageManager, "android.hardware.usb.action.USB_ACCESSORY_ATTACHED");
                    if (parser != null) {
                        XmlUtils.nextElement(parser);
                        while (parser.getEventType() != 1) {
                            if ("usb-accessory".equals(parser.getName())) {
                                AccessoryFilter filter = AccessoryFilter.read(parser);
                                if (filter.matches(accessory)) {
                                    if (parser != null) {
                                        parser.close();
                                    }
                                    return true;
                                }
                            }
                            XmlUtils.nextElement(parser);
                        }
                        if (parser != null) {
                            parser.close();
                        }
                    } else if (parser != null) {
                        parser.close();
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Unable to load component info " + activityInfo.toString(), e);
                }
            }
            return false;
        }
        return false;
    }

    private ActivityInfo[] getPackageActivities(String packageName) {
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 129);
            return packageInfo.activities;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        int numDeviceAttachedActivities;
        long token = dump.start(idName, id);
        synchronized (this.mLock) {
            dump.write("user_id", (long) CompanionMessage.MESSAGE_ID, this.mUser.getIdentifier());
            List<ResolveInfo> deviceAttachedActivities = queryIntentActivities(new Intent("android.hardware.usb.action.USB_DEVICE_ATTACHED"));
            int numDeviceAttachedActivities2 = deviceAttachedActivities.size();
            for (int activityNum = 0; activityNum < numDeviceAttachedActivities2; activityNum++) {
                ResolveInfo deviceAttachedActivity = deviceAttachedActivities.get(activityNum);
                long deviceAttachedActivityToken = dump.start("device_attached_activities", 2246267895812L);
                DumpUtils.writeComponentName(dump, HostingRecord.HOSTING_TYPE_ACTIVITY, 1146756268033L, new ComponentName(deviceAttachedActivity.activityInfo.packageName, deviceAttachedActivity.activityInfo.name));
                ArrayList<DeviceFilter> deviceFilters = UsbProfileGroupSettingsManager.getDeviceFilters(this.mPackageManager, deviceAttachedActivity);
                if (deviceFilters != null) {
                    int filterNum = 0;
                    for (int numDeviceFilters = deviceFilters.size(); filterNum < numDeviceFilters; numDeviceFilters = numDeviceFilters) {
                        deviceFilters.get(filterNum).dump(dump, "filters", 2246267895810L);
                        filterNum++;
                        deviceFilters = deviceFilters;
                    }
                }
                dump.end(deviceAttachedActivityToken);
            }
            List<ResolveInfo> accessoryAttachedActivities = queryIntentActivities(new Intent("android.hardware.usb.action.USB_ACCESSORY_ATTACHED"));
            int numAccessoryAttachedActivities = accessoryAttachedActivities.size();
            int activityNum2 = 0;
            while (activityNum2 < numAccessoryAttachedActivities) {
                ResolveInfo accessoryAttachedActivity = accessoryAttachedActivities.get(activityNum2);
                long accessoryAttachedActivityToken = dump.start("accessory_attached_activities", 2246267895813L);
                List<ResolveInfo> deviceAttachedActivities2 = deviceAttachedActivities;
                int numDeviceAttachedActivities3 = numDeviceAttachedActivities2;
                List<ResolveInfo> accessoryAttachedActivities2 = accessoryAttachedActivities;
                DumpUtils.writeComponentName(dump, HostingRecord.HOSTING_TYPE_ACTIVITY, 1146756268033L, new ComponentName(accessoryAttachedActivity.activityInfo.packageName, accessoryAttachedActivity.activityInfo.name));
                ArrayList<AccessoryFilter> accessoryFilters = UsbProfileGroupSettingsManager.getAccessoryFilters(this.mPackageManager, accessoryAttachedActivity);
                if (accessoryFilters == null) {
                    numDeviceAttachedActivities = numDeviceAttachedActivities3;
                } else {
                    int filterNum2 = 0;
                    for (int numAccessoryFilters = accessoryFilters.size(); filterNum2 < numAccessoryFilters; numAccessoryFilters = numAccessoryFilters) {
                        accessoryFilters.get(filterNum2).dump(dump, "filters", 2246267895810L);
                        filterNum2++;
                        numDeviceAttachedActivities3 = numDeviceAttachedActivities3;
                        accessoryFilters = accessoryFilters;
                    }
                    numDeviceAttachedActivities = numDeviceAttachedActivities3;
                }
                dump.end(accessoryAttachedActivityToken);
                activityNum2++;
                accessoryAttachedActivities = accessoryAttachedActivities2;
                numDeviceAttachedActivities2 = numDeviceAttachedActivities;
                deviceAttachedActivities = deviceAttachedActivities2;
            }
        }
        dump.end(token);
    }
}
