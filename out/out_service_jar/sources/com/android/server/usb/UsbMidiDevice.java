package com.android.server.usb;

import android.content.Context;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceServer;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;
import com.android.internal.midi.MidiEventScheduler;
import com.android.internal.util.dump.DualDumpOutputStream;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public final class UsbMidiDevice implements Closeable {
    private static final int BUFFER_SIZE = 512;
    private static final String TAG = "UsbMidiDevice";
    private final int mAlsaCard;
    private final int mAlsaDevice;
    private MidiEventScheduler[] mEventSchedulers;
    private FileDescriptor[] mFileDescriptors;
    private FileInputStream[] mInputStreams;
    private boolean mIsOpen;
    private final InputReceiverProxy[] mMidiInputPortReceivers;
    private final int mNumInputs;
    private final int mNumOutputs;
    private FileOutputStream[] mOutputStreams;
    private StructPollfd[] mPollFDs;
    private MidiDeviceServer mServer;
    private boolean mServerAvailable;
    private final Object mLock = new Object();
    private int mPipeFD = -1;
    private final MidiDeviceServer.Callback mCallback = new MidiDeviceServer.Callback() { // from class: com.android.server.usb.UsbMidiDevice.1
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
            synchronized (UsbMidiDevice.this.mLock) {
                Log.d(UsbMidiDevice.TAG, "numOpenPorts: " + numOpenPorts + " isOpen: " + UsbMidiDevice.this.mIsOpen + " mServerAvailable: " + UsbMidiDevice.this.mServerAvailable);
                if (numOpenPorts > 0 && !UsbMidiDevice.this.mIsOpen && UsbMidiDevice.this.mServerAvailable) {
                    UsbMidiDevice.this.openLocked();
                } else if (numOpenPorts == 0 && UsbMidiDevice.this.mIsOpen) {
                    UsbMidiDevice.this.closeLocked();
                }
            }
        }

        public void onClose() {
        }
    };

    private native void nativeClose(FileDescriptor[] fileDescriptorArr);

    private native FileDescriptor[] nativeOpen(int i, int i2, int i3, int i4);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class InputReceiverProxy extends MidiReceiver {
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

    public static UsbMidiDevice create(Context context, Bundle properties, int card, int device, int numInputs, int numOutputs) {
        UsbMidiDevice midiDevice = new UsbMidiDevice(card, device, numInputs, numOutputs);
        if (!midiDevice.register(context, properties)) {
            IoUtils.closeQuietly(midiDevice);
            Log.e(TAG, "createDeviceServer failed");
            return null;
        }
        return midiDevice;
    }

    private UsbMidiDevice(int card, int device, int numInputs, int numOutputs) {
        this.mAlsaCard = card;
        this.mAlsaDevice = device;
        this.mNumInputs = numInputs;
        this.mNumOutputs = numOutputs;
        this.mMidiInputPortReceivers = new InputReceiverProxy[numOutputs];
        for (int port = 0; port < numOutputs; port++) {
            this.mMidiInputPortReceivers[port] = new InputReceiverProxy();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Type inference failed for: r14v0, types: [com.android.server.usb.UsbMidiDevice$3] */
    /* JADX WARN: Type inference failed for: r5v4, types: [com.android.server.usb.UsbMidiDevice$2] */
    public boolean openLocked() {
        int inputStreamCount = this.mNumInputs;
        if (inputStreamCount > 0) {
            inputStreamCount++;
        }
        int outputStreamCount = this.mNumOutputs;
        FileDescriptor[] fileDescriptors = nativeOpen(this.mAlsaCard, this.mAlsaDevice, inputStreamCount, outputStreamCount);
        if (fileDescriptors == null) {
            Log.e(TAG, "nativeOpen failed");
            return false;
        }
        this.mFileDescriptors = fileDescriptors;
        this.mPollFDs = new StructPollfd[inputStreamCount];
        this.mInputStreams = new FileInputStream[inputStreamCount];
        for (int i = 0; i < inputStreamCount; i++) {
            FileDescriptor fd = fileDescriptors[i];
            StructPollfd pollfd = new StructPollfd();
            pollfd.fd = fd;
            pollfd.events = (short) OsConstants.POLLIN;
            this.mPollFDs[i] = pollfd;
            this.mInputStreams[i] = new FileInputStream(fd);
        }
        this.mOutputStreams = new FileOutputStream[outputStreamCount];
        this.mEventSchedulers = new MidiEventScheduler[outputStreamCount];
        for (int i2 = 0; i2 < outputStreamCount; i2++) {
            this.mOutputStreams[i2] = new FileOutputStream(fileDescriptors[inputStreamCount + i2]);
            MidiEventScheduler scheduler = new MidiEventScheduler();
            this.mEventSchedulers[i2] = scheduler;
            this.mMidiInputPortReceivers[i2].setReceiver(scheduler.getReceiver());
        }
        final MidiReceiver[] outputReceivers = this.mServer.getOutputPortReceivers();
        if (inputStreamCount > 0) {
            new Thread("UsbMidiDevice input thread") { // from class: com.android.server.usb.UsbMidiDevice.2
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    byte[] buffer = new byte[512];
                    while (true) {
                        try {
                            long timestamp = System.nanoTime();
                            synchronized (UsbMidiDevice.this.mLock) {
                                if (!UsbMidiDevice.this.mIsOpen) {
                                    break;
                                }
                                for (int index = 0; index < UsbMidiDevice.this.mPollFDs.length; index++) {
                                    StructPollfd pfd = UsbMidiDevice.this.mPollFDs[index];
                                    if ((pfd.revents & (OsConstants.POLLERR | OsConstants.POLLHUP)) != 0) {
                                        break;
                                    }
                                    if ((pfd.revents & OsConstants.POLLIN) != 0) {
                                        pfd.revents = (short) 0;
                                        if (index == UsbMidiDevice.this.mInputStreams.length - 1) {
                                            break;
                                        }
                                        int count = UsbMidiDevice.this.mInputStreams[index].read(buffer);
                                        outputReceivers[index].send(buffer, 0, count, timestamp);
                                    }
                                }
                            }
                            Os.poll(UsbMidiDevice.this.mPollFDs, -1);
                        } catch (ErrnoException e) {
                            Log.d(UsbMidiDevice.TAG, "reader thread exiting");
                        } catch (IOException e2) {
                            Log.d(UsbMidiDevice.TAG, "reader thread exiting");
                        }
                    }
                    Log.d(UsbMidiDevice.TAG, "input thread exit");
                }
            }.start();
        }
        for (int port = 0; port < outputStreamCount; port++) {
            final MidiEventScheduler eventSchedulerF = this.mEventSchedulers[port];
            final FileOutputStream outputStreamF = this.mOutputStreams[port];
            final int portF = port;
            new Thread("UsbMidiDevice output thread " + port) { // from class: com.android.server.usb.UsbMidiDevice.3
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    MidiEventScheduler.MidiEvent event;
                    while (true) {
                        try {
                            event = eventSchedulerF.waitNextEvent();
                        } catch (InterruptedException e) {
                        }
                        if (event == null) {
                            Log.d(UsbMidiDevice.TAG, "output thread exit");
                            return;
                        }
                        try {
                            outputStreamF.write(event.data, 0, event.count);
                        } catch (IOException e2) {
                            Log.e(UsbMidiDevice.TAG, "write failed for port " + portF);
                        }
                        eventSchedulerF.addEventToPool(event);
                    }
                }
            }.start();
        }
        this.mIsOpen = true;
        return true;
    }

    private boolean register(Context context, Bundle properties) {
        MidiManager midiManager = (MidiManager) context.getSystemService("midi");
        if (midiManager == null) {
            Log.e(TAG, "No MidiManager in UsbMidiDevice.register()");
            return false;
        }
        this.mServerAvailable = true;
        MidiDeviceServer createDeviceServer = midiManager.createDeviceServer(this.mMidiInputPortReceivers, this.mNumInputs, null, null, properties, 1, -1, this.mCallback);
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
        for (int i = 0; i < this.mEventSchedulers.length; i++) {
            this.mMidiInputPortReceivers[i].setReceiver(null);
            this.mEventSchedulers[i].close();
        }
        this.mEventSchedulers = null;
        int i2 = 0;
        while (true) {
            FileInputStream[] fileInputStreamArr = this.mInputStreams;
            if (i2 >= fileInputStreamArr.length) {
                break;
            }
            IoUtils.closeQuietly(fileInputStreamArr[i2]);
            i2++;
        }
        this.mInputStreams = null;
        int i3 = 0;
        while (true) {
            FileOutputStream[] fileOutputStreamArr = this.mOutputStreams;
            if (i3 < fileOutputStreamArr.length) {
                IoUtils.closeQuietly(fileOutputStreamArr[i3]);
                i3++;
            } else {
                this.mOutputStreams = null;
                nativeClose(this.mFileDescriptors);
                this.mFileDescriptors = null;
                this.mIsOpen = false;
                return;
            }
        }
    }

    public void dump(String deviceAddr, DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        dump.write("device_address", 1138166333443L, deviceAddr);
        dump.write("card", (long) CompanionMessage.MESSAGE_ID, this.mAlsaCard);
        dump.write("device", 1120986464258L, this.mAlsaDevice);
        dump.end(token);
    }
}
