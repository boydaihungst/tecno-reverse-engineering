package com.mediatek.view.impl;

import com.mediatek.view.SurfaceExt;
import com.mediatek.view.SurfaceFactory;
/* loaded from: classes.dex */
public class SurfaceFactoryImpl extends SurfaceFactory {
    private static SurfaceExt mSurfaceExt = null;

    public SurfaceExt getSurfaceExt() {
        if (mSurfaceExt == null) {
            mSurfaceExt = new SurfaceExtimpl();
        }
        return mSurfaceExt;
    }
}
