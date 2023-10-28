package com.android.server.app;

import com.android.server.SystemService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public interface GameServiceProviderSelector {
    GameServiceConfiguration get(SystemService.TargetUser targetUser, String str);
}
