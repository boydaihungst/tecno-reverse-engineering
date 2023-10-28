package com.android.server.usb.descriptors;

import android.hardware.usb.UsbDevice;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes2.dex */
public final class UsbDescriptorParser {
    public static final boolean DEBUG = false;
    private static final int DESCRIPTORS_ALLOC_SIZE = 128;
    private static final float IN_HEADSET_TRIGGER = 0.75f;
    private static final int MS_MIDI_1_0 = 256;
    private static final int MS_MIDI_2_0 = 512;
    private static final float OUT_HEADSET_TRIGGER = 0.75f;
    private static final String TAG = "UsbDescriptorParser";
    private int mACInterfacesSpec;
    private UsbConfigDescriptor mCurConfigDescriptor;
    private UsbInterfaceDescriptor mCurInterfaceDescriptor;
    private final ArrayList<UsbDescriptor> mDescriptors;
    private final String mDeviceAddr;
    private UsbDeviceDescriptor mDeviceDescriptor;
    private int mVCInterfacesSpec;

    private native String getDescriptorString_native(String str, int i);

    private native byte[] getRawDescriptors_native(String str);

    public UsbDescriptorParser(String deviceAddr, ArrayList<UsbDescriptor> descriptors) {
        this.mACInterfacesSpec = 256;
        this.mVCInterfacesSpec = 256;
        this.mDeviceAddr = deviceAddr;
        this.mDescriptors = descriptors;
        this.mDeviceDescriptor = (UsbDeviceDescriptor) descriptors.get(0);
    }

    public UsbDescriptorParser(String deviceAddr, byte[] rawDescriptors) {
        this.mACInterfacesSpec = 256;
        this.mVCInterfacesSpec = 256;
        this.mDeviceAddr = deviceAddr;
        this.mDescriptors = new ArrayList<>(128);
        parseDescriptors(rawDescriptors);
    }

    public String getDeviceAddr() {
        return this.mDeviceAddr;
    }

    public int getUsbSpec() {
        UsbDeviceDescriptor usbDeviceDescriptor = this.mDeviceDescriptor;
        if (usbDeviceDescriptor != null) {
            return usbDeviceDescriptor.getSpec();
        }
        throw new IllegalArgumentException();
    }

    public void setACInterfaceSpec(int spec) {
        this.mACInterfacesSpec = spec;
    }

    public int getACInterfaceSpec() {
        return this.mACInterfacesSpec;
    }

    public void setVCInterfaceSpec(int spec) {
        this.mVCInterfacesSpec = spec;
    }

