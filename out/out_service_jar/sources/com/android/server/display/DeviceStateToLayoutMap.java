package com.android.server.display;

import android.os.Environment;
import android.os.SystemProperties;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.DisplayAddress;
import com.android.server.display.config.layout.Display;
import com.android.server.display.config.layout.Layouts;
import com.android.server.display.config.layout.XmlParser;
import com.android.server.display.layout.Layout;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
class DeviceStateToLayoutMap {
    private static final String CONFIG_FILE_PATH = "etc/displayconfig/display_layout_configuration.xml";
    public static final int STATE_DEFAULT = -1;
    private static final String TAG = "DeviceStateToLayoutMap";
    public static boolean mMultipleDisplayFlipSupport;
    private final SparseArray<Layout> mLayoutMap = new SparseArray<>();
    private final SparseIntArray mDeviceStateMap = new SparseIntArray();

    static {
        mMultipleDisplayFlipSupport = SystemProperties.getInt("ro.product.multiple_display_flip.support", 0) == 1;
    }

    public int getDualDeviceStateLocked(int currentState) {
        return this.mDeviceStateMap.get(currentState, -1);
    }

    private void configDualDeviceState() {
        if (mMultipleDisplayFlipSupport) {
            this.mDeviceStateMap.put(1, 99);
            this.mDeviceStateMap.put(2, 99);
            return;
        }
        for (int i = 0; i < this.mLayoutMap.size(); i++) {
            Layout layout = this.mLayoutMap.valueAt(i);
            for (int j = 0; j < this.mLayoutMap.size(); j++) {
                if (layout.isDualLayout(this.mLayoutMap.valueAt(j))) {
                    this.mDeviceStateMap.put(this.mLayoutMap.keyAt(i), this.mLayoutMap.keyAt(j));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DeviceStateToLayoutMap() {
        loadLayoutsFromConfig();
        configDualDeviceState();
        createLayout(-1);
    }

    public void dumpLocked(IndentingPrintWriter ipw) {
        ipw.println("DeviceStateToLayoutMap:");
        ipw.increaseIndent();
        ipw.println("Registered Layouts:");
        for (int i = 0; i < this.mLayoutMap.size(); i++) {
            ipw.println("state(" + this.mLayoutMap.keyAt(i) + "): " + this.mLayoutMap.valueAt(i));
        }
        ipw.println("");
        ipw.println("State Map:");
        for (int i2 = 0; i2 < this.mDeviceStateMap.size(); i2++) {
            ipw.println("state(" + this.mDeviceStateMap.keyAt(i2) + ") -> dual state(" + this.mDeviceStateMap.valueAt(i2) + ")");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Layout get(int state) {
        Layout layout = this.mLayoutMap.get(state);
        if (layout == null) {
            return this.mLayoutMap.get(-1);
        }
        return layout;
    }

    private Layout createLayout(int state) {
        if (this.mLayoutMap.contains(state)) {
            Slog.e(TAG, "Attempted to create a second layout for state " + state);
            return null;
        }
        Layout layout = new Layout();
        this.mLayoutMap.append(state, layout);
        return layout;
    }

    private void loadLayoutsFromConfig() {
        File configFile = Environment.buildPath(Environment.getVendorDirectory(), new String[]{CONFIG_FILE_PATH});
        if (!configFile.exists()) {
            return;
        }
        Slog.i(TAG, "Loading display layouts from " + configFile);
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(configFile));
            Layouts layouts = XmlParser.read(in);
            if (layouts == null) {
                Slog.i(TAG, "Display layout config not found: " + configFile);
                in.close();
                return;
            }
            for (com.android.server.display.config.layout.Layout l : layouts.getLayout()) {
                int state = l.getState().intValue();
                Layout layout = createLayout(state);
                for (Display d : l.getDisplay()) {
                    layout.createDisplayLocked(DisplayAddress.fromPhysicalDisplayId(d.getAddress().longValue()), d.isDefaultDisplay(), d.isEnabled());
                }
            }
            in.close();
        } catch (IOException | DatatypeConfigurationException | XmlPullParserException e) {
            Slog.e(TAG, "Encountered an error while reading/parsing display layout config file: " + configFile, e);
        }
    }
}
