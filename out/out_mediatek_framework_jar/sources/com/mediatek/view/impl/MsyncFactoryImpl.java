package com.mediatek.view.impl;

import com.mediatek.view.MsyncExt;
import com.mediatek.view.MsyncFactory;
/* loaded from: classes.dex */
public class MsyncFactoryImpl extends MsyncFactory {
    private static MsyncExt mMsyncExt = null;

    public MsyncExt getMsyncExt() {
        if (mMsyncExt == null) {
            mMsyncExt = new MsyncExtimpl();
        }
        return mMsyncExt;
    }
}
