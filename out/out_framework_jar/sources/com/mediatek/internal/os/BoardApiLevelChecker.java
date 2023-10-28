package com.mediatek.internal.os;

import android.os.SystemProperties;
/* loaded from: classes4.dex */
public class BoardApiLevelChecker {
    private static final int BOARD_API_LEVEL = getBoardApiLevel();
    private static final String PROP_API_LEVEL = "ro.board.api_level";
    private static final String PROP_FIRST_API_LEVEL = "ro.board.first_api_level";

    private static int getBoardApiLevel() {
        int firstApiLevel = SystemProperties.getInt(PROP_FIRST_API_LEVEL, 0);
        int apiLevel = SystemProperties.getInt(PROP_API_LEVEL, firstApiLevel);
        return apiLevel;
    }

    public static boolean check(int requiredBoardApiLevel) {
        int i = BOARD_API_LEVEL;
        return i == 0 || i >= requiredBoardApiLevel;
    }
}
