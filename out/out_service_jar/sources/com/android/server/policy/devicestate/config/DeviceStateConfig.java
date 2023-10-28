package com.android.server.policy.devicestate.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class DeviceStateConfig {
    private List<DeviceState> deviceState;

    public List<DeviceState> getDeviceState() {
        if (this.deviceState == null) {
            this.deviceState = new ArrayList();
        }
        return this.deviceState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DeviceStateConfig read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        DeviceStateConfig instance = new DeviceStateConfig();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("device-state")) {
                    DeviceState value = DeviceState.read(parser);
                    instance.getDeviceState().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("DeviceStateConfig is not closed");
        }
        return instance;
    }
}