    public int getVCInterfaceSpec() {
        return this.mVCInterfacesSpec;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class UsbDescriptorsStreamFormatException extends Exception {
        String mMessage;

        UsbDescriptorsStreamFormatException(String message) {
            this.mMessage = message;
        }

        @Override // java.lang.Throwable
        public String toString() {
            return "Descriptor Stream Format Exception: " + this.mMessage;
        }
    }

    private UsbDescriptor allocDescriptor(ByteStream stream) throws UsbDescriptorsStreamFormatException {
        stream.resetReadCount();
        int length = stream.getUnsignedByte();
        byte type = stream.getByte();
        UsbDescriptor.logDescriptorName(type, length);
        UsbDescriptor descriptor = null;
        switch (type) {
            case 1:
                UsbDeviceDescriptor usbDeviceDescriptor = new UsbDeviceDescriptor(length, type);
                this.mDeviceDescriptor = usbDeviceDescriptor;
                descriptor = usbDeviceDescriptor;
                break;
            case 2:
                UsbConfigDescriptor usbConfigDescriptor = new UsbConfigDescriptor(length, type);
                this.mCurConfigDescriptor = usbConfigDescriptor;
                descriptor = usbConfigDescriptor;
                UsbDeviceDescriptor usbDeviceDescriptor2 = this.mDeviceDescriptor;
                if (usbDeviceDescriptor2 == null) {
                    Log.e(TAG, "Config Descriptor found with no associated Device Descriptor!");
                    throw new UsbDescriptorsStreamFormatException("Config Descriptor found with no associated Device Descriptor!");
                }
                usbDeviceDescriptor2.addConfigDescriptor(usbConfigDescriptor);
                break;
            case 4:
                UsbInterfaceDescriptor usbInterfaceDescriptor = new UsbInterfaceDescriptor(length, type);
                this.mCurInterfaceDescriptor = usbInterfaceDescriptor;
                descriptor = usbInterfaceDescriptor;
                UsbConfigDescriptor usbConfigDescriptor2 = this.mCurConfigDescriptor;
                if (usbConfigDescriptor2 == null) {
                    Log.e(TAG, "Interface Descriptor found with no associated Config Descriptor!");
                    throw new UsbDescriptorsStreamFormatException("Interface Descriptor found with no associated Config Descriptor!");
                }
                usbConfigDescriptor2.addInterfaceDescriptor(usbInterfaceDescriptor);
                break;
            case 5:
                descriptor = new UsbEndpointDescriptor(length, type);
                UsbInterfaceDescriptor usbInterfaceDescriptor2 = this.mCurInterfaceDescriptor;
                if (usbInterfaceDescriptor2 == null) {
                    Log.e(TAG, "Endpoint Descriptor found with no associated Interface Descriptor!");
                    throw new UsbDescriptorsStreamFormatException("Endpoint Descriptor found with no associated Interface Descriptor!");
                }
                usbInterfaceDescriptor2.addEndpointDescriptor((UsbEndpointDescriptor) descriptor);
                break;
            case 11:
                descriptor = new UsbInterfaceAssoc(length, type);
                break;
            case 33:
                descriptor = new UsbHIDDescriptor(length, type);
                break;
            case 36:
                UsbInterfaceDescriptor usbInterfaceDescriptor3 = this.mCurInterfaceDescriptor;
                if (usbInterfaceDescriptor3 != null) {
                    switch (usbInterfaceDescriptor3.getUsbClass()) {
                        case 1:
                            descriptor = UsbACInterface.allocDescriptor(this, stream, length, type);
                            if (descriptor instanceof UsbMSMidiHeader) {
                                this.mCurInterfaceDescriptor.setMidiHeaderInterfaceDescriptor(descriptor);
                                break;
                            }
                            break;
                        case 14:
                            descriptor = UsbVCInterface.allocDescriptor(this, stream, length, type);
                            break;
                        case 16:
                            break;
                        default:
                            Log.w(TAG, "  Unparsed Class-specific");
                            break;
                    }
                }
                break;
            case 37:
                UsbInterfaceDescriptor usbInterfaceDescriptor4 = this.mCurInterfaceDescriptor;
                if (usbInterfaceDescriptor4 != null) {
                    int subClass = usbInterfaceDescriptor4.getUsbClass();
                    switch (subClass) {
                        case 1:
                            Byte subType = Byte.valueOf(stream.getByte());
                            descriptor = UsbACEndpoint.allocDescriptor(this, length, type, subType.byteValue());
                            break;
                        case 14:
                            Byte subType2 = Byte.valueOf(stream.getByte());
                            descriptor = UsbVCEndpoint.allocDescriptor(this, length, type, subType2.byteValue());
                            break;
                        case 16:
                            break;
                        default:
                            Log.w(TAG, "  Unparsed Class-specific Endpoint:0x" + Integer.toHexString(subClass));
                            break;
                    }
                }
                break;
        }
        if (descriptor == null) {
            return new UsbUnknown(length, type);
        }
        return descriptor;
    }

    public UsbDeviceDescriptor getDeviceDescriptor() {
        return this.mDeviceDescriptor;
    }

    public UsbInterfaceDescriptor getCurInterface() {
        return this.mCurInterfaceDescriptor;
    }

    public void parseDescriptors(byte[] descriptors) {
        ByteStream stream = new ByteStream(descriptors);
        while (stream.available() > 0) {
            UsbDescriptor descriptor = null;
            try {
                descriptor = allocDescriptor(stream);
            } catch (Exception ex) {
                Log.e(TAG, "Exception allocating USB descriptor.", ex);
            }
            if (descriptor != null) {
                try {
                    try {
                        descriptor.parseRawDescriptors(stream);
                        descriptor.postParse(stream);
                    } catch (Exception ex2) {
                        descriptor.postParse(stream);
                        Log.w(TAG, "Exception parsing USB descriptors. type:0x" + ((int) descriptor.getType()) + " status:" + descriptor.getStatus());
                        StackTraceElement[] stackElems = ex2.getStackTrace();
                        if (stackElems.length > 0) {
                            Log.i(TAG, "  class:" + stackElems[0].getClassName() + " @ " + stackElems[0].getLineNumber());
                        }
                        if (stackElems.length > 1) {
                            Log.i(TAG, "  class:" + stackElems[1].getClassName() + " @ " + stackElems[1].getLineNumber());
                        }
                        descriptor.setStatus(4);
                    }
                } finally {
                    this.mDescriptors.add(descriptor);
                }
            }
        }
    }

    public byte[] getRawDescriptors() {
        return getRawDescriptors_native(this.mDeviceAddr);
    }

    public String getDescriptorString(int stringId) {
        return getDescriptorString_native(this.mDeviceAddr, stringId);
    }

    public int getParsingSpec() {
        UsbDeviceDescriptor usbDeviceDescriptor = this.mDeviceDescriptor;
        if (usbDeviceDescriptor != null) {
            return usbDeviceDescriptor.getSpec();
        }
        return 0;
    }

    public ArrayList<UsbDescriptor> getDescriptors() {
        return this.mDescriptors;
    }

    public UsbDevice.Builder toAndroidUsbDeviceBuilder() {
        UsbDeviceDescriptor usbDeviceDescriptor = this.mDeviceDescriptor;
        if (usbDeviceDescriptor == null) {
            Log.e(TAG, "toAndroidUsbDevice() ERROR - No Device Descriptor");
            return null;
        }
        UsbDevice.Builder builder = usbDeviceDescriptor.toAndroid(this);
        if (builder == null) {
            Log.e(TAG, "toAndroidUsbDevice() ERROR Creating Device");
        }
        return builder;
    }

    public ArrayList<UsbDescriptor> getDescriptors(byte type) {
        ArrayList<UsbDescriptor> list = new ArrayList<>();
        Iterator<UsbDescriptor> it = this.mDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor.getType() == type) {
                list.add(descriptor);
            }
        }
        return list;
    }

    public ArrayList<UsbDescriptor> getInterfaceDescriptorsForClass(int usbClass) {
        ArrayList<UsbDescriptor> list = new ArrayList<>();
        Iterator<UsbDescriptor> it = this.mDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor.getType() == 4) {
                if (descriptor instanceof UsbInterfaceDescriptor) {
                    UsbInterfaceDescriptor intrDesc = (UsbInterfaceDescriptor) descriptor;
                    if (intrDesc.getUsbClass() == usbClass) {
                        list.add(descriptor);
                    }
                } else {
                    Log.w(TAG, "Unrecognized Interface l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
                }
            }
        }
        return list;
    }

    public ArrayList<UsbDescriptor> getACInterfaceDescriptors(byte subtype, int subclass) {
        ArrayList<UsbDescriptor> list = new ArrayList<>();
        Iterator<UsbDescriptor> it = this.mDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor.getType() == 36) {
                if (descriptor instanceof UsbACInterface) {
                    UsbACInterface acDescriptor = (UsbACInterface) descriptor;
                    if (acDescriptor.getSubtype() == subtype && acDescriptor.getSubclass() == subclass) {
                        list.add(descriptor);
                    }
                } else {
                    Log.w(TAG, "Unrecognized Audio Interface len: " + descriptor.getLength() + " type:0x" + Integer.toHexString(descriptor.getType()));
                }
            }
        }
        return list;
    }

    public boolean hasInput() {
        ArrayList<UsbDescriptor> acDescriptors = getACInterfaceDescriptors((byte) 2, 1);
        Iterator<UsbDescriptor> it = acDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor instanceof UsbACTerminal) {
                UsbACTerminal inDescr = (UsbACTerminal) descriptor;
                int type = inDescr.getTerminalType();
                int terminalCategory = type & (-256);
                if (terminalCategory != 256 && terminalCategory != 768) {
                    return true;
                }
            } else {
                Log.w(TAG, "Undefined Audio Input terminal l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
            }
        }
        return false;
    }

    public boolean hasOutput() {
        ArrayList<UsbDescriptor> acDescriptors = getACInterfaceDescriptors((byte) 3, 1);
        Iterator<UsbDescriptor> it = acDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor instanceof UsbACTerminal) {
                UsbACTerminal outDescr = (UsbACTerminal) descriptor;
                int type = outDescr.getTerminalType();
                int terminalCategory = type & (-256);
                if (terminalCategory != 256 && terminalCategory != 512) {
                    return true;
                }
            } else {
                Log.w(TAG, "Undefined Audio Input terminal l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
            }
        }
        return false;
    }

    public boolean hasMic() {
        ArrayList<UsbDescriptor> acDescriptors = getACInterfaceDescriptors((byte) 2, 1);
        Iterator<UsbDescriptor> it = acDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor instanceof UsbACTerminal) {
                UsbACTerminal inDescr = (UsbACTerminal) descriptor;
                if (inDescr.getTerminalType() == 513 || inDescr.getTerminalType() == 1026 || inDescr.getTerminalType() == 1024 || inDescr.getTerminalType() == 1539) {
                    return true;
                }
            } else {
                Log.w(TAG, "Undefined Audio Input terminal l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
            }
        }
        return false;
    }

    public boolean hasSpeaker() {
        ArrayList<UsbDescriptor> acDescriptors = getACInterfaceDescriptors((byte) 3, 1);
        Iterator<UsbDescriptor> it = acDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor instanceof UsbACTerminal) {
                UsbACTerminal outDescr = (UsbACTerminal) descriptor;
                if (outDescr.getTerminalType() == 769 || outDescr.getTerminalType() == 770 || outDescr.getTerminalType() == 1026) {
                    return true;
                }
            } else {
                Log.w(TAG, "Undefined Audio Output terminal l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
            }
        }
        return false;
    }

    public boolean hasAudioInterface() {
        ArrayList<UsbDescriptor> descriptors = getInterfaceDescriptorsForClass(1);
        return true ^ descriptors.isEmpty();
    }

    public boolean hasAudioTerminal(int subType) {
        Iterator<UsbDescriptor> it = this.mDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if ((descriptor instanceof UsbACInterface) && ((UsbACInterface) descriptor).getSubclass() == 1 && ((UsbACInterface) descriptor).getSubtype() == subType) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAudioPlayback() {
        return hasAudioTerminal(3);
    }

    public boolean hasAudioCapture() {
        return hasAudioTerminal(2);
    }

    public boolean hasVideoCapture() {
        Iterator<UsbDescriptor> it = this.mDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor instanceof UsbVCInputTerminal) {
                return true;
            }
        }
        return false;
    }

    public boolean hasVideoPlayback() {
        Iterator<UsbDescriptor> it = this.mDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor instanceof UsbVCOutputTerminal) {
                return true;
            }
        }
        return false;
    }

    public boolean hasHIDInterface() {
        ArrayList<UsbDescriptor> descriptors = getInterfaceDescriptorsForClass(3);
        return !descriptors.isEmpty();
    }

    public boolean hasStorageInterface() {
        ArrayList<UsbDescriptor> descriptors = getInterfaceDescriptorsForClass(8);
        return !descriptors.isEmpty();
    }

    public boolean hasMIDIInterface() {
        ArrayList<UsbDescriptor> descriptors = getInterfaceDescriptorsForClass(1);
        Iterator<UsbDescriptor> it = descriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor instanceof UsbInterfaceDescriptor) {
                UsbInterfaceDescriptor interfaceDescriptor = (UsbInterfaceDescriptor) descriptor;
                if (interfaceDescriptor.getUsbSubclass() == 3) {
                    return true;
                }
            } else {
                Log.w(TAG, "Undefined Audio Class Interface l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
            }
        }
        return false;
    }

    public boolean containsUniversalMidiDeviceEndpoint() {
        ArrayList<UsbInterfaceDescriptor> interfaceDescriptors = findUniversalMidiInterfaceDescriptors();
        return doesInterfaceContainEndpoint(interfaceDescriptors);
    }

    public boolean containsLegacyMidiDeviceEndpoint() {
        ArrayList<UsbInterfaceDescriptor> interfaceDescriptors = findLegacyMidiInterfaceDescriptors();
        return doesInterfaceContainEndpoint(interfaceDescriptors);
    }

    public boolean doesInterfaceContainEndpoint(ArrayList<UsbInterfaceDescriptor> interfaceDescriptors) {
        int outputCount = 0;
        int inputCount = 0;
        for (int interfaceIndex = 0; interfaceIndex < interfaceDescriptors.size(); interfaceIndex++) {
            UsbInterfaceDescriptor interfaceDescriptor = interfaceDescriptors.get(interfaceIndex);
            for (int endpointIndex = 0; endpointIndex < interfaceDescriptor.getNumEndpoints(); endpointIndex++) {
                UsbEndpointDescriptor endpoint = interfaceDescriptor.getEndpointDescriptor(endpointIndex);
                if (endpoint.getDirection() == 0) {
                    outputCount++;
                } else {
                    inputCount++;
                }
            }
        }
        return outputCount > 0 || inputCount > 0;
    }

    public ArrayList<UsbInterfaceDescriptor> findUniversalMidiInterfaceDescriptors() {
        return findMidiInterfaceDescriptors(512);
    }

    public ArrayList<UsbInterfaceDescriptor> findLegacyMidiInterfaceDescriptors() {
        return findMidiInterfaceDescriptors(256);
    }

    private ArrayList<UsbInterfaceDescriptor> findMidiInterfaceDescriptors(int type) {
        UsbDescriptor midiHeaderDescriptor;
        ArrayList<UsbDescriptor> descriptors = getInterfaceDescriptorsForClass(1);
        ArrayList<UsbInterfaceDescriptor> midiInterfaces = new ArrayList<>();
        Iterator<UsbDescriptor> it = descriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor instanceof UsbInterfaceDescriptor) {
                UsbInterfaceDescriptor interfaceDescriptor = (UsbInterfaceDescriptor) descriptor;
                if (interfaceDescriptor.getUsbSubclass() == 3 && (midiHeaderDescriptor = interfaceDescriptor.getMidiHeaderInterfaceDescriptor()) != null && (midiHeaderDescriptor instanceof UsbMSMidiHeader)) {
                    UsbMSMidiHeader midiHeader = (UsbMSMidiHeader) midiHeaderDescriptor;
                    if (midiHeader.getMidiStreamingClass() == type) {
                        midiInterfaces.add(interfaceDescriptor);
                    }
                }
            } else {
                Log.w(TAG, "Undefined Audio Class Interface l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
            }
        }
        return midiInterfaces;
    }

    public int calculateMidiInterfaceDescriptorsCount() {
        UsbDescriptor midiHeaderDescriptor;
        int count = 0;
        ArrayList<UsbDescriptor> descriptors = getInterfaceDescriptorsForClass(1);
        Iterator<UsbDescriptor> it = descriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor instanceof UsbInterfaceDescriptor) {
                UsbInterfaceDescriptor interfaceDescriptor = (UsbInterfaceDescriptor) descriptor;
                if (interfaceDescriptor.getUsbSubclass() == 3 && (midiHeaderDescriptor = interfaceDescriptor.getMidiHeaderInterfaceDescriptor()) != null && (midiHeaderDescriptor instanceof UsbMSMidiHeader)) {
                    UsbMSMidiHeader usbMSMidiHeader = (UsbMSMidiHeader) midiHeaderDescriptor;
                    count++;
                }
            } else {
                Log.w(TAG, "Undefined Audio Class Interface l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
            }
        }
        return count;
    }

    public float getInputHeadsetProbability() {
        if (hasMIDIInterface()) {
            return 0.0f;
        }
        float probability = 0.0f;
        boolean hasMic = hasMic();
        boolean hasSpeaker = hasSpeaker();
        if (hasMic && hasSpeaker) {
            probability = 0.0f + 0.75f;
        }
        if (hasMic && hasHIDInterface()) {
            return probability + 0.25f;
        }
        return probability;
    }

    public boolean isInputHeadset() {
        return getInputHeadsetProbability() >= 0.75f;
    }

    public float getOutputHeadsetProbability() {
        if (hasMIDIInterface()) {
            return 0.0f;
        }
        boolean hasSpeaker = false;
        ArrayList<UsbDescriptor> acDescriptors = getACInterfaceDescriptors((byte) 3, 1);
        Iterator<UsbDescriptor> it = acDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = it.next();
            if (descriptor instanceof UsbACTerminal) {
                UsbACTerminal outDescr = (UsbACTerminal) descriptor;
                if (outDescr.getTerminalType() == 769 || outDescr.getTerminalType() == 770 || outDescr.getTerminalType() == 1026) {
                    hasSpeaker = true;
                    break;
                }
            } else {
                Log.w(TAG, "Undefined Audio Output terminal l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
            }
        }
        float probability = hasSpeaker ? 0.0f + 0.75f : 0.0f;
        if (hasSpeaker && hasHIDInterface()) {
            return probability + 0.25f;
        }
        return probability;
    }

    public boolean isOutputHeadset() {
        return getOutputHeadsetProbability() >= 0.75f;
    }

    public boolean isDock() {
        if (hasMIDIInterface() || hasHIDInterface()) {
            return false;
        }
        ArrayList<UsbDescriptor> acDescriptors = getACInterfaceDescriptors((byte) 3, 1);
        if (acDescriptors.size() != 1) {
            return false;
        }
        if (acDescriptors.get(0) instanceof UsbACTerminal) {
            UsbACTerminal outDescr = (UsbACTerminal) acDescriptors.get(0);
            if (outDescr.getTerminalType() == 1538) {
                return true;
            }
        } else {
            Log.w(TAG, "Undefined Audio Output terminal l: " + acDescriptors.get(0).getLength() + " t:0x" + Integer.toHexString(acDescriptors.get(0).getType()));
        }
        return false;
    }
}
