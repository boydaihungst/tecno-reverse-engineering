package com.transsion.hubcore.server.pm;

import android.content.Context;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.parsing.PackageParser2;
import com.transsion.hubcore.server.pm.ITranPkmsExt;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranPkmsExt {
    public static final boolean DEBUG = true;
    public static final int DELETE_BY_OOBE = 16;
    public static final int INDEX_TR_CARRIER_OP_APP = 5;
    public static final int INDEX_TR_COMPANY_OP_APP = 3;
    public static final int INDEX_TR_MI_OP_APP = 1;
    public static final int INDEX_TR_PRELOAD_OP_APP = 2;
    public static final int INDEX_TR_PRODUCT_OP_APP = 0;
    public static final int INDEX_TR_REGION_OP_APP = 4;
    public static final int INDEX_TR_THEME_OP_APP = 6;
    public static final TranClassInfo<ITranPkmsExt> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.pm.TranPkmsExtImpl", ITranPkmsExt.class, new Supplier() { // from class: com.transsion.hubcore.server.pm.ITranPkmsExt$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranPkmsExt.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranPkmsExt {
    }

    static ITranPkmsExt Instance() {
        return (ITranPkmsExt) classInfo.getImpl();
    }

    default void init(PackageManagerService pms, UserManagerService ums) {
    }

    default void init(PackageManagerService pms, UserManagerService ums, Context context) {
    }

    default boolean needSkipRemovedPackage(String packageName, String codePath) {
        return false;
    }

    default boolean isPreloadApp(String packageName, String codePath) {
        return false;
    }

    default void storeRemovedPackage(String packageName, int deleteFlags) {
    }

    default ArrayList<String> getOobeRemovedPackages() {
        return null;
    }

    default boolean isPreloadPath(String codepath) {
        return false;
    }

    default void initReadedRemovePackageList() {
    }

    default boolean shouldAddReadedRemovePackages(String packageName) {
        return false;
    }

    default void storeReadedRemovePackages() {
    }

    default void applyFactoryDefaultGallerLPw(int userId) {
    }

    default boolean setDefaultGallerPackageNameInternal(String gallerPkg, int userId) {
        return false;
    }

    default String getDefaultGallerPackageNameInternal(int userId) {
        return "com.google.android.apps.photos";
    }

    default boolean packageIsGaller(String packageName, int userId) {
        return false;
    }

    default void scanTrDirLi(int defParseFlags, int defScanFlags, PackageParser2 packageParser, ExecutorService executorService) {
    }

    default void applyFactoryDefaultMusicLPw(int userId) {
    }

    default boolean setDefaultMusicPackageNameInternal(String musicPkg, int userId) {
        return false;
    }

    default String getDefaultMusicPackageNameInternal(int userId) {
        return "com.google.android.apps.youtube.music";
    }

    default boolean packageIsMusic(String packageName, int userId) {
        return false;
    }

    default void restoreOOBEFile() {
    }
}
