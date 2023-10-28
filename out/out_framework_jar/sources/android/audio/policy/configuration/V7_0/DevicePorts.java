package android.audio.policy.configuration.V7_0;

import android.content.Context;
import android.media.MediaFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class DevicePorts {
    private List<DevicePort> devicePort;

    /* loaded from: classes.dex */
    public static class DevicePort {
        private Boolean _default;
        private String address;
        private List<String> encodedFormats;
        private Gains gains;
        private List<Profile> profile;
        private Role role;
        private String tagName;
        private String type;

        public List<Profile> getProfile() {
            if (this.profile == null) {
                this.profile = new ArrayList();
            }
            return this.profile;
        }

        public Gains getGains() {
            return this.gains;
        }

        boolean hasGains() {
            if (this.gains == null) {
                return false;
            }
            return true;
        }

        public void setGains(Gains gains) {
            this.gains = gains;
        }

        public String getTagName() {
            return this.tagName;
        }

        boolean hasTagName() {
            if (this.tagName == null) {
                return false;
            }
            return true;
        }

        public void setTagName(String tagName) {
            this.tagName = tagName;
        }

        public String getType() {
            return this.type;
        }

        boolean hasType() {
            if (this.type == null) {
                return false;
            }
            return true;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Role getRole() {
            return this.role;
        }

        boolean hasRole() {
            if (this.role == null) {
                return false;
            }
            return true;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public String getAddress() {
            return this.address;
        }

        boolean hasAddress() {
            if (this.address == null) {
                return false;
            }
            return true;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public boolean get_default() {
            Boolean bool = this._default;
            if (bool == null) {
                return false;
            }
            return bool.booleanValue();
        }

        boolean has_default() {
            if (this._default == null) {
                return false;
            }
            return true;
        }

        public void set_default(boolean _default) {
            this._default = Boolean.valueOf(_default);
        }

        public List<String> getEncodedFormats() {
            if (this.encodedFormats == null) {
                this.encodedFormats = new ArrayList();
            }
            return this.encodedFormats;
        }

        boolean hasEncodedFormats() {
            if (this.encodedFormats == null) {
                return false;
            }
            return true;
        }

        public void setEncodedFormats(List<String> encodedFormats) {
            this.encodedFormats = encodedFormats;
        }

        static DevicePort read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
            int type;
            String[] split;
            DevicePort instance = new DevicePort();
            String raw = parser.getAttributeValue(null, "tagName");
            if (raw != null) {
                instance.setTagName(raw);
            }
            String raw2 = parser.getAttributeValue(null, "type");
            if (raw2 != null) {
                instance.setType(raw2);
            }
            String raw3 = parser.getAttributeValue(null, Context.ROLE_SERVICE);
            if (raw3 != null) {
                Role value = Role.fromString(raw3);
                instance.setRole(value);
            }
            String raw4 = parser.getAttributeValue(null, "address");
            if (raw4 != null) {
                instance.setAddress(raw4);
            }
            String raw5 = parser.getAttributeValue(null, "default");
            if (raw5 != null) {
                boolean value2 = Boolean.parseBoolean(raw5);
                instance.set_default(value2);
            }
            String raw6 = parser.getAttributeValue(null, "encodedFormats");
            if (raw6 != null) {
                List<String> value3 = new ArrayList<>();
                for (String token : raw6.split("\\s+")) {
                    value3.add(token);
                }
                instance.setEncodedFormats(value3);
            }
            parser.getDepth();
            while (true) {
                type = parser.next();
                if (type == 1 || type == 3) {
                    break;
                } else if (parser.getEventType() == 2) {
                    String tagName = parser.getName();
                    if (tagName.equals(MediaFormat.KEY_PROFILE)) {
                        Profile value4 = Profile.read(parser);
                        instance.getProfile().add(value4);
                    } else if (tagName.equals("gains")) {
                        Gains value5 = Gains.read(parser);
                        instance.setGains(value5);
                    } else {
                        XmlParser.skip(parser);
                    }
                }
            }
            if (type != 3) {
                throw new DatatypeConfigurationException("DevicePorts.DevicePort is not closed");
            }
            return instance;
        }
    }

    public List<DevicePort> getDevicePort() {
        if (this.devicePort == null) {
            this.devicePort = new ArrayList();
        }
        return this.devicePort;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DevicePorts read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        DevicePorts instance = new DevicePorts();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("devicePort")) {
                    DevicePort value = DevicePort.read(parser);
                    instance.getDevicePort().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("DevicePorts is not closed");
        }
        return instance;
    }
}
