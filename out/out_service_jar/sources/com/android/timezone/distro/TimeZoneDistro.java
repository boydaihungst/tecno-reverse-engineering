package com.android.timezone.distro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
/* loaded from: classes2.dex */
public final class TimeZoneDistro {
    private static final int BUFFER_SIZE = 8192;
    public static final String DISTRO_VERSION_FILE_NAME = "distro_version";
    public static final String FILE_NAME = "distro.zip";
    public static final String ICU_DATA_FILE_NAME = "icu/icu_tzdata.dat";
    private static final long MAX_GET_ENTRY_CONTENTS_SIZE = 131072;
    public static final String TELEPHONYLOOKUP_FILE_NAME = "telephonylookup.xml";
    public static final String TZDATA_FILE_NAME = "tzdata";
    public static final String TZLOOKUP_FILE_NAME = "tzlookup.xml";
    private final InputStream inputStream;

    public TimeZoneDistro(byte[] bytes) {
        this(new ByteArrayInputStream(bytes));
    }

    public TimeZoneDistro(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public DistroVersion getDistroVersion() throws DistroException, IOException {
        byte[] contents = getEntryContents(this.inputStream, DISTRO_VERSION_FILE_NAME);
        if (contents == null) {
            throw new DistroException("Distro version file entry not found");
        }
        return DistroVersion.fromBytes(contents);
    }

    private static byte[] getEntryContents(InputStream is, String entryName) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(is);
        while (true) {
            try {
                ZipEntry entry = zipInputStream.getNextEntry();
                if (entry != null) {
                    String name = entry.getName();
                    if (entryName.equals(name)) {
                        if (entry.getSize() > MAX_GET_ENTRY_CONTENTS_SIZE) {
                            throw new IOException("Entry " + entryName + " too large: " + entry.getSize());
                        }
                        byte[] buffer = new byte[8192];
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        while (true) {
                            int count = zipInputStream.read(buffer);
                            if (count != -1) {
                                baos.write(buffer, 0, count);
                            } else {
                                byte[] byteArray = baos.toByteArray();
                                baos.close();
                                zipInputStream.close();
                                return byteArray;
                            }
                        }
                    }
                } else {
                    zipInputStream.close();
                    return null;
                }
            } catch (Throwable th) {
                try {
                    zipInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }

    public void extractTo(File targetDir) throws IOException {
        extractZipSafely(this.inputStream, targetDir, true);
    }

    static void extractZipSafely(InputStream is, File targetDir, boolean makeWorldReadable) throws IOException {
        FileUtils.ensureDirectoriesExist(targetDir, makeWorldReadable);
        ZipInputStream zipInputStream = new ZipInputStream(is);
        try {
            byte[] buffer = new byte[8192];
            while (true) {
                ZipEntry entry = zipInputStream.getNextEntry();
                if (entry != null) {
                    String name = entry.getName();
                    File entryFile = FileUtils.createSubFile(targetDir, name);
                    if (entry.isDirectory()) {
                        FileUtils.ensureDirectoriesExist(entryFile, makeWorldReadable);
                    } else {
                        if (!entryFile.getParentFile().exists()) {
                            FileUtils.ensureDirectoriesExist(entryFile.getParentFile(), makeWorldReadable);
                        }
                        FileOutputStream fos = new FileOutputStream(entryFile);
                        while (true) {
                            int count = zipInputStream.read(buffer);
                            if (count == -1) {
                                break;
                            }
                            fos.write(buffer, 0, count);
                        }
                        fos.getFD().sync();
                        fos.close();
                        if (makeWorldReadable) {
                            FileUtils.makeWorldReadable(entryFile);
                        }
                    }
                } else {
                    zipInputStream.close();
                    return;
                }
            }
        } catch (Throwable th) {
            try {
                zipInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }
}
