package com.android.server.inputmethod;

import android.os.Environment;
import android.os.FileUtils;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
final class AdditionalSubtypeUtils {
    private static final String ADDITIONAL_SUBTYPES_FILE_NAME = "subtypes.xml";
    private static final String ATTR_ICON = "icon";
    private static final String ATTR_ID = "id";
    private static final String ATTR_IME_SUBTYPE_EXTRA_VALUE = "imeSubtypeExtraValue";
    private static final String ATTR_IME_SUBTYPE_ID = "subtypeId";
    private static final String ATTR_IME_SUBTYPE_LANGUAGE_TAG = "languageTag";
    private static final String ATTR_IME_SUBTYPE_LOCALE = "imeSubtypeLocale";
    private static final String ATTR_IME_SUBTYPE_MODE = "imeSubtypeMode";
    private static final String ATTR_IS_ASCII_CAPABLE = "isAsciiCapable";
    private static final String ATTR_IS_AUXILIARY = "isAuxiliary";
    private static final String ATTR_LABEL = "label";
    private static final String INPUT_METHOD_PATH = "inputmethod";
    private static final String NODE_IMI = "imi";
    private static final String NODE_SUBTYPE = "subtype";
    private static final String NODE_SUBTYPES = "subtypes";
    private static final String SYSTEM_PATH = "system";
    private static final String TAG = "AdditionalSubtypeUtils";

    private AdditionalSubtypeUtils() {
    }

    private static File getInputMethodDir(int userId) {
        File systemDir;
        if (userId == 0) {
            systemDir = new File(Environment.getDataDirectory(), "system");
        } else {
            systemDir = Environment.getUserSystemDirectory(userId);
        }
        return new File(systemDir, INPUT_METHOD_PATH);
    }

