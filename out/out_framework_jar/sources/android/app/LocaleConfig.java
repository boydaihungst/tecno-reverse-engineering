package android.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.LocaleList;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.R;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class LocaleConfig {
    public static final int STATUS_NOT_SPECIFIED = 1;
    public static final int STATUS_PARSING_FAILED = 2;
    public static final int STATUS_SUCCESS = 0;
    private static final String TAG = "LocaleConfig";
    public static final String TAG_LOCALE = "locale";
    public static final String TAG_LOCALE_CONFIG = "locale-config";
    private LocaleList mLocales;
    private int mStatus;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface Status {
    }

    public LocaleConfig(Context context) {
        int resId = 0;
        Resources res = context.getResources();
        try {
            resId = new ApplicationInfo(context.getApplicationInfo()).getLocaleConfigRes();
            XmlResourceParser parser = res.getXml(resId);
            parseLocaleConfig(parser, res);
        } catch (Resources.NotFoundException e) {
            Slog.w(TAG, "The resource file pointed to by the given resource ID isn't found.");
            this.mStatus = 1;
        } catch (IOException | XmlPullParserException e2) {
            Slog.w(TAG, "Failed to parse XML configuration from " + res.getResourceEntryName(resId), e2);
            this.mStatus = 2;
        }
    }

    private void parseLocaleConfig(XmlResourceParser parser, Resources res) throws IOException, XmlPullParserException {
        XmlUtils.beginDocument(parser, TAG_LOCALE_CONFIG);
        int outerDepth = parser.getDepth();
        AttributeSet attrs = Xml.asAttributeSet(parser);
        Set<String> localeNames = new HashSet<>();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if ("locale".equals(parser.getName())) {
                TypedArray attributes = res.obtainAttributes(attrs, R.styleable.LocaleConfig_Locale);
                String nameAttr = attributes.getString(0);
                localeNames.add(nameAttr);
                attributes.recycle();
            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }
        this.mStatus = 0;
        this.mLocales = LocaleList.forLanguageTags(String.join(",", localeNames));
    }

    public LocaleList getSupportedLocales() {
        return this.mLocales;
    }

    public int getStatus() {
        return this.mStatus;
    }
}
