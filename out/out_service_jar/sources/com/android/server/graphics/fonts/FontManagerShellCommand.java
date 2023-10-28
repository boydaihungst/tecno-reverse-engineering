package com.android.server.graphics.fonts;

import android.content.Context;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.graphics.fonts.FontUpdateRequest;
import android.graphics.fonts.FontVariationAxis;
import android.graphics.fonts.SystemFonts;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.ShellCommand;
import android.text.FontConfig;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.Xml;
import com.android.internal.util.DumpUtils;
import com.android.server.am.HostingRecord;
import com.android.server.graphics.fonts.FontManagerService;
import com.android.server.wm.ActivityTaskManagerService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class FontManagerShellCommand extends ShellCommand {
    private static final int MAX_SIGNATURE_FILE_SIZE_BYTES = 8192;
    private static final String TAG = "FontManagerShellCommand";
    private final FontManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FontManagerShellCommand(FontManagerService service) {
        this.mService = service;
    }

    public int onCommand(String cmd) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != 2000) {
            getErrPrintWriter().println("Only shell or root user can execute font command.");
            return 1;
        }
        return execCommand(this, cmd);
    }

    public void onHelp() {
        PrintWriter w = getOutPrintWriter();
        w.println("Font service (font) commands");
        w.println("help");
        w.println("    Print this help text.");
        w.println();
        w.println("dump [family name]");
        w.println("    Dump all font files in the specified family name.");
        w.println("    Dump current system font configuration if no family name was specified.");
        w.println();
        w.println("update [font file path] [signature file path]");
        w.println("    Update installed font files with new font file.");
        w.println();
        w.println("update-family [family definition XML path]");
        w.println("    Update font families with the new definitions.");
        w.println();
        w.println("clear");
        w.println("    Remove all installed font files and reset to the initial state.");
        w.println();
        w.println(HostingRecord.HOSTING_TYPE_RESTART);
        w.println("    Restart FontManagerService emulating device reboot.");
        w.println("    WARNING: this is not a safe operation. Other processes may misbehave if");
        w.println("    they are using fonts updated by FontManagerService.");
        w.println("    This command exists merely for testing.");
        w.println();
        w.println("status");
        w.println("    Prints status of current system font configuration.");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpAll(IndentingPrintWriter w) {
        FontConfig fontConfig = this.mService.getSystemFontConfig();
        dumpFontConfig(w, fontConfig);
    }

    private void dumpSingleFontConfig(IndentingPrintWriter w, FontConfig.Font font) {
        StringBuilder sb = new StringBuilder();
        sb.append("style = ");
        sb.append(font.getStyle());
        sb.append(", path = ");
        sb.append(font.getFile().getAbsolutePath());
        if (font.getTtcIndex() != 0) {
            sb.append(", index = ");
            sb.append(font.getTtcIndex());
        }
        if (!font.getFontVariationSettings().isEmpty()) {
            sb.append(", axes = ");
            sb.append(font.getFontVariationSettings());
        }
        if (font.getFontFamilyName() != null) {
            sb.append(", fallback = ");
            sb.append(font.getFontFamilyName());
        }
        w.println(sb.toString());
        if (font.getOriginalFile() != null) {
            w.increaseIndent();
            w.println("Font is updated from " + font.getOriginalFile());
            w.decreaseIndent();
        }
    }

    private void dumpFontConfig(IndentingPrintWriter w, FontConfig fontConfig) {
        List<FontConfig.FontFamily> families = fontConfig.getFontFamilies();
        w.println("Named Font Families");
        w.increaseIndent();
        for (int i = 0; i < families.size(); i++) {
            FontConfig.FontFamily family = families.get(i);
            if (family.getName() != null) {
                w.println("Named Family (" + family.getName() + ")");
                List<FontConfig.Font> fonts = family.getFontList();
                w.increaseIndent();
                for (int j = 0; j < fonts.size(); j++) {
                    dumpSingleFontConfig(w, fonts.get(j));
                }
                w.decreaseIndent();
            }
        }
        w.decreaseIndent();
        w.println("Dump Fallback Families");
        w.increaseIndent();
        int c = 0;
        for (int i2 = 0; i2 < families.size(); i2++) {
            FontConfig.FontFamily family2 = families.get(i2);
            if (family2.getName() == null) {
                StringBuilder sb = new StringBuilder("Fallback Family [");
                int c2 = c + 1;
                sb.append(c);
                sb.append("]: lang=\"");
                sb.append(family2.getLocaleList().toLanguageTags());
                sb.append("\"");
                if (family2.getVariant() != 0) {
                    sb.append(", variant=");
                    switch (family2.getVariant()) {
                        case 1:
                            sb.append("Compact");
                            break;
                        case 2:
                            sb.append("Elegant");
                            break;
                        default:
                            sb.append("Unknown");
                            break;
                    }
                }
                w.println(sb.toString());
                List<FontConfig.Font> fonts2 = family2.getFontList();
                w.increaseIndent();
                for (int j2 = 0; j2 < fonts2.size(); j2++) {
                    dumpSingleFontConfig(w, fonts2.get(j2));
                }
                w.decreaseIndent();
                c = c2;
            }
        }
        w.decreaseIndent();
        w.println("Dump Family Aliases");
        w.increaseIndent();
        List<FontConfig.Alias> aliases = fontConfig.getAliases();
        for (int i3 = 0; i3 < aliases.size(); i3++) {
            FontConfig.Alias alias = aliases.get(i3);
            w.println("alias = " + alias.getName() + ", reference = " + alias.getOriginal() + ", width = " + alias.getWeight());
        }
        w.decreaseIndent();
    }

    private void dumpFallback(IndentingPrintWriter writer, FontFamily[] families) {
        for (FontFamily family : families) {
            dumpFamily(writer, family);
        }
    }

    private void dumpFamily(IndentingPrintWriter writer, FontFamily family) {
        StringBuilder sb = new StringBuilder("Family:");
        if (family.getLangTags() != null) {
            sb.append(" langTag = ");
            sb.append(family.getLangTags());
        }
        if (family.getVariant() != 0) {
            sb.append(" variant = ");
            switch (family.getVariant()) {
                case 1:
                    sb.append("Compact");
                    break;
                case 2:
                    sb.append("Elegant");
                    break;
                default:
                    sb.append("UNKNOWN");
                    break;
            }
        }
        writer.println(sb.toString());
        for (int i = 0; i < family.getSize(); i++) {
            writer.increaseIndent();
            try {
                dumpFont(writer, family.getFont(i));
                writer.decreaseIndent();
            } catch (Throwable th) {
                writer.decreaseIndent();
                throw th;
            }
        }
    }

    private void dumpFont(IndentingPrintWriter writer, Font font) {
        File file = font.getFile();
        StringBuilder sb = new StringBuilder();
        sb.append(font.getStyle());
        sb.append(", path = ");
        sb.append(file == null ? "[Not a file]" : file.getAbsolutePath());
        if (font.getTtcIndex() != 0) {
            sb.append(", index = ");
            sb.append(font.getTtcIndex());
        }
        FontVariationAxis[] axes = font.getAxes();
        if (axes != null && axes.length != 0) {
            sb.append(", axes = \"");
            sb.append(FontVariationAxis.toFontVariationSettings(axes));
            sb.append("\"");
        }
        writer.println(sb.toString());
    }

    private void writeCommandResult(ShellCommand shell, FontManagerService.SystemFontException e) {
        PrintWriter pw = shell.getErrPrintWriter();
        pw.println(e.getErrorCode());
        pw.println(e.getMessage());
        Slog.e(TAG, "Command failed: " + Arrays.toString(shell.getAllArgs()), e);
    }

    private int dump(ShellCommand shell) {
        Context ctx = this.mService.getContext();
        if (!DumpUtils.checkDumpPermission(ctx, TAG, shell.getErrPrintWriter())) {
            return 1;
        }
        IndentingPrintWriter writer = new IndentingPrintWriter(shell.getOutPrintWriter(), "  ");
        String nextArg = shell.getNextArg();
        FontConfig fontConfig = this.mService.getSystemFontConfig();
        if (nextArg == null) {
            dumpFontConfig(writer, fontConfig);
            return 0;
        }
        Map<String, FontFamily[]> fallbackMap = SystemFonts.buildSystemFallback(fontConfig);
        FontFamily[] families = fallbackMap.get(nextArg);
        if (families == null) {
            writer.println("Font Family \"" + nextArg + "\" not found");
            return 0;
        }
        dumpFallback(writer, families);
        return 0;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    private int update(ShellCommand shell) throws FontManagerService.SystemFontException {
        ParcelFileDescriptor fontFd;
        ParcelFileDescriptor sigFd;
        String fontPath = shell.getNextArg();
        if (fontPath == null) {
            throw new FontManagerService.SystemFontException(-10003, "Font file path argument is required.");
        }
        String signaturePath = shell.getNextArg();
        if (signaturePath != null) {
            try {
                fontFd = shell.openFileForSystem(fontPath, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
                sigFd = shell.openFileForSystem(signaturePath, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
                try {
                } catch (Throwable th) {
                    if (sigFd != null) {
                        try {
                            sigFd.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (IOException e) {
                Slog.w(TAG, "Error while closing files", e);
            }
            if (fontFd == null) {
                throw new FontManagerService.SystemFontException(-10001, "Failed to open font file");
            }
            if (sigFd == null) {
                throw new FontManagerService.SystemFontException(-10002, "Failed to open signature file");
            }
            try {
                FileInputStream sigFis = new FileInputStream(sigFd.getFileDescriptor());
                try {
                    int len = sigFis.available();
                    if (len > 8192) {
                        throw new FontManagerService.SystemFontException(-10005, "Signature file is too large");
                    }
                    byte[] signature = new byte[len];
                    if (sigFis.read(signature, 0, len) != len) {
                        throw new FontManagerService.SystemFontException(-10004, "Invalid read length");
                    }
                    sigFis.close();
                    this.mService.update(-1, Collections.singletonList(new FontUpdateRequest(fontFd, signature)));
                    if (sigFd != null) {
                        sigFd.close();
                    }
                    if (fontFd != null) {
                        fontFd.close();
                    }
                    shell.getOutPrintWriter().println("Success");
                    return 0;
                } catch (Throwable th3) {
                    try {
                        sigFis.close();
                    } catch (Throwable th4) {
                        th3.addSuppressed(th4);
                    }
                    throw th3;
                }
            } catch (IOException e2) {
                throw new FontManagerService.SystemFontException(-10004, "Failed to read signature file.", e2);
            }
        }
        throw new FontManagerService.SystemFontException(-10003, "Signature file argument is required.");
    }

    private int updateFamily(ShellCommand shell) throws FontManagerService.SystemFontException {
        String xmlPath = shell.getNextArg();
        if (xmlPath == null) {
            throw new FontManagerService.SystemFontException(-10003, "XML file path argument is required.");
        }
        try {
            ParcelFileDescriptor xmlFd = shell.openFileForSystem(xmlPath, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
            List<FontUpdateRequest> requests = parseFontFamilyUpdateXml(new FileInputStream(xmlFd.getFileDescriptor()));
            if (xmlFd != null) {
                xmlFd.close();
            }
            this.mService.update(-1, requests);
            shell.getOutPrintWriter().println("Success");
            return 0;
        } catch (IOException e) {
            throw new FontManagerService.SystemFontException(-10006, "Failed to open XML file.", e);
        }
    }

    private static List<FontUpdateRequest> parseFontFamilyUpdateXml(InputStream inputStream) throws FontManagerService.SystemFontException {
        try {
            TypedXmlPullParser parser = Xml.resolvePullParser(inputStream);
            List<FontUpdateRequest> requests = new ArrayList<>();
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    if (type == 2) {
                        int depth = parser.getDepth();
                        String tag = parser.getName();
                        if (depth == 1) {
                            if (!"fontFamilyUpdateRequest".equals(tag)) {
                                throw new FontManagerService.SystemFontException(-10007, "Expected <fontFamilyUpdateRequest> but got: " + tag);
                            }
                        } else if (depth != 2) {
                            continue;
                        } else if ("family".equals(tag)) {
                            requests.add(new FontUpdateRequest(FontUpdateRequest.Family.readFromXml(parser)));
                        } else {
                            throw new FontManagerService.SystemFontException(-10007, "Expected <family> but got: " + tag);
                        }
                    }
                } else {
                    return requests;
                }
            }
        } catch (IOException | XmlPullParserException e) {
            throw new FontManagerService.SystemFontException(0, "Failed to parse xml", e);
        }
    }

    private int clear(ShellCommand shell) {
        this.mService.clearUpdates();
        shell.getOutPrintWriter().println("Success");
        return 0;
    }

    private int restart(ShellCommand shell) {
        this.mService.restart();
        shell.getOutPrintWriter().println("Success");
        return 0;
    }

    private int status(ShellCommand shell) {
        IndentingPrintWriter writer = new IndentingPrintWriter(shell.getOutPrintWriter(), "  ");
        FontConfig config = this.mService.getSystemFontConfig();
        writer.println("Current Version: " + config.getConfigVersion());
        LocalDateTime dt = LocalDateTime.ofEpochSecond(config.getLastModifiedTimeMillis(), 0, ZoneOffset.UTC);
        writer.println("Last Modified Date: " + dt.format(DateTimeFormatter.ISO_DATE_TIME));
        Map<String, File> fontFileMap = this.mService.getFontFileMap();
        writer.println("Number of updated font files: " + fontFileMap.size());
        return 0;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private int execCommand(ShellCommand shell, String cmd) {
        if (cmd == null) {
            return shell.handleDefaultCommands((String) null);
        }
        char c = 65535;
        try {
            switch (cmd.hashCode()) {
                case -892481550:
                    if (cmd.equals("status")) {
                        c = 5;
                        break;
                    }
                    break;
                case -838846263:
                    if (cmd.equals("update")) {
                        c = 1;
                        break;
                    }
                    break;
                case 3095028:
                    if (cmd.equals("dump")) {
                        c = 0;
                        break;
                    }
                    break;
                case 94746189:
                    if (cmd.equals("clear")) {
                        c = 3;
                        break;
                    }
                    break;
                case 1097506319:
                    if (cmd.equals(HostingRecord.HOSTING_TYPE_RESTART)) {
                        c = 4;
                        break;
                    }
                    break;
                case 1135462632:
                    if (cmd.equals("update-family")) {
                        c = 2;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    return dump(shell);
                case 1:
                    return update(shell);
                case 2:
                    return updateFamily(shell);
                case 3:
                    return clear(shell);
                case 4:
                    return restart(shell);
                case 5:
                    return status(shell);
                default:
                    return shell.handleDefaultCommands(cmd);
            }
        } catch (FontManagerService.SystemFontException e) {
            writeCommandResult(shell, e);
            return 1;
        }
    }
}
