package android.app.admin;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Printer;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public final class DeviceAdminInfo implements Parcelable {
    public static final Parcelable.Creator<DeviceAdminInfo> CREATOR;
    static final String TAG = "DeviceAdminInfo";
    public static final int USES_ENCRYPTED_STORAGE = 7;
    public static final int USES_POLICY_DISABLE_CAMERA = 8;
    public static final int USES_POLICY_DISABLE_KEYGUARD_FEATURES = 9;
    public static final int USES_POLICY_EXPIRE_PASSWORD = 6;
    public static final int USES_POLICY_FORCE_LOCK = 3;
    public static final int USES_POLICY_LIMIT_PASSWORD = 0;
    public static final int USES_POLICY_RESET_PASSWORD = 2;
    public static final int USES_POLICY_SETS_GLOBAL_PROXY = 5;
    public static final int USES_POLICY_WATCH_LOGIN = 1;
    public static final int USES_POLICY_WIPE_DATA = 4;
    final ActivityInfo mActivityInfo;
    boolean mSupportsTransferOwnership;
    int mUsesPolicies;
    boolean mVisible;
    static ArrayList<PolicyInfo> sPoliciesDisplayOrder = new ArrayList<>();
    static HashMap<String, Integer> sKnownPolicies = new HashMap<>();
    static SparseArray<PolicyInfo> sRevKnownPolicies = new SparseArray<>();

    /* loaded from: classes.dex */
    public static class PolicyInfo {
        public final int description;
        public final int descriptionForSecondaryUsers;
        public final int ident;
        public final int label;
        public final int labelForSecondaryUsers;
        public final String tag;

        public PolicyInfo(int ident, String tag, int label, int description) {
            this(ident, tag, label, description, label, description);
        }

        public PolicyInfo(int ident, String tag, int label, int description, int labelForSecondaryUsers, int descriptionForSecondaryUsers) {
            this.ident = ident;
            this.tag = tag;
            this.label = label;
            this.description = description;
            this.labelForSecondaryUsers = labelForSecondaryUsers;
            this.descriptionForSecondaryUsers = descriptionForSecondaryUsers;
        }
    }

    static {
        sPoliciesDisplayOrder.add(new PolicyInfo(4, "wipe-data", R.string.policylab_wipeData, R.string.policydesc_wipeData, R.string.policylab_wipeData_secondaryUser, R.string.policydesc_wipeData_secondaryUser));
        sPoliciesDisplayOrder.add(new PolicyInfo(2, "reset-password", R.string.policylab_resetPassword, R.string.policydesc_resetPassword));
        sPoliciesDisplayOrder.add(new PolicyInfo(0, "limit-password", R.string.policylab_limitPassword, R.string.policydesc_limitPassword));
        sPoliciesDisplayOrder.add(new PolicyInfo(1, "watch-login", R.string.policylab_watchLogin, R.string.policydesc_watchLogin, R.string.policylab_watchLogin, R.string.policydesc_watchLogin_secondaryUser));
        sPoliciesDisplayOrder.add(new PolicyInfo(3, "force-lock", R.string.policylab_forceLock, R.string.policydesc_forceLock));
        sPoliciesDisplayOrder.add(new PolicyInfo(5, "set-global-proxy", R.string.policylab_setGlobalProxy, R.string.policydesc_setGlobalProxy));
        sPoliciesDisplayOrder.add(new PolicyInfo(6, "expire-password", R.string.policylab_expirePassword, R.string.policydesc_expirePassword));
        sPoliciesDisplayOrder.add(new PolicyInfo(7, "encrypted-storage", R.string.policylab_encryptedStorage, R.string.policydesc_encryptedStorage));
        sPoliciesDisplayOrder.add(new PolicyInfo(8, "disable-camera", R.string.policylab_disableCamera, R.string.policydesc_disableCamera));
        sPoliciesDisplayOrder.add(new PolicyInfo(9, "disable-keyguard-features", R.string.policylab_disableKeyguardFeatures, R.string.policydesc_disableKeyguardFeatures));
        for (int i = 0; i < sPoliciesDisplayOrder.size(); i++) {
            PolicyInfo pi = sPoliciesDisplayOrder.get(i);
            sRevKnownPolicies.put(pi.ident, pi);
            sKnownPolicies.put(pi.tag, Integer.valueOf(pi.ident));
        }
        CREATOR = new Parcelable.Creator<DeviceAdminInfo>() { // from class: android.app.admin.DeviceAdminInfo.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public DeviceAdminInfo createFromParcel(Parcel source) {
                return new DeviceAdminInfo(source);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public DeviceAdminInfo[] newArray(int size) {
                return new DeviceAdminInfo[size];
            }
        };
    }

    public DeviceAdminInfo(Context context, ResolveInfo resolveInfo) throws XmlPullParserException, IOException {
        this(context, resolveInfo.activityInfo);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE] complete} */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x008d, code lost:
        if (r15 != r12) goto L51;
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x008f, code lost:
        r17 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x0092, code lost:
        r15 = r0.getName();
        r12 = android.app.admin.DeviceAdminInfo.sKnownPolicies.get(r15);
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x009e, code lost:
        if (r12 == null) goto L56;
     */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x00a0, code lost:
        r18.mUsesPolicies |= r8 << r12.intValue();
        r17 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x00af, code lost:
        r17 = r0;
        android.util.Log.w(android.app.admin.DeviceAdminInfo.TAG, "Unknown tag under uses-policies of " + getComponent() + ": " + r15);
     */
    /* JADX WARN: Code restructure failed: missing block: B:60:0x0118, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:91:?, code lost:
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public DeviceAdminInfo(Context context, ActivityInfo activityInfo) throws XmlPullParserException, IOException {
        int i;
        Resources res;
        int i2;
        Resources res2;
        int i3;
        this.mActivityInfo = activityInfo;
        PackageManager pm = context.getPackageManager();
        XmlResourceParser parser = null;
        try {
            try {
                parser = activityInfo.loadXmlMetaData(pm, DeviceAdminReceiver.DEVICE_ADMIN_META_DATA);
                if (parser == null) {
                    throw new XmlPullParserException("No android.app.device_admin meta-data");
                }
                Resources res3 = pm.getResourcesForApplication(activityInfo.applicationInfo);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                while (true) {
                    int type = parser.next();
                    i = 1;
                    if (type == 1 || type == 2) {
                        break;
                    }
                }
                String nodeName = parser.getName();
                if (!"device-admin".equals(nodeName)) {
                    throw new XmlPullParserException("Meta-data does not start with device-admin tag");
                }
                TypedArray sa = res3.obtainAttributes(attrs, R.styleable.DeviceAdmin);
                this.mVisible = sa.getBoolean(0, true);
                sa.recycle();
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == i) {
                        break;
                    }
                    int i4 = 3;
                    if (type2 == 3 && parser.getDepth() <= outerDepth) {
                        break;
                    }
                    if (type2 != 3) {
                        int i5 = 4;
                        if (type2 == 4) {
                            res = res3;
                            i2 = i;
                        } else {
                            String tagName = parser.getName();
                            if (tagName.equals("uses-policies")) {
                                int innerDepth = parser.getDepth();
                                while (true) {
                                    int type3 = parser.next();
                                    if (type3 == i) {
                                        res2 = res3;
                                        break;
                                    }
                                    if (type3 == i4 && parser.getDepth() <= innerDepth) {
                                        res2 = res3;
                                        break;
                                    }
                                    Resources res4 = res3;
                                    res3 = res4;
                                    i = 1;
                                    i4 = 3;
                                    i5 = 4;
                                }
                                i3 = 1;
                            } else {
                                res2 = res3;
                                if (!tagName.equals("support-transfer-ownership")) {
                                    i3 = 1;
                                } else if (parser.next() != 3) {
                                    throw new XmlPullParserException("support-transfer-ownership tag must be empty.");
                                } else {
                                    i3 = 1;
                                    this.mSupportsTransferOwnership = true;
                                }
                            }
                            i = i3;
                            res3 = res2;
                        }
                    } else {
                        res = res3;
                        i2 = i;
                    }
                    i = i2;
                    res3 = res;
                }
            } catch (PackageManager.NameNotFoundException e) {
                throw new XmlPullParserException("Unable to create context for: " + this.mActivityInfo.packageName);
            }
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    DeviceAdminInfo(Parcel source) {
        this.mActivityInfo = ActivityInfo.CREATOR.createFromParcel(source);
        this.mUsesPolicies = source.readInt();
        this.mSupportsTransferOwnership = source.readBoolean();
    }

    public String getPackageName() {
        return this.mActivityInfo.packageName;
    }

    public String getReceiverName() {
        return this.mActivityInfo.name;
    }

    public ActivityInfo getActivityInfo() {
        return this.mActivityInfo;
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mActivityInfo.packageName, this.mActivityInfo.name);
    }

    public CharSequence loadLabel(PackageManager pm) {
        return this.mActivityInfo.loadLabel(pm);
    }

    public CharSequence loadDescription(PackageManager pm) throws Resources.NotFoundException {
        if (this.mActivityInfo.descriptionRes != 0) {
            return pm.getText(this.mActivityInfo.packageName, this.mActivityInfo.descriptionRes, this.mActivityInfo.applicationInfo);
        }
        throw new Resources.NotFoundException();
    }

    public Drawable loadIcon(PackageManager pm) {
        return this.mActivityInfo.loadIcon(pm);
    }

    public boolean isVisible() {
        return this.mVisible;
    }

    public boolean usesPolicy(int policyIdent) {
        return (this.mUsesPolicies & (1 << policyIdent)) != 0;
    }

    public String getTagForPolicy(int policyIdent) {
        return sRevKnownPolicies.get(policyIdent).tag;
    }

    public boolean supportsTransferOwnership() {
        return this.mSupportsTransferOwnership;
    }

    public ArrayList<PolicyInfo> getUsedPolicies() {
        ArrayList<PolicyInfo> res = new ArrayList<>();
        for (int i = 0; i < sPoliciesDisplayOrder.size(); i++) {
            PolicyInfo pi = sPoliciesDisplayOrder.get(i);
            if (usesPolicy(pi.ident)) {
                res.add(pi);
            }
        }
        return res;
    }

    public void writePoliciesToXml(TypedXmlSerializer out) throws IllegalArgumentException, IllegalStateException, IOException {
        out.attributeInt(null, "flags", this.mUsesPolicies);
    }

    public void readPoliciesFromXml(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        this.mUsesPolicies = parser.getAttributeInt(null, "flags");
    }

    public void dump(Printer pw, String prefix) {
        pw.println(prefix + "Receiver:");
        this.mActivityInfo.dump(pw, prefix + "  ");
    }

    public String toString() {
        return "DeviceAdminInfo{" + this.mActivityInfo.name + "}";
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        this.mActivityInfo.writeToParcel(dest, flags);
        dest.writeInt(this.mUsesPolicies);
        dest.writeBoolean(this.mSupportsTransferOwnership);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
