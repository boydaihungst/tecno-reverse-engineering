package com.transsion.hubcore.utils;

import android.media.AudioSystem;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.Xml;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes4.dex */
public class TranDynamicFeatureSwitch {
    private static final String CONFIG_FILE_PATH = "/system/etc/thub_features_config.xml";
    private static final String TAG_FEATURE = "feature";
    private static final String TAG_FEATURES = "features";
    private static final String TAG_ITEM = "item";
    private static Map<String, String> mFeatureList;
    private static TranFeatureToggle mTranFeatureToggle;
    private static final String TAG = TranDynamicFeatureSwitch.class.getSimpleName();
    private static List<TranFeatureToggle> mTranFeatureToggles = new ArrayList();
    private static final boolean IS_USER_ROOT = "1".equals(SystemProperties.get("persist.user.root.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS));
    private static String mCurrentVersion = "1.0";
    private static boolean mInited = false;
    private static boolean DEBUG = true;

    public static boolean isFeatureOpen(String interfaceClass) {
        TranFeatureToggle tranFeatureToggle;
        if ((!Build.IS_USER || IS_USER_ROOT) && (tranFeatureToggle = getTranFeatureToggle(interfaceClass)) != null) {
            if (DEBUG) {
                Slog.d(TAG, "status = " + tranFeatureToggle.getStatus());
            }
            return tranFeatureToggle.getStatus();
        }
        return true;
    }

    public static String getFeatureName(String interfaceClass) {
        TranFeatureToggle tranFeatureToggle;
        if ((!Build.IS_USER || IS_USER_ROOT) && (tranFeatureToggle = getTranFeatureToggle(interfaceClass)) != null) {
            if (DEBUG) {
                Slog.d(TAG, "featureName = " + tranFeatureToggle.getFeatureName());
            }
            return tranFeatureToggle.getFeatureName();
        }
        return "unknown";
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [96=5] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:42:? */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:46:0x0005 */
    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r0v5, resolved type: java.io.BufferedInputStream */
    /* JADX DEBUG: Multi-variable search result rejected for r2v10, resolved type: org.xmlpull.v1.XmlPullParser */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v10 */
    /* JADX WARN: Type inference failed for: r0v11 */
    /* JADX WARN: Type inference failed for: r0v12 */
    /* JADX WARN: Type inference failed for: r0v3 */
    /* JADX WARN: Type inference failed for: r0v6 */
    /* JADX WARN: Type inference failed for: r0v7, types: [java.io.BufferedInputStream, java.io.InputStream] */
    /* JADX WARN: Type inference failed for: r0v8 */
    /* JADX WARN: Type inference failed for: r0v9 */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:23:0x003f -> B:40:0x0056). Please submit an issue!!! */
    private static TranFeatureToggle getTranFeatureToggle(String interfaceClass) {
        BufferedInputStream bis = mInited;
        if (bis == 0) {
            try {
                try {
                    bis = new BufferedInputStream(new FileInputStream(CONFIG_FILE_PATH));
                    try {
                        try {
                            try {
                                try {
                                    XmlPullParser parser = Xml.newPullParser();
                                    parser.setInput(bis, null);
                                    parse(parser);
                                    bis.close();
                                    bis = bis;
                                } catch (Throwable th) {
                                    try {
                                        bis.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    throw th;
                                }
                            } catch (NumberFormatException e2) {
                                e2.printStackTrace();
                                bis.close();
                                bis = bis;
                            }
                        } catch (XmlPullParserException e3) {
                            e3.printStackTrace();
                            bis.close();
                            bis = bis;
                        }
                    } catch (IOException e4) {
                        e4.printStackTrace();
                        bis.close();
                        bis = bis;
                    }
                } catch (FileNotFoundException e5) {
                    Slog.d(TAG, "thub features config file not existed!");
                    return null;
                }
            } catch (IOException e6) {
                e6.printStackTrace();
                bis = bis;
            }
        }
        for (TranFeatureToggle tranFeatureToggle : mTranFeatureToggles) {
            if (tranFeatureToggle.getFeatureList().containsKey(interfaceClass)) {
                return tranFeatureToggle;
            }
        }
        return null;
    }

    private static void parse(XmlPullParser parser) throws IOException, XmlPullParserException, NumberFormatException {
        while (true) {
            int type = parser.next();
            if (type != 1) {
                switch (type) {
                    case 2:
                        mInited = true;
                        if (!"features".equals(parser.getName())) {
                            if (TAG_FEATURE.equals(parser.getName())) {
                                mTranFeatureToggle = new TranFeatureToggle();
                                mFeatureList = new HashMap();
                                String featureName = parser.getAttributeValue(0);
                                mTranFeatureToggle.setFeatureName(featureName);
                                String featureStatus = parser.getAttributeValue(1);
                                mTranFeatureToggle.setStatus(true ^ "disable".equals(featureStatus.trim().toLowerCase()));
                                break;
                            } else if ("item".equals(parser.getName())) {
                                String interfaceName = parser.getAttributeValue(0);
                                String implName = parser.getAttributeValue(1);
                                mFeatureList.put(interfaceName, implName);
                                break;
                            } else {
                                break;
                            }
                        } else {
                            mCurrentVersion = parser.getAttributeValue(0);
                            if (DEBUG) {
                                Slog.d(TAG, "mCurrentVersion = " + mCurrentVersion);
                                break;
                            } else {
                                break;
                            }
                        }
                    case 3:
                        if (TAG_FEATURE.equals(parser.getName())) {
                            mTranFeatureToggle.setFeatureList(mFeatureList);
                            mTranFeatureToggles.add(mTranFeatureToggle);
                            break;
                        } else {
                            break;
                        }
                }
            } else {
                for (TranFeatureToggle tranFeatureToggle : mTranFeatureToggles) {
                    if (DEBUG) {
                        Slog.d(TAG, "parse: tranFeatureToggle => " + tranFeatureToggle);
                    }
                }
                return;
            }
        }
    }
}
