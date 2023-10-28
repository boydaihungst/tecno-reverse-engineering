package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.AtomicFile;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.am.HostingRecord;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
/* loaded from: classes.dex */
public class EntropyMixer extends Binder {
    static final String DEVICE_SPECIFIC_INFO_HEADER = "Copyright (C) 2009 The Android Open Source Project\nAll Your Randomness Are Belong To Us\n";
    static final int SEED_FILE_SIZE = 512;
    private static final int SEED_UPDATE_PERIOD = 10800000;
    private static final String TAG = "EntropyMixer";
    private static final int UPDATE_SEED_MSG = 1;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Handler mHandler;
    private final File randomReadDevice;
    private final File randomWriteDevice;
    private final AtomicFile seedFile;
    private static final long START_TIME = System.currentTimeMillis();
    private static final long START_NANOTIME = System.nanoTime();

    public EntropyMixer(Context context) {
        this(context, new File(getSystemDir(), "entropy.dat"), new File("/dev/urandom"), new File("/dev/urandom"));
    }

    EntropyMixer(Context context, File seedFile, File randomReadDevice, File randomWriteDevice) {
        Handler handler = new Handler(IoThread.getHandler().getLooper()) { // from class: com.android.server.EntropyMixer.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                if (msg.what != 1) {
                    Slog.e(EntropyMixer.TAG, "Will not process invalid message");
                    return;
                }
                EntropyMixer.this.updateSeedFile();
                EntropyMixer.this.scheduleSeedUpdater();
            }
        };
        this.mHandler = handler;
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.EntropyMixer.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                EntropyMixer.this.updateSeedFile();
            }
        };
        this.mBroadcastReceiver = broadcastReceiver;
        this.seedFile = new AtomicFile((File) Preconditions.checkNotNull(seedFile));
        this.randomReadDevice = (File) Preconditions.checkNotNull(randomReadDevice);
        this.randomWriteDevice = (File) Preconditions.checkNotNull(randomWriteDevice);
        loadInitialEntropy();
        updateSeedFile();
        scheduleSeedUpdater();
        IntentFilter broadcastFilter = new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
        broadcastFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        broadcastFilter.addAction("android.intent.action.REBOOT");
        context.registerReceiver(broadcastReceiver, broadcastFilter, null, handler);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleSeedUpdater() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 10800000L);
    }

    private void loadInitialEntropy() {
        byte[] seed = readSeedFile();
        try {
            FileOutputStream out = new FileOutputStream(this.randomWriteDevice);
            if (seed.length != 0) {
                out.write(seed);
                Slog.i(TAG, "Loaded existing seed file");
            }
            out.write(getDeviceSpecificInformation());
            out.close();
        } catch (IOException e) {
            Slog.e(TAG, "Error writing to " + this.randomWriteDevice, e);
        }
    }

    private byte[] readSeedFile() {
        try {
            return this.seedFile.readFully();
        } catch (FileNotFoundException e) {
            return new byte[0];
        } catch (IOException e2) {
            Slog.e(TAG, "Error reading " + this.seedFile.getBaseFile(), e2);
            return new byte[0];
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSeedFile() {
        FileInputStream in;
        byte[] oldSeed = readSeedFile();
        byte[] newSeed = new byte[512];
        try {
            in = new FileInputStream(this.randomReadDevice);
        } catch (IOException e) {
            Slog.e(TAG, "Error reading " + this.randomReadDevice + "; seed file won't be properly updated", e);
        }
        if (in.read(newSeed) != newSeed.length) {
            throw new IOException("unexpected EOF");
        }
        in.close();
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update("Android EntropyMixer v1".getBytes());
            sha256.update(longToBytes(System.currentTimeMillis()));
            sha256.update(longToBytes(System.nanoTime()));
            sha256.update(longToBytes(oldSeed.length));
            sha256.update(oldSeed);
            sha256.update(longToBytes(newSeed.length));
            sha256.update(newSeed);
            byte[] digest = sha256.digest();
            System.arraycopy(digest, 0, newSeed, newSeed.length - digest.length, digest.length);
            writeNewSeed(newSeed);
            if (oldSeed.length == 0) {
                Slog.i(TAG, "Created seed file");
            } else {
                Slog.i(TAG, "Updated seed file");
            }
        } catch (NoSuchAlgorithmException e2) {
            Slog.wtf(TAG, "SHA-256 algorithm not found; seed file won't be updated", e2);
        }
    }

    private void writeNewSeed(byte[] newSeed) {
        FileOutputStream out = null;
        try {
            out = this.seedFile.startWrite();
            out.write(newSeed);
            this.seedFile.finishWrite(out);
        } catch (IOException e) {
            Slog.e(TAG, "Error writing " + this.seedFile.getBaseFile(), e);
            this.seedFile.failWrite(out);
        }
    }

    private static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }

    private byte[] getDeviceSpecificInformation() {
        StringBuilder b = new StringBuilder();
        b.append(DEVICE_SPECIFIC_INFO_HEADER);
        b.append(START_TIME).append('\n');
        b.append(START_NANOTIME).append('\n');
        b.append(SystemProperties.get("ro.serialno")).append('\n');
        b.append(SystemProperties.get("ro.bootmode")).append('\n');
        b.append(SystemProperties.get("ro.baseband")).append('\n');
        b.append(SystemProperties.get("ro.carrier")).append('\n');
        b.append(SystemProperties.get("ro.bootloader")).append('\n');
        b.append(SystemProperties.get("ro.hardware")).append('\n');
        b.append(SystemProperties.get("ro.revision")).append('\n');
        b.append(SystemProperties.get("ro.build.fingerprint")).append('\n');
        b.append(new Object().hashCode()).append('\n');
        b.append(System.currentTimeMillis()).append('\n');
        b.append(System.nanoTime()).append('\n');
        return b.toString().getBytes();
    }

    private static File getSystemDir() {
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, HostingRecord.HOSTING_TYPE_SYSTEM);
        systemDir.mkdirs();
        return systemDir;
    }
}
