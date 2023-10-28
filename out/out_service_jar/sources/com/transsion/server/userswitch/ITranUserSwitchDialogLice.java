package com.transsion.server.userswitch;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.UserInfo;
import com.android.server.am.ActivityManagerService;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_FRAMEWORK)
/* loaded from: classes2.dex */
public interface ITranUserSwitchDialogLice {
    public static final LiceInfo<ITranUserSwitchDialogLice> sLiceInfo = new LiceInfo<>("com.transsion.server.userswitch.v1.TranUserSwitchDialogLice", ITranUserSwitchDialogLice.class, new Supplier() { // from class: com.transsion.server.userswitch.ITranUserSwitchDialogLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranUserSwitchDialogLice.lambda$static$0();
        }
    });

    static /* synthetic */ ITranUserSwitchDialogLice lambda$static$0() {
        return new ITranUserSwitchDialogLice() { // from class: com.transsion.server.userswitch.ITranUserSwitchDialogLice.1
        };
    }

    static ITranUserSwitchDialogLice Instance() {
        return (ITranUserSwitchDialogLice) sLiceInfo.getImpl();
    }

    default Dialog instanceUserSwitchDialog(ActivityManagerService service, Context context, UserInfo fromUser, UserInfo toUser, boolean aboveSystem, String switchingFromSystemUserMessage, String switchingToSystemUserMessage) {
        return null;
    }
}
