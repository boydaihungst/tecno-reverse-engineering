package com.transsion.hubcore.server.inputmethod;

import android.content.Intent;
import android.content.pm.IPackageManager;
import android.view.inputmethod.InputMethodInfo;
import com.transsion.hubcore.server.inputmethod.ITranInputMethodManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranInputMethodManagerService {
    public static final TranClassInfo<ITranInputMethodManagerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.inputmethod.TranInputMethodManagerServiceImpl", ITranInputMethodManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.inputmethod.ITranInputMethodManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranInputMethodManagerService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranInputMethodManagerService {
    }

    static ITranInputMethodManagerService Instance() {
        return (ITranInputMethodManagerService) classInfo.getImpl();
    }

    default void hookInputMethodShown(boolean inputShown) {
    }

    default void hookSetInputMethodLocked(InputMethodInfo info) {
    }

    default void hookChangeIme(String action, String id) {
    }

    default void hookStartInputUncheckedLocked(Intent intent) {
    }

    default void hookstartInputOrWindowGainedFocus(String focusPkgName) {
    }

    default String getCurrentFocusedPkgName() {
        return null;
    }

    default boolean hookStartInputVisible(IPackageManager mIPackageManager, int uid) {
        return false;
    }

    default void setConnectInputShow(boolean connectInputShow) {
    }

    default boolean getConnectInputShow() {
        return false;
    }

    default void setTouchFromScreen(boolean touchFromScreen) {
    }

    default boolean getTouchFromScreen() {
        return true;
    }

    default void setConnectSessionId(int sessionId) {
    }

    default int getConnectSessionId() {
        return 0;
    }
}
