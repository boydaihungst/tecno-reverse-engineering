package com.android.server.appwidget;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.os.Build;
import android.text.TextUtils;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import java.io.IOException;
import java.util.Objects;
/* loaded from: classes.dex */
public class AppWidgetXmlUtil {
    private static final String ATTR_AUTO_ADVANCED_VIEW_ID = "auto_advance_view_id";
    private static final String ATTR_CONFIGURE = "configure";
    private static final String ATTR_DESCRIPTION_RES = "description_res";
    private static final String ATTR_ICON = "icon";
    private static final String ATTR_INITIAL_KEYGUARD_LAYOUT = "initial_keyguard_layout";
    private static final String ATTR_INITIAL_LAYOUT = "initial_layout";
    private static final String ATTR_LABEL = "label";
    private static final String ATTR_MAX_RESIZE_HEIGHT = "max_resize_height";
    private static final String ATTR_MAX_RESIZE_WIDTH = "max_resize_width";
    private static final String ATTR_MIN_HEIGHT = "min_height";
    private static final String ATTR_MIN_RESIZE_HEIGHT = "min_resize_height";
    private static final String ATTR_MIN_RESIZE_WIDTH = "min_resize_width";
    private static final String ATTR_MIN_WIDTH = "min_width";
    private static final String ATTR_OS_FINGERPRINT = "os_fingerprint";
    private static final String ATTR_PREVIEW_IMAGE = "preview_image";
    private static final String ATTR_PREVIEW_LAYOUT = "preview_layout";
    private static final String ATTR_RESIZE_MODE = "resize_mode";
    private static final String ATTR_TARGET_CELL_HEIGHT = "target_cell_height";
    private static final String ATTR_TARGET_CELL_WIDTH = "target_cell_width";
    private static final String ATTR_UPDATE_PERIOD_MILLIS = "update_period_millis";
    private static final String ATTR_WIDGET_CATEGORY = "widget_category";
    private static final String ATTR_WIDGET_FEATURES = "widget_features";
    private static final String TAG = "AppWidgetXmlUtil";

    public static void writeAppWidgetProviderInfoLocked(TypedXmlSerializer out, AppWidgetProviderInfo info) throws IOException {
        Objects.requireNonNull(out);
        Objects.requireNonNull(info);
        out.attributeInt((String) null, ATTR_MIN_WIDTH, info.minWidth);
        out.attributeInt((String) null, ATTR_MIN_HEIGHT, info.minHeight);
        out.attributeInt((String) null, ATTR_MIN_RESIZE_WIDTH, info.minResizeWidth);
        out.attributeInt((String) null, ATTR_MIN_RESIZE_HEIGHT, info.minResizeHeight);
        out.attributeInt((String) null, ATTR_MAX_RESIZE_WIDTH, info.maxResizeWidth);
        out.attributeInt((String) null, ATTR_MAX_RESIZE_HEIGHT, info.maxResizeHeight);
        out.attributeInt((String) null, ATTR_TARGET_CELL_WIDTH, info.targetCellWidth);
        out.attributeInt((String) null, ATTR_TARGET_CELL_HEIGHT, info.targetCellHeight);
        out.attributeInt((String) null, ATTR_UPDATE_PERIOD_MILLIS, info.updatePeriodMillis);
        out.attributeInt((String) null, ATTR_INITIAL_LAYOUT, info.initialLayout);
        out.attributeInt((String) null, ATTR_INITIAL_KEYGUARD_LAYOUT, info.initialKeyguardLayout);
        if (info.configure != null) {
            out.attribute((String) null, ATTR_CONFIGURE, info.configure.flattenToShortString());
        }
        if (info.label != null) {
            out.attribute((String) null, ATTR_LABEL, info.label);
        } else {
            Slog.e(TAG, "Label is empty in " + info.provider);
        }
        out.attributeInt((String) null, ATTR_ICON, info.icon);
        out.attributeInt((String) null, ATTR_PREVIEW_IMAGE, info.previewImage);
        out.attributeInt((String) null, ATTR_PREVIEW_LAYOUT, info.previewLayout);
        out.attributeInt((String) null, ATTR_AUTO_ADVANCED_VIEW_ID, info.autoAdvanceViewId);
        out.attributeInt((String) null, ATTR_RESIZE_MODE, info.resizeMode);
        out.attributeInt((String) null, ATTR_WIDGET_CATEGORY, info.widgetCategory);
        out.attributeInt((String) null, ATTR_WIDGET_FEATURES, info.widgetFeatures);
        out.attributeInt((String) null, ATTR_DESCRIPTION_RES, info.descriptionRes);
        out.attribute((String) null, ATTR_OS_FINGERPRINT, Build.FINGERPRINT);
    }

