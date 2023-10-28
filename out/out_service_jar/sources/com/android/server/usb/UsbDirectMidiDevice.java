package com.android.server.usb;

import android.content.Context;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceServer;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.util.Log;
import com.android.internal.midi.MidiEventScheduler;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.usb.descriptors.UsbDescriptorParser;
import com.android.server.usb.descriptors.UsbEndpointDescriptor;
import com.android.server.usb.descriptors.UsbInterfaceDescriptor;
import com.android.server.usb.descriptors.UsbMidiBlockParser;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public final class UsbDirectMidiDevice implements Closeable {
    private static final int BULK_TRANSFER_TIMEOUT_MILLISECONDS = 10;
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbDirectMidiDevice";
    private static final int THREAD_JOIN_TIMEOUT_MILLISECONDS = 200;
    private Context mContext;
    private MidiEventScheduler[] mEventSchedulers;
    private ArrayList<ArrayList<UsbEndpoint>> mInputUsbEndpoints;
    private boolean mIsOpen;
    private final boolean mIsUniversalMidiDevice;
    private final InputReceiverProxy[] mMidiInputPortReceivers;
    private String mName;
    private final int mNumInputs;
    private final int mNumOutputs;
    private ArrayList<ArrayList<UsbEndpoint>> mOutputUsbEndpoints;
    private UsbDescriptorParser mParser;
    private MidiDeviceServer mServer;
    private boolean mServerAvailable;
    private final boolean mShouldCallSetInterface;
    private ArrayList<Thread> mThreads;
    private final String mUniqueUsbDeviceIdentifier;
    private UsbDevice mUsbDevice;
    private ArrayList<UsbDeviceConnection> mUsbDeviceConnections;
    private ArrayList<UsbInterfaceDescriptor> mUsbInterfaces;
    private UsbMidiPacketConverter mUsbMidiPacketConverter;
    private UsbMidiBlockParser mMidiBlockParser = new UsbMidiBlockParser();
    private int mDefaultMidiProtocol = 1;
    private final Object mLock = new Object();
    private final MidiDeviceServer.Callback mCallback = new MidiDeviceServer.Callback() { // from class: com.android.server.usb.UsbDirectMidiDevice.1
        public void onDeviceStatusChanged(MidiDeviceServer server, MidiDeviceStatus status) {
            MidiDeviceInfo deviceInfo = status.getDeviceInfo();
            int numInputPorts = deviceInfo.getInputPortCount();
            int numOutputPorts = deviceInfo.getOutputPortCount();
            int numOpenPorts = 0;
            for (int i = 0; i < numInputPorts; i++) {
                if (status.isInputPortOpen(i)) {
                    numOpenPorts++;
                }
            }
            for (int i2 = 0; i2 < numOutputPorts; i2++) {
                if (status.getOutputPortOpenCount(i2) > 0) {
                    numOpenPorts += status.getOutputPortOpenCount(i2);
                }
            }
            synchronized (UsbDirectMidiDevice.this.mLock) {
                Log.d(UsbDirectMidiDevice.TAG, "numOpenPorts: " + numOpenPorts + " isOpen: " + UsbDirectMidiDevice.this.mIsOpen + " mServerAvailable: " + UsbDirectMidiDevice.this.mServerAvailable);
                if (numOpenPorts > 0 && !UsbDirectMidiDevice.this.mIsOpen && UsbDirectMidiDevice.this.mServerAvailable) {
                    UsbDirectMidiDevice.this.openLocked();
                } else if (numOpenPorts == 0 && UsbDirectMidiDevice.this.mIsOpen) {
                    UsbDirectMidiDevice.this.closeLocked();
                }
            }
        }

        public void onClose() {
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class InputReceiverProxy extends MidiReceiver {
        private MidiReceiver mReceiver;

        private InputReceiverProxy() {
        }

        @Override // android.media.midi.MidiReceiver
        public void onSend(byte[] msg, int offset, int count, long timestamp) throws IOException {
            MidiReceiver receiver = this.mReceiver;
            if (receiver != null) {
                receiver.send(msg, offset, count, timestamp);
            }
        }

        public void setReceiver(MidiReceiver receiver) {
            this.mReceiver = receiver;
        }

        @Override // android.media.midi.MidiReceiver
        public void onFlush() throws IOException {
            MidiReceiver receiver = this.mReceiver;
            if (receiver != null) {
                receiver.flush();
            }
        }
    }

    public static UsbDirectMidiDevice create(Context context, UsbDevice usbDevice, UsbDescriptorParser parser, boolean isUniversalMidiDevice, String uniqueUsbDeviceIdentifier) {
        UsbDirectMidiDevice midiDevice = new UsbDirectMidiDevice(usbDevice, parser, isUniversalMidiDevice, uniqueUsbDeviceIdentifier);
        if (!midiDevice.register(context)) {
            IoUtils.closeQuietly(midiDevice);
            Log.e(TAG, "createDeviceServer failed");
            return null;
        }
        return midiDevice;
    }

    private UsbDirectMidiDevice(UsbDevice usbDevice, UsbDescriptorParser parser, boolean isUniversalMidiDevice, String uniqueUsbDeviceIdentifier) {
        ArrayList<UsbInterfaceDescriptor> midiInterfaceDescriptors;
        this.mUsbDevice = usbDevice;
        this.mParser = parser;
        this.mUniqueUsbDeviceIdentifier = uniqueUsbDeviceIdentifier;
        this.mIsUniversalMidiDevice = isUniversalMidiDevice;
        this.mShouldCallSetInterface = parser.calculateMidiInterfaceDescriptorsCount() > 1;
        if (isUniversalMidiDevice) {
            midiInterfaceDescriptors = parser.findUniversalMidiInterfaceDescriptors();
        } else {
            midiInterfaceDescriptors = parser.findLegacyMidiInterfaceDescriptors();
        }
        this.mUsbInterfaces = new ArrayList<>();
        if (this.mUsbDevice.getConfigurationCount() > 0) {
            UsbConfiguration usbConfiguration = this.mUsbDevice.getConfiguration(0);
            for (int interfaceIndex = 0; interfaceIndex < usbConfiguration.getInterfaceCount(); interfaceIndex++) {
                UsbInterface usbInterface = usbConfiguration.getInterface(interfaceIndex);
                Iterator<UsbInterfaceDescriptor> it = midiInterfaceDescriptors.iterator();
                while (true) {
                    if (it.hasNext()) {
                        UsbInterfaceDescriptor midiInterfaceDescriptor = it.next();
                        UsbInterface midiInterface = midiInterfaceDescriptor.toAndroid(this.mParser);
                        if (areEquivalent(usbInterface, midiInterface)) {
                            this.mUsbInterfaces.add(midiInterfaceDescriptor);
                            break;
                        }
                    }
                }
            }
            if (this.mUsbDevice.getConfigurationCount() > 1) {
                Log.w(TAG, "Skipping some USB configurations. Count: " + this.mUsbDevice.getConfigurationCount());
            }
        }
        int numInputs = 0;
        int numOutputs = 0;
        for (int interfaceIndex2 = 0; interfaceIndex2 < this.mUsbInterfaces.size(); interfaceIndex2++) {
            UsbInterfaceDescriptor interfaceDescriptor = this.mUsbInterfaces.get(interfaceIndex2);
            for (int endpointIndex = 0; endpointIndex < interfaceDescriptor.getNumEndpoints(); endpointIndex++) {
                UsbEndpointDescriptor endpoint = interfaceDescriptor.getEndpointDescriptor(endpointIndex);
                if (endpoint.getDirection() == 0) {
                    numOutputs++;
                } else {
                    numInputs++;
                }
            }
        }
        this.mNumInputs = numInputs;
        this.mNumOutputs = numOutputs;
        Log.d(TAG, "Created UsbDirectMidiDevice with " + numInputs + " inputs and " + numOutputs + " outputs. isUniversalMidiDevice: " + isUniversalMidiDevice);
        this.mMidiInputPortReceivers = new InputReceiverProxy[numOutputs];
        for (int port = 0; port < numOutputs; port++) {
            this.mMidiInputPortReceivers[port] = new InputReceiverProxy();
        }
    }

    private int calculateDefaultMidiProtocol() {
        UsbManager manager = (UsbManager) this.mContext.getSystemService(UsbManager.class);
        for (int interfaceIndex = 0; interfaceIndex < this.mUsbInterfaces.size(); interfaceIndex++) {
            UsbInterfaceDescriptor interfaceDescriptor = this.mUsbInterfaces.get(interfaceIndex);
            boolean doesInterfaceContainInput = false;
            boolean doesInterfaceContainOutput = false;
            for (int endpointIndex = 0; endpointIndex < interfaceDescriptor.getNumEndpoints() && (!doesInterfaceContainInput || !doesInterfaceContainOutput); endpointIndex++) {
                UsbEndpointDescriptor endpoint = interfaceDescriptor.getEndpointDescriptor(endpointIndex);
                if (endpoint.getDirection() == 0) {
                    doesInterfaceContainOutput = true;
                } else {
                    doesInterfaceContainInput = true;
                }
            }
            if (doesInterfaceContainInput && doesInterfaceContainOutput) {
                UsbDeviceConnection connection = manager.openDevice(this.mUsbDevice);
                UsbInterface usbInterface = interfaceDescriptor.toAndroid(this.mParser);
                if (updateUsbInterface(usbInterface, connection)) {
                    int defaultMidiProtocol = this.mMidiBlockParser.calculateMidiType(connection, interfaceDescriptor.getInterfaceNumber(), interfaceDescriptor.getAlternateSetting());
                    connection.close();
                    return defaultMidiProtocol;
                }
            }
        }
        Log.w(TAG, "Cannot find interface with both input and output endpoints");
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean openLocked() {
        Log.d(TAG, "openLocked()");
        UsbManager manager = (UsbManager) this.mContext.getSystemService(UsbManager.class);
        this.mUsbMidiPacketConverter = new UsbMidiPacketConverter(this.mNumOutputs);
        this.mUsbDeviceConnections = new ArrayList<>();
        this.mInputUsbEndpoints = new ArrayList<>();
        this.mOutputUsbEndpoints = new ArrayList<>();
        this.mThreads = new ArrayList<>();
        for (int interfaceIndex = 0; interfaceIndex < this.mUsbInterfaces.size(); interfaceIndex++) {
            ArrayList<UsbEndpoint> inputEndpoints = new ArrayList<>();
            ArrayList<UsbEndpoint> outputEndpoints = new ArrayList<>();
            UsbInterfaceDescriptor interfaceDescriptor = this.mUsbInterfaces.get(interfaceIndex);
            for (int endpointIndex = 0; endpointIndex < interfaceDescriptor.getNumEndpoints(); endpointIndex++) {
                UsbEndpointDescriptor endpoint = interfaceDescriptor.getEndpointDescriptor(endpointIndex);
                if (endpoint.getDirection() == 0) {
                    outputEndpoints.add(endpoint.toAndroid(this.mParser));
                } else {
                    inputEndpoints.add(endpoint.toAndroid(this.mParser));
                }
            }
            if (!outputEndpoints.isEmpty() || !inputEndpoints.isEmpty()) {
                UsbDeviceConnection connection = manager.openDevice(this.mUsbDevice);
                UsbInterface usbInterface = interfaceDescriptor.toAndroid(this.mParser);
                if (updateUsbInterface(usbInterface, connection)) {
                    this.mUsbDeviceConnections.add(connection);
                    this.mInputUsbEndpoints.add(inputEndpoints);
                    this.mOutputUsbEndpoints.add(outputEndpoints);
                }
            }
        }
        int interfaceIndex2 = this.mNumOutputs;
        this.mEventSchedulers = new MidiEventScheduler[interfaceIndex2];
        for (int i = 0; i < this.mNumOutputs; i++) {
            MidiEventScheduler scheduler = new MidiEventScheduler();
            this.mEventSchedulers[i] = scheduler;
            this.mMidiInputPortReceivers[i].setReceiver(scheduler.getReceiver());
        }
        final MidiReceiver[] outputReceivers = this.mServer.getOutputPortReceivers();
        int portNumber = 0;
        int connectionIndex = 0;
        while (connectionIndex < this.mInputUsbEndpoints.size()) {
            int portNumber2 = portNumber;
            for (int endpointIndex2 = 0; endpointIndex2 < this.mInputUsbEndpoints.get(connectionIndex).size(); endpointIndex2++) {
                final UsbDeviceConnection connectionFinal = this.mUsbDeviceConnections.get(connectionIndex);
                final UsbEndpoint endpointFinal = this.mInputUsbEndpoints.get(connectionIndex).get(endpointIndex2);
                final int portFinal = portNumber2;
                Thread newThread = new Thread("UsbDirectMidiDevice input thread " + portFinal) { // from class: com.android.server.usb.UsbDirectMidiDevice.2
                    @Override // java.lang.Thread, java.lang.Runnable
                    public void run() {
                        byte[] convertedArray;
                        MidiReceiver midiReceiver;
                        UsbRequest request = new UsbRequest();
                        try {
                            try {
                                try {
                                    request.initialize(connectionFinal, endpointFinal);
                                    byte[] inputBuffer = new byte[endpointFinal.getMaxPacketSize()];
                                    while (true) {
                                        Thread.currentThread();
                                        if (Thread.interrupted()) {
                                            Log.w(UsbDirectMidiDevice.TAG, "input thread interrupted");
                                            break;
                                        }
                                        long timestamp = System.nanoTime();
                                        ByteBuffer byteBuffer = ByteBuffer.wrap(inputBuffer);
                                        if (!request.queue(byteBuffer)) {
                                            Log.w(UsbDirectMidiDevice.TAG, "Cannot queue request");
                                            break;
                                        }
                                        UsbRequest response = connectionFinal.requestWait();
                                        if (response != request) {
                                            Log.w(UsbDirectMidiDevice.TAG, "Unexpected response");
                                            break;
                                        }
                                        int bytesRead = byteBuffer.position();
                                        if (bytesRead > 0) {
                                            if (UsbDirectMidiDevice.this.mIsUniversalMidiDevice) {
                                                convertedArray = UsbDirectMidiDevice.this.swapEndiannessPerWord(inputBuffer, bytesRead);
                                            } else if (UsbDirectMidiDevice.this.mUsbMidiPacketConverter == null) {
                                                Log.w(UsbDirectMidiDevice.TAG, "mUsbMidiPacketConverter is null");
                                                break;
                                            } else {
                                                convertedArray = UsbDirectMidiDevice.this.mUsbMidiPacketConverter.usbMidiToRawMidi(inputBuffer, bytesRead);
                                            }
                                            MidiReceiver[] midiReceiverArr = outputReceivers;
                                            if (midiReceiverArr == null || (midiReceiver = midiReceiverArr[portFinal]) == null) {
                                                break;
                                            }
                                            midiReceiver.send(convertedArray, 0, convertedArray.length, timestamp);
                                        }
                                    }
                                    Log.w(UsbDirectMidiDevice.TAG, "outputReceivers is null");
                                } catch (IOException e) {
                                    Log.d(UsbDirectMidiDevice.TAG, "reader thread exiting");
                                }
                            } catch (NullPointerException e2) {
                                Log.e(UsbDirectMidiDevice.TAG, "input thread: ", e2);
                            }
                            request.close();
                            Log.d(UsbDirectMidiDevice.TAG, "input thread exit");
                        } catch (Throwable th) {
                            request.close();
                            throw th;
                        }
                    }
                };
                newThread.start();
                this.mThreads.add(newThread);
                portNumber2++;
            }
            connectionIndex++;
            portNumber = portNumber2;
        }
        int portNumber3 = 0;
        int connectionIndex2 = 0;
        while (connectionIndex2 < this.mOutputUsbEndpoints.size()) {
            int portNumber4 = portNumber3;
            for (int endpointIndex3 = 0; endpointIndex3 < this.mOutputUsbEndpoints.get(connectionIndex2).size(); endpointIndex3++) {
                final UsbDeviceConnection connectionFinal2 = this.mUsbDeviceConnections.get(connectionIndex2);
                final UsbEndpoint endpointFinal2 = this.mOutputUsbEndpoints.get(connectionIndex2).get(endpointIndex3);
                final int portFinal2 = portNumber4;
                final MidiEventScheduler eventSchedulerFinal = this.mEventSchedulers[portFinal2];
                Thread newThread2 = new Thread("UsbDirectMidiDevice output thread " + portFinal2) { // from class: com.android.server.usb.UsbDirectMidiDevice.3
                    @Override // java.lang.Thread, java.lang.Runnable
                    public void run() {
                        byte[] convertedArray;
                        while (true) {
                            try {
                                Thread.currentThread();
                                if (Thread.interrupted()) {
                                    Log.w(UsbDirectMidiDevice.TAG, "output thread interrupted");
                                    break;
                                }
                                try {
                                    MidiEventScheduler.MidiEvent event = eventSchedulerFinal.waitNextEvent();
                                    if (event == null) {
                                        Log.w(UsbDirectMidiDevice.TAG, "event is null");
                                        break;
                                    }
                                    if (UsbDirectMidiDevice.this.mIsUniversalMidiDevice) {
                                        convertedArray = UsbDirectMidiDevice.this.swapEndiannessPerWord(event.data, event.count);
                                    } else if (UsbDirectMidiDevice.this.mUsbMidiPacketConverter == null) {
                                        Log.w(UsbDirectMidiDevice.TAG, "mUsbMidiPacketConverter is null");
                                        break;
                                    } else {
                                        convertedArray = UsbDirectMidiDevice.this.mUsbMidiPacketConverter.rawMidiToUsbMidi(event.data, event.count, portFinal2);
                                    }
                                    connectionFinal2.bulkTransfer(endpointFinal2, convertedArray, convertedArray.length, 10);
                                    eventSchedulerFinal.addEventToPool(event);
                                } catch (InterruptedException e) {
                                    Log.w(UsbDirectMidiDevice.TAG, "event scheduler interrupted");
                                }
                            } catch (NullPointerException e2) {
                                Log.e(UsbDirectMidiDevice.TAG, "output thread: ", e2);
                            }
                        }
                        Log.d(UsbDirectMidiDevice.TAG, "output thread exit");
                    }
                };
                newThread2.start();
                this.mThreads.add(newThread2);
                portNumber4++;
            }
            connectionIndex2++;
            portNumber3 = portNumber4;
        }
        this.mIsOpen = true;
        return true;
    }

    private boolean register(Context context) {
        String name;
        String name2;
        this.mContext = context;
        MidiManager midiManager = (MidiManager) context.getSystemService(MidiManager.class);
        if (midiManager == null) {
            Log.e(TAG, "No MidiManager in UsbDirectMidiDevice.register()");
            return false;
        }
        if (this.mIsUniversalMidiDevice) {
            this.mDefaultMidiProtocol = calculateDefaultMidiProtocol();
        } else {
            this.mDefaultMidiProtocol = -1;
        }
        Bundle properties = new Bundle();
        String manufacturer = this.mUsbDevice.getManufacturerName();
        String product = this.mUsbDevice.getProductName();
        String version = this.mUsbDevice.getVersion();
        if (manufacturer == null || manufacturer.isEmpty()) {
            name = product;
        } else if (product == null || product.isEmpty()) {
            name = manufacturer;
        } else {
            name = manufacturer + " " + product;
        }
        String name3 = name + "#" + this.mUniqueUsbDeviceIdentifier;
        if (this.mIsUniversalMidiDevice) {
            name2 = name3 + " MIDI 2.0";
        } else {
            name2 = name3 + " MIDI 1.0";
        }
        this.mName = name2;
        properties.putString("name", name2);
        properties.putString("manufacturer", manufacturer);
        properties.putString("product", product);
        properties.putString("version", version);
        properties.putString("serial_number", this.mUsbDevice.getSerialNumber());
        properties.putParcelable("usb_device", this.mUsbDevice);
        this.mServerAvailable = true;
        MidiDeviceServer createDeviceServer = midiManager.createDeviceServer(this.mMidiInputPortReceivers, this.mNumInputs, null, null, properties, 1, this.mDefaultMidiProtocol, this.mCallback);
        this.mServer = createDeviceServer;
        if (createDeviceServer == null) {
            return false;
        }
        return true;
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        synchronized (this.mLock) {
            if (this.mIsOpen) {
                closeLocked();
            }
            this.mServerAvailable = false;
        }
        MidiDeviceServer midiDeviceServer = this.mServer;
        if (midiDeviceServer != null) {
            IoUtils.closeQuietly(midiDeviceServer);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void closeLocked() {
        Log.d(TAG, "closeLocked()");
        Iterator<Thread> it = this.mThreads.iterator();
        while (it.hasNext()) {
            Thread thread = it.next();
            if (thread != null) {
                thread.interrupt();
            }
        }
        Iterator<Thread> it2 = this.mThreads.iterator();
        while (it2.hasNext()) {
            Thread thread2 = it2.next();
            if (thread2 != null) {
                try {
                    thread2.join(200L);
                } catch (InterruptedException e) {
                    Log.w(TAG, "thread join interrupted");
                }
            }
        }
        this.mThreads = null;
        for (int i = 0; i < this.mEventSchedulers.length; i++) {
            this.mMidiInputPortReceivers[i].setReceiver(null);
            this.mEventSchedulers[i].close();
        }
        this.mEventSchedulers = null;
        Iterator<UsbDeviceConnection> it3 = this.mUsbDeviceConnections.iterator();
        while (it3.hasNext()) {
            UsbDeviceConnection connection = it3.next();
            connection.close();
        }
        this.mUsbDeviceConnections = null;
        this.mInputUsbEndpoints = null;
        this.mOutputUsbEndpoints = null;
        this.mUsbMidiPacketConverter = null;
        this.mIsOpen = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public byte[] swapEndiannessPerWord(byte[] inputArray, int size) {
        int numberOfExcessBytes = size & 3;
        if (numberOfExcessBytes != 0) {
            Log.e(TAG, "size not multiple of 4: " + size);
        }
        byte[] outputArray = new byte[size - numberOfExcessBytes];
        for (int i = 0; i + 3 < size; i += 4) {
            outputArray[i] = inputArray[i + 3];
            outputArray[i + 1] = inputArray[i + 2];
            outputArray[i + 2] = inputArray[i + 1];
            outputArray[i + 3] = inputArray[i];
        }
        return outputArray;
    }

    private static void logByteArray(String prefix, byte[] value, int offset, int count) {
        StringBuilder builder = new StringBuilder(prefix);
        for (int i = offset; i < offset + count; i++) {
            builder.append(String.format("0x%02X", Byte.valueOf(value[i])));
            if (i != value.length - 1) {
                builder.append(", ");
            }
        }
        Log.d(TAG, builder.toString());
    }

    private boolean updateUsbInterface(UsbInterface usbInterface, UsbDeviceConnection connection) {
        if (usbInterface == null) {
            Log.e(TAG, "Usb Interface is null");
            return false;
        } else if (connection == null) {
            Log.e(TAG, "UsbDeviceConnection is null");
            return false;
        } else if (!connection.claimInterface(usbInterface, true)) {
            Log.e(TAG, "Can't claim interface");
            return false;
        } else {
            if (this.mShouldCallSetInterface) {
                if (!connection.setInterface(usbInterface)) {
                    Log.w(TAG, "Can't set interface");
                }
            } else {
                Log.w(TAG, "no alternate interface");
            }
            return true;
        }
    }

    private boolean areEquivalent(UsbInterface interface1, UsbInterface interface2) {
        if (interface1.getId() == interface2.getId() && interface1.getAlternateSetting() == interface2.getAlternateSetting() && interface1.getInterfaceClass() == interface2.getInterfaceClass() && interface1.getInterfaceSubclass() == interface2.getInterfaceSubclass() && interface1.getInterfaceProtocol() == interface2.getInterfaceProtocol() && interface1.getEndpointCount() == interface2.getEndpointCount()) {
            if (interface1.getName() == null) {
                if (interface2.getName() != null) {
                    return false;
                }
            } else if (!interface1.getName().equals(interface2.getName())) {
                return false;
            }
            for (int i = 0; i < interface1.getEndpointCount(); i++) {
                UsbEndpoint endpoint1 = interface1.getEndpoint(i);
                UsbEndpoint endpoint2 = interface2.getEndpoint(i);
                if (endpoint1.getAddress() != endpoint2.getAddress() || endpoint1.getAttributes() != endpoint2.getAttributes() || endpoint1.getMaxPacketSize() != endpoint2.getMaxPacketSize() || endpoint1.getInterval() != endpoint2.getInterval()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        dump.write("num_inputs", (long) CompanionMessage.MESSAGE_ID, this.mNumInputs);
        dump.write("num_outputs", 1120986464258L, this.mNumOutputs);
        dump.write("is_universal", 1133871366147L, this.mIsUniversalMidiDevice);
        dump.write("name", 1138166333444L, this.mName);
        if (this.mIsUniversalMidiDevice) {
            this.mMidiBlockParser.dump(dump, "block_parser", 1146756268037L);
        }
        dump.end(token);
    }
}
