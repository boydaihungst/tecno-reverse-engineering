package com.mediatek.anr;
/* loaded from: classes.dex */
public class AnrAppFactoryImpl extends AnrAppFactory {
    public AnrAppManager makeAnrAppManager() {
        return new AnrAppManagerImpl();
    }
}
