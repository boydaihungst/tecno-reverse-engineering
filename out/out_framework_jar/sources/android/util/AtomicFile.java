package android.util;

import android.annotation.SystemApi;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.media.AudioSystem;
import android.os.FileUtils;
import android.os.SystemProperties;
import com.transsion.hubcore.util.ITranAtomFile;
import com.transsion.xmlprotect.VerifyExt;
import com.transsion.xmlprotect.VerifyFacotry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;
/* loaded from: classes3.dex */
public class AtomicFile {
    private static final String LOG_TAG = "AtomicFile";
    private final File mBaseName;
    private SystemConfigFileCommitEventLogger mCommitEventLogger;
    private VerifyExt mFilesVerify;
    private final File mLegacyBackupName;
    private final File mNewName;
    private final boolean mXmlSupport;

    public AtomicFile(File baseName) {
        this(baseName, (SystemConfigFileCommitEventLogger) null);
    }

    public AtomicFile(File baseName, String commitTag) {
        this(baseName, new SystemConfigFileCommitEventLogger(commitTag));
    }

    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public AtomicFile(File baseName, SystemConfigFileCommitEventLogger commitEventLogger) {
        boolean equals = "1".equals(SystemProperties.get("ro.vendor.tran.xml.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS));
        this.mXmlSupport = equals;
        this.mBaseName = baseName;
        this.mNewName = new File(baseName.getPath() + ".new");
        this.mLegacyBackupName = new File(baseName.getPath() + ".bak");
        this.mCommitEventLogger = commitEventLogger;
        if (equals) {
            VerifyExt verifyExt = VerifyFacotry.getInstance().getVerifyExt();
            this.mFilesVerify = verifyExt;
            verifyExt.setBackupName(baseName);
        }
    }

    public File getBaseFile() {
        return this.mBaseName;
    }

    public void delete() {
        this.mBaseName.delete();
        this.mNewName.delete();
        this.mLegacyBackupName.delete();
    }

    public FileOutputStream startWrite() throws IOException {
        return startWrite(0L);
    }

    @Deprecated
    public FileOutputStream startWrite(long startTime) throws IOException {
        SystemConfigFileCommitEventLogger systemConfigFileCommitEventLogger = this.mCommitEventLogger;
        if (systemConfigFileCommitEventLogger != null) {
            if (startTime != 0) {
                systemConfigFileCommitEventLogger.setStartTime(startTime);
            }
            this.mCommitEventLogger.onStartWrite();
        }
        if (this.mLegacyBackupName.exists()) {
            rename(this.mLegacyBackupName, this.mBaseName);
        }
        try {
            return new FileOutputStream(this.mNewName);
        } catch (FileNotFoundException e) {
            File parent = this.mNewName.getParentFile();
            if (!parent.mkdirs()) {
                throw new IOException("Failed to create directory for " + this.mNewName);
            }
            FileUtils.setPermissions(parent.getPath(), 505, -1, -1);
            try {
                return new FileOutputStream(this.mNewName);
            } catch (FileNotFoundException e2) {
                throw new IOException("Failed to create new file " + this.mNewName, e2);
            }
        }
    }

    public void finishWrite(FileOutputStream str) {
        if (str == null) {
            return;
        }
        if (!FileUtils.sync(str)) {
            Log.e(LOG_TAG, "Failed to sync file output stream");
        }
        try {
            str.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to close file output stream", e);
        }
        rename(this.mNewName, this.mBaseName);
        SystemConfigFileCommitEventLogger systemConfigFileCommitEventLogger = this.mCommitEventLogger;
        if (systemConfigFileCommitEventLogger != null) {
            systemConfigFileCommitEventLogger.onFinishWrite();
        }
        ITranAtomFile.Instance().verifyFileBeforeFinishWrite(this.mBaseName);
    }

    public void failWrite(FileOutputStream str) {
        if (str == null) {
            return;
        }
        if (!FileUtils.sync(str)) {
            Log.e(LOG_TAG, "Failed to sync file output stream");
        }
        try {
            str.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to close file output stream", e);
        }
        if (!this.mNewName.delete()) {
            Log.e(LOG_TAG, "Failed to delete new file " + this.mNewName);
        }
    }

    @Deprecated
    public void truncate() throws IOException {
        try {
            FileOutputStream fos = new FileOutputStream(this.mBaseName);
            FileUtils.sync(fos);
            fos.close();
        } catch (FileNotFoundException e) {
            throw new IOException("Couldn't append " + this.mBaseName);
        } catch (IOException e2) {
        }
    }

    @Deprecated
    public FileOutputStream openAppend() throws IOException {
        try {
            return new FileOutputStream(this.mBaseName, true);
        } catch (FileNotFoundException e) {
            throw new IOException("Couldn't append " + this.mBaseName);
        }
    }

    public FileInputStream openRead() throws FileNotFoundException {
        if (this.mLegacyBackupName.exists()) {
            rename(this.mLegacyBackupName, this.mBaseName);
        }
        ITranAtomFile.Instance().verifyFileBeforeFinishRead(this.mBaseName);
        if (this.mNewName.exists() && this.mBaseName.exists() && !this.mNewName.delete()) {
            Log.e(LOG_TAG, "Failed to delete outdated new file " + this.mNewName);
        }
        return new FileInputStream(this.mBaseName);
    }

    public boolean exists() {
        return this.mBaseName.exists() || this.mLegacyBackupName.exists();
    }

    public long getLastModifiedTime() {
        if (this.mLegacyBackupName.exists()) {
            return this.mLegacyBackupName.lastModified();
        }
        return this.mBaseName.lastModified();
    }

    public byte[] readFully() throws IOException {
        FileInputStream stream = openRead();
        int pos = 0;
        try {
            byte[] data = new byte[stream.available()];
            while (true) {
                int amt = stream.read(data, pos, data.length - pos);
                if (amt <= 0) {
                    return data;
                }
                pos += amt;
                int avail = stream.available();
                if (avail > data.length - pos) {
                    byte[] newData = new byte[pos + avail];
                    System.arraycopy(data, 0, newData, 0, pos);
                    data = newData;
                }
            }
        } finally {
            stream.close();
        }
    }

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    public void write(Consumer<FileOutputStream> writeContent) {
        FileOutputStream out = null;
        try {
            out = startWrite();
            writeContent.accept(out);
            finishWrite(out);
        } finally {
        }
    }

    public String toString() {
        return "AtomicFile[" + this.mBaseName + NavigationBarInflaterView.SIZE_MOD_END;
    }

    private static void rename(File source, File target) {
        if (target.isDirectory() && !target.delete()) {
            Log.e(LOG_TAG, "Failed to delete file which is a directory " + target);
        }
        if (!source.renameTo(target)) {
            Log.e(LOG_TAG, "Failed to rename " + source + " to " + target);
        }
    }
}
