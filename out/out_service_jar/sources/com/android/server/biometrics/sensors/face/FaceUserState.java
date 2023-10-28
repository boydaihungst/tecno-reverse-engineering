package com.android.server.biometrics.sensors.face;

import android.content.Context;
import android.hardware.face.Face;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.server.biometrics.sensors.BiometricUserState;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class FaceUserState extends BiometricUserState<Face> {
    private static final String ATTR_DEVICE_ID = "deviceId";
    private static final String ATTR_FACE_ID = "faceId";
    private static final String ATTR_NAME = "name";
    private static final String TAG = "FaceState";
    private static final String TAG_FACE = "face";
    private static final String TAG_FACES = "faces";

    public FaceUserState(Context ctx, int userId, String fileName) {
        super(ctx, userId, fileName);
    }

    @Override // com.android.server.biometrics.sensors.BiometricUserState
    protected String getBiometricsTag() {
        return TAG_FACES;
    }

    @Override // com.android.server.biometrics.sensors.BiometricUserState
    protected int getNameTemplateResource() {
        return 17040305;
    }

    @Override // com.android.server.biometrics.sensors.BiometricUserState
    protected ArrayList<Face> getCopy(ArrayList<Face> array) {
        ArrayList<Face> result = new ArrayList<>();
        Iterator<Face> it = array.iterator();
        while (it.hasNext()) {
            Face f = it.next();
            result.add(new Face(f.getName(), f.getBiometricId(), f.getDeviceId()));
        }
        return result;
    }

    @Override // com.android.server.biometrics.sensors.BiometricUserState
    protected void doWriteState(TypedXmlSerializer serializer) throws Exception {
        ArrayList<Face> faces;
        synchronized (this) {
            faces = getCopy(this.mBiometrics);
        }
        serializer.startTag((String) null, TAG_FACES);
        int count = faces.size();
        for (int i = 0; i < count; i++) {
            Face f = faces.get(i);
            serializer.startTag((String) null, TAG_FACE);
            serializer.attributeInt((String) null, ATTR_FACE_ID, f.getBiometricId());
            serializer.attribute((String) null, "name", f.getName().toString());
            serializer.attributeLong((String) null, ATTR_DEVICE_ID, f.getDeviceId());
            serializer.endTag((String) null, TAG_FACE);
        }
        serializer.endTag((String) null, TAG_FACES);
    }

    @Override // com.android.server.biometrics.sensors.BiometricUserState
    protected void parseBiometricsLocked(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_FACE)) {
                            String name = parser.getAttributeValue((String) null, "name");
                            int faceId = parser.getAttributeInt((String) null, ATTR_FACE_ID);
                            long deviceId = parser.getAttributeLong((String) null, ATTR_DEVICE_ID);
                            this.mBiometrics.add(new Face(name, faceId, deviceId));
                        }
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
