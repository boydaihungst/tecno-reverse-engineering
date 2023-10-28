package com.android.server.app;

import com.android.server.app.GameServiceConfiguration;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public interface GameServiceProviderInstanceFactory {
    GameServiceProviderInstance create(GameServiceConfiguration.GameServiceComponentConfiguration gameServiceComponentConfiguration);
}
