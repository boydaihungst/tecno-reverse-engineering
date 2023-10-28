package com.android.server.policy.role;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.CollectionUtils;
import com.android.server.LocalServices;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.role.RoleServicePlatformHelper;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import libcore.util.HexEncoding;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class RoleServicePlatformHelperImpl implements RoleServicePlatformHelper {
    private static final String ATTRIBUTE_NAME = "name";
    private static final String LOG_TAG = RoleServicePlatformHelperImpl.class.getSimpleName();
    private static final String ROLES_FILE_NAME = "roles.xml";
    private static final String TAG_HOLDER = "holder";
    private static final String TAG_ROLE = "role";
    private static final String TAG_ROLES = "roles";
    private final Context mContext;

    public RoleServicePlatformHelperImpl(Context context) {
        this.mContext = context;
    }

    @Override // com.android.server.role.RoleServicePlatformHelper
    public Map<String, Set<String>> getLegacyRoleState(int userId) {
        Map<String, Set<String>> roles = readFile(userId);
        if (roles == null) {
            return readFromLegacySettings(userId);
        }
        return roles;
    }

    private Map<String, Set<String>> readFile(int userId) {
        File file = getFile(userId);
        try {
            try {
                FileInputStream in = new AtomicFile(file).openRead();
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(in, null);
                    Map<String, Set<String>> roles = parseXml(parser);
                    Slog.i(LOG_TAG, "Read legacy roles.xml successfully");
                    if (in != null) {
                        in.close();
                    }
                    return roles;
                } catch (Throwable th) {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (IOException | XmlPullParserException e) {
                Slog.wtf(LOG_TAG, "Failed to parse legacy roles.xml: " + file, e);
                return null;
            }
        } catch (FileNotFoundException e2) {
            Slog.i(LOG_TAG, "Legacy roles.xml not found");
            return null;
        }
    }

    private Map<String, Set<String>> parseXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        int depth;
        int innerDepth = parser.getDepth() + 1;
        while (true) {
            int type = parser.next();
            if (type == 1 || ((depth = parser.getDepth()) < innerDepth && type == 3)) {
                break;
            } else if (depth <= innerDepth && type == 2 && parser.getName().equals(TAG_ROLES)) {
                return parseRoles(parser);
            }
        }
        throw new IOException("Missing <roles> in roles.xml");
    }

    private Map<String, Set<String>> parseRoles(XmlPullParser parser) throws IOException, XmlPullParserException {
        int depth;
        Map<String, Set<String>> roles = new ArrayMap<>();
        int innerDepth = parser.getDepth() + 1;
        while (true) {
            int type = parser.next();
            if (type == 1 || ((depth = parser.getDepth()) < innerDepth && type == 3)) {
                break;
            } else if (depth <= innerDepth && type == 2 && parser.getName().equals(TAG_ROLE)) {
                String roleName = parser.getAttributeValue(null, "name");
                Set<String> roleHolders = parseRoleHoldersLocked(parser);
                roles.put(roleName, roleHolders);
            }
        }
        return roles;
    }

    private Set<String> parseRoleHoldersLocked(XmlPullParser parser) throws IOException, XmlPullParserException {
        int depth;
        Set<String> roleHolders = new ArraySet<>();
        int innerDepth = parser.getDepth() + 1;
        while (true) {
            int type = parser.next();
            if (type == 1 || ((depth = parser.getDepth()) < innerDepth && type == 3)) {
                break;
            } else if (depth <= innerDepth && type == 2 && parser.getName().equals(TAG_HOLDER)) {
                String roleHolder = parser.getAttributeValue(null, "name");
                roleHolders.add(roleHolder);
            }
        }
        return roleHolders;
    }

    private static File getFile(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), ROLES_FILE_NAME);
    }

    private Map<String, Set<String>> readFromLegacySettings(int userId) {
        String assistantPackageName;
        String dialerPackageName;
        String smsPackageName;
        Map<String, Set<String>> roles = new ArrayMap<>();
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String assistantSetting = Settings.Secure.getStringForUser(contentResolver, "assistant", userId);
        PackageManager packageManager = this.mContext.getPackageManager();
        String homePackageName = null;
        if (assistantSetting != null) {
            if (!assistantSetting.isEmpty()) {
                ComponentName componentName = ComponentName.unflattenFromString(assistantSetting);
                assistantPackageName = componentName != null ? componentName.getPackageName() : null;
            } else {
                assistantPackageName = null;
            }
        } else if (packageManager.isDeviceUpgrading()) {
            String defaultAssistant = this.mContext.getString(17039393);
            assistantPackageName = !TextUtils.isEmpty(defaultAssistant) ? defaultAssistant : null;
        } else {
            assistantPackageName = null;
        }
        if (assistantPackageName != null) {
            roles.put("android.app.role.ASSISTANT", Collections.singleton(assistantPackageName));
        }
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        String browserPackageName = packageManagerInternal.removeLegacyDefaultBrowserPackageName(userId);
        if (browserPackageName != null) {
            roles.put("android.app.role.BROWSER", Collections.singleton(browserPackageName));
        }
        String dialerSetting = Settings.Secure.getStringForUser(contentResolver, "dialer_default_application", userId);
        if (!TextUtils.isEmpty(dialerSetting)) {
            dialerPackageName = dialerSetting;
        } else if (packageManager.isDeviceUpgrading()) {
            dialerPackageName = this.mContext.getString(17039395);
        } else {
            dialerPackageName = null;
        }
        if (dialerPackageName != null) {
            roles.put("android.app.role.DIALER", Collections.singleton(dialerPackageName));
        }
        String smsSetting = Settings.Secure.getStringForUser(contentResolver, "sms_default_application", userId);
        if (!TextUtils.isEmpty(smsSetting)) {
            smsPackageName = smsSetting;
        } else if (this.mContext.getPackageManager().isDeviceUpgrading()) {
            smsPackageName = this.mContext.getString(17039396);
        } else {
            smsPackageName = null;
        }
        if (smsPackageName != null) {
            roles.put("android.app.role.SMS", Collections.singleton(smsPackageName));
        }
        if (packageManager.isDeviceUpgrading()) {
            ResolveInfo resolveInfo = packageManager.resolveActivityAsUser(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME"), 851968, userId);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                homePackageName = resolveInfo.activityInfo.packageName;
            }
            if (homePackageName != null && isSettingsApplication(homePackageName, userId)) {
                homePackageName = null;
            }
        } else {
            homePackageName = null;
        }
        if (homePackageName != null) {
            roles.put("android.app.role.HOME", Collections.singleton(homePackageName));
        }
        String emergencyPackageName = Settings.Secure.getStringForUser(contentResolver, "emergency_assistance_application", userId);
        if (emergencyPackageName != null) {
            roles.put("android.app.role.EMERGENCY", Collections.singleton(emergencyPackageName));
        }
        return roles;
    }

    private boolean isSettingsApplication(String packageName, int userId) {
        PackageManager packageManager = this.mContext.getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivityAsUser(new Intent("android.settings.SETTINGS"), 851968, userId);
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return false;
        }
        return Objects.equals(packageName, resolveInfo.activityInfo.packageName);
    }

    @Override // com.android.server.role.RoleServicePlatformHelper
    public String computePackageStateHash(final int userId) {
        final PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        MessageDigestOutputStream mdos = new MessageDigestOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(mdos));
        packageManagerInternal.forEachInstalledPackage(new Consumer() { // from class: com.android.server.policy.role.RoleServicePlatformHelperImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RoleServicePlatformHelperImpl.lambda$computePackageStateHash$0(dataOutputStream, packageManagerInternal, userId, (AndroidPackage) obj);
            }
        }, userId);
        return mdos.getDigestAsString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$computePackageStateHash$0(DataOutputStream dataOutputStream, PackageManagerInternal packageManagerInternal, int userId, AndroidPackage pkg) {
        Signature[] signatures;
        try {
            dataOutputStream.writeUTF(pkg.getPackageName());
            dataOutputStream.writeLong(pkg.getLongVersionCode());
            dataOutputStream.writeInt(packageManagerInternal.getApplicationEnabledState(pkg.getPackageName(), userId));
            List<String> requestedPermissions = pkg.getRequestedPermissions();
            int requestedPermissionsSize = requestedPermissions.size();
            dataOutputStream.writeInt(requestedPermissionsSize);
            for (int i = 0; i < requestedPermissionsSize; i++) {
                dataOutputStream.writeUTF(requestedPermissions.get(i));
            }
            ArraySet<String> enabledComponents = packageManagerInternal.getEnabledComponents(pkg.getPackageName(), userId);
            int enabledComponentsSize = CollectionUtils.size(enabledComponents);
            dataOutputStream.writeInt(enabledComponentsSize);
            for (int i2 = 0; i2 < enabledComponentsSize; i2++) {
                dataOutputStream.writeUTF(enabledComponents.valueAt(i2));
            }
            ArraySet<String> disabledComponents = packageManagerInternal.getDisabledComponents(pkg.getPackageName(), userId);
            int disabledComponentsSize = CollectionUtils.size(disabledComponents);
            for (int i3 = 0; i3 < disabledComponentsSize; i3++) {
                dataOutputStream.writeUTF(disabledComponents.valueAt(i3));
            }
            for (Signature signature : pkg.getSigningDetails().getSignatures()) {
                dataOutputStream.write(signature.toByteArray());
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /* loaded from: classes2.dex */
    private static class MessageDigestOutputStream extends OutputStream {
        private final MessageDigest mMessageDigest;

        MessageDigestOutputStream() {
            try {
                this.mMessageDigest = MessageDigest.getInstance("SHA256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to create MessageDigest", e);
            }
        }

        String getDigestAsString() {
            return HexEncoding.encodeToString(this.mMessageDigest.digest(), true);
        }

        @Override // java.io.OutputStream
        public void write(int b) throws IOException {
            this.mMessageDigest.update((byte) b);
        }

        @Override // java.io.OutputStream
        public void write(byte[] b) throws IOException {
            this.mMessageDigest.update(b);
        }

        @Override // java.io.OutputStream
        public void write(byte[] b, int off, int len) throws IOException {
            this.mMessageDigest.update(b, off, len);
        }
    }
}
