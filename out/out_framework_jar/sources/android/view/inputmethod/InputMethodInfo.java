package android.view.inputmethod;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Printer;
import android.util.Xml;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes3.dex */
public final class InputMethodInfo implements Parcelable {
    public static final Parcelable.Creator<InputMethodInfo> CREATOR = new Parcelable.Creator<InputMethodInfo>() { // from class: android.view.inputmethod.InputMethodInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InputMethodInfo createFromParcel(Parcel source) {
            return new InputMethodInfo(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InputMethodInfo[] newArray(int size) {
            return new InputMethodInfo[size];
        }
    };
    static final String TAG = "InputMethodInfo";
    private final boolean mForceDefault;
    private final int mHandledConfigChanges;
    final String mId;
    private final boolean mInlineSuggestionsEnabled;
    private final boolean mIsAuxIme;
    final int mIsDefaultResId;
    final boolean mIsVrOnly;
    final ResolveInfo mService;
    final String mSettingsActivityName;
    private final boolean mShowInInputMethodPicker;
    private final InputMethodSubtypeArray mSubtypes;
    private final boolean mSupportsInlineSuggestionsWithTouchExploration;
    private final boolean mSupportsStylusHandwriting;
    private final boolean mSupportsSwitchingToNextInputMethod;
    private final boolean mSuppressesSpellChecker;

    public static String computeId(ResolveInfo service) {
        ServiceInfo si = service.serviceInfo;
        return new ComponentName(si.packageName, si.name).flattenToShortString();
    }

    public InputMethodInfo(Context context, ResolveInfo service) throws XmlPullParserException, IOException {
        this(context, service, null);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [298=8, 302=10] */
    /* JADX WARN: Code restructure failed: missing block: B:100:0x0309, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:101:0x030a, code lost:
        if (r14 != null) goto L88;
     */
    /* JADX WARN: Code restructure failed: missing block: B:102:0x030c, code lost:
        r14.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:103:0x030f, code lost:
        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:13:0x0054, code lost:
        r5 = r14.getName();
     */
    /* JADX WARN: Code restructure failed: missing block: B:14:0x0060, code lost:
        if ("input-method".equals(r5) == false) goto L113;
     */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x0062, code lost:
        r5 = r17;
        r2 = r0.obtainAttributes(r5, com.android.internal.R.styleable.InputMethod);
     */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x006f, code lost:
        r22 = r2.getString(2);
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0079, code lost:
        r16 = r2.getBoolean(4, false);
        r16 = r2.getResourceId(1, 0);
        r16 = r2.getBoolean(3, false);
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x008e, code lost:
        r16 = r2.getBoolean(5, false);
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x0096, code lost:
        r16 = r2.getBoolean(9, false);
        r16 = r2.getBoolean(6, false);
        r24 = r2.getBoolean(7, true);
        r29.mHandledConfigChanges = r2.getInt(0, 0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x00b6, code lost:
        r29.mSupportsStylusHandwriting = r2.getBoolean(8, false);
        r2.recycle();
        r7 = r14.getDepth();
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x00c3, code lost:
        r11 = r14.next();
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x00ca, code lost:
        if (r11 != 3) goto L58;
     */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x00d0, code lost:
        if (r14.getDepth() <= r7) goto L37;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x00d5, code lost:
        if (r11 == 1) goto L77;
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x00d8, code lost:
        if (r11 != 2) goto L62;
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x00da, code lost:
        r11 = r14.getName();
        r26 = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x00e7, code lost:
        if ("subtype".equals(r11) == false) goto L74;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x00e9, code lost:
        r2 = r0.obtainAttributes(r5, com.android.internal.R.styleable.InputMethod_Subtype);
        r27 = r0;
        r28 = r5;
        r0 = new android.view.inputmethod.InputMethodSubtype.InputMethodSubtypeBuilder().setSubtypeNameResId(r2.getResourceId(0, 0));
        r17 = r7;
        r7 = r2.getResourceId(1, 0);
        r0 = r0.setSubtypeIconResId(r7).setLanguageTag(r2.getString(9)).setSubtypeLocale(r2.getString(2)).setSubtypeMode(r2.getString(3)).setSubtypeExtraValue(r2.getString(4)).setIsAuxiliary(r2.getBoolean(5, false)).setOverridesImplicitlyEnabledSubtype(r2.getBoolean(6, false)).setSubtypeId(r2.getInt(7, 0)).setIsAsciiCapable(r2.getBoolean(8, false)).build();
        r2.recycle();
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x0164, code lost:
        if (r0.isAuxiliary() != false) goto L72;
     */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x0166, code lost:
        r19 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x0169, code lost:
        r15.add(r0);
        r7 = r17;
        r2 = r26;
        r0 = r27;
        r5 = r28;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x0188, code lost:
        throw new org.xmlpull.v1.XmlPullParserException("Meta-data in input-method does not start with subtype tag");
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x0189, code lost:
        r7 = r7;
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x01a5, code lost:
        if (r14 == null) goto L40;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x01a7, code lost:
        r14.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:48:0x01ae, code lost:
        if (r15.size() != 0) goto L57;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x01b0, code lost:
        r0 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x01b5, code lost:
        r0 = r19;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x01b7, code lost:
        if (r32 == null) goto L54;
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x01b9, code lost:
        r2 = r32.size();
        r5 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x01be, code lost:
        if (r5 >= r2) goto L53;
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x01c0, code lost:
        r6 = r32.get(r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:0x01ca, code lost:
        if (r15.contains(r6) != false) goto L51;
     */
    /* JADX WARN: Code restructure failed: missing block: B:56:0x01cc, code lost:
        r15.add(r6);
     */
    /* JADX WARN: Code restructure failed: missing block: B:57:0x01d0, code lost:
        android.util.Slog.w(android.view.inputmethod.InputMethodInfo.TAG, "Duplicated subtype definition found: " + r6.getLocale() + ", " + r6.getMode());
     */
    /* JADX WARN: Code restructure failed: missing block: B:58:0x01fa, code lost:
        r5 = r5 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:59:0x01fd, code lost:
        r29.mSubtypes = new android.view.inputmethod.InputMethodSubtypeArray(r15);
        r29.mSettingsActivityName = r22;
        r29.mIsDefaultResId = r16;
        r29.mIsAuxIme = r0;
        r29.mSupportsSwitchingToNextInputMethod = r16;
        r29.mInlineSuggestionsEnabled = r16;
        r29.mSupportsInlineSuggestionsWithTouchExploration = r16;
        r29.mSuppressesSpellChecker = r16;
        r29.mShowInInputMethodPicker = r24;
        r29.mIsVrOnly = r16;
     */
    /* JADX WARN: Code restructure failed: missing block: B:60:0x021c, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:61:0x021d, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:63:0x0228, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:65:0x0233, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:67:0x0240, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:69:0x024d, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:71:0x0259, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:73:0x0265, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:75:0x0270, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:77:0x027b, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:79:0x0286, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:83:0x02a4, code lost:
        throw new org.xmlpull.v1.XmlPullParserException("Meta-data does not start with input-method tag");
     */
    /* JADX WARN: Code restructure failed: missing block: B:84:0x02a5, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:86:0x02af, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:99:0x0308, code lost:
        throw new org.xmlpull.v1.XmlPullParserException("Unable to create context for: " + r4.packageName);
     */
    /* JADX WARN: Not initialized variable reg: 18, insn: 0x02ca: MOVE  (r6 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY]) = (r18 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY] A[D('supportsSwitchingToNextInputMethod' boolean)]), block:B:91:0x02ca */
    /* JADX WARN: Not initialized variable reg: 18, insn: 0x02d2: MOVE  (r6 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY]) = (r18 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY] A[D('supportsSwitchingToNextInputMethod' boolean)]), block:B:93:0x02d2 */
    /* JADX WARN: Not initialized variable reg: 19, insn: 0x02cc: MOVE  (r5 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY]) = (r19 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY] A[D('isAuxIme' boolean)]), block:B:91:0x02ca */
    /* JADX WARN: Not initialized variable reg: 19, insn: 0x02d4: MOVE  (r5 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY]) = (r19 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY] A[D('isAuxIme' boolean)]), block:B:93:0x02d2 */
    /* JADX WARN: Not initialized variable reg: 22, insn: 0x02ce: MOVE  (r7 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY]) = (r22 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY] A[D('inlineSuggestionsEnabled' boolean)]), block:B:91:0x02ca */
    /* JADX WARN: Not initialized variable reg: 22, insn: 0x02d6: MOVE  (r7 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY]) = (r22 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY] A[D('inlineSuggestionsEnabled' boolean)]), block:B:93:0x02d2 */
    /* JADX WARN: Removed duplicated region for block: B:102:0x030c  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public InputMethodInfo(Context context, ResolveInfo service, List<InputMethodSubtype> additionalSubtypes) throws XmlPullParserException, IOException {
        int type;
        this.mService = service;
        ServiceInfo si = service.serviceInfo;
        this.mId = computeId(service);
        boolean isAuxIme = true;
        this.mForceDefault = false;
        PackageManager pm = context.getPackageManager();
        XmlResourceParser parser = null;
        ArrayList<InputMethodSubtype> subtypes = new ArrayList<>();
        try {
            parser = si.loadXmlMetaData(pm, InputMethod.SERVICE_META_DATA);
            try {
                if (parser == null) {
                    throw new XmlPullParserException("No android.view.im meta-data");
                }
                Resources res = pm.getResourcesForApplication(si.applicationInfo);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                while (true) {
                    int type2 = parser.next();
                    boolean isAuxIme2 = isAuxIme;
                    if (type2 == 1) {
                        type = type2;
                        break;
                    }
                    type = type2;
                    if (type != 2) {
                        isAuxIme = isAuxIme2;
                    }
                }
            } catch (PackageManager.NameNotFoundException | IndexOutOfBoundsException | NumberFormatException e) {
                e = e;
            } catch (Throwable th) {
                th = th;
            }
        } catch (PackageManager.NameNotFoundException | IndexOutOfBoundsException | NumberFormatException e2) {
            e = e2;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    InputMethodInfo(Parcel source) {
        this.mId = source.readString();
        this.mSettingsActivityName = source.readString();
        this.mIsDefaultResId = source.readInt();
        this.mIsAuxIme = source.readInt() == 1;
        this.mSupportsSwitchingToNextInputMethod = source.readInt() == 1;
        this.mInlineSuggestionsEnabled = source.readInt() == 1;
        this.mSupportsInlineSuggestionsWithTouchExploration = source.readInt() == 1;
        this.mSuppressesSpellChecker = source.readBoolean();
        this.mShowInInputMethodPicker = source.readBoolean();
        this.mIsVrOnly = source.readBoolean();
        this.mService = ResolveInfo.CREATOR.createFromParcel(source);
        this.mSubtypes = new InputMethodSubtypeArray(source);
        this.mHandledConfigChanges = source.readInt();
        this.mSupportsStylusHandwriting = source.readBoolean();
        this.mForceDefault = false;
    }

    public InputMethodInfo(String packageName, String className, CharSequence label, String settingsActivity) {
        this(buildFakeResolveInfo(packageName, className, label), false, settingsActivity, null, 0, false, true, false, false, 0, false, false);
    }

    public InputMethodInfo(String packageName, String className, CharSequence label, String settingsActivity, int handledConfigChanges) {
        this(buildFakeResolveInfo(packageName, className, label), false, settingsActivity, null, 0, false, true, false, false, handledConfigChanges, false, false);
    }

    public InputMethodInfo(ResolveInfo ri, boolean isAuxIme, String settingsActivity, List<InputMethodSubtype> subtypes, int isDefaultResId, boolean forceDefault) {
        this(ri, isAuxIme, settingsActivity, subtypes, isDefaultResId, forceDefault, true, false, false, 0, false, false);
    }

    public InputMethodInfo(ResolveInfo ri, boolean isAuxIme, String settingsActivity, List<InputMethodSubtype> subtypes, int isDefaultResId, boolean forceDefault, boolean supportsSwitchingToNextInputMethod, boolean isVrOnly) {
        this(ri, isAuxIme, settingsActivity, subtypes, isDefaultResId, forceDefault, supportsSwitchingToNextInputMethod, false, isVrOnly, 0, false, false);
    }

    public InputMethodInfo(ResolveInfo ri, boolean isAuxIme, String settingsActivity, List<InputMethodSubtype> subtypes, int isDefaultResId, boolean forceDefault, boolean supportsSwitchingToNextInputMethod, boolean inlineSuggestionsEnabled, boolean isVrOnly, int handledConfigChanges, boolean supportsStylusHandwriting, boolean supportsInlineSuggestionsWithTouchExploration) {
        ServiceInfo si = ri.serviceInfo;
        this.mService = ri;
        this.mId = new ComponentName(si.packageName, si.name).flattenToShortString();
        this.mSettingsActivityName = settingsActivity;
        this.mIsDefaultResId = isDefaultResId;
        this.mIsAuxIme = isAuxIme;
        this.mSubtypes = new InputMethodSubtypeArray(subtypes);
        this.mForceDefault = forceDefault;
        this.mSupportsSwitchingToNextInputMethod = supportsSwitchingToNextInputMethod;
        this.mInlineSuggestionsEnabled = inlineSuggestionsEnabled;
        this.mSupportsInlineSuggestionsWithTouchExploration = supportsInlineSuggestionsWithTouchExploration;
        this.mSuppressesSpellChecker = false;
        this.mShowInInputMethodPicker = true;
        this.mIsVrOnly = isVrOnly;
        this.mHandledConfigChanges = handledConfigChanges;
        this.mSupportsStylusHandwriting = supportsStylusHandwriting;
    }

    private static ResolveInfo buildFakeResolveInfo(String packageName, String className, CharSequence label) {
        ResolveInfo ri = new ResolveInfo();
        ServiceInfo si = new ServiceInfo();
        ApplicationInfo ai = new ApplicationInfo();
        ai.packageName = packageName;
        ai.enabled = true;
        si.applicationInfo = ai;
        si.enabled = true;
        si.packageName = packageName;
        si.name = className;
        si.exported = true;
        si.nonLocalizedLabel = label;
        ri.serviceInfo = si;
        return ri;
    }

    public String getId() {
        return this.mId;
    }

    public String getPackageName() {
        return this.mService.serviceInfo.packageName;
    }

    public String getServiceName() {
        return this.mService.serviceInfo.name;
    }

    public ServiceInfo getServiceInfo() {
        return this.mService.serviceInfo;
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mService.serviceInfo.packageName, this.mService.serviceInfo.name);
    }

    public CharSequence loadLabel(PackageManager pm) {
        return this.mService.loadLabel(pm);
    }

    public Drawable loadIcon(PackageManager pm) {
        return this.mService.loadIcon(pm);
    }

    public String getSettingsActivity() {
        return this.mSettingsActivityName;
    }

    public boolean isVrOnly() {
        return this.mIsVrOnly;
    }

    public int getSubtypeCount() {
        return this.mSubtypes.getCount();
    }

    public InputMethodSubtype getSubtypeAt(int index) {
        return this.mSubtypes.get(index);
    }

    public int getIsDefaultResourceId() {
        return this.mIsDefaultResId;
    }

    public boolean isDefault(Context context) {
        if (this.mForceDefault) {
            return true;
        }
        try {
            if (getIsDefaultResourceId() == 0) {
                return false;
            }
            Resources res = context.createPackageContext(getPackageName(), 0).getResources();
            return res.getBoolean(getIsDefaultResourceId());
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            return false;
        }
    }

    public int getConfigChanges() {
        return this.mHandledConfigChanges;
    }

    public boolean supportsStylusHandwriting() {
        return this.mSupportsStylusHandwriting;
    }

    public void dump(Printer pw, String prefix) {
        pw.println(prefix + "mId=" + this.mId + " mSettingsActivityName=" + this.mSettingsActivityName + " mIsVrOnly=" + this.mIsVrOnly + " mSupportsSwitchingToNextInputMethod=" + this.mSupportsSwitchingToNextInputMethod + " mInlineSuggestionsEnabled=" + this.mInlineSuggestionsEnabled + " mSupportsInlineSuggestionsWithTouchExploration=" + this.mSupportsInlineSuggestionsWithTouchExploration + " mSuppressesSpellChecker=" + this.mSuppressesSpellChecker + " mShowInInputMethodPicker=" + this.mShowInInputMethodPicker + " mSupportsStylusHandwriting=" + this.mSupportsStylusHandwriting);
        pw.println(prefix + "mIsDefaultResId=0x" + Integer.toHexString(this.mIsDefaultResId));
        pw.println(prefix + "Service:");
        this.mService.dump(pw, prefix + "  ");
    }

    public String toString() {
        return "InputMethodInfo{" + this.mId + ", settings: " + this.mSettingsActivityName + "}";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !(o instanceof InputMethodInfo)) {
            return false;
        }
        InputMethodInfo obj = (InputMethodInfo) o;
        return this.mId.equals(obj.mId);
    }

    public int hashCode() {
        return this.mId.hashCode();
    }

    public boolean isSystem() {
        return (this.mService.serviceInfo.applicationInfo.flags & 1) != 0;
    }

    public boolean isAuxiliaryIme() {
        return this.mIsAuxIme;
    }

    public boolean supportsSwitchingToNextInputMethod() {
        return this.mSupportsSwitchingToNextInputMethod;
    }

    public boolean isInlineSuggestionsEnabled() {
        return this.mInlineSuggestionsEnabled;
    }

    public boolean supportsInlineSuggestionsWithTouchExploration() {
        return this.mSupportsInlineSuggestionsWithTouchExploration;
    }

    public boolean suppressesSpellChecker() {
        return this.mSuppressesSpellChecker;
    }

    public boolean shouldShowInInputMethodPicker() {
        return this.mShowInInputMethodPicker;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mId);
        dest.writeString(this.mSettingsActivityName);
        dest.writeInt(this.mIsDefaultResId);
        dest.writeInt(this.mIsAuxIme ? 1 : 0);
        dest.writeInt(this.mSupportsSwitchingToNextInputMethod ? 1 : 0);
        dest.writeInt(this.mInlineSuggestionsEnabled ? 1 : 0);
        dest.writeInt(this.mSupportsInlineSuggestionsWithTouchExploration ? 1 : 0);
        dest.writeBoolean(this.mSuppressesSpellChecker);
        dest.writeBoolean(this.mShowInInputMethodPicker);
        dest.writeBoolean(this.mIsVrOnly);
        this.mService.writeToParcel(dest, flags);
        this.mSubtypes.writeToParcel(dest);
        dest.writeInt(this.mHandledConfigChanges);
        dest.writeBoolean(this.mSupportsStylusHandwriting);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
