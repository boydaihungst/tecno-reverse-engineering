package com.android.server.notification;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.function.TriPredicate;
import com.android.server.job.controllers.JobStatus;
import com.android.server.notification.NotificationManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.utils.TimingsTraceAndSlog;
import defpackage.CompanionAppsPermissions;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public abstract class ManagedServices {
    static final int APPROVAL_BY_COMPONENT = 1;
    static final int APPROVAL_BY_PACKAGE = 0;
    static final String ATT_APPROVED_LIST = "approved";
    static final String ATT_DEFAULTS = "defaults";
    static final String ATT_IS_PRIMARY = "primary";
    static final String ATT_USER_CHANGED = "user_changed";
    static final String ATT_USER_ID = "user";
    static final String ATT_USER_SET = "user_set_services";
    static final String ATT_USER_SET_OLD = "user_set";
    static final String ATT_VERSION = "version";
    static final String DB_VERSION = "4";
    private static final String DB_VERSION_1 = "1";
    private static final String DB_VERSION_2 = "2";
    private static final String DB_VERSION_3 = "3";
    protected static final String ENABLED_SERVICES_SEPARATOR = ":";
    private static final int ON_BINDING_DIED_REBIND_DELAY_MS = 10000;
    static final String TAG_MANAGED_SERVICES = "service_listing";
    protected final boolean DEBUG;
    protected final String TAG;
    protected int mApprovalLevel;
    protected ArrayMap<Integer, ArrayMap<Boolean, ArraySet<String>>> mApproved;
    private final Config mConfig;
    protected final Context mContext;
    protected final ArraySet<ComponentName> mDefaultComponents;
    protected final ArraySet<String> mDefaultPackages;
    protected final Object mDefaultsLock;
    private ArraySet<ComponentName> mEnabledServicesForCurrentProfiles;
    private ArraySet<String> mEnabledServicesPackageNames;
    private final Handler mHandler;
    protected ArrayMap<Integer, Boolean> mIsUserChanged;
    protected final Object mMutex;
    protected final IPackageManager mPm;
    private final ArrayList<ManagedServiceInfo> mServices;
    private final ArrayList<Pair<ComponentName, Integer>> mServicesBound;
    private final ArraySet<Pair<ComponentName, Integer>> mServicesRebinding;
    private ArraySet<ComponentName> mSnoozingForCurrentProfiles;
    protected final UserManager mUm;
    private boolean mUseXml;
    private final UserProfiles mUserProfiles;
    protected ArrayMap<Integer, ArraySet<String>> mUserSetServices;

    /* loaded from: classes2.dex */
    public static class Config {
        public String bindPermission;
        public String caption;
        public int clientLabel;
        public String secondarySettingName;
        public String secureSettingName;
        public String serviceInterface;
        public String settingsAction;
        public String xmlTag;
    }

    protected abstract IInterface asInterface(IBinder iBinder);

    protected abstract boolean checkType(IInterface iInterface);

    protected abstract void ensureFilters(ServiceInfo serviceInfo, int i);

    protected abstract Config getConfig();

    protected abstract String getRequiredPermission();

    protected abstract void loadDefaultsFromConfig();

    protected abstract void onServiceAdded(ManagedServiceInfo managedServiceInfo);

    public ManagedServices(Context context, Object mutex, UserProfiles userProfiles, IPackageManager pm) {
        String simpleName = getClass().getSimpleName();
        this.TAG = simpleName;
        this.DEBUG = Log.isLoggable(simpleName, 3);
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mServices = new ArrayList<>();
        this.mServicesBound = new ArrayList<>();
        this.mServicesRebinding = new ArraySet<>();
        this.mDefaultsLock = new Object();
        this.mDefaultComponents = new ArraySet<>();
        this.mDefaultPackages = new ArraySet<>();
        this.mEnabledServicesForCurrentProfiles = new ArraySet<>();
        this.mEnabledServicesPackageNames = new ArraySet<>();
        this.mSnoozingForCurrentProfiles = new ArraySet<>();
        this.mApproved = new ArrayMap<>();
        this.mUserSetServices = new ArrayMap<>();
        this.mIsUserChanged = new ArrayMap<>();
        this.mContext = context;
        this.mMutex = mutex;
        this.mUserProfiles = userProfiles;
        this.mPm = pm;
        this.mConfig = getConfig();
        this.mApprovalLevel = 1;
        this.mUm = (UserManager) context.getSystemService(ATT_USER_ID);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getCaption() {
        return this.mConfig.caption;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<ManagedServiceInfo> getServices() {
        List<ManagedServiceInfo> services;
        synchronized (this.mMutex) {
            services = new ArrayList<>(this.mServices);
        }
        return services;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addDefaultComponentOrPackage(String packageOrComponent) {
        if (!TextUtils.isEmpty(packageOrComponent)) {
            synchronized (this.mDefaultsLock) {
                if (this.mApprovalLevel == 0) {
                    this.mDefaultPackages.add(packageOrComponent);
                    return;
                }
                ComponentName cn = ComponentName.unflattenFromString(packageOrComponent);
                if (cn != null && this.mApprovalLevel == 1) {
                    this.mDefaultPackages.add(cn.getPackageName());
                    this.mDefaultComponents.add(cn);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDefaultComponentOrPackage(String packageOrComponent) {
        synchronized (this.mDefaultsLock) {
            ComponentName cn = ComponentName.unflattenFromString(packageOrComponent);
            if (cn == null) {
                return this.mDefaultPackages.contains(packageOrComponent);
            }
            return this.mDefaultComponents.contains(cn);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArraySet<ComponentName> getDefaultComponents() {
        ArraySet<ComponentName> arraySet;
        synchronized (this.mDefaultsLock) {
            arraySet = new ArraySet<>(this.mDefaultComponents);
        }
        return arraySet;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArraySet<String> getDefaultPackages() {
        ArraySet<String> arraySet;
        synchronized (this.mDefaultsLock) {
            arraySet = new ArraySet<>(this.mDefaultPackages);
        }
        return arraySet;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayMap<Boolean, ArrayList<ComponentName>> resetComponents(String packageName, int userId) {
        ArrayList<ComponentName> componentsToEnable = new ArrayList<>(this.mDefaultComponents.size());
        ArrayList<ComponentName> disabledComponents = new ArrayList<>(this.mDefaultComponents.size());
        ArraySet<ComponentName> enabledComponents = new ArraySet<>(getAllowedComponents(userId));
        boolean changed = false;
        synchronized (this.mDefaultsLock) {
            for (int i = 0; i < this.mDefaultComponents.size() && enabledComponents.size() > 0; i++) {
                ComponentName currentDefault = this.mDefaultComponents.valueAt(i);
                if (packageName.equals(currentDefault.getPackageName()) && !enabledComponents.contains(currentDefault)) {
                    componentsToEnable.add(currentDefault);
                }
            }
            synchronized (this.mApproved) {
                ArrayMap<Boolean, ArraySet<String>> approvedByType = this.mApproved.get(Integer.valueOf(userId));
                if (approvedByType != null) {
                    int M = approvedByType.size();
                    for (int j = 0; j < M; j++) {
                        ArraySet<String> approved = approvedByType.valueAt(j);
                        for (int i2 = 0; i2 < enabledComponents.size(); i2++) {
                            ComponentName currentComponent = enabledComponents.valueAt(i2);
                            if (packageName.equals(currentComponent.getPackageName()) && !this.mDefaultComponents.contains(currentComponent) && approved.remove(currentComponent.flattenToString())) {
                                disabledComponents.add(currentComponent);
                                clearUserSetFlagLocked(currentComponent, userId);
                                changed = true;
                            }
                        }
                        for (int i3 = 0; i3 < componentsToEnable.size(); i3++) {
                            ComponentName candidate = componentsToEnable.get(i3);
                            changed |= approved.add(candidate.flattenToString());
                        }
                    }
                }
            }
        }
        if (changed) {
            rebindServices(false, -1);
        }
        ArrayMap<Boolean, ArrayList<ComponentName>> changes = new ArrayMap<>();
        changes.put(true, componentsToEnable);
        changes.put(false, disabledComponents);
        return changes;
    }

    private boolean clearUserSetFlagLocked(ComponentName component, int userId) {
        String approvedValue = getApprovedValue(component.flattenToString());
        ArraySet<String> userSet = this.mUserSetServices.get(Integer.valueOf(userId));
        return userSet != null && userSet.remove(approvedValue);
    }

    protected int getBindFlags() {
        return 83886081;
    }

    protected void onServiceRemovedLocked(ManagedServiceInfo removed) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ManagedServiceInfo newServiceInfo(IInterface service, ComponentName component, int userId, boolean isSystem, ServiceConnection connection, int targetSdkVersion, int uid) {
        return new ManagedServiceInfo(service, component, userId, isSystem, connection, targetSdkVersion, uid);
    }

    public void onBootPhaseAppsCanStart() {
    }

    public void dump(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        pw.println("    Allowed " + getCaption() + "s:");
        synchronized (this.mApproved) {
            int N = this.mApproved.size();
            for (int i = 0; i < N; i++) {
                int userId = this.mApproved.keyAt(i).intValue();
                ArrayMap<Boolean, ArraySet<String>> approvedByType = this.mApproved.valueAt(i);
                Boolean userChanged = this.mIsUserChanged.get(Integer.valueOf(userId));
                if (approvedByType != null) {
                    int M = approvedByType.size();
                    for (int j = 0; j < M; j++) {
                        boolean isPrimary = approvedByType.keyAt(j).booleanValue();
                        ArraySet<String> approved = approvedByType.valueAt(j);
                        if (approvedByType != null && approvedByType.size() > 0) {
                            pw.println("      " + String.join(ENABLED_SERVICES_SEPARATOR, approved) + " (user: " + userId + " isPrimary: " + isPrimary + (userChanged == null ? "" : " isUserChanged: " + userChanged) + ")");
                        }
                    }
                }
            }
            pw.println("    Has user set:");
            Set<Integer> userIds = this.mUserSetServices.keySet();
            for (Integer num : userIds) {
                int userId2 = num.intValue();
                if (this.mIsUserChanged.get(Integer.valueOf(userId2)) == null) {
                    pw.println("      userId=" + userId2 + " value=" + this.mUserSetServices.get(Integer.valueOf(userId2)));
                }
            }
        }
        pw.println("    All " + getCaption() + "s (" + this.mEnabledServicesForCurrentProfiles.size() + ") enabled for current profiles:");
        Iterator<ComponentName> it = this.mEnabledServicesForCurrentProfiles.iterator();
        while (it.hasNext()) {
            ComponentName cmpt = it.next();
            if (filter == null || filter.matches(cmpt)) {
                pw.println("      " + cmpt);
            }
        }
        pw.println("    Live " + getCaption() + "s (" + this.mServices.size() + "):");
        synchronized (this.mMutex) {
            Iterator<ManagedServiceInfo> it2 = this.mServices.iterator();
            while (it2.hasNext()) {
                ManagedServiceInfo info = it2.next();
                if (filter == null || filter.matches(info.component)) {
                    pw.println("      " + info.component + " (user " + info.userid + "): " + info.service + (info.isSystem ? " SYSTEM" : "") + (info.isGuest(this) ? " GUEST" : ""));
                }
            }
        }
        pw.println("    Snoozed " + getCaption() + "s (" + this.mSnoozingForCurrentProfiles.size() + "):");
        Iterator<ComponentName> it3 = this.mSnoozingForCurrentProfiles.iterator();
        while (it3.hasNext()) {
            ComponentName name = it3.next();
            pw.println("      " + name.flattenToShortString());
        }
    }

    public void dump(ProtoOutputStream proto, NotificationManagerService.DumpFilter filter) {
        ArrayMap<Boolean, ArraySet<String>> approvedByType;
        int M;
        proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, getCaption());
        synchronized (this.mApproved) {
            int N = this.mApproved.size();
            for (int i = 0; i < N; i++) {
                int userId = this.mApproved.keyAt(i).intValue();
                ArrayMap<Boolean, ArraySet<String>> approvedByType2 = this.mApproved.valueAt(i);
                if (approvedByType2 != null) {
                    int M2 = approvedByType2.size();
                    int j = 0;
                    while (j < M2) {
                        boolean isPrimary = approvedByType2.keyAt(j).booleanValue();
                        ArraySet<String> approved = approvedByType2.valueAt(j);
                        if (approvedByType2 == null || approvedByType2.size() <= 0) {
                            approvedByType = approvedByType2;
                            M = M2;
                        } else {
                            long sToken = proto.start(2246267895810L);
                            Iterator<String> it = approved.iterator();
                            while (it.hasNext()) {
                                String s = it.next();
                                proto.write(2237677961217L, s);
                                approvedByType2 = approvedByType2;
                                M2 = M2;
                            }
                            approvedByType = approvedByType2;
                            M = M2;
                            proto.write(1120986464258L, userId);
                            proto.write(1133871366147L, isPrimary);
                            proto.end(sToken);
                        }
                        j++;
                        approvedByType2 = approvedByType;
                        M2 = M;
                    }
                }
            }
        }
        Iterator<ComponentName> it2 = this.mEnabledServicesForCurrentProfiles.iterator();
        while (it2.hasNext()) {
            ComponentName cmpt = it2.next();
            if (filter == null || filter.matches(cmpt)) {
                cmpt.dumpDebug(proto, 2246267895811L);
            }
        }
        synchronized (this.mMutex) {
            Iterator<ManagedServiceInfo> it3 = this.mServices.iterator();
            while (it3.hasNext()) {
                ManagedServiceInfo info = it3.next();
                if (filter == null || filter.matches(info.component)) {
                    info.dumpDebug(proto, 2246267895812L, this);
                }
            }
        }
        Iterator<ComponentName> it4 = this.mSnoozingForCurrentProfiles.iterator();
        while (it4.hasNext()) {
            ComponentName name = it4.next();
            name.dumpDebug(proto, 2246267895813L);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onSettingRestored(String element, String value, int backupSdkInt, int userId) {
        if (!this.mUseXml) {
            Slog.d(this.TAG, "Restored managed service setting: " + element);
            if (this.mConfig.secureSettingName.equals(element) || (this.mConfig.secondarySettingName != null && this.mConfig.secondarySettingName.equals(element))) {
                if (backupSdkInt < 26) {
                    String currentSetting = getApproved(userId, this.mConfig.secureSettingName.equals(element));
                    if (!TextUtils.isEmpty(currentSetting)) {
                        if (!TextUtils.isEmpty(value)) {
                            value = value + ENABLED_SERVICES_SEPARATOR + currentSetting;
                        } else {
                            value = currentSetting;
                        }
                    }
                }
                if (shouldReflectToSettings()) {
                    Settings.Secure.putStringForUser(this.mContext.getContentResolver(), element, value, userId);
                }
                for (UserInfo user : this.mUm.getUsers()) {
                    addApprovedList(value, user.id, this.mConfig.secureSettingName.equals(element));
                }
                Slog.d(this.TAG, "Done loading approved values from settings");
                rebindServices(false, userId);
            }
        }
    }

    void writeDefaults(TypedXmlSerializer out) throws IOException {
        synchronized (this.mDefaultsLock) {
            List<String> componentStrings = new ArrayList<>(this.mDefaultComponents.size());
            for (int i = 0; i < this.mDefaultComponents.size(); i++) {
                componentStrings.add(this.mDefaultComponents.valueAt(i).flattenToString());
            }
            String defaults = String.join(ENABLED_SERVICES_SEPARATOR, componentStrings);
            out.attribute((String) null, ATT_DEFAULTS, defaults);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:24:0x0089, code lost:
        r15 = "";
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void writeXml(TypedXmlSerializer out, boolean forBackup, int userId) throws IOException {
        int N;
        int N2;
        int i = userId;
        out.startTag((String) null, getConfig().xmlTag);
        out.attributeInt((String) null, ATT_VERSION, Integer.parseInt(DB_VERSION));
        writeDefaults(out);
        if (forBackup) {
            trimApprovedListsAccordingToInstalledServices(i);
        }
        synchronized (this.mApproved) {
            int N3 = this.mApproved.size();
            int i2 = 0;
            while (i2 < N3) {
                int approvedUserId = this.mApproved.keyAt(i2).intValue();
                if (forBackup && approvedUserId != i) {
                    N = N3;
                } else {
                    ArrayMap<Boolean, ArraySet<String>> approvedByType = this.mApproved.valueAt(i2);
                    Boolean isUserChanged = this.mIsUserChanged.get(Integer.valueOf(approvedUserId));
                    if (approvedByType == null) {
                        N = N3;
                    } else {
                        int M = approvedByType.size();
                        int j = 0;
                        while (j < M) {
                            boolean isPrimary = approvedByType.keyAt(j).booleanValue();
                            Set<String> approved = approvedByType.valueAt(j);
                            Set<String> userSet = this.mUserSetServices.get(Integer.valueOf(approvedUserId));
                            if (approved == null && userSet == null && isUserChanged == null) {
                                N2 = N3;
                                j++;
                                N3 = N2;
                            }
                            String allowedItems = String.join(ENABLED_SERVICES_SEPARATOR, approved);
                            N2 = N3;
                            out.startTag((String) null, TAG_MANAGED_SERVICES);
                            out.attribute((String) null, ATT_APPROVED_LIST, allowedItems);
                            out.attributeInt((String) null, ATT_USER_ID, approvedUserId);
                            out.attributeBoolean((String) null, ATT_IS_PRIMARY, isPrimary);
                            if (isUserChanged != null) {
                                out.attributeBoolean((String) null, ATT_USER_CHANGED, isUserChanged.booleanValue());
                            } else if (userSet != null) {
                                String userSetItems = String.join(ENABLED_SERVICES_SEPARATOR, userSet);
                                out.attribute((String) null, ATT_USER_SET, userSetItems);
                            }
                            writeExtraAttributes(out, approvedUserId);
                            out.endTag((String) null, TAG_MANAGED_SERVICES);
                            if (!forBackup && isPrimary && shouldReflectToSettings()) {
                                Settings.Secure.putStringForUser(this.mContext.getContentResolver(), getConfig().secureSettingName, allowedItems, approvedUserId);
                            }
                            j++;
                            N3 = N2;
                        }
                        N = N3;
                    }
                }
                i2++;
                i = userId;
                N3 = N;
            }
        }
        writeExtraXmlTags(out);
        out.endTag((String) null, getConfig().xmlTag);
    }

    protected boolean shouldReflectToSettings() {
        return false;
    }

    protected void writeExtraAttributes(TypedXmlSerializer out, int userId) throws IOException {
    }

    protected void writeExtraXmlTags(TypedXmlSerializer out) throws IOException {
    }

    protected void readExtraTag(String tag, TypedXmlPullParser parser) throws IOException, XmlPullParserException {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void migrateToXml() {
        for (UserInfo user : this.mUm.getUsers()) {
            ContentResolver cr = this.mContext.getContentResolver();
            if (!TextUtils.isEmpty(getConfig().secureSettingName)) {
                addApprovedList(Settings.Secure.getStringForUser(cr, getConfig().secureSettingName, user.id), user.id, true);
            }
            if (!TextUtils.isEmpty(getConfig().secondarySettingName)) {
                addApprovedList(Settings.Secure.getStringForUser(cr, getConfig().secondarySettingName, user.id), user.id, false);
            }
        }
    }

    void readDefaults(TypedXmlPullParser parser) {
        String defaultComponents = XmlUtils.readStringAttribute(parser, ATT_DEFAULTS);
        if (!TextUtils.isEmpty(defaultComponents)) {
            String[] components = defaultComponents.split(ENABLED_SERVICES_SEPARATOR);
            synchronized (this.mDefaultsLock) {
                for (int i = 0; i < components.length; i++) {
                    if (!TextUtils.isEmpty(components[i])) {
                        ComponentName cn = ComponentName.unflattenFromString(components[i]);
                        if (cn != null) {
                            this.mDefaultPackages.add(cn.getPackageName());
                            this.mDefaultComponents.add(cn);
                        } else {
                            this.mDefaultPackages.add(components[i]);
                        }
                    }
                }
            }
        }
    }

    public void readXml(TypedXmlPullParser parser, TriPredicate<String, Integer, String> allowedManagedServicePackages, boolean forRestore, int userId) throws XmlPullParserException, IOException {
        boolean z;
        int resolvedUserId;
        String version = XmlUtils.readStringAttribute(parser, ATT_VERSION);
        boolean needUpgradeUserset = false;
        readDefaults(parser);
        while (true) {
            int type = parser.next();
            z = true;
            if (type == 1) {
                break;
            }
            String tag = parser.getName();
            if (type == 3 && getConfig().xmlTag.equals(tag)) {
                break;
            } else if (type == 2) {
                if (TAG_MANAGED_SERVICES.equals(tag)) {
                    Slog.i(this.TAG, "Read " + this.mConfig.caption + " permissions from xml");
                    String approved = XmlUtils.readStringAttribute(parser, ATT_APPROVED_LIST);
                    if (!forRestore) {
                        resolvedUserId = parser.getAttributeInt((String) null, ATT_USER_ID, 0);
                    } else {
                        resolvedUserId = userId;
                    }
                    boolean isPrimary = parser.getAttributeBoolean((String) null, ATT_IS_PRIMARY, true);
                    String isUserChanged = XmlUtils.readStringAttribute(parser, ATT_USER_CHANGED);
                    String isUserChanged_Old = XmlUtils.readStringAttribute(parser, ATT_USER_SET_OLD);
                    String userSetComponent = XmlUtils.readStringAttribute(parser, ATT_USER_SET);
                    if (DB_VERSION.equals(version)) {
                        if (isUserChanged == null) {
                            userSetComponent = TextUtils.emptyIfNull(userSetComponent);
                        } else {
                            this.mIsUserChanged.put(Integer.valueOf(resolvedUserId), Boolean.valueOf(isUserChanged));
                            userSetComponent = Boolean.valueOf(isUserChanged).booleanValue() ? approved : "";
                        }
                    } else {
                        needUpgradeUserset = true;
                        if (userSetComponent == null) {
                            if (isUserChanged_Old != null && Boolean.valueOf(isUserChanged_Old).booleanValue()) {
                                userSetComponent = approved;
                                this.mIsUserChanged.put(Integer.valueOf(resolvedUserId), true);
                                needUpgradeUserset = false;
                            } else {
                                userSetComponent = "";
                                needUpgradeUserset = true;
                            }
                        }
                    }
                    readExtraAttributes(tag, parser, resolvedUserId);
                    if (allowedManagedServicePackages == null || allowedManagedServicePackages.test(getPackageName(approved), Integer.valueOf(resolvedUserId), getRequiredPermission()) || approved.isEmpty()) {
                        if (this.mUm.getUserInfo(resolvedUserId) != null) {
                            addApprovedList(approved, resolvedUserId, isPrimary, userSetComponent);
                        }
                        this.mUseXml = true;
                    }
                } else {
                    readExtraTag(tag, parser);
                }
            }
        }
        if (!TextUtils.isEmpty(version) && !DB_VERSION_1.equals(version) && !DB_VERSION_2.equals(version) && !DB_VERSION_3.equals(version)) {
            z = false;
        }
        boolean isOldVersion = z;
        if (isOldVersion) {
            upgradeDefaultsXmlVersion();
        }
        if (needUpgradeUserset) {
            upgradeUserSet();
        }
        rebindServices(false, -1);
    }

    void upgradeDefaultsXmlVersion() {
        int defaultsSize = this.mDefaultComponents.size() + this.mDefaultPackages.size();
        if (defaultsSize == 0) {
            if (this.mApprovalLevel == 1) {
                List<ComponentName> approvedComponents = getAllowedComponents(0);
                for (int i = 0; i < approvedComponents.size(); i++) {
                    addDefaultComponentOrPackage(approvedComponents.get(i).flattenToString());
                }
            }
            if (this.mApprovalLevel == 0) {
                List<String> approvedPkgs = getAllowedPackages(0);
                for (int i2 = 0; i2 < approvedPkgs.size(); i2++) {
                    addDefaultComponentOrPackage(approvedPkgs.get(i2));
                }
            }
        }
        int defaultsSize2 = this.mDefaultComponents.size() + this.mDefaultPackages.size();
        if (defaultsSize2 == 0) {
            loadDefaultsFromConfig();
        }
    }

    protected void upgradeUserSet() {
    }

    protected void readExtraAttributes(String tag, TypedXmlPullParser parser, int userId) throws IOException {
    }

    protected void addApprovedList(String approved, int userId, boolean isPrimary) {
        addApprovedList(approved, userId, isPrimary, approved);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addApprovedList(String approved, int userId, boolean isPrimary, String userSet) {
        if (TextUtils.isEmpty(approved)) {
            approved = "";
        }
        if (userSet == null) {
            userSet = approved;
        }
        synchronized (this.mApproved) {
            ArrayMap<Boolean, ArraySet<String>> approvedByType = this.mApproved.get(Integer.valueOf(userId));
            if (approvedByType == null) {
                approvedByType = new ArrayMap<>();
                this.mApproved.put(Integer.valueOf(userId), approvedByType);
            }
            ArraySet<String> approvedList = approvedByType.get(Boolean.valueOf(isPrimary));
            if (approvedList == null) {
                approvedList = new ArraySet<>();
                approvedByType.put(Boolean.valueOf(isPrimary), approvedList);
            }
            String[] approvedArray = approved.split(ENABLED_SERVICES_SEPARATOR);
            for (String pkgOrComponent : approvedArray) {
                String approvedItem = getApprovedValue(pkgOrComponent);
                if (approvedItem != null) {
                    approvedList.add(approvedItem);
                }
            }
            ArraySet<String> userSetList = this.mUserSetServices.get(Integer.valueOf(userId));
            if (userSetList == null) {
                userSetList = new ArraySet<>();
                this.mUserSetServices.put(Integer.valueOf(userId), userSetList);
            }
            String[] userSetArray = userSet.split(ENABLED_SERVICES_SEPARATOR);
            for (String pkgOrComponent2 : userSetArray) {
                String approvedItem2 = getApprovedValue(pkgOrComponent2);
                if (approvedItem2 != null) {
                    userSetList.add(approvedItem2);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isComponentEnabledForPackage(String pkg) {
        return this.mEnabledServicesPackageNames.contains(pkg);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setPackageOrComponentEnabled(String pkgOrComponent, int userId, boolean isPrimary, boolean enabled) {
        setPackageOrComponentEnabled(pkgOrComponent, userId, isPrimary, enabled, true);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setPackageOrComponentEnabled(String pkgOrComponent, int userId, boolean isPrimary, boolean enabled, boolean userSet) {
        Slog.i(this.TAG, (enabled ? " Allowing " : "Disallowing ") + this.mConfig.caption + " " + pkgOrComponent + " (userSet: " + userSet + ")");
        synchronized (this.mApproved) {
            ArrayMap<Boolean, ArraySet<String>> allowedByType = this.mApproved.get(Integer.valueOf(userId));
            if (allowedByType == null) {
                allowedByType = new ArrayMap<>();
                this.mApproved.put(Integer.valueOf(userId), allowedByType);
            }
            ArraySet<String> approved = allowedByType.get(Boolean.valueOf(isPrimary));
            if (approved == null) {
                approved = new ArraySet<>();
                allowedByType.put(Boolean.valueOf(isPrimary), approved);
            }
            String approvedItem = getApprovedValue(pkgOrComponent);
            if (approvedItem != null) {
                if (enabled) {
                    approved.add(approvedItem);
                } else {
                    approved.remove(approvedItem);
                }
            }
            ArraySet<String> userSetServices = this.mUserSetServices.get(Integer.valueOf(userId));
            if (userSetServices == null) {
                userSetServices = new ArraySet<>();
                this.mUserSetServices.put(Integer.valueOf(userId), userSetServices);
            }
            if (userSet) {
                userSetServices.add(pkgOrComponent);
            } else {
                userSetServices.remove(pkgOrComponent);
            }
        }
        rebindServices(false, userId);
    }

    private String getApprovedValue(String pkgOrComponent) {
        if (this.mApprovalLevel == 1) {
            if (ComponentName.unflattenFromString(pkgOrComponent) != null) {
                return pkgOrComponent;
            }
            return null;
        }
        return getPackageName(pkgOrComponent);
    }

    protected String getApproved(int userId, boolean primary) {
        String join;
        synchronized (this.mApproved) {
            ArrayMap<Boolean, ArraySet<String>> allowedByType = this.mApproved.getOrDefault(Integer.valueOf(userId), new ArrayMap<>());
            ArraySet<String> approved = allowedByType.getOrDefault(Boolean.valueOf(primary), new ArraySet<>());
            join = String.join(ENABLED_SERVICES_SEPARATOR, approved);
        }
        return join;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<ComponentName> getAllowedComponents(int userId) {
        List<ComponentName> allowedComponents = new ArrayList<>();
        synchronized (this.mApproved) {
            ArrayMap<Boolean, ArraySet<String>> allowedByType = this.mApproved.getOrDefault(Integer.valueOf(userId), new ArrayMap<>());
            for (int i = 0; i < allowedByType.size(); i++) {
                ArraySet<String> allowed = allowedByType.valueAt(i);
                for (int j = 0; j < allowed.size(); j++) {
                    ComponentName cn = ComponentName.unflattenFromString(allowed.valueAt(j));
                    if (cn != null) {
                        allowedComponents.add(cn);
                    }
                }
            }
        }
        return allowedComponents;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<String> getAllowedPackages(int userId) {
        List<String> allowedPackages = new ArrayList<>();
        synchronized (this.mApproved) {
            ArrayMap<Boolean, ArraySet<String>> allowedByType = this.mApproved.getOrDefault(Integer.valueOf(userId), new ArrayMap<>());
            for (int i = 0; i < allowedByType.size(); i++) {
                ArraySet<String> allowed = allowedByType.valueAt(i);
                for (int j = 0; j < allowed.size(); j++) {
                    String pkgName = getPackageName(allowed.valueAt(j));
                    if (!TextUtils.isEmpty(pkgName)) {
                        allowedPackages.add(pkgName);
                    }
                }
            }
        }
        return allowedPackages;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isPackageOrComponentAllowed(String pkgOrComponent, int userId) {
        synchronized (this.mApproved) {
            ArrayMap<Boolean, ArraySet<String>> allowedByType = this.mApproved.getOrDefault(Integer.valueOf(userId), new ArrayMap<>());
            for (int i = 0; i < allowedByType.size(); i++) {
                ArraySet<String> allowed = allowedByType.valueAt(i);
                if (allowed.contains(pkgOrComponent)) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPackageOrComponentUserSet(String pkgOrComponent, int userId) {
        boolean z;
        synchronized (this.mApproved) {
            ArraySet<String> services = this.mUserSetServices.get(Integer.valueOf(userId));
            z = services != null && services.contains(pkgOrComponent);
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isPackageAllowed(String pkg, int userId) {
        if (pkg == null) {
            return false;
        }
        synchronized (this.mApproved) {
            ArrayMap<Boolean, ArraySet<String>> allowedByType = this.mApproved.getOrDefault(Integer.valueOf(userId), new ArrayMap<>());
            for (int i = 0; i < allowedByType.size(); i++) {
                ArraySet<String> allowed = allowedByType.valueAt(i);
                Iterator<String> it = allowed.iterator();
                while (it.hasNext()) {
                    String allowedEntry = it.next();
                    ComponentName component = ComponentName.unflattenFromString(allowedEntry);
                    if (component != null) {
                        if (pkg.equals(component.getPackageName())) {
                            return true;
                        }
                    } else if (pkg.equals(allowedEntry)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public void onPackagesChanged(boolean removingPackage, String[] pkgList, int[] uidList) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "onPackagesChanged removingPackage=" + removingPackage + " pkgList=" + (pkgList == null ? null : Arrays.asList(pkgList)) + " mEnabledServicesPackageNames=" + this.mEnabledServicesPackageNames);
        }
        if (pkgList != null && pkgList.length > 0) {
            boolean anyServicesInvolved = false;
            if (removingPackage && uidList != null) {
                int size = Math.min(pkgList.length, uidList.length);
                for (int i = 0; i < size; i++) {
                    String pkg = pkgList[i];
                    int userId = UserHandle.getUserId(uidList[i]);
                    anyServicesInvolved = removeUninstalledItemsFromApprovedLists(userId, pkg);
                }
            }
            for (String pkgName : pkgList) {
                if (this.mEnabledServicesPackageNames.contains(pkgName)) {
                    anyServicesInvolved = true;
                }
                if (uidList != null && uidList.length > 0) {
                    for (int uid : uidList) {
                        if (isPackageAllowed(pkgName, UserHandle.getUserId(uid))) {
                            anyServicesInvolved = true;
                        }
                    }
                }
            }
            if (anyServicesInvolved) {
                rebindServices(false, -1);
            }
        }
    }

    public void onUserRemoved(int user) {
        Slog.i(this.TAG, "Removing approved services for removed user " + user);
        synchronized (this.mApproved) {
            this.mApproved.remove(Integer.valueOf(user));
        }
        rebindServices(true, user);
    }

    public void onUserSwitched(int user) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "onUserSwitched u=" + user);
        }
        unbindOtherUserServices(user);
        rebindServices(true, user);
    }

    public void onUserUnlocked(int user) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "onUserUnlocked u=" + user);
        }
        rebindServices(false, user);
    }

    private ManagedServiceInfo getServiceFromTokenLocked(IInterface service) {
        if (service == null) {
            return null;
        }
        IBinder token = service.asBinder();
        int N = this.mServices.size();
        for (int i = 0; i < N; i++) {
            ManagedServiceInfo info = this.mServices.get(i);
            if (info.service.asBinder() == token) {
                return info;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isServiceTokenValidLocked(IInterface service) {
        if (service == null) {
            return false;
        }
        ManagedServiceInfo info = getServiceFromTokenLocked(service);
        if (info == null) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public ManagedServiceInfo checkServiceTokenLocked(IInterface service) {
        checkNotNull(service);
        ManagedServiceInfo info = getServiceFromTokenLocked(service);
        if (info != null) {
            return info;
        }
        throw new SecurityException("Disallowed call from unknown " + getCaption() + ": " + service + " " + service.getClass());
    }

    public boolean isSameUser(IInterface service, int userId) {
        checkNotNull(service);
        synchronized (this.mMutex) {
            ManagedServiceInfo info = getServiceFromTokenLocked(service);
            if (info != null) {
                return info.isSameUser(userId);
            }
            return false;
        }
    }

    public void unregisterService(IInterface service, int userid) {
        checkNotNull(service);
        unregisterServiceImpl(service, userid);
    }

    public void registerSystemService(IInterface service, ComponentName component, int userid, int uid) {
        checkNotNull(service);
        ManagedServiceInfo info = registerServiceImpl(service, component, userid, 10000, uid);
        if (info != null) {
            onServiceAdded(info);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void registerGuestService(ManagedServiceInfo guest) {
        checkNotNull(guest.service);
        if (!checkType(guest.service)) {
            throw new IllegalArgumentException();
        }
        if (registerServiceImpl(guest) != null) {
            onServiceAdded(guest);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setComponentState(ComponentName component, int userId, boolean enabled) {
        boolean previous = !this.mSnoozingForCurrentProfiles.contains(component);
        if (previous == enabled) {
            return;
        }
        if (enabled) {
            this.mSnoozingForCurrentProfiles.remove(component);
        } else {
            this.mSnoozingForCurrentProfiles.add(component);
        }
        Slog.d(this.TAG, (enabled ? "Enabling " : "Disabling ") + "component " + component.flattenToShortString());
        synchronized (this.mMutex) {
            if (enabled) {
                if (!isPackageOrComponentAllowed(component.flattenToString(), userId) && !isPackageOrComponentAllowed(component.getPackageName(), userId)) {
                    Slog.d(this.TAG, component + " no longer has permission to be bound");
                }
                registerServiceLocked(component, userId);
            } else {
                unregisterServiceLocked(component, userId);
            }
        }
    }

    private ArraySet<ComponentName> loadComponentNamesFromValues(ArraySet<String> approved, int userId) {
        if (approved == null || approved.size() == 0) {
            return new ArraySet<>();
        }
        ArraySet<ComponentName> result = new ArraySet<>(approved.size());
        for (int i = 0; i < approved.size(); i++) {
            String packageOrComponent = approved.valueAt(i);
            if (!TextUtils.isEmpty(packageOrComponent)) {
                ComponentName component = ComponentName.unflattenFromString(packageOrComponent);
                if (component != null) {
                    result.add(component);
                } else {
                    result.addAll(queryPackageForServices(packageOrComponent, userId));
                }
            }
        }
        return result;
    }

    protected Set<ComponentName> queryPackageForServices(String packageName, int userId) {
        return queryPackageForServices(packageName, 0, userId);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public ArraySet<ComponentName> queryPackageForServices(String packageName, int extraFlags, int userId) {
        ArraySet<ComponentName> installed = new ArraySet<>();
        PackageManager pm = this.mContext.getPackageManager();
        Intent queryIntent = new Intent(this.mConfig.serviceInterface);
        if (!TextUtils.isEmpty(packageName)) {
            queryIntent.setPackage(packageName);
        }
        List<ResolveInfo> installedServices = pm.queryIntentServicesAsUser(queryIntent, extraFlags | 132, userId);
        if (this.DEBUG) {
            Slog.v(this.TAG, this.mConfig.serviceInterface + " services: " + installedServices);
        }
        if (installedServices != null) {
            int count = installedServices.size();
            for (int i = 0; i < count; i++) {
                ResolveInfo resolveInfo = installedServices.get(i);
                ServiceInfo info = resolveInfo.serviceInfo;
                ComponentName component = new ComponentName(info.packageName, info.name);
                if (!this.mConfig.bindPermission.equals(info.permission)) {
                    Slog.w(this.TAG, "Skipping " + getCaption() + " service " + info.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + info.name + ": it does not require the permission " + this.mConfig.bindPermission);
                } else {
                    installed.add(component);
                }
            }
        }
        return installed;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Set<String> getAllowedPackages() {
        Set<String> allowedPackages = new ArraySet<>();
        synchronized (this.mApproved) {
            for (int k = 0; k < this.mApproved.size(); k++) {
                ArrayMap<Boolean, ArraySet<String>> allowedByType = this.mApproved.valueAt(k);
                for (int i = 0; i < allowedByType.size(); i++) {
                    ArraySet<String> allowed = allowedByType.valueAt(i);
                    for (int j = 0; j < allowed.size(); j++) {
                        String pkgName = getPackageName(allowed.valueAt(j));
                        if (!TextUtils.isEmpty(pkgName)) {
                            allowedPackages.add(pkgName);
                        }
                    }
                }
            }
        }
        return allowedPackages;
    }

    private void trimApprovedListsAccordingToInstalledServices(int userId) {
        synchronized (this.mApproved) {
            ArrayMap<Boolean, ArraySet<String>> approvedByType = this.mApproved.get(Integer.valueOf(userId));
            if (approvedByType == null) {
                return;
            }
            for (int i = 0; i < approvedByType.size(); i++) {
                ArraySet<String> approved = approvedByType.valueAt(i);
                for (int j = approved.size() - 1; j >= 0; j--) {
                    String approvedPackageOrComponent = approved.valueAt(j);
                    if (!isValidEntry(approvedPackageOrComponent, userId)) {
                        approved.removeAt(j);
                        Slog.v(this.TAG, "Removing " + approvedPackageOrComponent + " from approved list; no matching services found");
                    } else if (this.DEBUG) {
                        Slog.v(this.TAG, "Keeping " + approvedPackageOrComponent + " on approved list; matching services found");
                    }
                }
            }
        }
    }

    private boolean removeUninstalledItemsFromApprovedLists(int uninstalledUserId, String pkg) {
        synchronized (this.mApproved) {
            ArrayMap<Boolean, ArraySet<String>> approvedByType = this.mApproved.get(Integer.valueOf(uninstalledUserId));
            if (approvedByType != null) {
                int M = approvedByType.size();
                for (int j = 0; j < M; j++) {
                    ArraySet<String> approved = approvedByType.valueAt(j);
                    int O = approved.size();
                    for (int k = O - 1; k >= 0; k--) {
                        String packageOrComponent = approved.valueAt(k);
                        String packageName = getPackageName(packageOrComponent);
                        if (TextUtils.equals(pkg, packageName)) {
                            approved.removeAt(k);
                            if (this.DEBUG) {
                                Slog.v(this.TAG, "Removing " + packageOrComponent + " from approved list; uninstalled");
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    protected String getPackageName(String packageOrComponent) {
        ComponentName component = ComponentName.unflattenFromString(packageOrComponent);
        if (component != null) {
            return component.getPackageName();
        }
        return packageOrComponent;
    }

    protected boolean isValidEntry(String packageOrComponent, int userId) {
        return hasMatchingServices(packageOrComponent, userId);
    }

    private boolean hasMatchingServices(String packageOrComponent, int userId) {
        if (TextUtils.isEmpty(packageOrComponent)) {
            return false;
        }
        String packageName = getPackageName(packageOrComponent);
        return queryPackageForServices(packageName, userId).size() > 0;
    }

    protected SparseArray<ArraySet<ComponentName>> getAllowedComponents(IntArray userIds) {
        int nUserIds = userIds.size();
        SparseArray<ArraySet<ComponentName>> componentsByUser = new SparseArray<>();
        for (int i = 0; i < nUserIds; i++) {
            int userId = userIds.get(i);
            synchronized (this.mApproved) {
                ArrayMap<Boolean, ArraySet<String>> approvedLists = this.mApproved.get(Integer.valueOf(userId));
                if (approvedLists != null) {
                    int N = approvedLists.size();
                    for (int j = 0; j < N; j++) {
                        ArraySet<ComponentName> approvedByUser = componentsByUser.get(userId);
                        if (approvedByUser == null) {
                            approvedByUser = new ArraySet<>();
                            componentsByUser.put(userId, approvedByUser);
                        }
                        approvedByUser.addAll((ArraySet<? extends ComponentName>) loadComponentNamesFromValues(approvedLists.valueAt(j), userId));
                    }
                }
            }
        }
        return componentsByUser;
    }

    protected void populateComponentsToBind(SparseArray<Set<ComponentName>> componentsToBind, IntArray activeUsers, SparseArray<ArraySet<ComponentName>> approvedComponentsByUser) {
        this.mEnabledServicesForCurrentProfiles.clear();
        this.mEnabledServicesPackageNames.clear();
        int nUserIds = activeUsers.size();
        for (int i = 0; i < nUserIds; i++) {
            int userId = activeUsers.get(i);
            ArraySet<ComponentName> userComponents = approvedComponentsByUser.get(userId);
            if (userComponents == null) {
                componentsToBind.put(userId, new ArraySet());
            } else {
                Set<ComponentName> add = new HashSet<>(userComponents);
                add.removeAll(this.mSnoozingForCurrentProfiles);
                componentsToBind.put(userId, add);
                this.mEnabledServicesForCurrentProfiles.addAll((ArraySet<? extends ComponentName>) userComponents);
                for (int j = 0; j < userComponents.size(); j++) {
                    ComponentName component = userComponents.valueAt(j);
                    this.mEnabledServicesPackageNames.add(component.getPackageName());
                }
            }
        }
    }

    protected Set<ManagedServiceInfo> getRemovableConnectedServices() {
        Set<ManagedServiceInfo> removableBoundServices = new ArraySet<>();
        Iterator<ManagedServiceInfo> it = this.mServices.iterator();
        while (it.hasNext()) {
            ManagedServiceInfo service = it.next();
            if (!service.isSystem && !service.isGuest(this)) {
                removableBoundServices.add(service);
            }
        }
        return removableBoundServices;
    }

    protected void populateComponentsToUnbind(boolean forceRebind, Set<ManagedServiceInfo> removableBoundServices, SparseArray<Set<ComponentName>> allowedComponentsToBind, SparseArray<Set<ComponentName>> componentsToUnbind) {
        for (ManagedServiceInfo info : removableBoundServices) {
            Set<ComponentName> allowedComponents = allowedComponentsToBind.get(info.userid);
            if (allowedComponents != null && (forceRebind || !allowedComponents.contains(info.component))) {
                Set<ComponentName> toUnbind = componentsToUnbind.get(info.userid, new ArraySet());
                toUnbind.add(info.component);
                componentsToUnbind.put(info.userid, toUnbind);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void rebindServices(boolean forceRebind, int userToRebind) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "rebindServices " + forceRebind + " " + userToRebind);
        }
        IntArray userIds = this.mUserProfiles.getCurrentProfileIds();
        if (userToRebind != -1) {
            userIds = new IntArray(1);
            userIds.add(userToRebind);
        }
        SparseArray<Set<ComponentName>> componentsToBind = new SparseArray<>();
        SparseArray<Set<ComponentName>> componentsToUnbind = new SparseArray<>();
        synchronized (this.mMutex) {
            SparseArray<ArraySet<ComponentName>> approvedComponentsByUser = getAllowedComponents(userIds);
            Set<ManagedServiceInfo> removableBoundServices = getRemovableConnectedServices();
            populateComponentsToBind(componentsToBind, userIds, approvedComponentsByUser);
            populateComponentsToUnbind(forceRebind, removableBoundServices, componentsToBind, componentsToUnbind);
        }
        unbindFromServices(componentsToUnbind);
        bindToServices(componentsToBind);
    }

    void unbindOtherUserServices(int currentUser) {
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        t.traceBegin("ManagedServices.unbindOtherUserServices_current" + currentUser);
        SparseArray<Set<ComponentName>> componentsToUnbind = new SparseArray<>();
        synchronized (this.mMutex) {
            Set<ManagedServiceInfo> removableBoundServices = getRemovableConnectedServices();
            for (ManagedServiceInfo info : removableBoundServices) {
                if (info.userid != currentUser) {
                    Set<ComponentName> toUnbind = componentsToUnbind.get(info.userid, new ArraySet());
                    toUnbind.add(info.component);
                    componentsToUnbind.put(info.userid, toUnbind);
                }
            }
        }
        unbindFromServices(componentsToUnbind);
        t.traceEnd();
    }

    protected void unbindFromServices(SparseArray<Set<ComponentName>> componentsToUnbind) {
        for (int i = 0; i < componentsToUnbind.size(); i++) {
            int userId = componentsToUnbind.keyAt(i);
            Set<ComponentName> removableComponents = componentsToUnbind.get(userId);
            for (ComponentName cn : removableComponents) {
                Slog.v(this.TAG, "disabling " + getCaption() + " for user " + userId + ": " + cn);
                unregisterService(cn, userId);
            }
        }
    }

    private void bindToServices(SparseArray<Set<ComponentName>> componentsToBind) {
        for (int i = 0; i < componentsToBind.size(); i++) {
            int userId = componentsToBind.keyAt(i);
            Set<ComponentName> add = componentsToBind.get(userId);
            for (ComponentName component : add) {
                try {
                    ServiceInfo info = this.mPm.getServiceInfo(component, 786560L, userId);
                    if (info == null) {
                        Slog.w(this.TAG, "Not binding " + getCaption() + " service " + component + ": service not found");
                    } else if (!this.mConfig.bindPermission.equals(info.permission)) {
                        Slog.w(this.TAG, "Not binding " + getCaption() + " service " + component + ": it does not require the permission " + this.mConfig.bindPermission);
                    } else {
                        Slog.v(this.TAG, "enabling " + getCaption() + " for " + userId + ": " + component);
                        registerService(info, userId);
                    }
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
            }
        }
    }

    void registerService(ServiceInfo si, int userId) {
        ensureFilters(si, userId);
        registerService(si.getComponentName(), userId);
    }

    void registerService(ComponentName cn, int userId) {
        synchronized (this.mMutex) {
            registerServiceLocked(cn, userId);
        }
    }

    public void registerSystemService(ComponentName name, int userid) {
        synchronized (this.mMutex) {
            registerServiceLocked(name, userid, true);
        }
    }

    private void registerServiceLocked(ComponentName name, int userid) {
        registerServiceLocked(name, userid, false);
    }

    private void registerServiceLocked(ComponentName name, final int userid, final boolean isSystem) {
        ApplicationInfo appInfo;
        if (this.DEBUG) {
            Slog.v(this.TAG, "registerService: " + name + " u=" + userid);
        }
        final Pair<ComponentName, Integer> servicesBindingTag = Pair.create(name, Integer.valueOf(userid));
        if (this.mServicesBound.contains(servicesBindingTag)) {
            Slog.v(this.TAG, "Not registering " + name + " is already bound");
            return;
        }
        this.mServicesBound.add(servicesBindingTag);
        int N = this.mServices.size();
        for (int i = N - 1; i >= 0; i--) {
            ManagedServiceInfo info = this.mServices.get(i);
            if (name.equals(info.component) && info.userid == userid) {
                Slog.v(this.TAG, "    disconnecting old " + getCaption() + ": " + info.service);
                removeServiceLocked(i);
                if (info.connection != null) {
                    unbindService(info.connection, info.component, info.userid);
                }
            }
        }
        Intent intent = new Intent(this.mConfig.serviceInterface);
        intent.setComponent(name);
        intent.putExtra("android.intent.extra.client_label", this.mConfig.clientLabel);
        PendingIntent pendingIntent = PendingIntent.getActivity(this.mContext, 0, new Intent(this.mConfig.settingsAction), 67108864);
        intent.putExtra("android.intent.extra.client_intent", pendingIntent);
        try {
            ApplicationInfo appInfo2 = this.mContext.getPackageManager().getApplicationInfo(name.getPackageName(), 0);
            appInfo = appInfo2;
        } catch (PackageManager.NameNotFoundException e) {
            appInfo = null;
        }
        final int targetSdkVersion = appInfo != null ? appInfo.targetSdkVersion : 1;
        final int uid = appInfo != null ? appInfo.uid : -1;
        try {
            Slog.v(this.TAG, "binding: " + intent);
            try {
                ServiceConnection serviceConnection = new ServiceConnection() { // from class: com.android.server.notification.ManagedServices.1
                    IInterface mService;

                    @Override // android.content.ServiceConnection
                    public void onServiceConnected(ComponentName name2, IBinder binder) {
                        Slog.v(ManagedServices.this.TAG, userid + " " + ManagedServices.this.getCaption() + " service connected: " + name2);
                        boolean added = false;
                        ManagedServiceInfo info2 = null;
                        synchronized (ManagedServices.this.mMutex) {
                            ManagedServices.this.mServicesRebinding.remove(servicesBindingTag);
                            try {
                                IInterface asInterface = ManagedServices.this.asInterface(binder);
                                this.mService = asInterface;
                                info2 = ManagedServices.this.newServiceInfo(asInterface, name2, userid, isSystem, this, targetSdkVersion, uid);
                                binder.linkToDeath(info2, 0);
                                added = ManagedServices.this.mServices.add(info2);
                            } catch (RemoteException e2) {
                                Slog.e(ManagedServices.this.TAG, "Failed to linkToDeath, already dead", e2);
                            }
                        }
                        if (added) {
                            ManagedServices.this.onServiceAdded(info2);
                        }
                    }

                    @Override // android.content.ServiceConnection
                    public void onServiceDisconnected(ComponentName name2) {
                        Slog.v(ManagedServices.this.TAG, userid + " " + ManagedServices.this.getCaption() + " connection lost: " + name2);
                    }

                    @Override // android.content.ServiceConnection
                    public void onBindingDied(final ComponentName name2) {
                        Slog.w(ManagedServices.this.TAG, userid + " " + ManagedServices.this.getCaption() + " binding died: " + name2);
                        synchronized (ManagedServices.this.mMutex) {
                            ManagedServices.this.unbindService(this, name2, userid);
                            if (!ManagedServices.this.mServicesRebinding.contains(servicesBindingTag)) {
                                ManagedServices.this.mServicesRebinding.add(servicesBindingTag);
                                ManagedServices.this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.notification.ManagedServices.1.1
                                    @Override // java.lang.Runnable
                                    public void run() {
                                        ManagedServices.this.registerService(name2, userid);
                                    }
                                }, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                            } else {
                                Slog.v(ManagedServices.this.TAG, ManagedServices.this.getCaption() + " not rebinding in user " + userid + " as a previous rebind attempt was made: " + name2);
                            }
                        }
                    }

                    @Override // android.content.ServiceConnection
                    public void onNullBinding(ComponentName name2) {
                        Slog.v(ManagedServices.this.TAG, "onNullBinding() called with: name = [" + name2 + "]");
                        ManagedServices.this.mContext.unbindService(this);
                    }
                };
                if (!this.mContext.bindServiceAsUser(intent, serviceConnection, getBindFlags(), new UserHandle(userid))) {
                    this.mServicesBound.remove(servicesBindingTag);
                    Slog.w(this.TAG, "Unable to bind " + getCaption() + " service: " + intent + " in user " + userid);
                }
            } catch (SecurityException e2) {
                ex = e2;
                this.mServicesBound.remove(servicesBindingTag);
                Slog.e(this.TAG, "Unable to bind " + getCaption() + " service: " + intent, ex);
            }
        } catch (SecurityException e3) {
            ex = e3;
        }
    }

    boolean isBound(ComponentName cn, int userId) {
        Pair<ComponentName, Integer> servicesBindingTag = Pair.create(cn, Integer.valueOf(userId));
        return this.mServicesBound.contains(servicesBindingTag);
    }

    private void unregisterService(ComponentName name, int userid) {
        synchronized (this.mMutex) {
            unregisterServiceLocked(name, userid);
        }
    }

    private void unregisterServiceLocked(ComponentName name, int userid) {
        int N = this.mServices.size();
        for (int i = N - 1; i >= 0; i--) {
            ManagedServiceInfo info = this.mServices.get(i);
            if (name.equals(info.component) && info.userid == userid) {
                removeServiceLocked(i);
                if (info.connection != null) {
                    unbindService(info.connection, info.component, info.userid);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ManagedServiceInfo removeServiceImpl(IInterface service, int userid) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "removeServiceImpl service=" + service + " u=" + userid);
        }
        ManagedServiceInfo serviceInfo = null;
        synchronized (this.mMutex) {
            int N = this.mServices.size();
            for (int i = N - 1; i >= 0; i--) {
                ManagedServiceInfo info = this.mServices.get(i);
                if (info.service.asBinder() == service.asBinder() && info.userid == userid) {
                    Slog.d(this.TAG, "Removing active service " + info.component);
                    serviceInfo = removeServiceLocked(i);
                }
            }
        }
        return serviceInfo;
    }

    private ManagedServiceInfo removeServiceLocked(int i) {
        ManagedServiceInfo info = this.mServices.remove(i);
        onServiceRemovedLocked(info);
        return info;
    }

    private void checkNotNull(IInterface service) {
        if (service == null) {
            throw new IllegalArgumentException(getCaption() + " must not be null");
        }
    }

    private ManagedServiceInfo registerServiceImpl(IInterface service, ComponentName component, int userid, int targetSdk, int uid) {
        ManagedServiceInfo info = newServiceInfo(service, component, userid, true, null, targetSdk, uid);
        return registerServiceImpl(info);
    }

    private ManagedServiceInfo registerServiceImpl(ManagedServiceInfo info) {
        synchronized (this.mMutex) {
            try {
                try {
                    info.service.asBinder().linkToDeath(info, 0);
                    this.mServices.add(info);
                } catch (RemoteException e) {
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return info;
    }

    private void unregisterServiceImpl(IInterface service, int userid) {
        ManagedServiceInfo info = removeServiceImpl(service, userid);
        if (info != null && info.connection != null && !info.isGuest(this)) {
            unbindService(info.connection, info.component, info.userid);
        }
        try {
            info.service.asBinder().unlinkToDeath(info, 0);
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unbindService(ServiceConnection connection, ComponentName component, int userId) {
        try {
            this.mContext.unbindService(connection);
        } catch (IllegalArgumentException e) {
            Slog.e(this.TAG, getCaption() + " " + component + " could not be unbound", e);
        }
        synchronized (this.mMutex) {
            this.mServicesBound.remove(Pair.create(component, Integer.valueOf(userId)));
        }
    }

    /* loaded from: classes2.dex */
    public class ManagedServiceInfo implements IBinder.DeathRecipient {
        public ComponentName component;
        public ServiceConnection connection;
        public boolean isSystem;
        public Pair<ComponentName, Integer> mKey;
        public IInterface service;
        public int targetSdkVersion;
        public int uid;
        public int userid;

        public ManagedServiceInfo(IInterface service, ComponentName component, int userid, boolean isSystem, ServiceConnection connection, int targetSdkVersion, int uid) {
            this.service = service;
            this.component = component;
            this.userid = userid;
            this.isSystem = isSystem;
            this.connection = connection;
            this.targetSdkVersion = targetSdkVersion;
            this.uid = uid;
            this.mKey = Pair.create(component, Integer.valueOf(userid));
        }

        public boolean isGuest(ManagedServices host) {
            return ManagedServices.this != host;
        }

        public ManagedServices getOwner() {
            return ManagedServices.this;
        }

        public String toString() {
            return "ManagedServiceInfo[component=" + this.component + ",userid=" + this.userid + ",isSystem=" + this.isSystem + ",targetSdkVersion=" + this.targetSdkVersion + ",connection=" + (this.connection == null ? null : "<connection>") + ",service=" + this.service + ']';
        }

        public void dumpDebug(ProtoOutputStream proto, long fieldId, ManagedServices host) {
            long token = proto.start(fieldId);
            this.component.dumpDebug(proto, 1146756268033L);
            proto.write(1120986464258L, this.userid);
            proto.write(1138166333443L, this.service.getClass().getName());
            proto.write(1133871366148L, this.isSystem);
            proto.write(1133871366149L, isGuest(host));
            proto.end(token);
        }

        public boolean isSameUser(int userId) {
            if (isEnabledForCurrentProfiles()) {
                return userId == -1 || userId == this.userid;
            }
            return false;
        }

        public boolean enabledAndUserMatches(int nid) {
            if (isEnabledForCurrentProfiles()) {
                int i = this.userid;
                if (i == -1 || this.isSystem || nid == -1 || nid == i) {
                    return true;
                }
                return supportsProfiles() && ManagedServices.this.mUserProfiles.isCurrentProfile(nid) && isPermittedForProfile(nid);
            }
            return false;
        }

        public boolean supportsProfiles() {
            return this.targetSdkVersion >= 21;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            if (ManagedServices.this.DEBUG) {
                Slog.d(ManagedServices.this.TAG, "binderDied");
            }
            ManagedServices.this.removeServiceImpl(this.service, this.userid);
        }

        public boolean isEnabledForCurrentProfiles() {
            if (this.isSystem) {
                return true;
            }
            if (this.connection == null) {
                return false;
            }
            return ManagedServices.this.mEnabledServicesForCurrentProfiles.contains(this.component);
        }

        public boolean isPermittedForProfile(int userId) {
            if (!ManagedServices.this.mUserProfiles.isManagedProfile(userId)) {
                return true;
            }
            DevicePolicyManager dpm = (DevicePolicyManager) ManagedServices.this.mContext.getSystemService("device_policy");
            long identity = Binder.clearCallingIdentity();
            try {
                return dpm.isNotificationListenerServicePermitted(this.component.getPackageName(), userId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ManagedServiceInfo that = (ManagedServiceInfo) o;
            if (this.userid == that.userid && this.isSystem == that.isSystem && this.targetSdkVersion == that.targetSdkVersion && Objects.equals(this.service, that.service) && Objects.equals(this.component, that.component) && Objects.equals(this.connection, that.connection)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.service, this.component, Integer.valueOf(this.userid), Boolean.valueOf(this.isSystem), this.connection, Integer.valueOf(this.targetSdkVersion));
        }
    }

    public boolean isComponentEnabledForCurrentProfiles(ComponentName component) {
        return this.mEnabledServicesForCurrentProfiles.contains(component);
    }

    /* loaded from: classes2.dex */
    public static class UserProfiles {
        private final SparseArray<UserInfo> mCurrentProfiles = new SparseArray<>();

        public void updateCache(Context context) {
            UserManager userManager = (UserManager) context.getSystemService(ManagedServices.ATT_USER_ID);
            if (userManager != null) {
                int currentUserId = ActivityManager.getCurrentUser();
                List<UserInfo> profiles = userManager.getProfiles(currentUserId);
                synchronized (this.mCurrentProfiles) {
                    this.mCurrentProfiles.clear();
                    for (UserInfo user : profiles) {
                        this.mCurrentProfiles.put(user.id, user);
                    }
                }
            }
        }

        public IntArray getCurrentProfileIds() {
            IntArray users;
            synchronized (this.mCurrentProfiles) {
                users = new IntArray(this.mCurrentProfiles.size());
                int N = this.mCurrentProfiles.size();
                for (int i = 0; i < N; i++) {
                    users.add(this.mCurrentProfiles.keyAt(i));
                }
            }
            return users;
        }

        public boolean isCurrentProfile(int userId) {
            boolean z;
            synchronized (this.mCurrentProfiles) {
                z = this.mCurrentProfiles.get(userId) != null;
            }
            return z;
        }

        public boolean isManagedProfile(int userId) {
            boolean z;
            synchronized (this.mCurrentProfiles) {
                UserInfo user = this.mCurrentProfiles.get(userId);
                z = user != null && user.isManagedProfile();
            }
            return z;
        }

        public boolean isDualProfile(int userId) {
            boolean z;
            synchronized (this.mCurrentProfiles) {
                UserInfo user = this.mCurrentProfiles.get(userId);
                z = user != null && user.isDualProfile();
            }
            return z;
        }
    }
}
