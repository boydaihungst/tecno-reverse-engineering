package android.audio.policy.configuration.V7_0;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Gains {
    private List<Gain> gain;

    /* loaded from: classes.dex */
    public static class Gain {
        private AudioChannelMask channel_mask;
        private Integer defaultValueMB;
        private Integer maxRampMs;
        private Integer maxValueMB;
        private Integer minRampMs;
        private Integer minValueMB;
        private List<AudioGainMode> mode;
        private String name;
        private Integer stepValueMB;
        private Boolean useForVolume;

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

        public List<AudioGainMode> getMode() {
            if (this.mode == null) {
                this.mode = new ArrayList();
            }
            return this.mode;
        }

        boolean hasMode() {
            if (this.mode == null) {
                return false;
            }
            return true;
        }

        public void setMode(List<AudioGainMode> mode) {
            this.mode = mode;
        }

        public AudioChannelMask getChannel_mask() {
            return this.channel_mask;
        }

        boolean hasChannel_mask() {
            if (this.channel_mask == null) {
                return false;
            }
            return true;
        }

        public void setChannel_mask(AudioChannelMask channel_mask) {
            this.channel_mask = channel_mask;
        }

        public int getMinValueMB() {
            Integer num = this.minValueMB;
            if (num == null) {
                return 0;
            }
            return num.intValue();
        }

        boolean hasMinValueMB() {
            if (this.minValueMB == null) {
                return false;
            }
            return true;
        }

        public void setMinValueMB(int minValueMB) {
            this.minValueMB = Integer.valueOf(minValueMB);
        }

        public int getMaxValueMB() {
            Integer num = this.maxValueMB;
            if (num == null) {
                return 0;
            }
            return num.intValue();
        }

        boolean hasMaxValueMB() {
            if (this.maxValueMB == null) {
                return false;
            }
            return true;
        }

        public void setMaxValueMB(int maxValueMB) {
            this.maxValueMB = Integer.valueOf(maxValueMB);
        }

        public int getDefaultValueMB() {
            Integer num = this.defaultValueMB;
            if (num == null) {
                return 0;
            }
            return num.intValue();
        }

        boolean hasDefaultValueMB() {
            if (this.defaultValueMB == null) {
                return false;
            }
            return true;
        }

        public void setDefaultValueMB(int defaultValueMB) {
            this.defaultValueMB = Integer.valueOf(defaultValueMB);
        }

        public int getStepValueMB() {
            Integer num = this.stepValueMB;
            if (num == null) {
                return 0;
            }
            return num.intValue();
        }

        boolean hasStepValueMB() {
            if (this.stepValueMB == null) {
                return false;
            }
            return true;
        }

        public void setStepValueMB(int stepValueMB) {
            this.stepValueMB = Integer.valueOf(stepValueMB);
        }

        public int getMinRampMs() {
            Integer num = this.minRampMs;
            if (num == null) {
                return 0;
            }
            return num.intValue();
        }

        boolean hasMinRampMs() {
            if (this.minRampMs == null) {
                return false;
            }
            return true;
        }

        public void setMinRampMs(int minRampMs) {
            this.minRampMs = Integer.valueOf(minRampMs);
        }

        public int getMaxRampMs() {
            Integer num = this.maxRampMs;
            if (num == null) {
                return 0;
            }
            return num.intValue();
        }

        boolean hasMaxRampMs() {
            if (this.maxRampMs == null) {
                return false;
            }
            return true;
        }

        public void setMaxRampMs(int maxRampMs) {
            this.maxRampMs = Integer.valueOf(maxRampMs);
        }

        public boolean getUseForVolume() {
            Boolean bool = this.useForVolume;
            if (bool == null) {
                return false;
            }
            return bool.booleanValue();
        }

        boolean hasUseForVolume() {
            if (this.useForVolume == null) {
                return false;
            }
            return true;
        }

        public void setUseForVolume(boolean useForVolume) {
            this.useForVolume = Boolean.valueOf(useForVolume);
        }

        static Gain read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
            String[] split;
            Gain instance = new Gain();
            String raw = parser.getAttributeValue(null, "name");
            if (raw != null) {
                instance.setName(raw);
            }
            String raw2 = parser.getAttributeValue(null, "mode");
            if (raw2 != null) {
                List<AudioGainMode> value = new ArrayList<>();
                for (String token : raw2.split("\\s+")) {
                    value.add(AudioGainMode.fromString(token));
                }
                instance.setMode(value);
            }
            String raw3 = parser.getAttributeValue(null, "channel_mask");
            if (raw3 != null) {
                AudioChannelMask value2 = AudioChannelMask.fromString(raw3);
                instance.setChannel_mask(value2);
            }
            String raw4 = parser.getAttributeValue(null, "minValueMB");
            if (raw4 != null) {
                int value3 = Integer.parseInt(raw4);
                instance.setMinValueMB(value3);
            }
            String raw5 = parser.getAttributeValue(null, "maxValueMB");
            if (raw5 != null) {
                int value4 = Integer.parseInt(raw5);
                instance.setMaxValueMB(value4);
            }
            String raw6 = parser.getAttributeValue(null, "defaultValueMB");
            if (raw6 != null) {
                int value5 = Integer.parseInt(raw6);
                instance.setDefaultValueMB(value5);
            }
            String raw7 = parser.getAttributeValue(null, "stepValueMB");
            if (raw7 != null) {
                int value6 = Integer.parseInt(raw7);
                instance.setStepValueMB(value6);
            }
            String raw8 = parser.getAttributeValue(null, "minRampMs");
            if (raw8 != null) {
                int value7 = Integer.parseInt(raw8);
                instance.setMinRampMs(value7);
            }
            String raw9 = parser.getAttributeValue(null, "maxRampMs");
            if (raw9 != null) {
                int value8 = Integer.parseInt(raw9);
                instance.setMaxRampMs(value8);
            }
            String raw10 = parser.getAttributeValue(null, "useForVolume");
            if (raw10 != null) {
                boolean value9 = Boolean.parseBoolean(raw10);
                instance.setUseForVolume(value9);
            }
            XmlParser.skip(parser);
            return instance;
        }
    }

    public List<Gain> getGain() {
        if (this.gain == null) {
            this.gain = new ArrayList();
        }
        return this.gain;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Gains read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Gains instance = new Gains();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("gain")) {
                    Gain value = Gain.read(parser);
                    instance.getGain().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Gains is not closed");
        }
        return instance;
    }
}
