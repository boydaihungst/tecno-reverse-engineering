package com.android.server.tv.tunerresourcemanager;

import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.Xml;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.SystemService;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class UseCasePriorityHints {
    private static final int INVALID_PRIORITY_VALUE = -1;
    private static final int INVALID_USE_CASE = -1;
    private static final String PATH_TO_VENDOR_CONFIG_XML = "/vendor/etc/tunerResourceManagerUseCaseConfig.xml";
    private static final String TAG = "UseCasePriorityHints";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String NS = null;
    SparseArray<int[]> mPriorityHints = new SparseArray<>();
    Set<Integer> mVendorDefinedUseCase = new HashSet();
    private int mDefaultForeground = 150;
    private int mDefaultBackground = 50;

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getForegroundPriority(int useCase) {
        if (this.mPriorityHints.get(useCase) != null && this.mPriorityHints.get(useCase).length == 2) {
            return this.mPriorityHints.get(useCase)[0];
        }
        return this.mDefaultForeground;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getBackgroundPriority(int useCase) {
        if (this.mPriorityHints.get(useCase) != null && this.mPriorityHints.get(useCase).length == 2) {
            return this.mPriorityHints.get(useCase)[1];
        }
        return this.mDefaultBackground;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDefinedUseCase(int useCase) {
        return this.mVendorDefinedUseCase.contains(Integer.valueOf(useCase)) || isPredefinedUseCase(useCase);
    }

    public void parse() {
        File file = new File(PATH_TO_VENDOR_CONFIG_XML);
        if (file.exists()) {
            try {
                InputStream in = new FileInputStream(file);
                parseInternal(in);
                return;
            } catch (IOException e) {
                Slog.e(TAG, "Error reading vendor file: " + file, e);
                return;
            } catch (XmlPullParserException e2) {
                Slog.e(TAG, "Unable to parse vendor file: " + file, e2);
                return;
            }
        }
        if (DEBUG) {
            Slog.i(TAG, "no vendor priority configuration available. Using default priority");
        }
        addNewUseCasePriority(100, FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CREDENTIAL_MANAGEMENT_APP_REQUEST_ACCEPTED, 100);
        addNewUseCasePriority(200, 450, 200);
        addNewUseCasePriority(300, SystemService.PHASE_LOCK_SETTINGS_READY, 300);
        addNewUseCasePriority(400, 490, 400);
        addNewUseCasePriority(500, 600, 500);
    }

    protected void parseInternal(InputStream in) throws IOException, XmlPullParserException {
        try {
            TypedXmlPullParser parser = Xml.resolvePullParser(in);
            parser.nextTag();
            readUseCase(parser);
            in.close();
            for (int i = 0; i < this.mPriorityHints.size(); i++) {
                int useCase = this.mPriorityHints.keyAt(i);
                int[] priorities = this.mPriorityHints.get(useCase);
                if (DEBUG) {
                    Slog.d(TAG, "{defaultFg=" + this.mDefaultForeground + ", defaultBg=" + this.mDefaultBackground + "}");
                    Slog.d(TAG, "{useCase=" + useCase + ", fg=" + priorities[0] + ", bg=" + priorities[1] + "}");
                }
            }
        } catch (IOException | XmlPullParserException e) {
            throw e;
        }
    }

    private void readUseCase(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(2, NS, "config");
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                if (name.equals("useCaseDefault")) {
                    this.mDefaultForeground = readAttributeToInt("fgPriority", parser);
                    this.mDefaultBackground = readAttributeToInt("bgPriority", parser);
                    parser.nextTag();
                    parser.require(3, NS, name);
                } else if (name.equals("useCasePreDefined")) {
                    int useCase = formatTypeToNum(DatabaseHelper.SoundModelContract.KEY_TYPE, parser);
                    if (useCase == -1) {
                        Slog.e(TAG, "Wrong predefined use case name given in the vendor config.");
                    } else {
                        addNewUseCasePriority(useCase, readAttributeToInt("fgPriority", parser), readAttributeToInt("bgPriority", parser));
                        parser.nextTag();
                        parser.require(3, NS, name);
                    }
                } else if (name.equals("useCaseVendor")) {
                    int useCase2 = readAttributeToInt("id", parser);
                    addNewUseCasePriority(useCase2, readAttributeToInt("fgPriority", parser), readAttributeToInt("bgPriority", parser));
                    this.mVendorDefinedUseCase.add(Integer.valueOf(useCase2));
                    parser.nextTag();
                    parser.require(3, NS, name);
                } else {
                    skip(parser);
                }
            }
        }
    }

    private void skip(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != 2) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case 2:
                    depth++;
                    break;
                case 3:
                    depth--;
                    break;
            }
        }
    }

    private int readAttributeToInt(String attributeName, TypedXmlPullParser parser) throws XmlPullParserException {
        return parser.getAttributeInt((String) null, attributeName);
    }

    private void addNewUseCasePriority(int useCase, int fgPriority, int bgPriority) {
        int[] priorities = {fgPriority, bgPriority};
        this.mPriorityHints.append(useCase, priorities);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static int formatTypeToNum(String attributeName, TypedXmlPullParser parser) {
        char c;
        String useCaseName = parser.getAttributeValue((String) null, attributeName);
        switch (useCaseName.hashCode()) {
            case -884787515:
                if (useCaseName.equals("USE_CASE_BACKGROUND")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 377959794:
                if (useCaseName.equals("USE_CASE_PLAYBACK")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1222007747:
                if (useCaseName.equals("USE_CASE_LIVE")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1222209876:
                if (useCaseName.equals("USE_CASE_SCAN")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1990900072:
                if (useCaseName.equals("USE_CASE_RECORD")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return 100;
            case 1:
                return 200;
            case 2:
                return 300;
            case 3:
                return 400;
            case 4:
                return 500;
            default:
                return -1;
        }
    }

    private static boolean isPredefinedUseCase(int useCase) {
        switch (useCase) {
            case 100:
            case 200:
            case 300:
            case 400:
            case 500:
                return true;
            default:
                return false;
        }
    }
}
