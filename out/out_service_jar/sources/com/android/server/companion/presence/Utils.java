package com.android.server.companion.presence;

import android.bluetooth.BluetoothDevice;
/* loaded from: classes.dex */
class Utils {
    static String btDeviceToString(BluetoothDevice btDevice) {
        StringBuilder sb = new StringBuilder(btDevice.getAddress());
        sb.append(" [name=");
        String name = btDevice.getName();
        if (name != null) {
            sb.append('\'').append(name).append('\'');
        } else {
            sb.append("null");
        }
        String alias = btDevice.getAlias();
        if (alias != null) {
            sb.append(", alias='").append(alias).append("'");
        }
        return sb.append(']').toString();
    }

    private Utils() {
    }
}
