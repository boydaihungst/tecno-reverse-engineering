package com.transsion.hubcore.utils;

import android.content.res.Resources;
import android.media.TtmlUtils;
/* loaded from: classes4.dex */
public class TranResourceUtils {
    private static final String TYPE_ANDROID = "android";
    private static final String TYPE_MTK = "com.mediatek";
    private static final String TYPE_OS = "com.transsion";
    private static final String TYPE_THUB = "com.transsion.thub.res";
    private static final String TYPE_UNISOC = "com.unisoc";

    /* loaded from: classes4.dex */
    public static class Thub {
        public static int getLayoutId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, TtmlUtils.TAG_LAYOUT, TranResourceUtils.TYPE_THUB);
        }

        public static int getStringId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "string", TranResourceUtils.TYPE_THUB);
        }

        public static int getDrawableId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "drawable", TranResourceUtils.TYPE_THUB);
        }

        public static int getMipmapId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "mipmap", TranResourceUtils.TYPE_THUB);
        }

        public static int getColorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "color", TranResourceUtils.TYPE_THUB);
        }

        public static int getDimenId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "dimen", TranResourceUtils.TYPE_THUB);
        }

        public static int getAttrId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "attr", TranResourceUtils.TYPE_THUB);
        }

        public static int getStyleId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "style", TranResourceUtils.TYPE_THUB);
        }

        public static int getAnimId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "anim", TranResourceUtils.TYPE_THUB);
        }

        public static int getAnimatorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "animator", TranResourceUtils.TYPE_THUB);
        }

        public static int getArrayId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "array", TranResourceUtils.TYPE_THUB);
        }

        public static int getIntegerId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "integer", TranResourceUtils.TYPE_THUB);
        }

        public static int getBoolId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "bool", TranResourceUtils.TYPE_THUB);
        }

        public static int getXmlId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "xml", TranResourceUtils.TYPE_THUB);
        }

        public static int getInterpolatorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "interpolator", TranResourceUtils.TYPE_THUB);
        }

        public static int getPluralsId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "plurals", TranResourceUtils.TYPE_THUB);
        }

        public static int getStyleableId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "styleable", TranResourceUtils.TYPE_THUB);
        }

        public static int getIdId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "id", TranResourceUtils.TYPE_THUB);
        }

        public static int getOthersId(String resourceName, String resourceType) {
            return TranResourceUtils.getIdentifierByType(resourceName, resourceType, TranResourceUtils.TYPE_THUB);
        }
    }

    /* loaded from: classes4.dex */
    public static class Android {
        public static int getLayoutId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, TtmlUtils.TAG_LAYOUT, "android");
        }

        public static int getStringId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "string", "android");
        }

        public static int getDrawableId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "drawable", "android");
        }

        public static int getMipmapId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "mipmap", "android");
        }

        public static int getColorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "color", "android");
        }

        public static int getDimenId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "dimen", "android");
        }

        public static int getAttrId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "attr", "android");
        }

        public static int getStyleId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "style", "android");
        }

        public static int getAnimId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "anim", "android");
        }

        public static int getAnimatorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "animator", "android");
        }

        public static int getArrayId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "array", "android");
        }

        public static int getIntegerId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "integer", "android");
        }

        public static int getBoolId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "bool", "android");
        }

        public static int getXmlId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "xml", "android");
        }

        public static int getInterpolatorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "interpolator", "android");
        }

        public static int getPluralsId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "plurals", "android");
        }

        public static int getStyleableId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "styleable", "android");
        }

        public static int getIdId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "id", "android");
        }

        public static int getOthersId(String resourceName, String resourceType) {
            return TranResourceUtils.getIdentifierByType(resourceName, resourceType, "android");
        }
    }

    /* loaded from: classes4.dex */
    public static class Os {
        public static int getLayoutId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, TtmlUtils.TAG_LAYOUT, TranResourceUtils.TYPE_OS);
        }

        public static int getStringId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "string", TranResourceUtils.TYPE_OS);
        }

        public static int getDrawableId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "drawable", TranResourceUtils.TYPE_OS);
        }

        public static int getMipmapId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "mipmap", TranResourceUtils.TYPE_OS);
        }

        public static int getColorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "color", TranResourceUtils.TYPE_OS);
        }

        public static int getDimenId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "dimen", TranResourceUtils.TYPE_OS);
        }

        public static int getAttrId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "attr", TranResourceUtils.TYPE_OS);
        }

        public static int getStyleId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "style", TranResourceUtils.TYPE_OS);
        }

        public static int getAnimId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "anim", TranResourceUtils.TYPE_OS);
        }

        public static int getAnimatorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "animator", TranResourceUtils.TYPE_OS);
        }

        public static int getArrayId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "array", TranResourceUtils.TYPE_OS);
        }

        public static int getIntegerId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "integer", TranResourceUtils.TYPE_OS);
        }

        public static int getBoolId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "bool", TranResourceUtils.TYPE_OS);
        }

        public static int getXmlId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "xml", TranResourceUtils.TYPE_OS);
        }

        public static int getInterpolatorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "interpolator", TranResourceUtils.TYPE_OS);
        }

        public static int getPluralsId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "plurals", TranResourceUtils.TYPE_OS);
        }

        public static int getStyleableId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "styleable", TranResourceUtils.TYPE_OS);
        }

        public static int getIdId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "id", TranResourceUtils.TYPE_OS);
        }

        public static int getOthersId(String resourceName, String resourceType) {
            return TranResourceUtils.getIdentifierByType(resourceName, resourceType, TranResourceUtils.TYPE_OS);
        }
    }

    /* loaded from: classes4.dex */
    public static class Mtk {
        public static int getLayoutId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, TtmlUtils.TAG_LAYOUT, TranResourceUtils.TYPE_MTK);
        }

        public static int getStringId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "string", TranResourceUtils.TYPE_MTK);
        }

        public static int getDrawableId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "drawable", TranResourceUtils.TYPE_MTK);
        }

        public static int getMipmapId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "mipmap", TranResourceUtils.TYPE_MTK);
        }

        public static int getColorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "color", TranResourceUtils.TYPE_MTK);
        }

        public static int getDimenId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "dimen", TranResourceUtils.TYPE_MTK);
        }

        public static int getAttrId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "attr", TranResourceUtils.TYPE_MTK);
        }

        public static int getStyleId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "style", TranResourceUtils.TYPE_MTK);
        }

        public static int getAnimId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "anim", TranResourceUtils.TYPE_MTK);
        }

        public static int getAnimatorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "animator", TranResourceUtils.TYPE_MTK);
        }

        public static int getArrayId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "array", TranResourceUtils.TYPE_MTK);
        }

        public static int getIntegerId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "integer", TranResourceUtils.TYPE_MTK);
        }

        public static int getBoolId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "bool", TranResourceUtils.TYPE_MTK);
        }

        public static int getXmlId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "xml", TranResourceUtils.TYPE_MTK);
        }

        public static int getInterpolatorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "interpolator", TranResourceUtils.TYPE_MTK);
        }

        public static int getPluralsId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "plurals", TranResourceUtils.TYPE_MTK);
        }

        public static int getStyleableId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "styleable", TranResourceUtils.TYPE_MTK);
        }

        public static int getIdId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "id", TranResourceUtils.TYPE_MTK);
        }

        public static int getOthersId(String resourceName, String resourceType) {
            return TranResourceUtils.getIdentifierByType(resourceName, resourceType, TranResourceUtils.TYPE_MTK);
        }
    }

    /* loaded from: classes4.dex */
    public static class Unisoc {
        public static int getLayoutId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, TtmlUtils.TAG_LAYOUT, TranResourceUtils.TYPE_UNISOC);
        }

        public static int getStringId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "string", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getDrawableId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "drawable", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getMipmapId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "mipmap", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getColorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "color", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getDimenId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "dimen", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getAttrId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "attr", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getStyleId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "style", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getAnimId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "anim", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getAnimatorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "animator", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getArrayId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "array", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getIntegerId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "integer", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getBoolId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "bool", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getXmlId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "xml", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getInterpolatorId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "interpolator", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getPluralsId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "plurals", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getStyleableId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "styleable", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getIdId(String resourceName) {
            return TranResourceUtils.getIdentifierByType(resourceName, "id", TranResourceUtils.TYPE_UNISOC);
        }

        public static int getOthersId(String resourceName, String resourceType) {
            return TranResourceUtils.getIdentifierByType(resourceName, resourceType, TranResourceUtils.TYPE_UNISOC);
        }
    }

    /* loaded from: classes4.dex */
    public static class Other {
        public static int getOthersId(String resourceName, String resourceType, String resourcePkgName) {
            return TranResourceUtils.getIdentifierByType(resourceName, resourceType, resourcePkgName);
        }
    }

    public static int getIdentifierByType(String resourceName, String defType, String resourcePkg) {
        return Resources.getSystem().getIdentifier(resourceName, defType, resourcePkg);
    }
}
