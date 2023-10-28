package android.util.proto;

import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import java.io.IOException;
/* loaded from: classes3.dex */
public class ProtoUtils {
    public static void toAggStatsProto(ProtoOutputStream proto, long fieldId, long min, long average, long max, int meanKb, int maxKb) {
        long aggStatsToken = proto.start(fieldId);
        proto.write(1112396529665L, min);
        proto.write(1112396529666L, average);
        proto.write(1112396529667L, max);
        proto.write(1120986464260L, meanKb);
        proto.write(1120986464261L, maxKb);
        proto.end(aggStatsToken);
    }

    public static void toAggStatsProto(ProtoOutputStream proto, long fieldId, long min, long average, long max) {
        toAggStatsProto(proto, fieldId, min, average, max, 0, 0);
    }

    public static void toDuration(ProtoOutputStream proto, long fieldId, long startMs, long endMs) {
        long token = proto.start(fieldId);
        proto.write(1112396529665L, startMs);
        proto.write(1112396529666L, endMs);
        proto.end(token);
    }

    public static void writeBitWiseFlagsToProtoEnum(ProtoOutputStream proto, long fieldId, int flags, int[] origEnums, int[] protoEnums) {
        if (protoEnums.length != origEnums.length) {
            throw new IllegalArgumentException("The length of origEnums must match protoEnums");
        }
        int len = origEnums.length;
        for (int i = 0; i < len; i++) {
            if (origEnums[i] == 0 && flags == 0) {
                proto.write(fieldId, protoEnums[i]);
                return;
            }
            if ((origEnums[i] & flags) != 0) {
                proto.write(fieldId, protoEnums[i]);
            }
        }
    }

    public static String currentFieldToString(ProtoInputStream proto) throws IOException {
        StringBuilder sb = new StringBuilder();
        int fieldNumber = proto.getFieldNumber();
        int wireType = proto.getWireType();
        sb.append("Offset : 0x" + Integer.toHexString(proto.getOffset()));
        sb.append("\nField Number : 0x" + Integer.toHexString(proto.getFieldNumber()));
        sb.append("\nWire Type : ");
        switch (wireType) {
            case 0:
                sb.append("varint");
                long fieldConstant = ProtoStream.makeFieldId(fieldNumber, 1112396529664L);
                sb.append("\nField Value : 0x" + Long.toHexString(proto.readLong(fieldConstant)));
                break;
            case 1:
                sb.append("fixed64");
                long fieldConstant2 = ProtoStream.makeFieldId(fieldNumber, 1125281431552L);
                sb.append("\nField Value : 0x" + Long.toHexString(proto.readLong(fieldConstant2)));
                break;
            case 2:
                sb.append("length delimited");
                long fieldConstant3 = ProtoStream.makeFieldId(fieldNumber, 1151051235328L);
                sb.append("\nField Bytes : " + proto.readBytes(fieldConstant3));
                break;
            case 3:
                sb.append("start group");
                break;
            case 4:
                sb.append("end group");
                break;
            case 5:
                sb.append("fixed32");
                long fieldConstant4 = ProtoStream.makeFieldId(fieldNumber, 1129576398848L);
                sb.append("\nField Value : 0x" + Integer.toHexString(proto.readInt(fieldConstant4)));
                break;
            default:
                sb.append("unknown(" + proto.getWireType() + NavigationBarInflaterView.KEY_CODE_END);
                break;
        }
        return sb.toString();
    }
}
