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
public class MixPorts {
    private List<MixPort> mixPort;

    /* loaded from: classes.dex */
    public static class MixPort {
        private List<AudioInOutFlag> flags;
        private Gains gains;
        private Long maxActiveCount;
        private Long maxOpenCount;
        private String name;
        private List<AudioUsage> preferredUsage;
        private List<Profile> profile;
        private Role role;

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

        public String getName() {
            return this.name;
        }

        boolean hasName() {
            if (this.name == null) {
                return false;
            }
            return true;
        }

        public void setName(String name) {
            this.name = name;
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

        public List<AudioInOutFlag> getFlags() {
            if (this.flags == null) {
                this.flags = new ArrayList();
            }
            return this.flags;
        }

        boolean hasFlags() {
            if (this.flags == null) {
                return false;
            }
            return true;
        }

        public void setFlags(List<AudioInOutFlag> flags) {
            this.flags = flags;
        }

        public long getMaxOpenCount() {
            Long l = this.maxOpenCount;
            if (l == null) {
                return 0L;
            }
            return l.longValue();
        }

        boolean hasMaxOpenCount() {
            if (this.maxOpenCount == null) {
                return false;
            }
            return true;
        }

        public void setMaxOpenCount(long maxOpenCount) {
            this.maxOpenCount = Long.valueOf(maxOpenCount);
        }

        public long getMaxActiveCount() {
            Long l = this.maxActiveCount;
            if (l == null) {
                return 0L;
            }
            return l.longValue();
        }

        boolean hasMaxActiveCount() {
            if (this.maxActiveCount == null) {
                return false;
            }
            return true;
        }

        public void setMaxActiveCount(long maxActiveCount) {
            this.maxActiveCount = Long.valueOf(maxActiveCount);
        }

        public List<AudioUsage> getPreferredUsage() {
            if (this.preferredUsage == null) {
                this.preferredUsage = new ArrayList();
            }
            return this.preferredUsage;
        }

        boolean hasPreferredUsage() {
            if (this.preferredUsage == null) {
                return false;
            }
            return true;
        }

        public void setPreferredUsage(List<AudioUsage> preferredUsage) {
            this.preferredUsage = preferredUsage;
        }

        static MixPort read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
            int type;
            String[] split;
            String[] split2;
            MixPort instance = new MixPort();
            String raw = parser.getAttributeValue(null, "name");
            if (raw != null) {
                instance.setName(raw);
            }
            String raw2 = parser.getAttributeValue(null, Context.ROLE_SERVICE);
            if (raw2 != null) {
                Role value = Role.fromString(raw2);
                instance.setRole(value);
            }
            String raw3 = parser.getAttributeValue(null, "flags");
            if (raw3 != null) {
                List<AudioInOutFlag> value2 = new ArrayList<>();
                for (String token : raw3.split("\\s+")) {
                    value2.add(AudioInOutFlag.fromString(token));
                }
                instance.setFlags(value2);
            }
            String raw4 = parser.getAttributeValue(null, "maxOpenCount");
            if (raw4 != null) {
                long value3 = Long.parseLong(raw4);
                instance.setMaxOpenCount(value3);
            }
            String raw5 = parser.getAttributeValue(null, "maxActiveCount");
            if (raw5 != null) {
                long value4 = Long.parseLong(raw5);
                instance.setMaxActiveCount(value4);
            }
            String raw6 = parser.getAttributeValue(null, "preferredUsage");
            if (raw6 != null) {
                List<AudioUsage> value5 = new ArrayList<>();
                for (String token2 : raw6.split("\\s+")) {
                    value5.add(AudioUsage.fromString(token2));
                }
                instance.setPreferredUsage(value5);
            }
            parser.getDepth();
            while (true) {
                type = parser.next();
                if (type == 1 || type == 3) {
                    break;
                } else if (parser.getEventType() == 2) {
                    String tagName = parser.getName();
                    if (tagName.equals(MediaFormat.KEY_PROFILE)) {
                        Profile value6 = Profile.read(parser);
                        instance.getProfile().add(value6);
                    } else if (tagName.equals("gains")) {
                        Gains value7 = Gains.read(parser);
                        instance.setGains(value7);
                    } else {
                        XmlParser.skip(parser);
                    }
                }
            }
            if (type != 3) {
                throw new DatatypeConfigurationException("MixPorts.MixPort is not closed");
            }
            return instance;
        }
    }

    public List<MixPort> getMixPort() {
        if (this.mixPort == null) {
            this.mixPort = new ArrayList();
        }
        return this.mixPort;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static MixPorts read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        MixPorts instance = new MixPorts();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("mixPort")) {
                    MixPort value = MixPort.read(parser);
                    instance.getMixPort().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("MixPorts is not closed");
        }
        return instance;
    }
}
