package com.android.server.voiceinteraction;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
class RecognitionServiceInfo {
    private static final String TAG = "RecognitionServiceInfo";
    private final String mParseError;
    private final boolean mSelectableAsDefault;
    private final ServiceInfo mServiceInfo;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static List<RecognitionServiceInfo> getAvailableServices(Context context, int user) {
        List<RecognitionServiceInfo> services = new ArrayList<>();
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentServicesAsUser(new Intent("android.speech.RecognitionService"), 786432, user);
        for (ResolveInfo resolveInfo : resolveInfos) {
            RecognitionServiceInfo service = parseInfo(context.getPackageManager(), resolveInfo.serviceInfo);
            if (!TextUtils.isEmpty(service.mParseError)) {
                Log.w(TAG, "Parse error in getAvailableServices: " + service.mParseError);
            }
            services.add(service);
        }
        return services;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static RecognitionServiceInfo parseInfo(PackageManager pm, ServiceInfo si) {
        XmlResourceParser parser;
        String parseError = "";
        boolean selectableAsDefault = true;
        try {
            parser = si.loadXmlMetaData(pm, "android.speech");
        } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e) {
            parseError = "Error parsing recognition service meta-data: " + e;
        }
        if (parser == null) {
            RecognitionServiceInfo recognitionServiceInfo = new RecognitionServiceInfo(si, true, "No android.speech meta-data for " + si.packageName);
            if (parser != null) {
                parser.close();
            }
            return recognitionServiceInfo;
        }
        Resources res = pm.getResourcesForApplication(si.applicationInfo);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        for (int type = 0; type != 1 && type != 2; type = parser.next()) {
        }
        String nodeName = parser.getName();
        if (!"recognition-service".equals(nodeName)) {
            throw new XmlPullParserException("Meta-data does not start with recognition-service tag");
        }
        TypedArray values = res.obtainAttributes(attrs, R.styleable.RecognitionService);
        selectableAsDefault = values.getBoolean(1, true);
        values.recycle();
        if (parser != null) {
            parser.close();
        }
        return new RecognitionServiceInfo(si, selectableAsDefault, parseError);
    }

    private RecognitionServiceInfo(ServiceInfo si, boolean selectableAsDefault, String parseError) {
        this.mServiceInfo = si;
        this.mSelectableAsDefault = selectableAsDefault;
        this.mParseError = parseError;
    }

    public String getParseError() {
        return this.mParseError;
    }

    public ServiceInfo getServiceInfo() {
        return this.mServiceInfo;
    }

    public boolean isSelectableAsDefault() {
        return this.mSelectableAsDefault;
    }
}
