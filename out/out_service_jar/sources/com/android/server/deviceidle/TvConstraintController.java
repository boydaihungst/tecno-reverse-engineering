package com.android.server.deviceidle;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import com.android.server.DeviceIdleInternal;
import com.android.server.LocalServices;
/* loaded from: classes.dex */
public class TvConstraintController implements ConstraintController {
    private final BluetoothConstraint mBluetoothConstraint;
    private final Context mContext;
    private final DeviceIdleInternal mDeviceIdleService;
    private final Handler mHandler;

    public TvConstraintController(Context context, Handler handler) {
        BluetoothConstraint bluetoothConstraint;
        this.mContext = context;
        this.mHandler = handler;
        DeviceIdleInternal deviceIdleInternal = (DeviceIdleInternal) LocalServices.getService(DeviceIdleInternal.class);
        this.mDeviceIdleService = deviceIdleInternal;
        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature("android.hardware.bluetooth")) {
            bluetoothConstraint = new BluetoothConstraint(context, handler, deviceIdleInternal);
        } else {
            bluetoothConstraint = null;
        }
        this.mBluetoothConstraint = bluetoothConstraint;
    }

    public void start() {
        BluetoothConstraint bluetoothConstraint = this.mBluetoothConstraint;
        if (bluetoothConstraint != null) {
            this.mDeviceIdleService.registerDeviceIdleConstraint(bluetoothConstraint, "bluetooth", 1);
        }
    }

    public void stop() {
        BluetoothConstraint bluetoothConstraint = this.mBluetoothConstraint;
        if (bluetoothConstraint != null) {
            this.mDeviceIdleService.unregisterDeviceIdleConstraint(bluetoothConstraint);
        }
    }
}
