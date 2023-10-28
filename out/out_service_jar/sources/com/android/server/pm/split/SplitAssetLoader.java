package com.android.server.pm.split;

import android.content.res.ApkAssets;
import android.content.res.AssetManager;
/* loaded from: classes2.dex */
public interface SplitAssetLoader extends AutoCloseable {
    ApkAssets getBaseApkAssets();

    AssetManager getBaseAssetManager() throws IllegalArgumentException;

    AssetManager getSplitAssetManager(int i) throws IllegalArgumentException;
}
