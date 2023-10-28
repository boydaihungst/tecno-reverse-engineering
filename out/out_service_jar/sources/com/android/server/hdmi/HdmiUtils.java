package com.android.server.hdmi;

import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.Xml;
import com.android.internal.util.HexDump;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.google.android.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class HdmiUtils {
    private static final Map<Integer, List<Integer>> ADDRESS_TO_TYPE = new HashMap<Integer, List<Integer>>() { // from class: com.android.server.hdmi.HdmiUtils.1
        {
            put(0, Lists.newArrayList(new Integer[]{0}));
            put(1, Lists.newArrayList(new Integer[]{1}));
            put(2, Lists.newArrayList(new Integer[]{1}));
            put(3, Lists.newArrayList(new Integer[]{3}));
            put(4, Lists.newArrayList(new Integer[]{4}));
            put(5, Lists.newArrayList(new Integer[]{5}));
            put(6, Lists.newArrayList(new Integer[]{3}));
            put(7, Lists.newArrayList(new Integer[]{3}));
            put(8, Lists.newArrayList(new Integer[]{4}));
            put(9, Lists.newArrayList(new Integer[]{1}));
            put(10, Lists.newArrayList(new Integer[]{3}));
            put(11, Lists.newArrayList(new Integer[]{4}));
            put(12, Lists.newArrayList(new Integer[]{4, 1, 3, 7}));
            put(13, Lists.newArrayList(new Integer[]{4, 1, 3, 7}));
            put(14, Lists.newArrayList(new Integer[]{0}));
            put(15, Collections.emptyList());
        }
    };
    private static final String[] DEFAULT_NAMES = {"TV", "Recorder_1", "Recorder_2", "Tuner_1", "Playback_1", "AudioSystem", "Tuner_2", "Tuner_3", "Playback_2", "Recorder_3", "Tuner_4", "Playback_3", "Backup_1", "Backup_2", "Secondary_TV"};
    private static final String TAG = "HdmiUtils";
    static final int TARGET_NOT_UNDER_LOCAL_DEVICE = -1;
    static final int TARGET_SAME_PHYSICAL_ADDRESS = 0;

    private HdmiUtils() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isValidAddress(int address) {
        return address >= 0 && address <= 14;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isEligibleAddressForDevice(int deviceType, int logicalAddress) {
        return isValidAddress(logicalAddress) && ADDRESS_TO_TYPE.get(Integer.valueOf(logicalAddress)).contains(Integer.valueOf(deviceType));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isEligibleAddressForCecVersion(int cecVersion, int logicalAddress) {
        if (isValidAddress(logicalAddress)) {
            return !(logicalAddress == 12 || logicalAddress == 13) || cecVersion >= 6;
        }
        return false;
    }

    static List<Integer> getTypeFromAddress(int logicalAddress) {
        return isValidAddress(logicalAddress) ? ADDRESS_TO_TYPE.get(Integer.valueOf(logicalAddress)) : Lists.newArrayList(new Integer[]{-1});
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String getDefaultDeviceName(int address) {
        if (isValidAddress(address)) {
            return DEFAULT_NAMES[address];
        }
        return "";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void verifyAddressType(int logicalAddress, int deviceType) {
        List<Integer> actualDeviceTypes = getTypeFromAddress(logicalAddress);
        if (!actualDeviceTypes.contains(Integer.valueOf(deviceType))) {
            throw new IllegalArgumentException("Device type missmatch:[Expected:" + deviceType + ", Actual:" + actualDeviceTypes);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean checkCommandSource(HdmiCecMessage cmd, int expectedAddress, String tag) {
        int src = cmd.getSource();
        if (src != expectedAddress) {
            Slog.w(tag, "Invalid source [Expected:" + expectedAddress + ", Actual:" + src + "]");
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean parseCommandParamSystemAudioStatus(HdmiCecMessage cmd) {
        return cmd.getParams()[0] == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isAudioStatusMute(HdmiCecMessage cmd) {
        byte[] params = cmd.getParams();
        return (params[0] & 128) == 128;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getAudioStatusVolume(HdmiCecMessage cmd) {
        byte[] params = cmd.getParams();
        int volume = params[0] & Byte.MAX_VALUE;
        if (volume < 0 || 100 < volume) {
            return -1;
        }
        return volume;
    }

    static List<Integer> asImmutableList(int[] is) {
        ArrayList<Integer> list = new ArrayList<>(is.length);
        for (int type : is) {
            list.add(Integer.valueOf(type));
        }
        return Collections.unmodifiableList(list);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int twoBytesToInt(byte[] data) {
        return ((data[0] & 255) << 8) | (data[1] & 255);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int twoBytesToInt(byte[] data, int offset) {
        return ((data[offset] & 255) << 8) | (data[offset + 1] & 255);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int threeBytesToInt(byte[] data) {
        return ((data[0] & 255) << 16) | ((data[1] & 255) << 8) | (data[2] & 255);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static <T> List<T> sparseArrayToList(SparseArray<T> array) {
        ArrayList<T> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            list.add(array.valueAt(i));
        }
        return list;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static <T> List<T> mergeToUnmodifiableList(List<T> a, List<T> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return Collections.emptyList();
        }
        if (a.isEmpty()) {
            return Collections.unmodifiableList(b);
        }
        if (b.isEmpty()) {
            return Collections.unmodifiableList(a);
        }
        List<T> newList = new ArrayList<>();
        newList.addAll(a);
        newList.addAll(b);
        return Collections.unmodifiableList(newList);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isAffectingActiveRoutingPath(int activePath, int newPath) {
        int i = 0;
        while (true) {
            if (i > 12) {
                break;
            }
            int nibble = (newPath >> i) & 15;
            if (nibble == 0) {
                i += 4;
            } else {
                int mask = 65520 << i;
                newPath &= mask;
                break;
            }
        }
        if (newPath == 0) {
            return true;
        }
        return isInActiveRoutingPath(activePath, newPath);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isInActiveRoutingPath(int activePath, int newPath) {
        int pathRelationship = pathRelationship(newPath, activePath);
        return pathRelationship == 2 || pathRelationship == 3 || pathRelationship == 5;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int pathRelationship(int firstPath, int secondPath) {
        if (firstPath == 65535 || secondPath == 65535) {
            return 0;
        }
        for (int nibbleIndex = 0; nibbleIndex <= 3; nibbleIndex++) {
            int shift = 12 - (nibbleIndex * 4);
            int firstPathNibble = (firstPath >> shift) & 15;
            int secondPathNibble = (secondPath >> shift) & 15;
            if (firstPathNibble != secondPathNibble) {
                int firstPathNextNibble = (firstPath >> (shift - 4)) & 15;
                int secondPathNextNibble = (secondPath >> (shift - 4)) & 15;
                if (firstPathNibble == 0) {
                    return 2;
                }
                if (secondPathNibble == 0) {
                    return 3;
                }
                if (nibbleIndex != 3) {
                    if (firstPathNextNibble == 0 && secondPathNextNibble == 0) {
                        return 4;
                    }
                    return 1;
                }
                return 4;
            }
        }
        return 5;
    }

    static <T> void dumpSparseArray(IndentingPrintWriter pw, String name, SparseArray<T> sparseArray) {
        printWithTrailingColon(pw, name);
        pw.increaseIndent();
        int size = sparseArray.size();
        for (int i = 0; i < size; i++) {
            int key = sparseArray.keyAt(i);
            T value = sparseArray.get(key);
            pw.printPair(Integer.toString(key), value);
            pw.println();
        }
        pw.decreaseIndent();
    }

    private static void printWithTrailingColon(IndentingPrintWriter pw, String name) {
        pw.println(name.endsWith(":") ? name : name.concat(":"));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static <K, V> void dumpMap(IndentingPrintWriter pw, String name, Map<K, V> map) {
        printWithTrailingColon(pw, name);
        pw.increaseIndent();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            pw.printPair(entry.getKey().toString(), entry.getValue());
            pw.println();
        }
        pw.decreaseIndent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static <T> void dumpIterable(IndentingPrintWriter pw, String name, Iterable<T> values) {
        printWithTrailingColon(pw, name);
        pw.increaseIndent();
        for (T value : values) {
            pw.println(value);
        }
        pw.decreaseIndent();
    }

    public static int getLocalPortFromPhysicalAddress(int targetPhysicalAddress, int myPhysicalAddress) {
        if (myPhysicalAddress == targetPhysicalAddress) {
            return 0;
        }
        int mask = 61440;
        int finalMask = 61440;
        int maskedAddress = myPhysicalAddress;
        while (maskedAddress != 0) {
            maskedAddress = myPhysicalAddress & mask;
            finalMask |= mask;
            mask >>= 4;
        }
        int portAddress = targetPhysicalAddress & finalMask;
        if (((finalMask << 4) & portAddress) != myPhysicalAddress) {
            return -1;
        }
        int port = portAddress & (mask << 4);
        while ((port >> 4) != 0) {
            port >>= 4;
        }
        return port;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getAbortFeatureOpcode(HdmiCecMessage cmd) {
        return cmd.getParams()[0] & 255;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getAbortReason(HdmiCecMessage cmd) {
        return cmd.getParams()[1];
    }

    public static HdmiCecMessage buildMessage(String message) {
        String[] parts = message.split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Message is too short");
        }
        for (String part : parts) {
            if (part.length() != 2) {
                throw new IllegalArgumentException("Malformatted CEC message: " + message);
            }
        }
        int src = Integer.parseInt(parts[0].substring(0, 1), 16);
        int dest = Integer.parseInt(parts[0].substring(1, 2), 16);
        int opcode = Integer.parseInt(parts[1], 16);
        byte[] params = new byte[parts.length - 2];
        for (int i = 0; i < params.length; i++) {
            params[i] = (byte) Integer.parseInt(parts[i + 2], 16);
        }
        return HdmiCecMessage.build(src, dest, opcode, params);
    }

    public static int getEndOfSequence(byte[] params, int offset) {
        if (offset < 0) {
            return -1;
        }
        while (offset < params.length && ((params[offset] >> 7) & 1) == 1) {
            offset++;
        }
        if (offset >= params.length) {
            return -1;
        }
        return offset;
    }

    /* loaded from: classes.dex */
    public static class ShortAudioDescriptorXmlParser {
        private static final String NS = null;

        public static List<DeviceConfig> parse(InputStream in) throws XmlPullParserException, IOException {
            TypedXmlPullParser parser = Xml.resolvePullParser(in);
            parser.nextTag();
            return readDevices(parser);
        }

        private static void skip(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
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

        private static List<DeviceConfig> readDevices(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
            List<DeviceConfig> devices = new ArrayList<>();
            parser.require(2, NS, "config");
            while (parser.next() != 3) {
                if (parser.getEventType() == 2) {
                    String name = parser.getName();
                    if (name.equals("device")) {
                        String deviceType = parser.getAttributeValue((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE);
                        DeviceConfig config = null;
                        if (deviceType != null) {
                            config = readDeviceConfig(parser, deviceType);
                        }
                        if (config != null) {
                            devices.add(config);
                        }
                    } else {
                        skip(parser);
                    }
                }
            }
            return devices;
        }

        private static DeviceConfig readDeviceConfig(TypedXmlPullParser parser, String deviceType) throws XmlPullParserException, IOException {
            List<CodecSad> codecSads = new ArrayList<>();
            parser.require(2, NS, "device");
            while (parser.next() != 3) {
                if (parser.getEventType() == 2) {
                    String tagName = parser.getName();
                    if (tagName.equals("supportedFormat")) {
                        String codecAttriValue = parser.getAttributeValue((String) null, "format");
                        String sadAttriValue = parser.getAttributeValue((String) null, "descriptor");
                        int format = codecAttriValue == null ? 0 : formatNameToNum(codecAttriValue);
                        byte[] descriptor = readSad(sadAttriValue);
                        if (format != 0 && descriptor != null) {
                            codecSads.add(new CodecSad(format, descriptor));
                        }
                        parser.nextTag();
                        parser.require(3, NS, "supportedFormat");
                    } else {
                        skip(parser);
                    }
                }
            }
            if (codecSads.size() == 0) {
                return null;
            }
            return new DeviceConfig(deviceType, codecSads);
        }

        private static byte[] readSad(String sad) {
            if (sad == null || sad.length() == 0) {
                return null;
            }
            byte[] sadBytes = HexDump.hexStringToByteArray(sad);
            if (sadBytes.length != 3) {
                Slog.w(HdmiUtils.TAG, "SAD byte array length is not 3. Length = " + sadBytes.length);
                return null;
            }
            return sadBytes;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private static int formatNameToNum(String codecAttriValue) {
            char c;
            switch (codecAttriValue.hashCode()) {
                case -2131742975:
                    if (codecAttriValue.equals("AUDIO_FORMAT_WMAPRO")) {
                        c = 14;
                        break;
                    }
                    c = 65535;
                    break;
                case -1197237630:
                    if (codecAttriValue.equals("AUDIO_FORMAT_ATRAC")) {
                        c = '\b';
                        break;
                    }
                    c = 65535;
                    break;
                case -1194465888:
                    if (codecAttriValue.equals("AUDIO_FORMAT_DTSHD")) {
                        c = 11;
                        break;
                    }
                    c = 65535;
                    break;
                case -1186286867:
                    if (codecAttriValue.equals("AUDIO_FORMAT_MPEG1")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -1186286866:
                    if (codecAttriValue.equals("AUDIO_FORMAT_MPEG2")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -358943216:
                    if (codecAttriValue.equals("AUDIO_FORMAT_ONEBITAUDIO")) {
                        c = '\t';
                        break;
                    }
                    c = 65535;
                    break;
                case -282810364:
                    if (codecAttriValue.equals("AUDIO_FORMAT_AAC")) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case -282807375:
                    if (codecAttriValue.equals("AUDIO_FORMAT_DDP")) {
                        c = '\n';
                        break;
                    }
                    c = 65535;
                    break;
                case -282806906:
                    if (codecAttriValue.equals("AUDIO_FORMAT_DST")) {
                        c = '\r';
                        break;
                    }
                    c = 65535;
                    break;
                case -282806876:
                    if (codecAttriValue.equals("AUDIO_FORMAT_DTS")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case -282798811:
                    if (codecAttriValue.equals("AUDIO_FORMAT_MAX")) {
                        c = 15;
                        break;
                    }
                    c = 65535;
                    break;
                case -282798383:
                    if (codecAttriValue.equals("AUDIO_FORMAT_MP3")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case -176844499:
                    if (codecAttriValue.equals("AUDIO_FORMAT_LPCM")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -176785545:
                    if (codecAttriValue.equals("AUDIO_FORMAT_NONE")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 129424511:
                    if (codecAttriValue.equals("AUDIO_FORMAT_DD")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 2082539401:
                    if (codecAttriValue.equals("AUDIO_FORMAT_TRUEHD")) {
                        c = '\f';
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
                    return 0;
                case 1:
                    return 1;
                case 2:
                    return 2;
                case 3:
                    return 3;
                case 4:
                    return 4;
                case 5:
                    return 5;
                case 6:
                    return 6;
                case 7:
                    return 7;
                case '\b':
                    return 8;
                case '\t':
                    return 9;
                case '\n':
                    return 10;
                case 11:
                    return 11;
                case '\f':
                    return 12;
                case '\r':
                    return 13;
                case 14:
                    return 14;
                case 15:
                    return 15;
                default:
                    return 0;
            }
        }
    }

    /* loaded from: classes.dex */
    public static class DeviceConfig {
        public final String name;
        public final List<CodecSad> supportedCodecs;

        public DeviceConfig(String name, List<CodecSad> supportedCodecs) {
            this.name = name;
            this.supportedCodecs = supportedCodecs;
        }

        public boolean equals(Object obj) {
            if (obj instanceof DeviceConfig) {
                DeviceConfig that = (DeviceConfig) obj;
                return that.name.equals(this.name) && that.supportedCodecs.equals(this.supportedCodecs);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.name, Integer.valueOf(this.supportedCodecs.hashCode()));
        }
    }

    /* loaded from: classes.dex */
    public static class CodecSad {
        public final int audioCodec;
        public final byte[] sad;

        public CodecSad(int audioCodec, byte[] sad) {
            this.audioCodec = audioCodec;
            this.sad = sad;
        }

        public CodecSad(int audioCodec, String sad) {
            this.audioCodec = audioCodec;
            this.sad = HexDump.hexStringToByteArray(sad);
        }

        public boolean equals(Object obj) {
            if (obj instanceof CodecSad) {
                CodecSad that = (CodecSad) obj;
                return that.audioCodec == this.audioCodec && Arrays.equals(that.sad, this.sad);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.audioCodec), Integer.valueOf(Arrays.hashCode(this.sad)));
        }
    }
}
