package com.android.server.pm;

import android.text.TextUtils;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ShareTargetInfo {
    private static final String ATTR_HOST = "host";
    private static final String ATTR_MIME_TYPE = "mimeType";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PATH = "path";
    private static final String ATTR_PATH_PATTERN = "pathPattern";
    private static final String ATTR_PATH_PREFIX = "pathPrefix";
    private static final String ATTR_PORT = "port";
    private static final String ATTR_SCHEME = "scheme";
    private static final String ATTR_TARGET_CLASS = "targetClass";
    private static final String TAG_CATEGORY = "category";
    private static final String TAG_DATA = "data";
    private static final String TAG_SHARE_TARGET = "share-target";
    final String[] mCategories;
    final String mTargetClass;
    final TargetData[] mTargetData;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class TargetData {
        final String mHost;
        final String mMimeType;
        final String mPath;
        final String mPathPattern;
        final String mPathPrefix;
        final String mPort;
        final String mScheme;

        /* JADX INFO: Access modifiers changed from: package-private */
        public TargetData(String scheme, String host, String port, String path, String pathPattern, String pathPrefix, String mimeType) {
            this.mScheme = scheme;
            this.mHost = host;
            this.mPort = port;
            this.mPath = path;
            this.mPathPattern = pathPattern;
            this.mPathPrefix = pathPrefix;
            this.mMimeType = mimeType;
        }

        public void toStringInner(StringBuilder strBuilder) {
            if (!TextUtils.isEmpty(this.mScheme)) {
                strBuilder.append(" scheme=").append(this.mScheme);
            }
            if (!TextUtils.isEmpty(this.mHost)) {
                strBuilder.append(" host=").append(this.mHost);
            }
            if (!TextUtils.isEmpty(this.mPort)) {
                strBuilder.append(" port=").append(this.mPort);
            }
            if (!TextUtils.isEmpty(this.mPath)) {
                strBuilder.append(" path=").append(this.mPath);
            }
            if (!TextUtils.isEmpty(this.mPathPattern)) {
                strBuilder.append(" pathPattern=").append(this.mPathPattern);
            }
            if (!TextUtils.isEmpty(this.mPathPrefix)) {
                strBuilder.append(" pathPrefix=").append(this.mPathPrefix);
            }
            if (!TextUtils.isEmpty(this.mMimeType)) {
                strBuilder.append(" mimeType=").append(this.mMimeType);
            }
        }

        public String toString() {
            StringBuilder strBuilder = new StringBuilder();
            toStringInner(strBuilder);
            return strBuilder.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ShareTargetInfo(TargetData[] data, String targetClass, String[] categories) {
        this.mTargetData = data;
        this.mTargetClass = targetClass;
        this.mCategories = categories;
    }

    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("targetClass=").append(this.mTargetClass);
        for (int i = 0; i < this.mTargetData.length; i++) {
            strBuilder.append(" data={");
            this.mTargetData[i].toStringInner(strBuilder);
            strBuilder.append("}");
        }
        for (int i2 = 0; i2 < this.mCategories.length; i2++) {
            strBuilder.append(" category=").append(this.mCategories[i2]);
        }
        return strBuilder.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveToXml(TypedXmlSerializer out) throws IOException {
        out.startTag((String) null, TAG_SHARE_TARGET);
        ShortcutService.writeAttr(out, ATTR_TARGET_CLASS, this.mTargetClass);
        for (int i = 0; i < this.mTargetData.length; i++) {
            out.startTag((String) null, "data");
            ShortcutService.writeAttr(out, ATTR_SCHEME, this.mTargetData[i].mScheme);
            ShortcutService.writeAttr(out, "host", this.mTargetData[i].mHost);
            ShortcutService.writeAttr(out, ATTR_PORT, this.mTargetData[i].mPort);
            ShortcutService.writeAttr(out, ATTR_PATH, this.mTargetData[i].mPath);
            ShortcutService.writeAttr(out, ATTR_PATH_PATTERN, this.mTargetData[i].mPathPattern);
            ShortcutService.writeAttr(out, ATTR_PATH_PREFIX, this.mTargetData[i].mPathPrefix);
            ShortcutService.writeAttr(out, ATTR_MIME_TYPE, this.mTargetData[i].mMimeType);
            out.endTag((String) null, "data");
        }
        for (int i2 = 0; i2 < this.mCategories.length; i2++) {
            out.startTag((String) null, TAG_CATEGORY);
            ShortcutService.writeAttr(out, "name", this.mCategories[i2]);
            out.endTag((String) null, TAG_CATEGORY);
        }
        out.endTag((String) null, TAG_SHARE_TARGET);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:11:0x002f, code lost:
        if (r3.equals(com.android.server.pm.ShareTargetInfo.TAG_CATEGORY) != false) goto L32;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static ShareTargetInfo loadFromXml(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        String targetClass = ShortcutService.parseStringAttribute(parser, ATTR_TARGET_CLASS);
        ArrayList<TargetData> targetData = new ArrayList<>();
        ArrayList<String> categories = new ArrayList<>();
        while (true) {
            int type = parser.next();
            boolean z = true;
            if (type != 1) {
                if (type == 2) {
                    String name = parser.getName();
                    switch (name.hashCode()) {
                        case 3076010:
                            if (name.equals("data")) {
                                z = false;
                                break;
                            }
                            z = true;
                            break;
                        case 50511102:
                            break;
                        default:
                            z = true;
                            break;
                    }
                    switch (z) {
                        case false:
                            targetData.add(parseTargetData(parser));
                            continue;
                        case true:
                            categories.add(ShortcutService.parseStringAttribute(parser, "name"));
                            continue;
                    }
                } else if (type == 3 && parser.getName().equals(TAG_SHARE_TARGET)) {
                }
            }
        }
        if (targetData.isEmpty() || targetClass == null || categories.isEmpty()) {
            return null;
        }
        return new ShareTargetInfo((TargetData[]) targetData.toArray(new TargetData[targetData.size()]), targetClass, (String[]) categories.toArray(new String[categories.size()]));
    }

    private static TargetData parseTargetData(TypedXmlPullParser parser) {
        String scheme = ShortcutService.parseStringAttribute(parser, ATTR_SCHEME);
        String host = ShortcutService.parseStringAttribute(parser, "host");
        String port = ShortcutService.parseStringAttribute(parser, ATTR_PORT);
        String path = ShortcutService.parseStringAttribute(parser, ATTR_PATH);
        String pathPattern = ShortcutService.parseStringAttribute(parser, ATTR_PATH_PATTERN);
        String pathPrefix = ShortcutService.parseStringAttribute(parser, ATTR_PATH_PREFIX);
        String mimeType = ShortcutService.parseStringAttribute(parser, ATTR_MIME_TYPE);
        return new TargetData(scheme, host, port, path, pathPattern, pathPrefix, mimeType);
    }
}
