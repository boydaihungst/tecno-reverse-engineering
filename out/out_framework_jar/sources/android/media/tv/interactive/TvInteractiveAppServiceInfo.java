package android.media.tv.interactive;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public final class TvInteractiveAppServiceInfo implements Parcelable {
    public static final Parcelable.Creator<TvInteractiveAppServiceInfo> CREATOR = new Parcelable.Creator<TvInteractiveAppServiceInfo>() { // from class: android.media.tv.interactive.TvInteractiveAppServiceInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TvInteractiveAppServiceInfo createFromParcel(Parcel in) {
            return new TvInteractiveAppServiceInfo(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TvInteractiveAppServiceInfo[] newArray(int size) {
            return new TvInteractiveAppServiceInfo[size];
        }
    };
    private static final boolean DEBUG = false;
    public static final int INTERACTIVE_APP_TYPE_ATSC = 2;
    public static final int INTERACTIVE_APP_TYPE_GINGA = 4;
    public static final int INTERACTIVE_APP_TYPE_HBBTV = 1;
    private static final String TAG = "TvInteractiveAppServiceInfo";
    private static final String XML_START_TAG_NAME = "tv-interactive-app";
    private final String mId;
    private final ResolveInfo mService;
    private int mTypes;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface InteractiveAppType {
    }

    public TvInteractiveAppServiceInfo(Context context, ComponentName component) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null.");
        }
        Intent intent = new Intent(TvInteractiveAppService.SERVICE_INTERFACE).setComponent(component);
        ResolveInfo resolveInfo = context.getPackageManager().resolveService(intent, 132);
        if (resolveInfo == null) {
            throw new IllegalArgumentException("Invalid component. Can't find the service.");
        }
        ComponentName componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
        String id = generateInteractiveAppServiceId(componentName);
        List<String> types = new ArrayList<>();
        parseServiceMetadata(resolveInfo, context, types);
        this.mService = resolveInfo;
        this.mId = id;
        this.mTypes = toTypesFlag(types);
    }

    private TvInteractiveAppServiceInfo(ResolveInfo service, String id, int types) {
        this.mService = service;
        this.mId = id;
        this.mTypes = types;
    }

    private TvInteractiveAppServiceInfo(Parcel in) {
        this.mService = ResolveInfo.CREATOR.createFromParcel(in);
        this.mId = in.readString();
        this.mTypes = in.readInt();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        this.mService.writeToParcel(dest, flags);
        dest.writeString(this.mId);
        dest.writeInt(this.mTypes);
    }

    public String getId() {
        return this.mId;
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mService.serviceInfo.packageName, this.mService.serviceInfo.name);
    }

    public ServiceInfo getServiceInfo() {
        return this.mService.serviceInfo;
    }

    public int getSupportedTypes() {
        return this.mTypes;
    }

    private static String generateInteractiveAppServiceId(ComponentName name) {
        return name.flattenToShortString();
    }

    private static void parseServiceMetadata(ResolveInfo resolveInfo, Context context, List<String> types) {
        ServiceInfo si = resolveInfo.serviceInfo;
        PackageManager pm = context.getPackageManager();
        try {
            XmlResourceParser parser = si.loadXmlMetaData(pm, TvInteractiveAppService.SERVICE_META_DATA);
            if (parser == null) {
                throw new IllegalStateException("No android.media.tv.interactive.app meta-data found for " + si.name);
            }
            Resources res = pm.getResourcesForApplication(si.applicationInfo);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            while (true) {
                int type = parser.next();
                if (type == 1 || type == 2) {
                    break;
                }
            }
            String nodeName = parser.getName();
            if (!XML_START_TAG_NAME.equals(nodeName)) {
                throw new IllegalStateException("Meta-data does not start with tv-interactive-app tag for " + si.name);
            }
            TypedArray sa = res.obtainAttributes(attrs, R.styleable.TvInteractiveAppService);
            CharSequence[] textArr = sa.getTextArray(0);
            for (CharSequence cs : textArr) {
                types.add(cs.toString().toLowerCase());
            }
            sa.recycle();
            if (parser != null) {
                parser.close();
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("No resources found for " + si.packageName, e);
        } catch (IOException | XmlPullParserException e2) {
            throw new IllegalStateException("Failed reading meta-data for " + si.packageName, e2);
        }
    }

    private static int toTypesFlag(List<String> types) {
        int flag = 0;
        for (String type : types) {
            char c = 65535;
            switch (type.hashCode()) {
                case 3004867:
                    if (type.equals("atsc")) {
                        c = 1;
                        break;
                    }
                    break;
                case 98359718:
                    if (type.equals("ginga")) {
                        c = 2;
                        break;
                    }
                    break;
                case 99063594:
                    if (type.equals("hbbtv")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    flag |= 1;
                    break;
                case 1:
                    flag |= 2;
                    break;
                case 2:
                    flag |= 4;
                    break;
            }
        }
        return flag;
    }
}
