package com.android.server.pm;

import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.UserTypeDetails;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public final class UserTypeFactory {
    private static final String LOG_TAG = "UserTypeFactory";

    private UserTypeFactory() {
    }

    public static ArrayMap<String, UserTypeDetails> getUserTypes() {
        ArrayMap<String, UserTypeDetails.Builder> builders = getDefaultBuilders();
        XmlResourceParser parser = Resources.getSystem().getXml(18284550);
        try {
            customizeBuilders(builders, parser);
            if (parser != null) {
                parser.close();
            }
            ArrayMap<String, UserTypeDetails> types = new ArrayMap<>(builders.size());
            for (int i = 0; i < builders.size(); i++) {
                types.put(builders.keyAt(i), builders.valueAt(i).createUserTypeDetails());
            }
            return types;
        } catch (Throwable th) {
            if (parser != null) {
                try {
                    parser.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static ArrayMap<String, UserTypeDetails.Builder> getDefaultBuilders() {
        ArrayMap<String, UserTypeDetails.Builder> builders = new ArrayMap<>();
        builders.put("android.os.usertype.profile.MANAGED", getDefaultTypeProfileManaged());
        builders.put("android.os.usertype.full.SYSTEM", getDefaultTypeFullSystem());
        builders.put("android.os.usertype.full.SECONDARY", getDefaultTypeFullSecondary());
        builders.put("android.os.usertype.full.GUEST", getDefaultTypeFullGuest());
        builders.put("android.os.usertype.full.DEMO", getDefaultTypeFullDemo());
        builders.put("android.os.usertype.full.RESTRICTED", getDefaultTypeFullRestricted());
        builders.put("android.os.usertype.system.HEADLESS", getDefaultTypeSystemHeadless());
        builders.put("android.os.usertype.profile.CLONE", getDefaultTypeProfileClone());
        if (Build.IS_DEBUGGABLE) {
            builders.put("android.os.usertype.profile.TEST", getDefaultTypeProfileTest());
        }
        builders.put("android.os.usertype.profile.DUAL", getDefaultTypeProfileDual());
        return builders;
    }

    private static UserTypeDetails.Builder getDefaultTypeProfileClone() {
        return new UserTypeDetails.Builder().setName("android.os.usertype.profile.CLONE").setBaseType(4096).setMaxAllowedPerParent(1).setLabel(0).setDefaultRestrictions(null).setIsMediaSharedWithParent(true).setIsCredentialSharableWithParent(true);
    }

    private static UserTypeDetails.Builder getDefaultTypeProfileManaged() {
        return new UserTypeDetails.Builder().setName("android.os.usertype.profile.MANAGED").setBaseType(4096).setDefaultUserInfoPropertyFlags(32).setMaxAllowedPerParent(1).setLabel(0).setIconBadge(17302430).setBadgePlain(17302425).setBadgeNoBackground(17302427).setBadgeLabels(17040668, 17040669, 17040670).setBadgeColors(17171052, 17171054, 17171056).setDarkThemeBadgeColors(17171053, 17171055, 17171057).setDefaultRestrictions(getDefaultManagedProfileRestrictions()).setDefaultSecureSettings(getDefaultManagedProfileSecureSettings()).setDefaultCrossProfileIntentFilters(getDefaultManagedCrossProfileIntentFilter()).setIsCredentialSharableWithParent(true);
    }

    private static UserTypeDetails.Builder getDefaultTypeProfileDual() {
        return new UserTypeDetails.Builder().setName("android.os.usertype.profile.DUAL").setBaseType(4096).setDefaultUserInfoPropertyFlags(32768).setMaxAllowedPerParent(1).setLabel(0).setIconBadge(17303163).setBadgePlain(17302425).setBadgeNoBackground(17302427).setBadgeLabels(101580805, 101580806, 101580807).setBadgeColors(17171052, 17171054, 17171056).setDarkThemeBadgeColors(17171053, 17171055, 17171057).setDefaultRestrictions(null).setIsMediaSharedWithParent(true);
    }

    private static UserTypeDetails.Builder getDefaultTypeProfileTest() {
        Bundle restrictions = new Bundle();
        restrictions.putBoolean("no_fun", true);
        return new UserTypeDetails.Builder().setName("android.os.usertype.profile.TEST").setBaseType(4096).setMaxAllowedPerParent(2).setLabel(0).setIconBadge(17302430).setBadgePlain(17302425).setBadgeNoBackground(17302427).setBadgeLabels(17040668, 17040669, 17040670).setBadgeColors(17171052, 17171054, 17171056).setDarkThemeBadgeColors(17171053, 17171055, 17171057).setDefaultRestrictions(restrictions);
    }

    private static UserTypeDetails.Builder getDefaultTypeFullSecondary() {
        return new UserTypeDetails.Builder().setName("android.os.usertype.full.SECONDARY").setBaseType(1024).setMaxAllowed(-1).setDefaultRestrictions(getDefaultSecondaryUserRestrictions());
    }

    private static UserTypeDetails.Builder getDefaultTypeFullGuest() {
        boolean ephemeralGuests = Resources.getSystem().getBoolean(17891674);
        int flags = (ephemeralGuests ? 256 : 0) | 4;
        return new UserTypeDetails.Builder().setName("android.os.usertype.full.GUEST").setBaseType(1024).setDefaultUserInfoPropertyFlags(flags).setMaxAllowed(1).setDefaultRestrictions(getDefaultGuestUserRestrictions());
    }

    private static UserTypeDetails.Builder getDefaultTypeFullDemo() {
        return new UserTypeDetails.Builder().setName("android.os.usertype.full.DEMO").setBaseType(1024).setDefaultUserInfoPropertyFlags(512).setMaxAllowed(-1).setDefaultRestrictions(null);
    }

    private static UserTypeDetails.Builder getDefaultTypeFullRestricted() {
        return new UserTypeDetails.Builder().setName("android.os.usertype.full.RESTRICTED").setBaseType(1024).setDefaultUserInfoPropertyFlags(8).setMaxAllowed(-1).setDefaultRestrictions(null);
    }

    private static UserTypeDetails.Builder getDefaultTypeFullSystem() {
        return new UserTypeDetails.Builder().setName("android.os.usertype.full.SYSTEM").setBaseType(3072);
    }

    private static UserTypeDetails.Builder getDefaultTypeSystemHeadless() {
        return new UserTypeDetails.Builder().setName("android.os.usertype.system.HEADLESS").setBaseType(2048);
    }

    private static Bundle getDefaultSecondaryUserRestrictions() {
        Bundle restrictions = new Bundle();
        restrictions.putBoolean("no_outgoing_calls", true);
        restrictions.putBoolean("no_sms", true);
        return restrictions;
    }

    private static Bundle getDefaultGuestUserRestrictions() {
        Bundle restrictions = getDefaultSecondaryUserRestrictions();
        restrictions.putBoolean("no_config_wifi", true);
        restrictions.putBoolean("no_install_unknown_sources", true);
        restrictions.putBoolean("no_config_credentials", true);
        return restrictions;
    }

    private static Bundle getDefaultManagedProfileRestrictions() {
        Bundle restrictions = new Bundle();
        restrictions.putBoolean("no_wallpaper", true);
        return restrictions;
    }

    private static Bundle getDefaultManagedProfileSecureSettings() {
        Bundle settings = new Bundle();
        settings.putString("managed_profile_contact_remote_search", "1");
        settings.putString("cross_profile_calendar_enabled", "1");
        return settings;
    }

    private static List<DefaultCrossProfileIntentFilter> getDefaultManagedCrossProfileIntentFilter() {
        return DefaultCrossProfileIntentFiltersUtils.getDefaultManagedProfileFilters();
    }

    static void customizeBuilders(ArrayMap<String, UserTypeDetails.Builder> builders, XmlResourceParser parser) {
        boolean isProfile;
        final UserTypeDetails.Builder builder;
        try {
            XmlUtils.beginDocument(parser, "user-types");
            XmlUtils.nextElement(parser);
            while (true) {
                boolean isValid = true;
                if (parser.getEventType() != 1) {
                    String elementName = parser.getName();
                    if ("profile-type".equals(elementName)) {
                        isProfile = true;
                    } else if ("full-type".equals(elementName)) {
                        isProfile = false;
                    } else {
                        if ("change-user-type".equals(elementName)) {
                            XmlUtils.skipCurrentTag(parser);
                        } else {
                            Slog.w(LOG_TAG, "Skipping unknown element " + elementName + " in " + parser.getPositionDescription());
                            XmlUtils.skipCurrentTag(parser);
                        }
                        XmlUtils.nextElement(parser);
                    }
                    String typeName = parser.getAttributeValue(null, "name");
                    if (typeName != null && !typeName.equals("")) {
                        String typeName2 = typeName.intern();
                        if (typeName2.startsWith("android.")) {
                            Slog.i(LOG_TAG, "Customizing user type " + typeName2);
                            builder = builders.get(typeName2);
                            if (builder == null) {
                                throw new IllegalArgumentException("Illegal custom user type name " + typeName2 + ": Non-AOSP user types cannot start with 'android.'");
                            }
                            if ((!isProfile || builder.getBaseType() != 4096) && (isProfile || builder.getBaseType() != 1024)) {
                                isValid = false;
                            }
                            if (!isValid) {
                                throw new IllegalArgumentException("Wrong base type to customize user type (" + typeName2 + "), which is type " + UserInfo.flagsToString(builder.getBaseType()));
                            }
                        } else if (isProfile) {
                            Slog.i(LOG_TAG, "Creating custom user type " + typeName2);
                            builder = new UserTypeDetails.Builder();
                            builder.setName(typeName2);
                            builder.setBaseType(4096);
                            builders.put(typeName2, builder);
                        } else {
                            throw new IllegalArgumentException("Creation of non-profile user type (" + typeName2 + ") is not currently supported.");
                        }
                        if (isProfile) {
                            Objects.requireNonNull(builder);
                            setIntAttribute(parser, "max-allowed-per-parent", new Consumer() { // from class: com.android.server.pm.UserTypeFactory$$ExternalSyntheticLambda0
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    UserTypeDetails.Builder.this.setMaxAllowedPerParent(((Integer) obj).intValue());
                                }
                            });
                            Objects.requireNonNull(builder);
                            setResAttribute(parser, "icon-badge", new Consumer() { // from class: com.android.server.pm.UserTypeFactory$$ExternalSyntheticLambda1
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    UserTypeDetails.Builder.this.setIconBadge(((Integer) obj).intValue());
                                }
                            });
                            Objects.requireNonNull(builder);
                            setResAttribute(parser, "badge-plain", new Consumer() { // from class: com.android.server.pm.UserTypeFactory$$ExternalSyntheticLambda2
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    UserTypeDetails.Builder.this.setBadgePlain(((Integer) obj).intValue());
                                }
                            });
                            Objects.requireNonNull(builder);
                            setResAttribute(parser, "badge-no-background", new Consumer() { // from class: com.android.server.pm.UserTypeFactory$$ExternalSyntheticLambda3
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    UserTypeDetails.Builder.this.setBadgeNoBackground(((Integer) obj).intValue());
                                }
                            });
                        }
                        Objects.requireNonNull(builder);
                        setIntAttribute(parser, ServiceConfigAccessor.PROVIDER_MODE_ENABLED, new Consumer() { // from class: com.android.server.pm.UserTypeFactory$$ExternalSyntheticLambda4
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                UserTypeDetails.Builder.this.setEnabled(((Integer) obj).intValue());
                            }
                        });
                        int depth = parser.getDepth();
                        while (XmlUtils.nextElementWithin(parser, depth)) {
                            String childName = parser.getName();
                            if ("default-restrictions".equals(childName)) {
                                Bundle restrictions = UserRestrictionsUtils.readRestrictions(XmlUtils.makeTyped(parser));
                                builder.setDefaultRestrictions(restrictions);
                            } else if (isProfile && "badge-labels".equals(childName)) {
                                Objects.requireNonNull(builder);
                                setResAttributeArray(parser, new Consumer() { // from class: com.android.server.pm.UserTypeFactory$$ExternalSyntheticLambda5
                                    @Override // java.util.function.Consumer
                                    public final void accept(Object obj) {
                                        UserTypeDetails.Builder.this.setBadgeLabels((int[]) obj);
                                    }
                                });
                            } else if (isProfile && "badge-colors".equals(childName)) {
                                Objects.requireNonNull(builder);
                                setResAttributeArray(parser, new Consumer() { // from class: com.android.server.pm.UserTypeFactory$$ExternalSyntheticLambda6
                                    @Override // java.util.function.Consumer
                                    public final void accept(Object obj) {
                                        UserTypeDetails.Builder.this.setBadgeColors((int[]) obj);
                                    }
                                });
                            } else if (isProfile && "badge-colors-dark".equals(childName)) {
                                Objects.requireNonNull(builder);
                                setResAttributeArray(parser, new Consumer() { // from class: com.android.server.pm.UserTypeFactory$$ExternalSyntheticLambda7
                                    @Override // java.util.function.Consumer
                                    public final void accept(Object obj) {
                                        UserTypeDetails.Builder.this.setDarkThemeBadgeColors((int[]) obj);
                                    }
                                });
                            } else {
                                Slog.w(LOG_TAG, "Unrecognized tag " + childName + " in " + parser.getPositionDescription());
                            }
                        }
                        XmlUtils.nextElement(parser);
                    }
                    Slog.w(LOG_TAG, "Skipping user type with no name in " + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                    XmlUtils.nextElement(parser);
                } else {
                    return;
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Slog.w(LOG_TAG, "Cannot read user type configuration file.", e);
        }
    }

    private static void setIntAttribute(XmlResourceParser parser, String attributeName, Consumer<Integer> fcn) {
        String intValue = parser.getAttributeValue(null, attributeName);
        if (intValue == null) {
            return;
        }
        try {
            fcn.accept(Integer.valueOf(Integer.parseInt(intValue)));
        } catch (NumberFormatException e) {
            Slog.e(LOG_TAG, "Cannot parse value of '" + intValue + "' for " + attributeName + " in " + parser.getPositionDescription(), e);
            throw e;
        }
    }

    private static void setResAttribute(XmlResourceParser parser, String attributeName, Consumer<Integer> fcn) {
        if (parser.getAttributeValue(null, attributeName) != null) {
            int resId = parser.getAttributeResourceValue(null, attributeName, 0);
            fcn.accept(Integer.valueOf(resId));
        }
    }

    private static void setResAttributeArray(XmlResourceParser parser, Consumer<int[]> fcn) throws IOException, XmlPullParserException {
        ArrayList<Integer> resList = new ArrayList<>();
        int depth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, depth)) {
            String elementName = parser.getName();
            if (!Settings.TAG_ITEM.equals(elementName)) {
                Slog.w(LOG_TAG, "Skipping unknown child element " + elementName + " in " + parser.getPositionDescription());
                XmlUtils.skipCurrentTag(parser);
            } else {
                int resId = parser.getAttributeResourceValue(null, "res", -1);
                if (resId != -1) {
                    resList.add(Integer.valueOf(resId));
                }
            }
        }
        int[] result = new int[resList.size()];
        for (int i = 0; i < resList.size(); i++) {
            result[i] = resList.get(i).intValue();
        }
        fcn.accept(result);
    }

    public static int getUserTypeVersion() {
        XmlResourceParser parser = Resources.getSystem().getXml(18284550);
        try {
            int userTypeVersion = getUserTypeVersion(parser);
            if (parser != null) {
                parser.close();
            }
            return userTypeVersion;
        } catch (Throwable th) {
            if (parser != null) {
                try {
                    parser.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    static int getUserTypeVersion(XmlResourceParser parser) {
        try {
            XmlUtils.beginDocument(parser, "user-types");
            String versionValue = parser.getAttributeValue(null, "version");
            if (versionValue != null) {
                try {
                    int version = Integer.parseInt(versionValue);
                    return version;
                } catch (NumberFormatException e) {
                    Slog.e(LOG_TAG, "Cannot parse value of '" + versionValue + "' for version in " + parser.getPositionDescription(), e);
                    throw e;
                }
            }
            return 0;
        } catch (IOException | XmlPullParserException e2) {
            Slog.w(LOG_TAG, "Cannot read user type configuration file.", e2);
            return 0;
        }
    }

    public static List<UserTypeUpgrade> getUserTypeUpgrades() {
        XmlResourceParser parser = Resources.getSystem().getXml(18284550);
        try {
            List<UserTypeUpgrade> userUpgrades = parseUserUpgrades(getDefaultBuilders(), parser);
            if (parser != null) {
                parser.close();
            }
            return userUpgrades;
        } catch (Throwable th) {
            if (parser != null) {
                try {
                    parser.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    static List<UserTypeUpgrade> parseUserUpgrades(ArrayMap<String, UserTypeDetails.Builder> builders, XmlResourceParser parser) {
        List<UserTypeUpgrade> userUpgrades = new ArrayList<>();
        try {
            XmlUtils.beginDocument(parser, "user-types");
            XmlUtils.nextElement(parser);
            while (parser.getEventType() != 1) {
                String elementName = parser.getName();
                if ("change-user-type".equals(elementName)) {
                    String fromType = parser.getAttributeValue(null, "from");
                    String toType = parser.getAttributeValue(null, "to");
                    validateUserTypeIsProfile(fromType, builders);
                    validateUserTypeIsProfile(toType, builders);
                    try {
                        int maxVersionToConvert = Integer.parseInt(parser.getAttributeValue(null, "whenVersionLeq"));
                        UserTypeUpgrade userTypeUpgrade = new UserTypeUpgrade(fromType, toType, maxVersionToConvert);
                        userUpgrades.add(userTypeUpgrade);
                    } catch (NumberFormatException e) {
                        Slog.e(LOG_TAG, "Cannot parse value of whenVersionLeq in " + parser.getPositionDescription(), e);
                        throw e;
                    }
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
                XmlUtils.nextElement(parser);
            }
        } catch (IOException | XmlPullParserException e2) {
            Slog.w(LOG_TAG, "Cannot read user type configuration file.", e2);
        }
        return userUpgrades;
    }

    private static void validateUserTypeIsProfile(String userType, ArrayMap<String, UserTypeDetails.Builder> builders) {
        UserTypeDetails.Builder builder = builders.get(userType);
        if (builder != null && builder.getBaseType() != 4096) {
            throw new IllegalArgumentException("Illegal upgrade of user type " + userType + " : Can only upgrade profiles user types");
        }
    }

    /* loaded from: classes2.dex */
    public static class UserTypeUpgrade {
        private final String mFromType;
        private final String mToType;
        private final int mUpToVersion;

        public UserTypeUpgrade(String fromType, String toType, int upToVersion) {
            this.mFromType = fromType;
            this.mToType = toType;
            this.mUpToVersion = upToVersion;
        }

        public String getFromType() {
            return this.mFromType;
        }

        public String getToType() {
            return this.mToType;
        }

        public int getUpToVersion() {
            return this.mUpToVersion;
        }
    }
}
