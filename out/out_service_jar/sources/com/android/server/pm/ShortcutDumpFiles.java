package com.android.server.pm;

import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
/* loaded from: classes2.dex */
public class ShortcutDumpFiles {
    private static final boolean DEBUG = false;
    private static final String TAG = "ShortcutService";
    private final ShortcutService mService;

    public ShortcutDumpFiles(ShortcutService service) {
        this.mService = service;
    }

    public boolean save(String filename, Consumer<PrintWriter> dumper) {
        try {
            File directory = this.mService.getDumpPath();
            directory.mkdirs();
            if (!directory.exists()) {
                Slog.e(TAG, "Failed to create directory: " + directory);
                return false;
            }
            File path = new File(directory, filename);
            PrintWriter pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(path)));
            dumper.accept(pw);
            pw.close();
            return true;
        } catch (IOException | RuntimeException e) {
            Slog.w(TAG, "Failed to create dump file: " + filename, e);
            return false;
        }
    }

    public boolean save(String filename, final byte[] utf8bytes) {
        return save(filename, new Consumer() { // from class: com.android.server.pm.ShortcutDumpFiles$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((PrintWriter) obj).println(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(utf8bytes)).toString());
            }
        });
    }

    public void dumpAll(PrintWriter pw) {
        try {
            File directory = this.mService.getDumpPath();
            File[] files = directory.listFiles(new FileFilter() { // from class: com.android.server.pm.ShortcutDumpFiles$$ExternalSyntheticLambda1
                @Override // java.io.FileFilter
                public final boolean accept(File file) {
                    boolean isFile;
                    isFile = file.isFile();
                    return isFile;
                }
            });
            if (directory.exists() && !ArrayUtils.isEmpty(files)) {
                Arrays.sort(files, Comparator.comparing(new Function() { // from class: com.android.server.pm.ShortcutDumpFiles$$ExternalSyntheticLambda2
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        String name;
                        name = ((File) obj).getName();
                        return name;
                    }
                }));
                for (File path : files) {
                    pw.print("*** Dumping: ");
                    pw.println(path.getName());
                    pw.print("mtime: ");
                    pw.println(ShortcutService.formatTime(path.lastModified()));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        pw.println(line);
                    }
                    reader.close();
                }
                return;
            }
            pw.print("  No dump files found.");
        } catch (IOException | RuntimeException e) {
            Slog.w(TAG, "Failed to print dump files", e);
        }
    }
}
