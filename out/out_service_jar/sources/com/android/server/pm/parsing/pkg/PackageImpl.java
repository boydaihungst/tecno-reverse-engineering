package com.android.server.pm.parsing.pkg;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.SigningDetails;
import android.content.res.TypedArray;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import com.android.internal.util.CollectionUtils;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.SELinuxUtil;
import com.android.server.pm.pkg.component.ComponentMutateUtils;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.pm.pkg.component.ParsedService;
import com.android.server.pm.pkg.parsing.ParsingPackage;
import com.android.server.pm.pkg.parsing.ParsingPackageImpl;
import java.io.File;
/* loaded from: classes2.dex */
public class PackageImpl extends ParsingPackageImpl implements ParsedPackage, AndroidPackage, AndroidPackageHidden {
    public static final Parcelable.Creator<PackageImpl> CREATOR = new Parcelable.Creator<PackageImpl>() { // from class: com.android.server.pm.parsing.pkg.PackageImpl.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PackageImpl createFromParcel(Parcel source) {
            return new PackageImpl(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PackageImpl[] newArray(int size) {
            return new PackageImpl[size];
        }
    };
    private String mBaseAppDataCredentialProtectedDirForSystemUser;
    private String mBaseAppDataDeviceProtectedDirForSystemUser;
    private int mBaseAppInfoFlags;
    private int mBaseAppInfoPrivateFlags;
    private int mBaseAppInfoPrivateFlagsExt;
    private int mBooleans;
    private final String manifestPackageName;
    protected String nativeLibraryDir;
    protected String nativeLibraryRootDir;
    private boolean nativeLibraryRootRequiresIsa;
    protected String primaryCpuAbi;
    protected String seInfo;
    protected String secondaryCpuAbi;
    protected String secondaryNativeLibraryDir;
    private int uid;

    public static PackageImpl forParsing(String packageName, String baseCodePath, String codePath, TypedArray manifestArray, boolean isCoreApp) {
        return new PackageImpl(packageName, baseCodePath, codePath, manifestArray, isCoreApp);
    }

    public static AndroidPackage buildFakeForDeletion(String packageName, String volumeUuid) {
        return ((ParsedPackage) forTesting(packageName).setVolumeUuid(volumeUuid).hideAsParsed()).hideAsFinal();
    }

    public static ParsingPackage forTesting(String packageName) {
        return forTesting(packageName, "");
    }

    public static ParsingPackage forTesting(String packageName, String baseCodePath) {
        return new PackageImpl(packageName, baseCodePath, baseCodePath, null, false);
    }

    /* loaded from: classes2.dex */
    private static class Booleans {
        private static final int CORE_APP = 1;
        private static final int FACTORY_TEST = 4;
        private static final int NATIVE_LIBRARY_ROOT_REQUIRES_ISA = 1024;
        private static final int ODM = 256;
        private static final int OEM = 32;
        private static final int PRIVILEGED = 16;
        private static final int PRODUCT = 128;
        private static final int SIGNED_WITH_PLATFORM_KEY = 512;
        private static final int STUB = 2048;
        private static final int SYSTEM = 2;
        private static final int SYSTEM_EXT = 8;
        private static final int VENDOR = 64;

        /* loaded from: classes2.dex */
        public @interface Flags {
        }

        private Booleans() {
        }
    }

    private ParsedPackage setBoolean(int flag, boolean value) {
        if (value) {
            this.mBooleans |= flag;
        } else {
            this.mBooleans &= ~flag;
        }
        return this;
    }

    private boolean getBoolean(int flag) {
        return (this.mBooleans & flag) != 0;
    }

    public PackageImpl(String packageName, String baseApkPath, String path, TypedArray manifestArray, boolean isCoreApp) {
        super(packageName, baseApkPath, path, manifestArray);
        this.uid = -1;
        this.manifestPackageName = this.packageName;
        setBoolean(1, isCoreApp);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackageImpl, com.android.server.pm.pkg.parsing.ParsingPackage
    public ParsedPackage hideAsParsed() {
        super.hideAsParsed();
        return this;
    }

    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public AndroidPackage hideAsFinal() {
        assignDerivedFields();
        return this;
    }

    private void assignDerivedFields() {
        this.mBaseAppInfoFlags = PackageInfoUtils.appInfoFlags(this, (PackageStateInternal) null);
        this.mBaseAppInfoPrivateFlags = PackageInfoUtils.appInfoPrivateFlags(this, (PackageStateInternal) null);
        this.mBaseAppInfoPrivateFlagsExt = PackageInfoUtils.appInfoPrivateFlagsExt(this, (PackageStateInternal) null);
        String baseAppDataDir = Environment.getDataDirectoryPath(getVolumeUuid()) + File.separator;
        String systemUserSuffix = File.separator + 0 + File.separator;
        this.mBaseAppDataCredentialProtectedDirForSystemUser = TextUtils.safeIntern(baseAppDataDir + "user" + systemUserSuffix);
        this.mBaseAppDataDeviceProtectedDirForSystemUser = TextUtils.safeIntern(baseAppDataDir + "user_de" + systemUserSuffix);
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageImpl, com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo, com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo, com.android.server.pm.pkg.AndroidPackageApi
    public long getLongVersionCode() {
        return PackageInfo.composeLongVersionCode(this.versionCodeMajor, this.versionCode);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl removePermission(int index) {
        this.permissions.remove(index);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl addUsesOptionalLibrary(int index, String libraryName) {
        this.usesOptionalLibraries = CollectionUtils.add(this.usesOptionalLibraries, index, TextUtils.safeIntern(libraryName));
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl addUsesLibrary(int index, String libraryName) {
        this.usesLibraries = CollectionUtils.add(this.usesLibraries, index, TextUtils.safeIntern(libraryName));
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl removeUsesLibrary(String libraryName) {
        this.usesLibraries = CollectionUtils.remove(this.usesLibraries, libraryName);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackageImpl, com.android.server.pm.pkg.parsing.ParsingPackage, com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl removeUsesOptionalLibrary(String libraryName) {
        super.removeUsesOptionalLibrary(libraryName);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackageImpl, com.android.server.pm.pkg.parsing.ParsingPackage, com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setSigningDetails(SigningDetails value) {
        super.setSigningDetails(value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackageImpl, com.android.server.pm.pkg.parsing.ParsingPackage, com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setRestrictUpdateHash(byte... value) {
        super.setRestrictUpdateHash(value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackageImpl, com.android.server.pm.pkg.parsing.ParsingPackage, com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setPersistent(boolean value) {
        super.setPersistent(value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackageImpl, com.android.server.pm.pkg.parsing.ParsingPackage, com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setDefaultToDeviceProtectedStorage(boolean value) {
        super.setDefaultToDeviceProtectedStorage(value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.parsing.ParsingPackageImpl, com.android.server.pm.pkg.parsing.ParsingPackage, com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setDirectBootAware(boolean value) {
        super.setDirectBootAware(value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl clearProtectedBroadcasts() {
        this.protectedBroadcasts.clear();
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl clearOriginalPackages() {
        this.originalPackages.clear();
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl clearAdoptPermissions() {
        this.adoptPermissions.clear();
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setPath(String path) {
        this.mPath = path;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setPackageName(String packageName) {
        this.packageName = TextUtils.safeIntern(packageName);
        int permissionsSize = this.permissions.size();
        for (int index = 0; index < permissionsSize; index++) {
            ComponentMutateUtils.setPackageName(this.permissions.get(index), this.packageName);
        }
        int permissionGroupsSize = this.permissionGroups.size();
        for (int index2 = 0; index2 < permissionGroupsSize; index2++) {
            ComponentMutateUtils.setPackageName(this.permissionGroups.get(index2), this.packageName);
        }
        int activitiesSize = this.activities.size();
        for (int index3 = 0; index3 < activitiesSize; index3++) {
            ComponentMutateUtils.setPackageName(this.activities.get(index3), this.packageName);
        }
        int receiversSize = this.receivers.size();
        for (int index4 = 0; index4 < receiversSize; index4++) {
            ComponentMutateUtils.setPackageName(this.receivers.get(index4), this.packageName);
        }
        int providersSize = this.providers.size();
        for (int index5 = 0; index5 < providersSize; index5++) {
            ComponentMutateUtils.setPackageName(this.providers.get(index5), this.packageName);
        }
        int servicesSize = this.services.size();
        for (int index6 = 0; index6 < servicesSize; index6++) {
            ComponentMutateUtils.setPackageName(this.services.get(index6), this.packageName);
        }
        int instrumentationsSize = this.instrumentations.size();
        for (int index7 = 0; index7 < instrumentationsSize; index7++) {
            ComponentMutateUtils.setPackageName(this.instrumentations.get(index7), this.packageName);
        }
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setAllComponentsDirectBootAware(boolean allComponentsDirectBootAware) {
        int activitiesSize = this.activities.size();
        for (int index = 0; index < activitiesSize; index++) {
            ComponentMutateUtils.setDirectBootAware(this.activities.get(index), allComponentsDirectBootAware);
        }
        int receiversSize = this.receivers.size();
        for (int index2 = 0; index2 < receiversSize; index2++) {
            ComponentMutateUtils.setDirectBootAware(this.receivers.get(index2), allComponentsDirectBootAware);
        }
        int providersSize = this.providers.size();
        for (int index3 = 0; index3 < providersSize; index3++) {
            ComponentMutateUtils.setDirectBootAware(this.providers.get(index3), allComponentsDirectBootAware);
        }
        int servicesSize = this.services.size();
        for (int index4 = 0; index4 < servicesSize; index4++) {
            ComponentMutateUtils.setDirectBootAware(this.services.get(index4), allComponentsDirectBootAware);
        }
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setBaseApkPath(String baseApkPath) {
        this.mBaseApkPath = TextUtils.safeIntern(baseApkPath);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setNativeLibraryDir(String nativeLibraryDir) {
        this.nativeLibraryDir = TextUtils.safeIntern(nativeLibraryDir);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setNativeLibraryRootDir(String nativeLibraryRootDir) {
        this.nativeLibraryRootDir = TextUtils.safeIntern(nativeLibraryRootDir);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setPrimaryCpuAbi(String primaryCpuAbi) {
        this.primaryCpuAbi = TextUtils.safeIntern(primaryCpuAbi);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setSecondaryCpuAbi(String secondaryCpuAbi) {
        this.secondaryCpuAbi = TextUtils.safeIntern(secondaryCpuAbi);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setSecondaryNativeLibraryDir(String secondaryNativeLibraryDir) {
        this.secondaryNativeLibraryDir = TextUtils.safeIntern(secondaryNativeLibraryDir);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setSeInfo(String seInfo) {
        this.seInfo = TextUtils.safeIntern(seInfo);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setSplitCodePaths(String[] splitCodePaths) {
        this.splitCodePaths = splitCodePaths;
        if (splitCodePaths != null) {
            int size = splitCodePaths.length;
            for (int index = 0; index < size; index++) {
                this.splitCodePaths[index] = TextUtils.safeIntern(this.splitCodePaths[index]);
            }
        }
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl capPermissionPriorities() {
        int size = this.permissionGroups.size();
        for (int index = size - 1; index >= 0; index--) {
            ComponentMutateUtils.setPriority(this.permissionGroups.get(index), 0);
        }
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl markNotActivitiesAsNotExportedIfSingleUser() {
        int receiversSize = this.receivers.size();
        for (int index = 0; index < receiversSize; index++) {
            ParsedActivity receiver = this.receivers.get(index);
            if ((1073741824 & receiver.getFlags()) != 0) {
                ComponentMutateUtils.setExported(receiver, false);
            }
        }
        int servicesSize = this.services.size();
        for (int index2 = 0; index2 < servicesSize; index2++) {
            ParsedService service = this.services.get(index2);
            if ((service.getFlags() & 1073741824) != 0) {
                ComponentMutateUtils.setExported(service, false);
            }
        }
        int providersSize = this.providers.size();
        for (int index3 = 0; index3 < providersSize; index3++) {
            ParsedProvider provider = this.providers.get(index3);
            if ((provider.getFlags() & 1073741824) != 0) {
                ComponentMutateUtils.setExported(provider, false);
            }
        }
        return this;
    }

    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public ParsedPackage setCoreApp(boolean coreApp) {
        return setBoolean(1, coreApp);
    }

    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public ParsedPackage setVersionCode(int versionCode) {
        this.versionCode = versionCode;
        return this;
    }

    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public ParsedPackage setVersionCodeMajor(int versionCodeMajor) {
        this.versionCodeMajor = versionCodeMajor;
        return this;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageImpl, com.android.server.pm.pkg.parsing.ParsingPackageHidden, com.android.server.pm.parsing.pkg.AndroidPackageHidden
    public ApplicationInfo toAppInfoWithoutState() {
        ApplicationInfo appInfo = super.toAppInfoWithoutStateWithoutFlags();
        appInfo.flags = this.mBaseAppInfoFlags;
        appInfo.privateFlags = this.mBaseAppInfoPrivateFlags;
        appInfo.privateFlagsExt = this.mBaseAppInfoPrivateFlagsExt;
        appInfo.nativeLibraryDir = this.nativeLibraryDir;
        appInfo.nativeLibraryRootDir = this.nativeLibraryRootDir;
        appInfo.nativeLibraryRootRequiresIsa = this.nativeLibraryRootRequiresIsa;
        appInfo.primaryCpuAbi = this.primaryCpuAbi;
        appInfo.secondaryCpuAbi = this.secondaryCpuAbi;
        appInfo.secondaryNativeLibraryDir = this.secondaryNativeLibraryDir;
        appInfo.seInfo = this.seInfo;
        appInfo.seInfoUser = SELinuxUtil.COMPLETE_STR;
        appInfo.uid = this.uid;
        return appInfo;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageImpl, android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // com.android.server.pm.pkg.parsing.ParsingPackageImpl, android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        sForInternedString.parcel(this.manifestPackageName, dest, flags);
        dest.writeString(this.nativeLibraryDir);
        dest.writeString(this.nativeLibraryRootDir);
        dest.writeBoolean(this.nativeLibraryRootRequiresIsa);
        sForInternedString.parcel(this.primaryCpuAbi, dest, flags);
        sForInternedString.parcel(this.secondaryCpuAbi, dest, flags);
        dest.writeString(this.secondaryNativeLibraryDir);
        dest.writeString(this.seInfo);
        dest.writeInt(this.uid);
        dest.writeInt(this.mBooleans);
    }

    public PackageImpl(Parcel in) {
        super(in);
        this.uid = -1;
        this.manifestPackageName = sForInternedString.unparcel(in);
        this.nativeLibraryDir = in.readString();
        this.nativeLibraryRootDir = in.readString();
        this.nativeLibraryRootRequiresIsa = in.readBoolean();
        this.primaryCpuAbi = sForInternedString.unparcel(in);
        this.secondaryCpuAbi = sForInternedString.unparcel(in);
        this.secondaryNativeLibraryDir = in.readString();
        this.seInfo = in.readString();
        this.uid = in.readInt();
        this.mBooleans = in.readInt();
        assignDerivedFields();
    }

    @Override // com.android.server.pm.parsing.pkg.AndroidPackage
    public String getManifestPackageName() {
        return this.manifestPackageName;
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public boolean isStub() {
        return getBoolean(2048);
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public String getNativeLibraryDir() {
        return this.nativeLibraryDir;
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public String getNativeLibraryRootDir() {
        return this.nativeLibraryRootDir;
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public boolean isNativeLibraryRootRequiresIsa() {
        return this.nativeLibraryRootRequiresIsa;
    }

    @Override // com.android.server.pm.parsing.pkg.AndroidPackageHidden
    public String getPrimaryCpuAbi() {
        return this.primaryCpuAbi;
    }

    @Override // com.android.server.pm.parsing.pkg.AndroidPackageHidden
    public String getSecondaryCpuAbi() {
        return this.secondaryCpuAbi;
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public String getSecondaryNativeLibraryDir() {
        return this.secondaryNativeLibraryDir;
    }

    @Override // com.android.server.pm.parsing.pkg.AndroidPackageHidden
    public String getSeInfo() {
        return this.seInfo;
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public boolean isCoreApp() {
        return getBoolean(1);
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public boolean isSystem() {
        return getBoolean(2);
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public boolean isFactoryTest() {
        return getBoolean(4);
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public boolean isSystemExt() {
        return getBoolean(8);
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public boolean isPrivileged() {
        return getBoolean(16);
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public boolean isOem() {
        return getBoolean(32);
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public boolean isVendor() {
        return getBoolean(64);
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public boolean isProduct() {
        return getBoolean(128);
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public boolean isOdm() {
        return getBoolean(256);
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public boolean isSignedWithPlatformKey() {
        return getBoolean(512);
    }

    @Override // com.android.server.pm.pkg.AndroidPackageApi
    public int getUid() {
        return this.uid;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setStub(boolean value) {
        setBoolean(2048, value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setNativeLibraryRootRequiresIsa(boolean value) {
        this.nativeLibraryRootRequiresIsa = value;
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setSystem(boolean value) {
        setBoolean(2, value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setFactoryTest(boolean value) {
        setBoolean(4, value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setSystemExt(boolean value) {
        setBoolean(8, value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setPrivileged(boolean value) {
        setBoolean(16, value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setOem(boolean value) {
        setBoolean(32, value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setVendor(boolean value) {
        setBoolean(64, value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setProduct(boolean value) {
        setBoolean(128, value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setOdm(boolean value) {
        setBoolean(256, value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setSignedWithPlatformKey(boolean value) {
        setBoolean(512, value);
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.parsing.pkg.ParsedPackage
    public PackageImpl setUid(int value) {
        this.uid = value;
        return this;
    }

    public String getBaseAppDataCredentialProtectedDirForSystemUser() {
        return this.mBaseAppDataCredentialProtectedDirForSystemUser;
    }

    public String getBaseAppDataDeviceProtectedDirForSystemUser() {
        return this.mBaseAppDataDeviceProtectedDirForSystemUser;
    }
}
