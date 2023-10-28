package com.android.server.statusbar;

import android.app.ITransientNotificationCallback;
import android.hardware.fingerprint.IUdfpsHbmListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.view.InsetsVisibilities;
import com.android.internal.view.AppearanceRegion;
import com.android.server.notification.NotificationDelegate;
import java.util.Map;
/* loaded from: classes2.dex */
public interface StatusBarManagerInternal {
    public static final boolean DEBUG_NAV = false;

    void abortTransient(int i, int[] iArr);

    void appTransitionCancelled(int i);

    void appTransitionFinished(int i);

    void appTransitionPending(int i);

    void appTransitionStarting(int i, long j, long j2);

    void cancelPreloadRecentApps();

    void collapsePanels();

    void dismissKeyboardShortcutsMenu();

    void handleWindowManagerLoggingCommand(String[] strArr, ParcelFileDescriptor parcelFileDescriptor);

    void hideRecentApps(boolean z, boolean z2);

    void hideToast(String str, IBinder iBinder);

    void onCameraLaunchGestureDetected(int i);

    void onDisplayReady(int i);

    void onEmergencyActionLaunchGestureDetected();

    void onProposedRotationChanged(int i, boolean z);

    void onRecentsAnimationStateChanged(boolean z);

    void onSystemBarAttributesChanged(int i, int i2, AppearanceRegion[] appearanceRegionArr, boolean z, int i3, InsetsVisibilities insetsVisibilities, String str);

    void preloadRecentApps();

    boolean requestWindowMagnificationConnection(boolean z);

    void setCurrentUser(int i);

    void setDisableFlags(int i, int i2, String str);

    void setNavigationBarLumaSamplingEnabled(int i, boolean z);

    void setNotificationDelegate(NotificationDelegate notificationDelegate);

    void setNotificationRanking(Map map);

    void setTopAppHidesStatusBar(boolean z);

    void setUdfpsHbmListener(IUdfpsHbmListener iUdfpsHbmListener);

    void setWindowState(int i, int i2, int i3);

    void showAssistDisclosure();

    void showChargingAnimation(int i);

    void showPictureInPictureMenu();

    void showRecentApps(boolean z);

    void showScreenPinningRequest(int i);

    boolean showShutdownUi(boolean z, String str);

    void showToast(int i, String str, IBinder iBinder, CharSequence charSequence, IBinder iBinder2, int i2, ITransientNotificationCallback iTransientNotificationCallback, int i3);

    void showTransient(int i, int[] iArr, boolean z);

    void startAssist(Bundle bundle);

    void toggleKeyboardShortcutsMenu(int i);

    void toggleRecentApps();

    void toggleSplitScreen();
}
