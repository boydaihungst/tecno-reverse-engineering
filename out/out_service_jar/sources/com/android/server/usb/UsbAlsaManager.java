package com.android.server.usb;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.media.IAudioService;
import android.os.Bundle;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.alsa.AlsaCardsParser;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.usb.descriptors.UsbDescriptorParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public final class UsbAlsaManager {
    private static final String ALSA_DIRECTORY = "/dev/snd/";
    private static final boolean DEBUG = false;
    private static final int USB_DENYLIST_INPUT = 2;
    private static final int USB_DENYLIST_OUTPUT = 1;
    private static final boolean mIsSingleMode = true;
    private IAudioService mAudioService;
    private final Context mContext;
    private final boolean mHasMidiFeature;
    private UsbAlsaDevice mSelectedDevice;
    private static final String TAG = UsbAlsaManager.class.getSimpleName();
    private static final int USB_VENDORID_SONY = 1356;
    private static final int USB_PRODUCTID_PS4CONTROLLER_ZCT1 = 1476;
    private static final int USB_PRODUCTID_PS4CONTROLLER_ZCT2 = 2508;
    static final List<DenyListEntry> sDeviceDenylist = Arrays.asList(new DenyListEntry(USB_VENDORID_SONY, USB_PRODUCTID_PS4CONTROLLER_ZCT1, 1), new DenyListEntry(USB_VENDORID_SONY, USB_PRODUCTID_PS4CONTROLLER_ZCT2, 1));
    private final AlsaCardsParser mCardsParser = new AlsaCardsParser();
    private final ArrayList<UsbAlsaDevice> mAlsaDevices = new ArrayList<>();
    private UsbMidiDevice mPeripheralMidiDevice = null;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class DenyListEntry {
        final int mFlags;
        final int mProductId;
        final int mVendorId;

        DenyListEntry(int vendorId, int productId, int flags) {
            this.mVendorId = vendorId;
            this.mProductId = productId;
            this.mFlags = flags;
        }
    }

    private static boolean isDeviceDenylisted(int vendorId, int productId, int flags) {
        for (DenyListEntry entry : sDeviceDenylist) {
            if (entry.mVendorId == vendorId && entry.mProductId == productId) {
                return (entry.mFlags & flags) != 0;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsbAlsaManager(Context context) {
        this.mContext = context;
        this.mHasMidiFeature = context.getPackageManager().hasSystemFeature("android.software.midi");
    }

    public void systemReady() {
        this.mAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
    }

    private synchronized void selectAlsaDevice(UsbAlsaDevice alsaDevice) {
        if (this.mSelectedDevice != null) {
            deselectAlsaDevice();
        }
        int isDisabled = Settings.Secure.getInt(this.mContext.getContentResolver(), "usb_audio_automatic_routing_disabled", 0);
        if (isDisabled != 0) {
            return;
        }
        this.mSelectedDevice = alsaDevice;
        alsaDevice.start();
    }

    private synchronized void deselectAlsaDevice() {
        UsbAlsaDevice usbAlsaDevice = this.mSelectedDevice;
        if (usbAlsaDevice != null) {
            usbAlsaDevice.stop();
            this.mSelectedDevice = null;
        }
    }

    private int getAlsaDeviceListIndexFor(String deviceAddress) {
        for (int index = 0; index < this.mAlsaDevices.size(); index++) {
            if (this.mAlsaDevices.get(index).getDeviceAddress().equals(deviceAddress)) {
                return index;
            }
        }
        return -1;
    }

    private UsbAlsaDevice removeAlsaDeviceFromList(String deviceAddress) {
        int index = getAlsaDeviceListIndexFor(deviceAddress);
        if (index > -1) {
            return this.mAlsaDevices.remove(index);
        }
        return null;
    }

    UsbAlsaDevice selectDefaultDevice() {
        if (this.mAlsaDevices.size() > 0) {
            UsbAlsaDevice alsaDevice = this.mAlsaDevices.get(0);
            if (alsaDevice != null) {
                selectAlsaDevice(alsaDevice);
            }
            return alsaDevice;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void usbDeviceAdded(String deviceAddress, UsbDevice usbDevice, UsbDescriptorParser parser) {
        this.mCardsParser.scan();
        AlsaCardsParser.AlsaCardRecord cardRec = this.mCardsParser.findCardNumFor(deviceAddress);
        if (cardRec == null) {
            return;
        }
        boolean z = true;
        boolean hasInput = parser.hasInput() && !isDeviceDenylisted(usbDevice.getVendorId(), usbDevice.getProductId(), 2);
        if (!parser.hasOutput() || isDeviceDenylisted(usbDevice.getVendorId(), usbDevice.getProductId(), 1)) {
            z = false;
        }
        boolean hasOutput = z;
        if (hasInput || hasOutput) {
            boolean isInputHeadset = parser.isInputHeadset();
            boolean isOutputHeadset = parser.isOutputHeadset();
            boolean isDock = parser.isDock();
            IAudioService iAudioService = this.mAudioService;
            if (iAudioService == null) {
                Slog.e(TAG, "no AudioService");
                return;
            }
            UsbAlsaDevice alsaDevice = new UsbAlsaDevice(iAudioService, cardRec.getCardNum(), 0, deviceAddress, hasOutput, hasInput, isInputHeadset, isOutputHeadset, isDock);
            alsaDevice.setDeviceNameAndDescription(cardRec.getCardName(), cardRec.getCardDescription());
            this.mAlsaDevices.add(0, alsaDevice);
            selectAlsaDevice(alsaDevice);
        }
        logDevices("deviceAdded()");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void usbDeviceRemoved(String deviceAddress) {
        UsbAlsaDevice alsaDevice = removeAlsaDeviceFromList(deviceAddress);
        Slog.i(TAG, "USB Audio Device Removed: " + alsaDevice);
        if (alsaDevice != null && alsaDevice == this.mSelectedDevice) {
            deselectAlsaDevice();
            selectDefaultDevice();
        }
        logDevices("usbDeviceRemoved()");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPeripheralMidiState(boolean enabled, int card, int device) {
        UsbMidiDevice usbMidiDevice;
        if (!this.mHasMidiFeature) {
            return;
        }
        if (enabled && this.mPeripheralMidiDevice == null) {
            Bundle properties = new Bundle();
            Resources r = this.mContext.getResources();
            properties.putString("name", r.getString(17041671));
            properties.putString("manufacturer", r.getString(17041670));
            properties.putString("product", r.getString(17041672));
            properties.putInt("alsa_card", card);
            properties.putInt("alsa_device", device);
            this.mPeripheralMidiDevice = UsbMidiDevice.create(this.mContext, properties, card, device, 1, 1);
        } else if (!enabled && (usbMidiDevice = this.mPeripheralMidiDevice) != null) {
            IoUtils.closeQuietly(usbMidiDevice);
            this.mPeripheralMidiDevice = null;
        }
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        dump.write("cards_parser", (long) CompanionMessage.MESSAGE_ID, this.mCardsParser.getScanStatus());
        Iterator<UsbAlsaDevice> it = this.mAlsaDevices.iterator();
        while (it.hasNext()) {
            UsbAlsaDevice usbAlsaDevice = it.next();
            usbAlsaDevice.dump(dump, "alsa_devices", 2246267895810L);
        }
        dump.end(token);
    }

    public void logDevicesList(String title) {
    }

    public void logDevices(String title) {
    }
}
