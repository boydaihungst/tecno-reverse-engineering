package com.android.server.policy;

import android.content.Context;
import android.os.UserManager;
import com.android.internal.globalactions.LongPressAction;
import com.android.internal.globalactions.SinglePressAction;
import com.android.server.policy.WindowManagerPolicy;
/* loaded from: classes2.dex */
public final class PowerAction extends SinglePressAction implements LongPressAction {
    private final Context mContext;
    private final WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;

    public PowerAction(Context context, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        super(17301552, 17040407);
        this.mContext = context;
        this.mWindowManagerFuncs = windowManagerFuncs;
    }

    public boolean onLongPress() {
        UserManager um = (UserManager) this.mContext.getSystemService(UserManager.class);
        if (!um.hasUserRestriction("no_safe_boot")) {
            this.mWindowManagerFuncs.rebootSafeMode(true);
            return true;
        }
        return false;
    }

    public boolean showDuringKeyguard() {
        return true;
    }

    public boolean showBeforeProvisioning() {
        return true;
    }

    public void onPress() {
        this.mWindowManagerFuncs.shutdown(false);
    }
}
