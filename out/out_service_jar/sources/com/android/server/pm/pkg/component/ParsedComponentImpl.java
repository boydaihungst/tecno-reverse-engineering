package com.android.server.pm.pkg.component;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.internal.util.CollectionUtils;
import com.android.server.pm.pkg.parsing.ParsingPackageImpl;
import com.android.server.pm.pkg.parsing.ParsingUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/* loaded from: classes2.dex */
public abstract class ParsedComponentImpl implements ParsedComponent, Parcelable {
    private int banner;
    private ComponentName componentName;
    private int descriptionRes;
    private int flags;
    private int icon;
    private List<ParsedIntentInfoImpl> intents;
    private int labelRes;
    private int logo;
    private Map<String, PackageManager.Property> mProperties;
    private Bundle metaData;
    private String name;
    private CharSequence nonLocalizedLabel;
    private String packageName;
    int themedIcon;

    public ParsedComponentImpl() {
        this.intents = Collections.emptyList();
        this.mProperties = Collections.emptyMap();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public ParsedComponentImpl(ParsedComponent other) {
        this.intents = Collections.emptyList();
        this.mProperties = Collections.emptyMap();
        this.metaData = other.getMetaData();
        this.name = other.getName();
        this.icon = other.getIcon();
        this.themedIcon = other.getThemedIcon();
        this.labelRes = other.getLabelRes();
        this.nonLocalizedLabel = other.getNonLocalizedLabel();
        this.logo = other.getLogo();
        this.banner = other.getBanner();
        this.descriptionRes = other.getDescriptionRes();
        this.flags = other.getFlags();
        this.packageName = other.getPackageName();
        this.componentName = other.getComponentName();
        this.intents = new ArrayList(((ParsedComponentImpl) other).intents);
        ArrayMap arrayMap = new ArrayMap();
        this.mProperties = arrayMap;
        arrayMap.putAll(other.getProperties());
    }

    public void addIntent(ParsedIntentInfoImpl intent) {
        this.intents = CollectionUtils.add(this.intents, intent);
    }

    public void addProperty(PackageManager.Property property) {
        this.mProperties = CollectionUtils.add(this.mProperties, property.getName(), property);
    }

    public ParsedComponentImpl setName(String name) {
        this.name = TextUtils.safeIntern(name);
        return this;
    }

    public void setPackageName(String packageName) {
        this.packageName = TextUtils.safeIntern(packageName);
        this.componentName = null;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public ComponentName getComponentName() {
        if (this.componentName == null) {
            this.componentName = new ComponentName(getPackageName(), getName());
        }
        return this.componentName;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public Bundle getMetaData() {
        Bundle bundle = this.metaData;
        return bundle == null ? Bundle.EMPTY : bundle;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public List<ParsedIntentInfo> getIntents() {
        return new ArrayList(this.intents);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(getIcon());
        dest.writeInt(getThemedIcon());
        dest.writeInt(getLabelRes());
        dest.writeCharSequence(getNonLocalizedLabel());
        dest.writeInt(getLogo());
        dest.writeInt(getBanner());
        dest.writeInt(getDescriptionRes());
        dest.writeInt(getFlags());
        ParsingPackageImpl.sForInternedString.parcel(this.packageName, dest, flags);
        dest.writeTypedList(this.intents);
        dest.writeBundle(this.metaData);
        dest.writeMap(this.mProperties);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public ParsedComponentImpl(Parcel in) {
        this.intents = Collections.emptyList();
        this.mProperties = Collections.emptyMap();
        ClassLoader boot = Object.class.getClassLoader();
        this.name = in.readString();
        this.icon = in.readInt();
        this.themedIcon = in.readInt();
        this.labelRes = in.readInt();
        this.nonLocalizedLabel = in.readCharSequence();
        this.logo = in.readInt();
        this.banner = in.readInt();
        this.descriptionRes = in.readInt();
        this.flags = in.readInt();
        this.packageName = ParsingPackageImpl.sForInternedString.unparcel(in);
        this.intents = ParsingUtils.createTypedInterfaceList(in, ParsedIntentInfoImpl.CREATOR);
        this.metaData = in.readBundle(boot);
        this.mProperties = in.readHashMap(boot);
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public String getName() {
        return this.name;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public int getIcon() {
        return this.icon;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public int getLabelRes() {
        return this.labelRes;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public int getThemedIcon() {
        return this.themedIcon;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public void setThemedIcon(int icon) {
        this.themedIcon = icon;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public CharSequence getNonLocalizedLabel() {
        return this.nonLocalizedLabel;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public int getLogo() {
        return this.logo;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public int getBanner() {
        return this.banner;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public int getDescriptionRes() {
        return this.descriptionRes;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public int getFlags() {
        return this.flags;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public String getPackageName() {
        return this.packageName;
    }

    @Override // com.android.server.pm.pkg.component.ParsedComponent
    public Map<String, PackageManager.Property> getProperties() {
        return this.mProperties;
    }

    public ParsedComponentImpl setIcon(int value) {
        this.icon = value;
        return this;
    }

    public ParsedComponentImpl setLabelRes(int value) {
        this.labelRes = value;
        return this;
    }

    public ParsedComponentImpl setNonLocalizedLabel(CharSequence value) {
        this.nonLocalizedLabel = value;
        return this;
    }

    public ParsedComponentImpl setLogo(int value) {
        this.logo = value;
        return this;
    }

    public ParsedComponentImpl setBanner(int value) {
        this.banner = value;
        return this;
    }

    public ParsedComponentImpl setDescriptionRes(int value) {
        this.descriptionRes = value;
        return this;
    }

    public ParsedComponentImpl setFlags(int value) {
        this.flags = value;
        return this;
    }

    public ParsedComponentImpl setMetaData(Bundle value) {
        this.metaData = value;
        return this;
    }

    @Deprecated
    private void __metadata() {
    }
}
