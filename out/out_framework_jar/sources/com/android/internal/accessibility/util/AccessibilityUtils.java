package com.android.internal.accessibility.util;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.ParcelableSpan;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.accessibility.common.ShortcutConstants;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import libcore.util.EmptyArray;
/* loaded from: classes4.dex */
public final class AccessibilityUtils {
    public static final int NONE = 0;
    public static final int PARCELABLE_SPAN = 2;
    public static final int TEXT = 1;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes4.dex */
    public @interface A11yTextChangeType {
    }

    private AccessibilityUtils() {
    }

    public static Set<ComponentName> getEnabledServicesFromSettings(Context context, int userId) {
        String enabledServicesSetting = Settings.Secure.getStringForUser(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, userId);
        if (TextUtils.isEmpty(enabledServicesSetting)) {
            return Collections.emptySet();
        }
        Set<ComponentName> enabledServices = new HashSet<>();
        TextUtils.StringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(ShortcutConstants.SERVICES_SEPARATOR);
        colonSplitter.setString(enabledServicesSetting);
        for (String componentNameString : colonSplitter) {
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
            if (enabledService != null) {
                enabledServices.add(enabledService);
            }
        }
        return enabledServices;
    }

    public static void setAccessibilityServiceState(Context context, ComponentName componentName, boolean enabled) {
        setAccessibilityServiceState(context, componentName, enabled, UserHandle.myUserId());
    }

    public static void setAccessibilityServiceState(Context context, ComponentName componentName, boolean enabled, int userId) {
        Set<ComponentName> enabledServices = getEnabledServicesFromSettings(context, userId);
        if (enabledServices.isEmpty()) {
            enabledServices = new ArraySet(1);
        }
        if (enabled) {
            enabledServices.add(componentName);
        } else {
            enabledServices.remove(componentName);
        }
        StringBuilder enabledServicesBuilder = new StringBuilder();
        for (ComponentName enabledService : enabledServices) {
            enabledServicesBuilder.append(enabledService.flattenToString());
            enabledServicesBuilder.append(ShortcutConstants.SERVICES_SEPARATOR);
        }
        int enabledServicesBuilderLength = enabledServicesBuilder.length();
        if (enabledServicesBuilderLength > 0) {
            enabledServicesBuilder.deleteCharAt(enabledServicesBuilderLength - 1);
        }
        Settings.Secure.putStringForUser(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, enabledServicesBuilder.toString(), userId);
    }

    public static int getAccessibilityServiceFragmentType(AccessibilityServiceInfo accessibilityServiceInfo) {
        int targetSdk = accessibilityServiceInfo.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion;
        boolean requestA11yButton = (accessibilityServiceInfo.flags & 256) != 0;
        if (targetSdk <= 29) {
            return 0;
        }
        return requestA11yButton ? 1 : 2;
    }

    public static boolean isAccessibilityServiceEnabled(Context context, String componentId) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(-1);
        for (AccessibilityServiceInfo info : enabledServices) {
            String id = info.getComponentName().flattenToString();
            if (id.equals(componentId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUserSetupCompleted(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 0, -2) != 0;
    }

    public static int textOrSpanChanged(CharSequence before, CharSequence after) {
        if (!TextUtils.equals(before, after)) {
            return 1;
        }
        if (((before instanceof Spanned) || (after instanceof Spanned)) && !parcelableSpansEquals(before, after)) {
            return 2;
        }
        return 0;
    }

    private static boolean parcelableSpansEquals(CharSequence before, CharSequence after) {
        Object[] spansA = EmptyArray.OBJECT;
        Object[] spansB = EmptyArray.OBJECT;
        Spanned a = null;
        Spanned b = null;
        if (before instanceof Spanned) {
            a = (Spanned) before;
            spansA = a.getSpans(0, a.length(), ParcelableSpan.class);
        }
        if (after instanceof Spanned) {
            b = (Spanned) after;
            spansB = b.getSpans(0, b.length(), ParcelableSpan.class);
        }
        if (spansA.length != spansB.length) {
            return false;
        }
        for (int i = 0; i < spansA.length; i++) {
            Object thisSpan = spansA[i];
            Object otherSpan = spansB[i];
            if (thisSpan.getClass() != otherSpan.getClass() || a.getSpanStart(thisSpan) != b.getSpanStart(otherSpan) || a.getSpanEnd(thisSpan) != b.getSpanEnd(otherSpan) || a.getSpanFlags(thisSpan) != b.getSpanFlags(otherSpan)) {
                return false;
            }
        }
        return true;
    }
}
