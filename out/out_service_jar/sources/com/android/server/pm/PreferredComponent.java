package com.android.server.pm;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.pm.pkg.PackageUserState;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class PreferredComponent {
    private static final String ATTR_ALWAYS = "always";
    private static final String ATTR_MATCH = "match";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SET = "set";
    private static final String TAG_SET = "set";
    public boolean mAlways;
    private final Callbacks mCallbacks;
    public final ComponentName mComponent;
    public final int mMatch;
    private String mParseError;
    final String[] mSetClasses;
    final String[] mSetComponents;
    final String[] mSetPackages;
    final String mShortComponent;

    /* loaded from: classes2.dex */
    public interface Callbacks {
        boolean onReadTag(String str, TypedXmlPullParser typedXmlPullParser) throws XmlPullParserException, IOException;
    }

    public PreferredComponent(Callbacks callbacks, int match, ComponentName[] set, ComponentName component, boolean always) {
        this.mCallbacks = callbacks;
        this.mMatch = 268369920 & match;
        this.mComponent = component;
        this.mAlways = always;
        this.mShortComponent = component.flattenToShortString();
        this.mParseError = null;
        if (set != null) {
            int N = set.length;
            String[] myPackages = new String[N];
            String[] myClasses = new String[N];
            String[] myComponents = new String[N];
            for (int i = 0; i < N; i++) {
                ComponentName cn = set[i];
                if (cn == null) {
                    this.mSetPackages = null;
                    this.mSetClasses = null;
                    this.mSetComponents = null;
                    return;
                }
                myPackages[i] = cn.getPackageName().intern();
                myClasses[i] = cn.getClassName().intern();
                myComponents[i] = cn.flattenToShortString();
            }
            this.mSetPackages = myPackages;
            this.mSetClasses = myClasses;
            this.mSetComponents = myComponents;
            return;
        }
        this.mSetPackages = null;
        this.mSetClasses = null;
        this.mSetComponents = null;
    }

    public PreferredComponent(Callbacks callbacks, TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        this.mCallbacks = callbacks;
        String str = null;
        String attributeValue = parser.getAttributeValue((String) null, "name");
        this.mShortComponent = attributeValue;
        ComponentName unflattenFromString = ComponentName.unflattenFromString(attributeValue);
        this.mComponent = unflattenFromString;
        if (unflattenFromString == null) {
            this.mParseError = "Bad activity name " + attributeValue;
        }
        this.mMatch = parser.getAttributeIntHex((String) null, ATTR_MATCH, 0);
        int setCount = parser.getAttributeInt((String) null, "set", 0);
        int i = 1;
        this.mAlways = parser.getAttributeBoolean((String) null, ATTR_ALWAYS, true);
        String[] myPackages = setCount > 0 ? new String[setCount] : null;
        String[] myClasses = setCount > 0 ? new String[setCount] : null;
        String[] myComponents = setCount > 0 ? new String[setCount] : null;
        int setPos = 0;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == i || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type == 3 || type == 4) {
                str = null;
                i = 1;
            } else {
                String tagName = parser.getName();
                if (!tagName.equals("set")) {
                    if (!this.mCallbacks.onReadTag(tagName, parser)) {
                        Slog.w("PreferredComponent", "Unknown element: " + parser.getName());
                        XmlUtils.skipCurrentTag(parser);
                    }
                } else {
                    String name = parser.getAttributeValue(str, "name");
                    if (name == null) {
                        if (this.mParseError == null) {
                            this.mParseError = "No name in set tag in preferred activity " + this.mShortComponent;
                        }
                    } else if (setPos >= setCount) {
                        if (this.mParseError == null) {
                            this.mParseError = "Too many set tags in preferred activity " + this.mShortComponent;
                        }
                    } else {
                        ComponentName cn = ComponentName.unflattenFromString(name);
                        if (cn == null) {
                            if (this.mParseError == null) {
                                this.mParseError = "Bad set name " + name + " in preferred activity " + this.mShortComponent;
                            }
                        } else {
                            myPackages[setPos] = cn.getPackageName();
                            myClasses[setPos] = cn.getClassName();
                            myComponents[setPos] = name;
                            setPos++;
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                }
                str = null;
                i = 1;
            }
        }
        if (setPos != setCount && this.mParseError == null) {
            this.mParseError = "Not enough set tags (expected " + setCount + " but found " + setPos + ") in " + this.mShortComponent;
        }
        this.mSetPackages = myPackages;
        this.mSetClasses = myClasses;
        this.mSetComponents = myComponents;
    }

    public String getParseError() {
        return this.mParseError;
    }

    public void writeToXml(TypedXmlSerializer serializer, boolean full) throws IOException {
        String[] strArr = this.mSetClasses;
        int NS = strArr != null ? strArr.length : 0;
        serializer.attribute((String) null, "name", this.mShortComponent);
        if (full) {
            int i = this.mMatch;
            if (i != 0) {
                serializer.attributeIntHex((String) null, ATTR_MATCH, i);
            }
            serializer.attributeBoolean((String) null, ATTR_ALWAYS, this.mAlways);
            serializer.attributeInt((String) null, "set", NS);
            for (int s = 0; s < NS; s++) {
                serializer.startTag((String) null, "set");
                serializer.attribute((String) null, "name", this.mSetComponents[s]);
                serializer.endTag((String) null, "set");
            }
        }
    }

    public boolean sameSet(List<ResolveInfo> query, boolean excludeSetupWizardPackage, int userId) {
        PackageUserState pkgUserState;
        boolean z = false;
        if (this.mSetPackages == null) {
            return query == null;
        } else if (query == null) {
            return false;
        } else {
            int NQ = query.size();
            int NS = this.mSetPackages.length;
            PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            String setupWizardPackageName = pmi.getSetupWizardPackageName();
            int numMatch = 0;
            for (int i = 0; i < NQ; i++) {
                ResolveInfo ri = query.get(i);
                ActivityInfo ai = ri.activityInfo;
                boolean good = false;
                if ((!excludeSetupWizardPackage || !ai.packageName.equals(setupWizardPackageName)) && ((pkgUserState = pmi.getPackageStateInternal(ai.packageName).getUserStates().get(userId)) == null || pkgUserState.getInstallReason() != 3)) {
                    int j = 0;
                    while (true) {
                        if (j >= NS) {
                            break;
                        } else if (!this.mSetPackages[j].equals(ai.packageName) || !this.mSetClasses[j].equals(ai.name)) {
                            j++;
                        } else {
                            numMatch++;
                            good = true;
                            break;
                        }
                    }
                    if (!good) {
                        return false;
                    }
                    z = false;
                }
            }
            if (numMatch == NS) {
                return true;
            }
            return z;
        }
    }

    public boolean sameSet(ComponentName[] comps) {
        String[] strArr = this.mSetPackages;
        if (strArr == null) {
            return false;
        }
        int NS = strArr.length;
        int numMatch = 0;
        for (ComponentName cn : comps) {
            boolean good = false;
            int j = 0;
            while (true) {
                if (j >= NS) {
                    break;
                } else if (!this.mSetPackages[j].equals(cn.getPackageName()) || !this.mSetClasses[j].equals(cn.getClassName())) {
                    j++;
                } else {
                    numMatch++;
                    good = true;
                    break;
                }
            }
            if (!good) {
                return false;
            }
        }
        return numMatch == NS;
    }

    public boolean sameSet(PreferredComponent pc) {
        if (this.mSetPackages == null || pc == null || pc.mSetPackages == null || !sameComponent(pc.mComponent)) {
            return false;
        }
        int otherPackageCount = pc.mSetPackages.length;
        int packageCount = this.mSetPackages.length;
        if (otherPackageCount != packageCount) {
            return false;
        }
        for (int i = 0; i < packageCount; i++) {
            if (!this.mSetPackages[i].equals(pc.mSetPackages[i]) || !this.mSetClasses[i].equals(pc.mSetClasses[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean sameComponent(ComponentName comp) {
        ComponentName componentName = this.mComponent;
        return componentName != null && comp != null && componentName.getPackageName().equals(comp.getPackageName()) && this.mComponent.getClassName().equals(comp.getClassName());
    }

    public boolean isSuperset(List<ResolveInfo> query, boolean excludeSetupWizardPackage) {
        if (this.mSetPackages == null) {
            return query == null;
        } else if (query == null) {
            return true;
        } else {
            int NQ = query.size();
            int NS = this.mSetPackages.length;
            if (excludeSetupWizardPackage || NS >= NQ) {
                PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                String setupWizardPackageName = pmi.getSetupWizardPackageName();
                for (int i = 0; i < NQ; i++) {
                    ResolveInfo ri = query.get(i);
                    ActivityInfo ai = ri.activityInfo;
                    boolean foundMatch = false;
                    if (!excludeSetupWizardPackage || !ai.packageName.equals(setupWizardPackageName)) {
                        int j = 0;
                        while (true) {
                            if (j >= NS) {
                                break;
                            } else if (!this.mSetPackages[j].equals(ai.packageName) || !this.mSetClasses[j].equals(ai.name)) {
                                j++;
                            } else {
                                foundMatch = true;
                                break;
                            }
                        }
                        if (!foundMatch) {
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        }
    }

    public ComponentName[] discardObsoleteComponents(List<ResolveInfo> query) {
        if (this.mSetPackages == null || query == null) {
            return new ComponentName[0];
        }
        int NQ = query.size();
        int NS = this.mSetPackages.length;
        ArrayList<ComponentName> aliveComponents = new ArrayList<>();
        for (int i = 0; i < NQ; i++) {
            ResolveInfo ri = query.get(i);
            ActivityInfo ai = ri.activityInfo;
            int j = 0;
            while (true) {
                if (j >= NS) {
                    break;
                } else if (!this.mSetPackages[j].equals(ai.packageName) || !this.mSetClasses[j].equals(ai.name)) {
                    j++;
                } else {
                    aliveComponents.add(new ComponentName(this.mSetPackages[j], this.mSetClasses[j]));
                    break;
                }
            }
        }
        int i2 = aliveComponents.size();
        return (ComponentName[]) aliveComponents.toArray(new ComponentName[i2]);
    }

    public void dump(PrintWriter out, String prefix, Object ident) {
        out.print(prefix);
        out.print(Integer.toHexString(System.identityHashCode(ident)));
        out.print(' ');
        out.println(this.mShortComponent);
        out.print(prefix);
        out.print(" mMatch=0x");
        out.print(Integer.toHexString(this.mMatch));
        out.print(" mAlways=");
        out.println(this.mAlways);
        if (this.mSetComponents != null) {
            out.print(prefix);
            out.println("  Selected from:");
            for (int i = 0; i < this.mSetComponents.length; i++) {
                out.print(prefix);
                out.print("    ");
                out.println(this.mSetComponents[i]);
            }
        }
    }
}
