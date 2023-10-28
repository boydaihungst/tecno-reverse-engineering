package android.window;

import android.os.Bundle;
/* loaded from: classes4.dex */
public interface WindowProvider {
    public static final String KEY_IS_WINDOW_PROVIDER_SERVICE = "android.windowContext.isWindowProviderService";

    Bundle getWindowContextOptions();

    int getWindowType();
}
