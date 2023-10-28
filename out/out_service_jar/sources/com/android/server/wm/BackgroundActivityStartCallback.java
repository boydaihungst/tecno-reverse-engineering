package com.android.server.wm;

import android.os.IBinder;
import java.util.Collection;
/* loaded from: classes2.dex */
public interface BackgroundActivityStartCallback {
    boolean canCloseSystemDialogs(Collection<IBinder> collection, int i);

    boolean isActivityStartAllowed(Collection<IBinder> collection, int i, String str);
}
