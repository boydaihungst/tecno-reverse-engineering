package android.audio.policy.configuration.V7_0;

import android.provider.Telephony;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Profile {
    private List<AudioChannelMask> channelMasks;
    private AudioEncapsulationType encapsulationType;
    private String format;
    private String name;
    private List<BigInteger> samplingRates;

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

    public String getFormat() {
        return this.format;
    }

    boolean hasFormat() {
        if (this.format == null) {
            return false;
        }
        return true;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<BigInteger> getSamplingRates() {
        if (this.samplingRates == null) {
            this.samplingRates = new ArrayList();
        }
        return this.samplingRates;
    }

    boolean hasSamplingRates() {
        if (this.samplingRates == null) {
            return false;
        }
        return true;
    }

    public void setSamplingRates(List<BigInteger> samplingRates) {
        this.samplingRates = samplingRates;
    }

    public List<AudioChannelMask> getChannelMasks() {
        if (this.channelMasks == null) {
            this.channelMasks = new ArrayList();
        }
        return this.channelMasks;
    }

    boolean hasChannelMasks() {
        if (this.channelMasks == null) {
            return false;
        }
        return true;
    }

    public void setChannelMasks(List<AudioChannelMask> channelMasks) {
        this.channelMasks = channelMasks;
    }

    public AudioEncapsulationType getEncapsulationType() {
        return this.encapsulationType;
    }

    boolean hasEncapsulationType() {
        if (this.encapsulationType == null) {
            return false;
        }
        return true;
    }

    public void setEncapsulationType(AudioEncapsulationType encapsulationType) {
        this.encapsulationType = encapsulationType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Profile read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        String[] split;
        String[] split2;
        Profile instance = new Profile();
        String raw = parser.getAttributeValue(null, "name");
        if (raw != null) {
            instance.setName(raw);
        }
        String raw2 = parser.getAttributeValue(null, Telephony.CellBroadcasts.MESSAGE_FORMAT);
        if (raw2 != null) {
            instance.setFormat(raw2);
        }
        String raw3 = parser.getAttributeValue(null, "samplingRates");
        if (raw3 != null) {
            List<BigInteger> value = new ArrayList<>();
            for (String token : raw3.split("\\s+")) {
                value.add(new BigInteger(token));
            }
            instance.setSamplingRates(value);
        }
        String raw4 = parser.getAttributeValue(null, "channelMasks");
        if (raw4 != null) {
            List<AudioChannelMask> value2 = new ArrayList<>();
            for (String token2 : raw4.split("\\s+")) {
                value2.add(AudioChannelMask.fromString(token2));
            }
            instance.setChannelMasks(value2);
        }
        String raw5 = parser.getAttributeValue(null, "encapsulationType");
        if (raw5 != null) {
            instance.setEncapsulationType(AudioEncapsulationType.fromString(raw5));
        }
        XmlParser.skip(parser);
        return instance;
    }
}
