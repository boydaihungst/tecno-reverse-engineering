package com.android.server.input;

import android.text.TextUtils;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
class ConfigurationProcessor {
    private static final String TAG = "ConfigurationProcessor";

    ConfigurationProcessor() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static List<String> processExcludedDeviceNames(InputStream xml) throws Exception {
        List<String> names = new ArrayList<>();
        TypedXmlPullParser parser = Xml.resolvePullParser(xml);
        XmlUtils.beginDocument(parser, "devices");
        while (true) {
            XmlUtils.nextElement(parser);
            if ("device".equals(parser.getName())) {
                String name = parser.getAttributeValue((String) null, "name");
                if (name != null) {
                    names.add(name);
                }
            } else {
                return names;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Map<String, Integer> processInputPortAssociations(InputStream xml) throws Exception {
        Map<String, Integer> associations = new HashMap<>();
        TypedXmlPullParser parser = Xml.resolvePullParser(xml);
        XmlUtils.beginDocument(parser, "ports");
        while (true) {
            XmlUtils.nextElement(parser);
            String entryName = parser.getName();
            if ("port".equals(entryName)) {
                String inputPort = parser.getAttributeValue((String) null, "input");
                String displayPortStr = parser.getAttributeValue((String) null, "display");
                if (TextUtils.isEmpty(inputPort) || TextUtils.isEmpty(displayPortStr)) {
                    Slog.wtf(TAG, "Ignoring incomplete entry");
                } else {
                    try {
                        int displayPort = Integer.parseUnsignedInt(displayPortStr);
                        associations.put(inputPort, Integer.valueOf(displayPort));
                    } catch (NumberFormatException e) {
                        Slog.wtf(TAG, "Display port should be an integer");
                    }
                }
            } else {
                return associations;
            }
        }
    }
}