    public static AppWidgetProviderInfo readAppWidgetProviderInfoLocked(TypedXmlPullParser parser) {
        Objects.requireNonNull(parser);
        String fingerprint = parser.getAttributeValue((String) null, ATTR_OS_FINGERPRINT);
        if (!Build.FINGERPRINT.equals(fingerprint)) {
            return null;
        }
        AppWidgetProviderInfo info = new AppWidgetProviderInfo();
        info.minWidth = parser.getAttributeInt((String) null, ATTR_MIN_WIDTH, 0);
        info.minHeight = parser.getAttributeInt((String) null, ATTR_MIN_HEIGHT, 0);
        info.minResizeWidth = parser.getAttributeInt((String) null, ATTR_MIN_RESIZE_WIDTH, 0);
        info.minResizeHeight = parser.getAttributeInt((String) null, ATTR_MIN_RESIZE_HEIGHT, 0);
        info.maxResizeWidth = parser.getAttributeInt((String) null, ATTR_MAX_RESIZE_WIDTH, 0);
        info.maxResizeHeight = parser.getAttributeInt((String) null, ATTR_MAX_RESIZE_HEIGHT, 0);
        info.targetCellWidth = parser.getAttributeInt((String) null, ATTR_TARGET_CELL_WIDTH, 0);
        info.targetCellHeight = parser.getAttributeInt((String) null, ATTR_TARGET_CELL_HEIGHT, 0);
        info.updatePeriodMillis = parser.getAttributeInt((String) null, ATTR_UPDATE_PERIOD_MILLIS, 0);
        info.initialLayout = parser.getAttributeInt((String) null, ATTR_INITIAL_LAYOUT, 0);
        info.initialKeyguardLayout = parser.getAttributeInt((String) null, ATTR_INITIAL_KEYGUARD_LAYOUT, 0);
        String configure = parser.getAttributeValue((String) null, ATTR_CONFIGURE);
        if (!TextUtils.isEmpty(configure)) {
            info.configure = ComponentName.unflattenFromString(configure);
        }
        info.label = parser.getAttributeValue((String) null, ATTR_LABEL);
        info.icon = parser.getAttributeInt((String) null, ATTR_ICON, 0);
        info.previewImage = parser.getAttributeInt((String) null, ATTR_PREVIEW_IMAGE, 0);
        info.previewLayout = parser.getAttributeInt((String) null, ATTR_PREVIEW_LAYOUT, 0);
        info.autoAdvanceViewId = parser.getAttributeInt((String) null, ATTR_AUTO_ADVANCED_VIEW_ID, 0);
        info.resizeMode = parser.getAttributeInt((String) null, ATTR_RESIZE_MODE, 0);
        info.widgetCategory = parser.getAttributeInt((String) null, ATTR_WIDGET_CATEGORY, 0);
        info.widgetFeatures = parser.getAttributeInt((String) null, ATTR_WIDGET_FEATURES, 0);
        info.descriptionRes = parser.getAttributeInt((String) null, ATTR_DESCRIPTION_RES, 0);
        return info;
    }
}
