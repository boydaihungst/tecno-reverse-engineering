package com.transsion.hubcore.server.wm.tranrefreshrate;

import android.text.TextUtils;
import android.util.Slog;
/* loaded from: classes2.dex */
public class FilePathCheckUtil {
    private static final int CHAR_0 = 48;
    private static final int CHAR_9 = 57;
    private static final int CHAR_BIG_A = 65;
    private static final int CHAR_BIG_Z = 90;
    private static final char CHAR_INVALID = 0;
    private static final int CHAR_SMALL_A = 97;
    private static final int CHAR_SMALL_Z = 122;
    private static final String TAG = "FilePathCheckUtil";

    public static String checkFilePath(String oriFilePath, boolean isLogOpen) {
        if (TextUtils.isEmpty(oriFilePath)) {
            return oriFilePath;
        }
        String destFilePath = "";
        int length = oriFilePath.length();
        for (int index = 0; index < length; index++) {
            char curChar = oriFilePath.charAt(index);
            char nextChar = CHAR_INVALID;
            if (index < length - 1) {
                nextChar = oriFilePath.charAt(index + 1);
            }
            char curDesChar = checkChar(curChar);
            if (curDesChar != 0 && (curDesChar != '.' || nextChar != '.')) {
                destFilePath = destFilePath + curDesChar;
            }
        }
        if (isLogOpen) {
            Slog.d(TAG, "oriFilePath: " + oriFilePath + ", destFilePath: " + destFilePath);
        }
        return destFilePath;
    }

    private static char checkChar(char oriChar) {
        if (oriChar >= '0' && oriChar <= '9') {
            return oriChar;
        }
        if (oriChar >= 'A' && oriChar <= 'Z') {
            return oriChar;
        }
        if (oriChar >= 'a' && oriChar <= 'z') {
            return oriChar;
        }
        switch (oriChar) {
            case '-':
            case '.':
            case '/':
            case ':':
            case '\\':
            case '_':
                return oriChar;
            default:
                return CHAR_INVALID;
        }
    }
}
