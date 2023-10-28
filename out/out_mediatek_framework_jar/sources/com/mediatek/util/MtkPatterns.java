package com.mediatek.util;

import android.os.Bundle;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/* loaded from: classes.dex */
public class MtkPatterns {
    public static final String KEY_URLDATA_END = "end";
    public static final String KEY_URLDATA_START = "start";
    public static final String KEY_URLDATA_VALUE = "value";
    private static final String TAG = "MtkPatterns";
    private static final String[] MTK_WEB_PROTOCOL_NAMES = {"http://", "https://", "rtsp://", "ftp://"};
    private static final String mValidCharRegex = "a-zA-Z0-9\\-_";
    private static final String mBadFrontRemovingRegex = String.format("(^[^.]*[^%s.://#&=]+)(?:[a-zA-Z]+://|[%s]+.)", mValidCharRegex, mValidCharRegex);
    private static final String mBadEndRemovingRegex = String.format("([\\.\\:][%s)]+[/%s]*)([\\.\\:]?[^%s\\.\\:\\s/]+[^\\.=&%%/]*$)", mValidCharRegex, mValidCharRegex, mValidCharRegex);

    public static String[] getWebProtocolNames(String[] defaultProtocols) {
        String[] protocols = MTK_WEB_PROTOCOL_NAMES;
        return protocols;
    }

    private static final String replaceGroup(String regex, String source, int groupToReplace, String replacement) {
        return replaceGroup(regex, source, groupToReplace, 1, replacement);
    }

    private static final String replaceGroup(String regex, String source, int groupToReplace, int groupOccurrence, String replacement) {
        Matcher m = Pattern.compile(regex).matcher(source);
        for (int i = 0; i < groupOccurrence; i++) {
            if (!m.find()) {
                return source;
            }
        }
        return new StringBuilder(source).replace(m.start(groupToReplace), m.end(groupToReplace), replacement).toString();
    }

    public static Bundle getWebUrl(String urlStr, int start, int end) {
        Log.d("@M_MtkPatterns", "getWebUrl,  start=" + start + " end=" + end);
        if (urlStr != null) {
            String str = mBadFrontRemovingRegex;
            Pattern p1 = Pattern.compile(str);
            Matcher m1 = p1.matcher(urlStr);
            if (m1.find()) {
                urlStr = replaceGroup(str, urlStr, 1, "");
                start = end - urlStr.length();
            }
            String str2 = mBadEndRemovingRegex;
            Pattern p2 = Pattern.compile(str2);
            Matcher m2 = p2.matcher(urlStr);
            if (m2.find()) {
                urlStr = replaceGroup(str2, urlStr, 2, "");
                end = start + urlStr.length();
            }
        }
        Bundle data = new Bundle();
        data.putString(KEY_URLDATA_VALUE, urlStr);
        data.putInt(KEY_URLDATA_START, start);
        data.putInt(KEY_URLDATA_END, end);
        return data;
    }

    public static final Pattern getMtkWebUrlPattern(Pattern defaultPattern) {
        Pattern ret = ChinaPatterns.CHINA_AUTOLINK_WEB_URL;
        return ret;
    }
}
