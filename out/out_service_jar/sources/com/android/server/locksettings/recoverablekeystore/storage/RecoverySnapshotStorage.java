package com.android.server.locksettings.recoverablekeystore.storage;

import android.os.Environment;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.util.Log;
import android.util.SparseArray;
import com.android.server.locksettings.recoverablekeystore.serialization.KeyChainSnapshotDeserializer;
import com.android.server.locksettings.recoverablekeystore.serialization.KeyChainSnapshotParserException;
import com.android.server.locksettings.recoverablekeystore.serialization.KeyChainSnapshotSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.Locale;
/* loaded from: classes.dex */
public class RecoverySnapshotStorage {
    private static final String ROOT_PATH = "system";
    private static final String STORAGE_PATH = "recoverablekeystore/snapshots/";
    private static final String TAG = "RecoverySnapshotStorage";
    private final SparseArray<KeyChainSnapshot> mSnapshotByUid = new SparseArray<>();
    private final File rootDirectory;

    public static RecoverySnapshotStorage newInstance() {
        return new RecoverySnapshotStorage(new File(Environment.getDataDirectory(), "system"));
    }

    public RecoverySnapshotStorage(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public synchronized void put(int uid, KeyChainSnapshot snapshot) {
        this.mSnapshotByUid.put(uid, snapshot);
        try {
            writeToDisk(uid, snapshot);
        } catch (IOException | CertificateEncodingException e) {
            Log.e(TAG, String.format(Locale.US, "Error persisting snapshot for %d to disk", Integer.valueOf(uid)), e);
        }
    }

    public synchronized KeyChainSnapshot get(int uid) {
        KeyChainSnapshot snapshot = this.mSnapshotByUid.get(uid);
        if (snapshot != null) {
            return snapshot;
        }
        try {
            return readFromDisk(uid);
        } catch (KeyChainSnapshotParserException | IOException e) {
            Log.e(TAG, String.format(Locale.US, "Error reading snapshot for %d from disk", Integer.valueOf(uid)), e);
            return null;
        }
    }

    public synchronized void remove(int uid) {
        this.mSnapshotByUid.remove(uid);
        getSnapshotFile(uid).delete();
    }

    private void writeToDisk(int uid, KeyChainSnapshot snapshot) throws IOException, CertificateEncodingException {
        File snapshotFile = getSnapshotFile(uid);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(snapshotFile);
            KeyChainSnapshotSerializer.serialize(snapshot, fileOutputStream);
            fileOutputStream.close();
        } catch (IOException | CertificateEncodingException e) {
            snapshotFile.delete();
            throw e;
        }
    }

    private KeyChainSnapshot readFromDisk(int uid) throws IOException, KeyChainSnapshotParserException {
        File snapshotFile = getSnapshotFile(uid);
        try {
            FileInputStream fileInputStream = new FileInputStream(snapshotFile);
            KeyChainSnapshot deserialize = KeyChainSnapshotDeserializer.deserialize(fileInputStream);
            fileInputStream.close();
            return deserialize;
        } catch (KeyChainSnapshotParserException | IOException e) {
            snapshotFile.delete();
            throw e;
        }
    }

    private File getSnapshotFile(int uid) {
        File folder = getStorageFolder();
        String fileName = getSnapshotFileName(uid);
        return new File(folder, fileName);
    }

    private String getSnapshotFileName(int uid) {
        return String.format(Locale.US, "%d.xml", Integer.valueOf(uid));
    }

    private File getStorageFolder() {
        File folder = new File(this.rootDirectory, STORAGE_PATH);
        folder.mkdirs();
        return folder;
    }
}
