package com.android.server.biometrics.sensors.fingerprint;

import android.content.Context;
import android.hardware.fingerprint.Fingerprint;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.server.biometrics.sensors.BiometricUserState;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class FingerprintUserState extends BiometricUserState<Fingerprint> {
    private static final String ATTR_DEVICE_ID = "deviceId";
    private static final String ATTR_FINGER_ID = "fingerId";
    private static final String ATTR_GROUP_ID = "groupId";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PACKAGENAME = "packagename";
    private static final String ATTR_USR_ID = "userId";
    private static final String TAG = "FingerprintState";
    private static final String TAG_FINGERPRINT = "fingerprint";
    private static final String TAG_FINGERPRINTS = "fingerprints";

    public FingerprintUserState(Context context, int userId, String fileName) {
        super(context, userId, fileName);
    }

    @Override // com.android.server.biometrics.sensors.BiometricUserState
    protected String getBiometricsTag() {
        return TAG_FINGERPRINTS;
    }

    @Override // com.android.server.biometrics.sensors.BiometricUserState
    protected int getNameTemplateResource() {
        return 17040359;
    }

    @Override // com.android.server.biometrics.sensors.BiometricUserState
    protected ArrayList<Fingerprint> getCopy(ArrayList<Fingerprint> array) {
        ArrayList<Fingerprint> result = new ArrayList<>();
        Iterator<Fingerprint> it = array.iterator();
        while (it.hasNext()) {
            Fingerprint fp = it.next();
            result.add(new Fingerprint(fp.getName(), fp.getGroupId(), fp.getBiometricId(), fp.getDeviceId(), fp.getAppPkgName(), fp.getSubUserId()));
        }
        return result;
    }

    @Override // com.android.server.biometrics.sensors.BiometricUserState
    protected void doWriteState(TypedXmlSerializer serializer) throws Exception {
        ArrayList<Fingerprint> fingerprints;
        synchronized (this) {
            fingerprints = getCopy(this.mBiometrics);
        }
        serializer.startTag((String) null, TAG_FINGERPRINTS);
        int count = fingerprints.size();
        for (int i = 0; i < count; i++) {
            Fingerprint fp = fingerprints.get(i);
            serializer.startTag((String) null, TAG_FINGERPRINT);
            serializer.attributeInt((String) null, ATTR_FINGER_ID, fp.getBiometricId());
            serializer.attribute((String) null, "name", fp.getName().toString());
            serializer.attributeInt((String) null, ATTR_GROUP_ID, fp.getGroupId());
            serializer.attributeLong((String) null, ATTR_DEVICE_ID, fp.getDeviceId());
            serializer.attribute((String) null, ATTR_PACKAGENAME, fp.getAppPkgName().toString());
            serializer.attribute((String) null, "userId", Integer.toString(fp.getSubUserId()));
            serializer.endTag((String) null, TAG_FINGERPRINT);
        }
        serializer.endTag((String) null, TAG_FINGERPRINTS);
    }

    @Override // com.android.server.biometrics.sensors.BiometricUserState
    protected void parseBiometricsLocked(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth;
        TypedXmlPullParser typedXmlPullParser = parser;
        int outerDepth2 = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth2) {
                    if (type == 3) {
                        typedXmlPullParser = parser;
                    } else if (type != 4) {
                        String tagName = parser.getName();
                        if (!tagName.equals(TAG_FINGERPRINT)) {
                            outerDepth = outerDepth2;
                        } else {
                            String name = typedXmlPullParser.getAttributeValue((String) null, "name");
                            int groupId = typedXmlPullParser.getAttributeInt((String) null, ATTR_GROUP_ID);
                            int fingerId = typedXmlPullParser.getAttributeInt((String) null, ATTR_FINGER_ID);
                            long deviceId = typedXmlPullParser.getAttributeLong((String) null, ATTR_DEVICE_ID);
                            String packagename = typedXmlPullParser.getAttributeValue((String) null, ATTR_PACKAGENAME);
                            String userId = typedXmlPullParser.getAttributeValue((String) null, "userId");
                            outerDepth = outerDepth2;
                            this.mBiometrics.add(new Fingerprint(name, groupId, fingerId, deviceId, packagename, userId == null ? 0 : Integer.parseInt(userId)));
                        }
                        typedXmlPullParser = parser;
                        outerDepth2 = outerDepth;
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }
}