    private static AtomicFile getAdditionalSubtypeFile(File inputMethodDir) {
        File subtypeFile = new File(inputMethodDir, ADDITIONAL_SUBTYPES_FILE_NAME);
        return new AtomicFile(subtypeFile, "input-subtypes");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:67:0x01a9  */
    /* JADX WARN: Removed duplicated region for block: B:87:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static void save(ArrayMap<String, List<InputMethodSubtype>> allSubtypes, ArrayMap<String, InputMethodInfo> methodMap, int userId) {
        File inputMethodDir;
        ArrayMap<String, InputMethodInfo> arrayMap = methodMap;
        File inputMethodDir2 = getInputMethodDir(userId);
        if (allSubtypes.isEmpty()) {
            if (!inputMethodDir2.exists()) {
                return;
            }
            AtomicFile subtypesFile = getAdditionalSubtypeFile(inputMethodDir2);
            if (subtypesFile.exists()) {
                subtypesFile.delete();
            }
            if (FileUtils.listFilesOrEmpty(inputMethodDir2).length == 0 && !inputMethodDir2.delete()) {
                Slog.e(TAG, "Failed to delete the empty parent directory " + inputMethodDir2);
            }
        } else if (!inputMethodDir2.exists() && !inputMethodDir2.mkdirs()) {
            Slog.e(TAG, "Failed to create a parent directory " + inputMethodDir2);
        } else {
            boolean isSetMethodMap = arrayMap != null && methodMap.size() > 0;
            FileOutputStream fos = null;
            AtomicFile subtypesFile2 = getAdditionalSubtypeFile(inputMethodDir2);
            try {
                fos = subtypesFile2.startWrite();
                TypedXmlSerializer out = Xml.resolveSerializer(fos);
                String str = null;
                out.startDocument((String) null, true);
                out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                out.startTag((String) null, NODE_SUBTYPES);
                for (String imiId : allSubtypes.keySet()) {
                    if (isSetMethodMap) {
                        try {
                            if (!arrayMap.containsKey(imiId)) {
                                Slog.w(TAG, "IME uninstalled or not valid.: " + imiId);
                            }
                        } catch (IOException e) {
                            e = e;
                            Slog.w(TAG, "Error writing subtypes", e);
                            if (fos == null) {
                                subtypesFile2.failWrite(fos);
                                return;
                            }
                            return;
                        }
                    }
                    out.startTag(str, NODE_IMI);
                    out.attribute(str, ATTR_ID, imiId);
                    List<InputMethodSubtype> subtypesList = allSubtypes.get(imiId);
                    int numSubtypes = subtypesList.size();
                    int i = 0;
                    while (true) {
                        int numSubtypes2 = numSubtypes;
                        if (i >= numSubtypes2) {
                            break;
                        }
                        InputMethodSubtype subtype = subtypesList.get(i);
                        out.startTag((String) null, NODE_SUBTYPE);
                        if (!subtype.hasSubtypeId()) {
                            inputMethodDir = inputMethodDir2;
                        } else {
                            inputMethodDir = inputMethodDir2;
                            try {
                                out.attributeInt((String) null, ATTR_IME_SUBTYPE_ID, subtype.getSubtypeId());
                            } catch (IOException e2) {
                                e = e2;
                                Slog.w(TAG, "Error writing subtypes", e);
                                if (fos == null) {
                                }
                            }
                        }
                        out.attributeInt((String) null, ATTR_ICON, subtype.getIconResId());
                        out.attributeInt((String) null, ATTR_LABEL, subtype.getNameResId());
                        out.attribute((String) null, ATTR_IME_SUBTYPE_LOCALE, subtype.getLocale());
                        out.attribute((String) null, ATTR_IME_SUBTYPE_LANGUAGE_TAG, subtype.getLanguageTag());
                        out.attribute((String) null, ATTR_IME_SUBTYPE_MODE, subtype.getMode());
                        out.attribute((String) null, ATTR_IME_SUBTYPE_EXTRA_VALUE, subtype.getExtraValue());
                        out.attributeInt((String) null, ATTR_IS_AUXILIARY, subtype.isAuxiliary() ? 1 : 0);
                        out.attributeInt((String) null, ATTR_IS_ASCII_CAPABLE, subtype.isAsciiCapable() ? 1 : 0);
                        out.endTag((String) null, NODE_SUBTYPE);
                        i++;
                        numSubtypes = numSubtypes2;
                        inputMethodDir2 = inputMethodDir;
                    }
                    out.endTag((String) null, NODE_IMI);
                    arrayMap = methodMap;
                    inputMethodDir2 = inputMethodDir2;
                    str = null;
                }
                out.endTag((String) null, NODE_SUBTYPES);
                out.endDocument();
                subtypesFile2.finishWrite(fos);
            } catch (IOException e3) {
                e = e3;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [203=5] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:70:0x01dd A[Catch: all -> 0x01e1, TRY_ENTER, TRY_LEAVE, TryCatch #6 {IOException | NumberFormatException | XmlPullParserException -> 0x01e7, blocks: (B:59:0x01c0, B:75:0x01e6, B:70:0x01dd), top: B:91:0x0016 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static void load(ArrayMap<String, List<InputMethodSubtype>> allSubtypes, int userId) {
        Throwable th;
        int type;
        int i;
        int i2;
        String str;
        AtomicFile subtypesFile;
        int type2;
        String firstNodeName;
        int depth;
        String str2;
        AtomicFile subtypesFile2;
        int type3;
        String firstNodeName2;
        int depth2;
        String str3 = "1";
        allSubtypes.clear();
        AtomicFile subtypesFile3 = getAdditionalSubtypeFile(getInputMethodDir(userId));
        if (!subtypesFile3.exists()) {
            return;
        }
        try {
            try {
                FileInputStream fis = subtypesFile3.openRead();
                try {
                    TypedXmlPullParser parser = Xml.resolvePullParser(fis);
                    parser.getEventType();
                    do {
                        type = parser.next();
                        i = 1;
                        i2 = 2;
                        if (type == 2) {
                            break;
                        }
                    } while (type != 1);
                    String firstNodeName3 = parser.getName();
                    try {
                        if (!NODE_SUBTYPES.equals(firstNodeName3)) {
                            throw new XmlPullParserException("Xml doesn't start with subtypes");
                        }
                        int depth3 = parser.getDepth();
                        String currentImiId = null;
                        ArrayList<InputMethodSubtype> tempSubtypesArray = null;
                        while (true) {
                            int type4 = parser.next();
                            if (type4 == 3) {
                                try {
                                    if (parser.getDepth() <= depth3) {
                                        break;
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    if (fis != null) {
                                    }
                                    throw th;
                                }
                            }
                            if (type4 == i) {
                                break;
                            }
                            if (type4 != i2) {
                                str = str3;
                                subtypesFile = subtypesFile3;
                                type2 = type4;
                                firstNodeName = firstNodeName3;
                                depth = depth3;
                            } else {
                                String nodeName = parser.getName();
                                if (NODE_IMI.equals(nodeName)) {
                                    currentImiId = parser.getAttributeValue((String) null, ATTR_ID);
                                    if (TextUtils.isEmpty(currentImiId)) {
                                        Slog.w(TAG, "Invalid imi id found in subtypes.xml");
                                    } else {
                                        tempSubtypesArray = new ArrayList<>();
                                        try {
                                            allSubtypes.put(currentImiId, tempSubtypesArray);
                                            str2 = str3;
                                            subtypesFile2 = subtypesFile3;
                                            type3 = type4;
                                            firstNodeName2 = firstNodeName3;
                                            depth2 = depth3;
                                        } catch (Throwable th3) {
                                            th = th3;
                                            if (fis != null) {
                                            }
                                            throw th;
                                        }
                                    }
                                } else {
                                    try {
                                        if (NODE_SUBTYPE.equals(nodeName)) {
                                            if (TextUtils.isEmpty(currentImiId)) {
                                                str = str3;
                                                subtypesFile = subtypesFile3;
                                                type2 = type4;
                                                firstNodeName = firstNodeName3;
                                                depth = depth3;
                                            } else if (tempSubtypesArray == null) {
                                                str = str3;
                                                subtypesFile = subtypesFile3;
                                                type2 = type4;
                                                firstNodeName = firstNodeName3;
                                                depth = depth3;
                                            } else {
                                                int icon = parser.getAttributeInt((String) null, ATTR_ICON);
                                                int label = parser.getAttributeInt((String) null, ATTR_LABEL);
                                                String imeSubtypeLocale = parser.getAttributeValue((String) null, ATTR_IME_SUBTYPE_LOCALE);
                                                subtypesFile2 = subtypesFile3;
                                                String languageTag = parser.getAttributeValue((String) null, ATTR_IME_SUBTYPE_LANGUAGE_TAG);
                                                type3 = type4;
                                                String imeSubtypeMode = parser.getAttributeValue((String) null, ATTR_IME_SUBTYPE_MODE);
                                                firstNodeName2 = firstNodeName3;
                                                String imeSubtypeExtraValue = parser.getAttributeValue((String) null, ATTR_IME_SUBTYPE_EXTRA_VALUE);
                                                depth2 = depth3;
                                                boolean isAuxiliary = str3.equals(String.valueOf(parser.getAttributeValue((String) null, ATTR_IS_AUXILIARY)));
                                                boolean isAsciiCapable = str3.equals(String.valueOf(parser.getAttributeValue((String) null, ATTR_IS_ASCII_CAPABLE)));
                                                InputMethodSubtype.InputMethodSubtypeBuilder builder = new InputMethodSubtype.InputMethodSubtypeBuilder().setSubtypeNameResId(label).setSubtypeIconResId(icon).setSubtypeLocale(imeSubtypeLocale).setLanguageTag(languageTag).setSubtypeMode(imeSubtypeMode).setSubtypeExtraValue(imeSubtypeExtraValue).setIsAuxiliary(isAuxiliary).setIsAsciiCapable(isAsciiCapable);
                                                str2 = str3;
                                                int subtypeId = parser.getAttributeInt((String) null, ATTR_IME_SUBTYPE_ID, 0);
                                                if (subtypeId != 0) {
                                                    builder.setSubtypeId(subtypeId);
                                                }
                                                tempSubtypesArray.add(builder.build());
                                            }
                                            Slog.w(TAG, "IME uninstalled or not valid.: " + currentImiId);
                                        } else {
                                            str2 = str3;
                                            subtypesFile2 = subtypesFile3;
                                            type3 = type4;
                                            firstNodeName2 = firstNodeName3;
                                            depth2 = depth3;
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                        th = th;
                                        if (fis != null) {
                                            fis.close();
                                        }
                                        throw th;
                                    }
                                }
                                subtypesFile3 = subtypesFile2;
                                firstNodeName3 = firstNodeName2;
                                depth3 = depth2;
                                str3 = str2;
                                i = 1;
                                i2 = 2;
                            }
                            subtypesFile3 = subtypesFile;
                            firstNodeName3 = firstNodeName;
                            depth3 = depth;
                            str3 = str;
                            i = 1;
                            i2 = 2;
                        }
                        if (fis != null) {
                            fis.close();
                        }
                    } catch (Throwable th5) {
                        th = th5;
                    }
                } catch (Throwable th6) {
                    th = th6;
                }
            } catch (IOException | NumberFormatException | XmlPullParserException e) {
                e = e;
                Slog.w(TAG, "Error reading subtypes", e);
            }
        } catch (IOException | NumberFormatException | XmlPullParserException e2) {
            e = e2;
            Slog.w(TAG, "Error reading subtypes", e);
        }
    }
}
