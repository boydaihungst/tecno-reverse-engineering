package com.android.framework.protobuf;

import com.android.framework.protobuf.ArrayDecoders;
import com.android.framework.protobuf.ByteString;
import com.android.framework.protobuf.FieldSet;
import com.android.framework.protobuf.Internal;
import com.android.framework.protobuf.InvalidProtocolBufferException;
import com.android.framework.protobuf.MapEntryLite;
import com.android.framework.protobuf.WireFormat;
import com.android.framework.protobuf.Writer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import sun.misc.Unsafe;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes4.dex */
public final class MessageSchema<T> implements Schema<T> {
    private static final int ENFORCE_UTF8_MASK = 536870912;
    private static final int FIELD_TYPE_MASK = 267386880;
    private static final int INTS_PER_FIELD = 3;
    private static final int OFFSET_BITS = 20;
    private static final int OFFSET_MASK = 1048575;
    static final int ONEOF_TYPE_OFFSET = 51;
    private static final int REQUIRED_MASK = 268435456;
    private final int[] buffer;
    private final int checkInitializedCount;
    private final MessageLite defaultInstance;
    private final ExtensionSchema<?> extensionSchema;
    private final boolean hasExtensions;
    private final int[] intArray;
    private final ListFieldSchema listFieldSchema;
    private final boolean lite;
    private final MapFieldSchema mapFieldSchema;
    private final int maxFieldNumber;
    private final int minFieldNumber;
    private final NewInstanceSchema newInstanceSchema;
    private final Object[] objects;
    private final boolean proto3;
    private final int repeatedFieldOffsetStart;
    private final UnknownFieldSchema<?, ?> unknownFieldSchema;
    private final boolean useCachedSizeField;
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    private MessageSchema(int[] buffer, Object[] objects, int minFieldNumber, int maxFieldNumber, MessageLite defaultInstance, boolean proto3, boolean useCachedSizeField, int[] intArray, int checkInitialized, int mapFieldPositions, NewInstanceSchema newInstanceSchema, ListFieldSchema listFieldSchema, UnknownFieldSchema<?, ?> unknownFieldSchema, ExtensionSchema<?> extensionSchema, MapFieldSchema mapFieldSchema) {
        this.buffer = buffer;
        this.objects = objects;
        this.minFieldNumber = minFieldNumber;
        this.maxFieldNumber = maxFieldNumber;
        this.lite = defaultInstance instanceof GeneratedMessageLite;
        this.proto3 = proto3;
        this.hasExtensions = extensionSchema != null && extensionSchema.hasExtensions(defaultInstance);
        this.useCachedSizeField = useCachedSizeField;
        this.intArray = intArray;
        this.checkInitializedCount = checkInitialized;
        this.repeatedFieldOffsetStart = mapFieldPositions;
        this.newInstanceSchema = newInstanceSchema;
        this.listFieldSchema = listFieldSchema;
        this.unknownFieldSchema = unknownFieldSchema;
        this.extensionSchema = extensionSchema;
        this.defaultInstance = defaultInstance;
        this.mapFieldSchema = mapFieldSchema;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static <T> MessageSchema<T> newSchema(Class<T> messageClass, MessageInfo messageInfo, NewInstanceSchema newInstanceSchema, ListFieldSchema listFieldSchema, UnknownFieldSchema<?, ?> unknownFieldSchema, ExtensionSchema<?> extensionSchema, MapFieldSchema mapFieldSchema) {
        if (messageInfo instanceof RawMessageInfo) {
            return newSchemaForRawMessageInfo((RawMessageInfo) messageInfo, newInstanceSchema, listFieldSchema, unknownFieldSchema, extensionSchema, mapFieldSchema);
        }
        return newSchemaForMessageInfo((StructuralMessageInfo) messageInfo, newInstanceSchema, listFieldSchema, unknownFieldSchema, extensionSchema, mapFieldSchema);
    }

    /* JADX WARN: Removed duplicated region for block: B:123:0x02a5  */
    /* JADX WARN: Removed duplicated region for block: B:124:0x02a9  */
    /* JADX WARN: Removed duplicated region for block: B:127:0x02c3  */
    /* JADX WARN: Removed duplicated region for block: B:128:0x02c7  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    static <T> MessageSchema<T> newSchemaForRawMessageInfo(RawMessageInfo messageInfo, NewInstanceSchema newInstanceSchema, ListFieldSchema listFieldSchema, UnknownFieldSchema<?, ?> unknownFieldSchema, ExtensionSchema<?> extensionSchema, MapFieldSchema mapFieldSchema) {
        int objectsPosition;
        int[] intArray;
        int oneofCount;
        int minFieldNumber;
        int maxFieldNumber;
        int numEntries;
        int mapFieldCount;
        int checkInitialized;
        int i;
        int next;
        int i2;
        int next2;
        int i3;
        int next3;
        int i4;
        int next4;
        int i5;
        int next5;
        int i6;
        int next6;
        int i7;
        int next7;
        int i8;
        int next8;
        int length;
        boolean isProto3;
        int objectsPosition2;
        int fieldOffset;
        int fieldOffset2;
        String info;
        int objectsPosition3;
        int oneofCount2;
        int presenceFieldOffset;
        int index;
        int presenceFieldOffset2;
        Field hasBitsField;
        int i9;
        int next9;
        Object o;
        Field oneofField;
        Object o2;
        Field oneofCaseField;
        int i10;
        int next10;
        int i11;
        int next11;
        int i12;
        int next12;
        int i13;
        int next13;
        int i14;
        int next14;
        boolean isProto32 = messageInfo.getSyntax() == ProtoSyntax.PROTO3;
        String info2 = messageInfo.getStringInfo();
        int length2 = info2.length();
        int i15 = 0 + 1;
        int next15 = info2.charAt(0);
        int i16 = 55296;
        if (next15 >= 55296) {
            int result = next15 & 8191;
            int shift = 13;
            while (true) {
                i14 = i15 + 1;
                next14 = info2.charAt(i15);
                if (next14 < 55296) {
                    break;
                }
                result |= (next14 & 8191) << shift;
                shift += 13;
                i15 = i14;
            }
            next15 = result | (next14 << shift);
            i15 = i14;
        }
        int flags = next15;
        int i17 = i15 + 1;
        int next16 = info2.charAt(i15);
        if (next16 >= 55296) {
            int result2 = next16 & 8191;
            int shift2 = 13;
            while (true) {
                i13 = i17 + 1;
                next13 = info2.charAt(i17);
                if (next13 < 55296) {
                    break;
                }
                result2 |= (next13 & 8191) << shift2;
                shift2 += 13;
                i17 = i13;
            }
            next16 = result2 | (next13 << shift2);
            i17 = i13;
        }
        int fieldCount = next16;
        if (fieldCount == 0) {
            int[] intArray2 = EMPTY_INT_ARRAY;
            oneofCount = 0;
            minFieldNumber = 0;
            maxFieldNumber = 0;
            numEntries = 0;
            mapFieldCount = 0;
            checkInitialized = 0;
            intArray = intArray2;
            objectsPosition = 0;
        } else {
            int i18 = i17 + 1;
            int next17 = info2.charAt(i17);
            if (next17 >= 55296) {
                int result3 = next17 & 8191;
                int shift3 = 13;
                while (true) {
                    i8 = i18 + 1;
                    next8 = info2.charAt(i18);
                    if (next8 < 55296) {
                        break;
                    }
                    result3 |= (next8 & 8191) << shift3;
                    shift3 += 13;
                    i18 = i8;
                }
                next17 = result3 | (next8 << shift3);
                i18 = i8;
            }
            int result4 = next17;
            int i19 = i18 + 1;
            int next18 = info2.charAt(i18);
            if (next18 >= 55296) {
                int result5 = next18 & 8191;
                int shift4 = 13;
                while (true) {
                    i7 = i19 + 1;
                    next7 = info2.charAt(i19);
                    if (next7 < 55296) {
                        break;
                    }
                    result5 |= (next7 & 8191) << shift4;
                    shift4 += 13;
                    i19 = i7;
                }
                next18 = result5 | (next7 << shift4);
                i19 = i7;
            }
            int result6 = next18;
            int i20 = i19 + 1;
            int next19 = info2.charAt(i19);
            if (next19 >= 55296) {
                int result7 = next19 & 8191;
                int shift5 = 13;
                while (true) {
                    i6 = i20 + 1;
                    next6 = info2.charAt(i20);
                    if (next6 < 55296) {
                        break;
                    }
                    result7 |= (next6 & 8191) << shift5;
                    shift5 += 13;
                    i20 = i6;
                }
                next19 = result7 | (next6 << shift5);
                i20 = i6;
            }
            int result8 = next19;
            int i21 = i20 + 1;
            int next20 = info2.charAt(i20);
            if (next20 >= 55296) {
                int result9 = next20 & 8191;
                int shift6 = 13;
                while (true) {
                    i5 = i21 + 1;
                    next5 = info2.charAt(i21);
                    if (next5 < 55296) {
                        break;
                    }
                    result9 |= (next5 & 8191) << shift6;
                    shift6 += 13;
                    i21 = i5;
                }
                next20 = result9 | (next5 << shift6);
                i21 = i5;
            }
            int result10 = next20;
            int i22 = i21 + 1;
            int next21 = info2.charAt(i21);
            if (next21 >= 55296) {
                int result11 = next21 & 8191;
                int shift7 = 13;
                while (true) {
                    i4 = i22 + 1;
                    next4 = info2.charAt(i22);
                    if (next4 < 55296) {
                        break;
                    }
                    result11 |= (next4 & 8191) << shift7;
                    shift7 += 13;
                    i22 = i4;
                }
                next21 = result11 | (next4 << shift7);
                i22 = i4;
            }
            int result12 = next21;
            int i23 = i22 + 1;
            int next22 = info2.charAt(i22);
            if (next22 >= 55296) {
                int result13 = next22 & 8191;
                int shift8 = 13;
                while (true) {
                    i3 = i23 + 1;
                    next3 = info2.charAt(i23);
                    if (next3 < 55296) {
                        break;
                    }
                    result13 |= (next3 & 8191) << shift8;
                    shift8 += 13;
                    i23 = i3;
                }
                next22 = result13 | (next3 << shift8);
                i23 = i3;
            }
            int result14 = next22;
            int i24 = i23 + 1;
            int next23 = info2.charAt(i23);
            if (next23 >= 55296) {
                int result15 = next23 & 8191;
                int shift9 = 13;
                while (true) {
                    i2 = i24 + 1;
                    next2 = info2.charAt(i24);
                    if (next2 < 55296) {
                        break;
                    }
                    result15 |= (next2 & 8191) << shift9;
                    shift9 += 13;
                    i24 = i2;
                }
                next23 = result15 | (next2 << shift9);
                i24 = i2;
            }
            int result16 = next23;
            int i25 = i24 + 1;
            next16 = info2.charAt(i24);
            if (next16 >= 55296) {
                int result17 = next16 & 8191;
                int shift10 = 13;
                while (true) {
                    i = i25 + 1;
                    next = info2.charAt(i25);
                    if (next < 55296) {
                        break;
                    }
                    result17 |= (next & 8191) << shift10;
                    shift10 += 13;
                    i25 = i;
                }
                next16 = result17 | (next << shift10);
                i25 = i;
            }
            int result18 = next16;
            int[] intArray3 = new int[result18 + result14 + result16];
            objectsPosition = (result4 * 2) + result6;
            intArray = intArray3;
            oneofCount = result4;
            minFieldNumber = result8;
            maxFieldNumber = result10;
            numEntries = result12;
            mapFieldCount = result14;
            checkInitialized = result18;
            i17 = i25;
        }
        Unsafe unsafe = UNSAFE;
        Object[] messageInfoObjects = messageInfo.getObjects();
        Class<?> messageClass = messageInfo.getDefaultInstance().getClass();
        int[] buffer = new int[numEntries * 3];
        Object[] objects = new Object[numEntries * 2];
        int mapFieldIndex = checkInitialized;
        int repeatedFieldIndex = checkInitialized + mapFieldCount;
        int checkInitializedPosition = 0;
        int mapFieldIndex2 = mapFieldIndex;
        int repeatedFieldIndex2 = repeatedFieldIndex;
        int bufferIndex = 0;
        int objectsPosition4 = objectsPosition;
        int i26 = i17;
        while (i26 < length2) {
            int i27 = i26 + 1;
            int next24 = info2.charAt(i26);
            if (next24 >= i16) {
                int result19 = next24 & 8191;
                int shift11 = 13;
                while (true) {
                    i12 = i27 + 1;
                    next12 = info2.charAt(i27);
                    if (next12 < i16) {
                        break;
                    }
                    result19 |= (next12 & 8191) << shift11;
                    shift11 += 13;
                    i27 = i12;
                }
                next24 = result19 | (next12 << shift11);
                i27 = i12;
            }
            int result20 = next24;
            int i28 = i27 + 1;
            int next25 = info2.charAt(i27);
            if (next25 >= i16) {
                int result21 = next25 & 8191;
                int shift12 = 13;
                while (true) {
                    i11 = i28 + 1;
                    next11 = info2.charAt(i28);
                    if (next11 < i16) {
                        break;
                    }
                    result21 |= (next11 & 8191) << shift12;
                    shift12 += 13;
                    i28 = i11;
                }
                next25 = result21 | (next11 << shift12);
                i28 = i11;
            }
            int result22 = next25;
            int fieldType = result22 & 255;
            if ((result22 & 1024) != 0) {
                intArray[checkInitializedPosition] = bufferIndex;
                checkInitializedPosition++;
            }
            if (fieldType >= 51) {
                int i29 = i28 + 1;
                int next26 = info2.charAt(i28);
                if (next26 >= i16) {
                    int result23 = next26 & 8191;
                    int shift13 = 13;
                    while (true) {
                        i10 = i29 + 1;
                        next10 = info2.charAt(i29);
                        if (next10 < i16) {
                            break;
                        }
                        result23 |= (next10 & 8191) << shift13;
                        shift13 += 13;
                        i29 = i10;
                    }
                    next26 = result23 | (next10 << shift13);
                    i29 = i10;
                }
                int result24 = next26;
                int oneofFieldType = fieldType - 51;
                if (oneofFieldType == 9) {
                    length = length2;
                } else if (oneofFieldType == 17) {
                    length = length2;
                } else {
                    if (oneofFieldType != 12) {
                        length = length2;
                    } else {
                        length = length2;
                        if ((flags & 1) == 1) {
                            objects[((bufferIndex / 3) * 2) + 1] = messageInfoObjects[objectsPosition4];
                            objectsPosition4++;
                        }
                    }
                    int index2 = result24 * 2;
                    o = messageInfoObjects[index2];
                    int next27 = next26;
                    if (!(o instanceof Field)) {
                        oneofField = (Field) o;
                    } else {
                        oneofField = reflectField(messageClass, (String) o);
                        messageInfoObjects[index2] = oneofField;
                    }
                    isProto3 = isProto32;
                    int i30 = i29;
                    int fieldOffset3 = (int) unsafe.objectFieldOffset(oneofField);
                    int index3 = index2 + 1;
                    o2 = messageInfoObjects[index3];
                    if (!(o2 instanceof Field)) {
                        oneofCaseField = (Field) o2;
                    } else {
                        oneofCaseField = reflectField(messageClass, (String) o2);
                        messageInfoObjects[index3] = oneofCaseField;
                    }
                    presenceFieldOffset2 = (int) unsafe.objectFieldOffset(oneofCaseField);
                    index = 0;
                    info = info2;
                    oneofCount2 = oneofCount;
                    fieldOffset2 = fieldOffset3;
                    next25 = next27;
                    i26 = i30;
                    fieldOffset = result20;
                }
                int length3 = bufferIndex / 3;
                objects[(length3 * 2) + 1] = messageInfoObjects[objectsPosition4];
                objectsPosition4++;
                int index22 = result24 * 2;
                o = messageInfoObjects[index22];
                int next272 = next26;
                if (!(o instanceof Field)) {
                }
                isProto3 = isProto32;
                int i302 = i29;
                int fieldOffset32 = (int) unsafe.objectFieldOffset(oneofField);
                int index32 = index22 + 1;
                o2 = messageInfoObjects[index32];
                if (!(o2 instanceof Field)) {
                }
                presenceFieldOffset2 = (int) unsafe.objectFieldOffset(oneofCaseField);
                index = 0;
                info = info2;
                oneofCount2 = oneofCount;
                fieldOffset2 = fieldOffset32;
                next25 = next272;
                i26 = i302;
                fieldOffset = result20;
            } else {
                length = length2;
                isProto3 = isProto32;
                int objectsPosition5 = objectsPosition4 + 1;
                Field field = reflectField(messageClass, (String) messageInfoObjects[objectsPosition4]);
                if (fieldType == 9 || fieldType == 17) {
                    objectsPosition2 = 1;
                    objects[((bufferIndex / 3) * 2) + 1] = field.getType();
                } else if (fieldType == 27 || fieldType == 49) {
                    objects[((bufferIndex / 3) * 2) + 1] = messageInfoObjects[objectsPosition5];
                    objectsPosition5++;
                    objectsPosition2 = 1;
                } else if (fieldType == 12 || fieldType == 30 || fieldType == 44) {
                    if ((flags & 1) != 1) {
                        objectsPosition2 = 1;
                    } else {
                        objects[((bufferIndex / 3) * 2) + 1] = messageInfoObjects[objectsPosition5];
                        objectsPosition5++;
                        objectsPosition2 = 1;
                    }
                } else if (fieldType != 50) {
                    objectsPosition2 = 1;
                } else {
                    int mapFieldIndex3 = mapFieldIndex2 + 1;
                    intArray[mapFieldIndex2] = bufferIndex;
                    int objectsPosition6 = objectsPosition5 + 1;
                    objects[(bufferIndex / 3) * 2] = messageInfoObjects[objectsPosition5];
                    if ((result22 & 2048) == 0) {
                        mapFieldIndex2 = mapFieldIndex3;
                        objectsPosition5 = objectsPosition6;
                        objectsPosition2 = 1;
                    } else {
                        objects[((bufferIndex / 3) * 2) + 1] = messageInfoObjects[objectsPosition6];
                        mapFieldIndex2 = mapFieldIndex3;
                        objectsPosition5 = objectsPosition6 + 1;
                        objectsPosition2 = 1;
                    }
                }
                fieldOffset = result20;
                fieldOffset2 = (int) unsafe.objectFieldOffset(field);
                if ((flags & 1) != objectsPosition2 || fieldType > 17) {
                    info = info2;
                    objectsPosition3 = objectsPosition5;
                    oneofCount2 = oneofCount;
                    presenceFieldOffset = 0;
                    index = 0;
                } else {
                    int i31 = i28 + 1;
                    next25 = info2.charAt(i28);
                    if (next25 >= 55296) {
                        int result25 = next25 & 8191;
                        int shift14 = 13;
                        while (true) {
                            i9 = i31 + 1;
                            next9 = info2.charAt(i31);
                            if (next9 < 55296) {
                                break;
                            }
                            result25 |= (next9 & 8191) << shift14;
                            shift14 += 13;
                            i31 = i9;
                        }
                        next25 = result25 | (next9 << shift14);
                        i28 = i9;
                    } else {
                        i28 = i31;
                    }
                    int i32 = next25;
                    int index4 = (oneofCount * 2) + (i32 / 32);
                    Object o3 = messageInfoObjects[index4];
                    info = info2;
                    if (o3 instanceof Field) {
                        hasBitsField = (Field) o3;
                    } else {
                        hasBitsField = reflectField(messageClass, (String) o3);
                        messageInfoObjects[index4] = hasBitsField;
                    }
                    objectsPosition3 = objectsPosition5;
                    oneofCount2 = oneofCount;
                    int presenceFieldOffset3 = (int) unsafe.objectFieldOffset(hasBitsField);
                    int presenceMaskShift = i32 % 32;
                    presenceFieldOffset = presenceFieldOffset3;
                    index = presenceMaskShift;
                }
                if (fieldType >= 18 && fieldType <= 49) {
                    intArray[repeatedFieldIndex2] = fieldOffset2;
                    presenceFieldOffset2 = presenceFieldOffset;
                    repeatedFieldIndex2++;
                    i26 = i28;
                    objectsPosition4 = objectsPosition3;
                } else {
                    presenceFieldOffset2 = presenceFieldOffset;
                    i26 = i28;
                    objectsPosition4 = objectsPosition3;
                }
            }
            int presenceFieldOffset4 = bufferIndex + 1;
            buffer[bufferIndex] = fieldOffset;
            int bufferIndex2 = presenceFieldOffset4 + 1;
            buffer[presenceFieldOffset4] = ((result22 & 512) != 0 ? 536870912 : 0) | ((result22 & 256) != 0 ? 268435456 : 0) | (fieldType << 20) | fieldOffset2;
            bufferIndex = bufferIndex2 + 1;
            buffer[bufferIndex2] = (index << 20) | presenceFieldOffset2;
            length2 = length;
            isProto32 = isProto3;
            info2 = info;
            oneofCount = oneofCount2;
            i16 = 55296;
        }
        return new MessageSchema<>(buffer, objects, minFieldNumber, maxFieldNumber, messageInfo.getDefaultInstance(), isProto32, false, intArray, checkInitialized, checkInitialized + mapFieldCount, newInstanceSchema, listFieldSchema, unknownFieldSchema, extensionSchema, mapFieldSchema);
    }

    private static Field reflectField(Class<?> messageClass, String fieldName) {
        try {
            return messageClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Field[] fields = messageClass.getDeclaredFields();
            for (Field field : fields) {
                if (fieldName.equals(field.getName())) {
                    return field;
                }
            }
            throw new RuntimeException("Field " + fieldName + " for " + messageClass.getName() + " not found. Known fields are " + Arrays.toString(fields));
        }
    }

    /* JADX WARN: Incorrect condition in loop: B:34:0x008b */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    static <T> MessageSchema<T> newSchemaForMessageInfo(StructuralMessageInfo messageInfo, NewInstanceSchema newInstanceSchema, ListFieldSchema listFieldSchema, UnknownFieldSchema<?, ?> unknownFieldSchema, ExtensionSchema<?> extensionSchema, MapFieldSchema mapFieldSchema) {
        int maxFieldNumber;
        int minFieldNumber;
        int[] checkInitialized;
        int[] mapFieldPositions;
        int[] repeatedFieldOffsets;
        int fieldIndex;
        boolean isProto3 = messageInfo.getSyntax() == ProtoSyntax.PROTO3;
        FieldInfo[] fis = messageInfo.getFields();
        if (fis.length == 0) {
            minFieldNumber = 0;
            maxFieldNumber = 0;
        } else {
            int minFieldNumber2 = fis[0].getFieldNumber();
            maxFieldNumber = fis[fis.length - 1].getFieldNumber();
            minFieldNumber = minFieldNumber2;
        }
        int numEntries = fis.length;
        int[] buffer = new int[numEntries * 3];
        Object[] objects = new Object[numEntries * 2];
        int mapFieldCount = 0;
        int repeatedFieldCount = 0;
        for (FieldInfo fi : fis) {
            if (fi.getType() == FieldType.MAP) {
                mapFieldCount++;
            } else if (fi.getType().id() >= 18 && fi.getType().id() <= 49) {
                repeatedFieldCount++;
            }
        }
        int[] mapFieldPositions2 = mapFieldCount > 0 ? new int[mapFieldCount] : null;
        int[] repeatedFieldOffsets2 = repeatedFieldCount > 0 ? new int[repeatedFieldCount] : null;
        int[] checkInitialized2 = messageInfo.getCheckInitialized();
        if (checkInitialized2 != null) {
            checkInitialized = checkInitialized2;
        } else {
            checkInitialized = EMPTY_INT_ARRAY;
        }
        int mapFieldCount2 = 0;
        int repeatedFieldCount2 = 0;
        int mapFieldCount3 = 0;
        int checkInitializedIndex = 0;
        int checkInitializedIndex2 = 0;
        while (checkInitializedIndex2 < repeatedFieldCount) {
            FieldInfo fi2 = fis[checkInitializedIndex2];
            int fieldNumber = fi2.getFieldNumber();
            storeFieldData(fi2, buffer, mapFieldCount3, isProto3, objects);
            if (checkInitializedIndex < checkInitialized.length && checkInitialized[checkInitializedIndex] == fieldNumber) {
                checkInitialized[checkInitializedIndex] = mapFieldCount3;
                checkInitializedIndex++;
            }
            if (fi2.getType() == FieldType.MAP) {
                mapFieldPositions2[mapFieldCount2] = mapFieldCount3;
                mapFieldCount2++;
                fieldIndex = checkInitializedIndex2;
            } else if (fi2.getType().id() < 18 || fi2.getType().id() > 49) {
                fieldIndex = checkInitializedIndex2;
            } else {
                fieldIndex = checkInitializedIndex2;
                repeatedFieldOffsets2[repeatedFieldCount2] = (int) UnsafeUtil.objectFieldOffset(fi2.getField());
                repeatedFieldCount2++;
            }
            checkInitializedIndex2 = fieldIndex + 1;
            mapFieldCount3 += 3;
        }
        if (mapFieldPositions2 != null) {
            mapFieldPositions = mapFieldPositions2;
        } else {
            mapFieldPositions = EMPTY_INT_ARRAY;
        }
        if (repeatedFieldOffsets2 != null) {
            repeatedFieldOffsets = repeatedFieldOffsets2;
        } else {
            repeatedFieldOffsets = EMPTY_INT_ARRAY;
        }
        int[] combined = new int[checkInitialized.length + mapFieldPositions.length + repeatedFieldOffsets.length];
        System.arraycopy(checkInitialized, 0, combined, 0, checkInitialized.length);
        System.arraycopy(mapFieldPositions, 0, combined, checkInitialized.length, mapFieldPositions.length);
        System.arraycopy(repeatedFieldOffsets, 0, combined, checkInitialized.length + mapFieldPositions.length, repeatedFieldOffsets.length);
        return new MessageSchema<>(buffer, objects, minFieldNumber, maxFieldNumber, messageInfo.getDefaultInstance(), isProto3, true, combined, checkInitialized.length, checkInitialized.length + mapFieldPositions.length, newInstanceSchema, listFieldSchema, unknownFieldSchema, extensionSchema, mapFieldSchema);
    }

    private static void storeFieldData(FieldInfo fi, int[] buffer, int bufferIndex, boolean proto3, Object[] objects) {
        int fieldOffset;
        int typeId;
        int typeId2;
        int presenceFieldOffset;
        OneofInfo oneof = fi.getOneof();
        if (oneof != null) {
            typeId = fi.getType().id() + 51;
            fieldOffset = (int) UnsafeUtil.objectFieldOffset(oneof.getValueField());
            typeId2 = (int) UnsafeUtil.objectFieldOffset(oneof.getCaseField());
            presenceFieldOffset = 0;
        } else {
            FieldType type = fi.getType();
            fieldOffset = (int) UnsafeUtil.objectFieldOffset(fi.getField());
            int typeId3 = type.id();
            if (!proto3 && !type.isList() && !type.isMap()) {
                int presenceFieldOffset2 = (int) UnsafeUtil.objectFieldOffset(fi.getPresenceField());
                typeId = typeId3;
                typeId2 = presenceFieldOffset2;
                presenceFieldOffset = Integer.numberOfTrailingZeros(fi.getPresenceMask());
            } else if (fi.getCachedSizeField() == null) {
                typeId = typeId3;
                typeId2 = 0;
                presenceFieldOffset = 0;
            } else {
                int presenceFieldOffset3 = (int) UnsafeUtil.objectFieldOffset(fi.getCachedSizeField());
                typeId = typeId3;
                typeId2 = presenceFieldOffset3;
                presenceFieldOffset = 0;
            }
        }
        buffer[bufferIndex] = fi.getFieldNumber();
        buffer[bufferIndex + 1] = (fi.isEnforceUtf8() ? 536870912 : 0) | (fi.isRequired() ? 268435456 : 0) | (typeId << 20) | fieldOffset;
        buffer[bufferIndex + 2] = (presenceFieldOffset << 20) | typeId2;
        Object messageFieldClass = fi.getMessageFieldClass();
        if (fi.getMapDefaultEntry() != null) {
            objects[(bufferIndex / 3) * 2] = fi.getMapDefaultEntry();
            if (messageFieldClass != null) {
                objects[((bufferIndex / 3) * 2) + 1] = messageFieldClass;
            } else if (fi.getEnumVerifier() != null) {
                objects[((bufferIndex / 3) * 2) + 1] = fi.getEnumVerifier();
            }
        } else if (messageFieldClass != null) {
            objects[((bufferIndex / 3) * 2) + 1] = messageFieldClass;
        } else if (fi.getEnumVerifier() != null) {
            objects[((bufferIndex / 3) * 2) + 1] = fi.getEnumVerifier();
        }
    }

    @Override // com.android.framework.protobuf.Schema
    public T newInstance() {
        return (T) this.newInstanceSchema.newInstance(this.defaultInstance);
    }

    @Override // com.android.framework.protobuf.Schema
    public boolean equals(T message, T other) {
        int bufferLength = this.buffer.length;
        for (int pos = 0; pos < bufferLength; pos += 3) {
            if (!equals(message, other, pos)) {
                return false;
            }
        }
        Object messageUnknown = this.unknownFieldSchema.getFromMessage(message);
        Object otherUnknown = this.unknownFieldSchema.getFromMessage(other);
        if (messageUnknown.equals(otherUnknown)) {
            if (this.hasExtensions) {
                FieldSet<?> messageExtensions = this.extensionSchema.getExtensions(message);
                FieldSet<?> otherExtensions = this.extensionSchema.getExtensions(other);
                return messageExtensions.equals(otherExtensions);
            }
            return true;
        }
        return false;
    }

    private boolean equals(T message, T other, int pos) {
        int typeAndOffset = typeAndOffsetAt(pos);
        long offset = offset(typeAndOffset);
        switch (type(typeAndOffset)) {
            case 0:
                return arePresentForEquals(message, other, pos) && Double.doubleToLongBits(UnsafeUtil.getDouble(message, offset)) == Double.doubleToLongBits(UnsafeUtil.getDouble(other, offset));
            case 1:
                return arePresentForEquals(message, other, pos) && Float.floatToIntBits(UnsafeUtil.getFloat(message, offset)) == Float.floatToIntBits(UnsafeUtil.getFloat(other, offset));
            case 2:
                return arePresentForEquals(message, other, pos) && UnsafeUtil.getLong(message, offset) == UnsafeUtil.getLong(other, offset);
            case 3:
                return arePresentForEquals(message, other, pos) && UnsafeUtil.getLong(message, offset) == UnsafeUtil.getLong(other, offset);
            case 4:
                return arePresentForEquals(message, other, pos) && UnsafeUtil.getInt(message, offset) == UnsafeUtil.getInt(other, offset);
            case 5:
                return arePresentForEquals(message, other, pos) && UnsafeUtil.getLong(message, offset) == UnsafeUtil.getLong(other, offset);
            case 6:
                return arePresentForEquals(message, other, pos) && UnsafeUtil.getInt(message, offset) == UnsafeUtil.getInt(other, offset);
            case 7:
                return arePresentForEquals(message, other, pos) && UnsafeUtil.getBoolean(message, offset) == UnsafeUtil.getBoolean(other, offset);
            case 8:
                return arePresentForEquals(message, other, pos) && SchemaUtil.safeEquals(UnsafeUtil.getObject(message, offset), UnsafeUtil.getObject(other, offset));
            case 9:
                return arePresentForEquals(message, other, pos) && SchemaUtil.safeEquals(UnsafeUtil.getObject(message, offset), UnsafeUtil.getObject(other, offset));
            case 10:
                return arePresentForEquals(message, other, pos) && SchemaUtil.safeEquals(UnsafeUtil.getObject(message, offset), UnsafeUtil.getObject(other, offset));
            case 11:
                return arePresentForEquals(message, other, pos) && UnsafeUtil.getInt(message, offset) == UnsafeUtil.getInt(other, offset);
            case 12:
                return arePresentForEquals(message, other, pos) && UnsafeUtil.getInt(message, offset) == UnsafeUtil.getInt(other, offset);
            case 13:
                return arePresentForEquals(message, other, pos) && UnsafeUtil.getInt(message, offset) == UnsafeUtil.getInt(other, offset);
            case 14:
                return arePresentForEquals(message, other, pos) && UnsafeUtil.getLong(message, offset) == UnsafeUtil.getLong(other, offset);
            case 15:
                return arePresentForEquals(message, other, pos) && UnsafeUtil.getInt(message, offset) == UnsafeUtil.getInt(other, offset);
            case 16:
                return arePresentForEquals(message, other, pos) && UnsafeUtil.getLong(message, offset) == UnsafeUtil.getLong(other, offset);
            case 17:
                return arePresentForEquals(message, other, pos) && SchemaUtil.safeEquals(UnsafeUtil.getObject(message, offset), UnsafeUtil.getObject(other, offset));
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
                return SchemaUtil.safeEquals(UnsafeUtil.getObject(message, offset), UnsafeUtil.getObject(other, offset));
            case 50:
                return SchemaUtil.safeEquals(UnsafeUtil.getObject(message, offset), UnsafeUtil.getObject(other, offset));
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
                return isOneofCaseEqual(message, other, pos) && SchemaUtil.safeEquals(UnsafeUtil.getObject(message, offset), UnsafeUtil.getObject(other, offset));
            default:
                return true;
        }
    }

    @Override // com.android.framework.protobuf.Schema
    public int hashCode(T message) {
        int hashCode = 0;
        int bufferLength = this.buffer.length;
        for (int pos = 0; pos < bufferLength; pos += 3) {
            int typeAndOffset = typeAndOffsetAt(pos);
            int entryNumber = numberAt(pos);
            long offset = offset(typeAndOffset);
            switch (type(typeAndOffset)) {
                case 0:
                    hashCode = (hashCode * 53) + Internal.hashLong(Double.doubleToLongBits(UnsafeUtil.getDouble(message, offset)));
                    break;
                case 1:
                    hashCode = (hashCode * 53) + Float.floatToIntBits(UnsafeUtil.getFloat(message, offset));
                    break;
                case 2:
                    hashCode = (hashCode * 53) + Internal.hashLong(UnsafeUtil.getLong(message, offset));
                    break;
                case 3:
                    hashCode = (hashCode * 53) + Internal.hashLong(UnsafeUtil.getLong(message, offset));
                    break;
                case 4:
                    hashCode = (hashCode * 53) + UnsafeUtil.getInt(message, offset);
                    break;
                case 5:
                    hashCode = (hashCode * 53) + Internal.hashLong(UnsafeUtil.getLong(message, offset));
                    break;
                case 6:
                    hashCode = (hashCode * 53) + UnsafeUtil.getInt(message, offset);
                    break;
                case 7:
                    hashCode = (hashCode * 53) + Internal.hashBoolean(UnsafeUtil.getBoolean(message, offset));
                    break;
                case 8:
                    int protoHash = hashCode * 53;
                    int hashCode2 = protoHash + ((String) UnsafeUtil.getObject(message, offset)).hashCode();
                    hashCode = hashCode2;
                    break;
                case 9:
                    int protoHash2 = 37;
                    Object submessage = UnsafeUtil.getObject(message, offset);
                    if (submessage != null) {
                        protoHash2 = submessage.hashCode();
                    }
                    hashCode = (hashCode * 53) + protoHash2;
                    break;
                case 10:
                    hashCode = (hashCode * 53) + UnsafeUtil.getObject(message, offset).hashCode();
                    break;
                case 11:
                    hashCode = (hashCode * 53) + UnsafeUtil.getInt(message, offset);
                    break;
                case 12:
                    hashCode = (hashCode * 53) + UnsafeUtil.getInt(message, offset);
                    break;
                case 13:
                    hashCode = (hashCode * 53) + UnsafeUtil.getInt(message, offset);
                    break;
                case 14:
                    hashCode = (hashCode * 53) + Internal.hashLong(UnsafeUtil.getLong(message, offset));
                    break;
                case 15:
                    hashCode = (hashCode * 53) + UnsafeUtil.getInt(message, offset);
                    break;
                case 16:
                    int protoHash3 = hashCode * 53;
                    int hashCode3 = protoHash3 + Internal.hashLong(UnsafeUtil.getLong(message, offset));
                    hashCode = hashCode3;
                    break;
                case 17:
                    int protoHash4 = 37;
                    Object submessage2 = UnsafeUtil.getObject(message, offset);
                    if (submessage2 != null) {
                        protoHash4 = submessage2.hashCode();
                    }
                    hashCode = (hashCode * 53) + protoHash4;
                    break;
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                    hashCode = (hashCode * 53) + UnsafeUtil.getObject(message, offset).hashCode();
                    break;
                case 50:
                    hashCode = (hashCode * 53) + UnsafeUtil.getObject(message, offset).hashCode();
                    break;
                case 51:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + Internal.hashLong(Double.doubleToLongBits(oneofDoubleAt(message, offset)));
                        break;
                    } else {
                        break;
                    }
                case 52:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + Float.floatToIntBits(oneofFloatAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 53:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + Internal.hashLong(oneofLongAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 54:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + Internal.hashLong(oneofLongAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 55:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + oneofIntAt(message, offset);
                        break;
                    } else {
                        break;
                    }
                case 56:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + Internal.hashLong(oneofLongAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 57:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + oneofIntAt(message, offset);
                        break;
                    } else {
                        break;
                    }
                case 58:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + Internal.hashBoolean(oneofBooleanAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 59:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + ((String) UnsafeUtil.getObject(message, offset)).hashCode();
                        break;
                    } else {
                        break;
                    }
                case 60:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + UnsafeUtil.getObject(message, offset).hashCode();
                        break;
                    } else {
                        break;
                    }
                case 61:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + UnsafeUtil.getObject(message, offset).hashCode();
                        break;
                    } else {
                        break;
                    }
                case 62:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + oneofIntAt(message, offset);
                        break;
                    } else {
                        break;
                    }
                case 63:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + oneofIntAt(message, offset);
                        break;
                    } else {
                        break;
                    }
                case 64:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + oneofIntAt(message, offset);
                        break;
                    } else {
                        break;
                    }
                case 65:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + Internal.hashLong(oneofLongAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 66:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + oneofIntAt(message, offset);
                        break;
                    } else {
                        break;
                    }
                case 67:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + Internal.hashLong(oneofLongAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 68:
                    if (isOneofPresent(message, entryNumber, pos)) {
                        hashCode = (hashCode * 53) + UnsafeUtil.getObject(message, offset).hashCode();
                        break;
                    } else {
                        break;
                    }
            }
        }
        int pos2 = hashCode * 53;
        int hashCode4 = pos2 + this.unknownFieldSchema.getFromMessage(message).hashCode();
        if (this.hasExtensions) {
            return (hashCode4 * 53) + this.extensionSchema.getExtensions(message).hashCode();
        }
        return hashCode4;
    }

    @Override // com.android.framework.protobuf.Schema
    public void mergeFrom(T message, T other) {
        if (other == null) {
            throw new NullPointerException();
        }
        for (int i = 0; i < this.buffer.length; i += 3) {
            mergeSingleField(message, other, i);
        }
        if (!this.proto3) {
            SchemaUtil.mergeUnknownFields(this.unknownFieldSchema, message, other);
            if (this.hasExtensions) {
                SchemaUtil.mergeExtensions(this.extensionSchema, message, other);
            }
        }
    }

    private void mergeSingleField(T message, T other, int pos) {
        int typeAndOffset = typeAndOffsetAt(pos);
        long offset = offset(typeAndOffset);
        int number = numberAt(pos);
        switch (type(typeAndOffset)) {
            case 0:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putDouble(message, offset, UnsafeUtil.getDouble(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 1:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putFloat(message, offset, UnsafeUtil.getFloat(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 2:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putLong(message, offset, UnsafeUtil.getLong(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 3:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putLong(message, offset, UnsafeUtil.getLong(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 4:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putInt(message, offset, UnsafeUtil.getInt(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 5:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putLong(message, offset, UnsafeUtil.getLong(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 6:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putInt(message, offset, UnsafeUtil.getInt(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 7:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putBoolean(message, offset, UnsafeUtil.getBoolean(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 8:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putObject(message, offset, UnsafeUtil.getObject(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 9:
                mergeMessage(message, other, pos);
                return;
            case 10:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putObject(message, offset, UnsafeUtil.getObject(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 11:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putInt(message, offset, UnsafeUtil.getInt(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 12:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putInt(message, offset, UnsafeUtil.getInt(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 13:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putInt(message, offset, UnsafeUtil.getInt(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 14:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putLong(message, offset, UnsafeUtil.getLong(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 15:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putInt(message, offset, UnsafeUtil.getInt(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 16:
                if (isFieldPresent(other, pos)) {
                    UnsafeUtil.putLong(message, offset, UnsafeUtil.getLong(other, offset));
                    setFieldPresent(message, pos);
                    return;
                }
                return;
            case 17:
                mergeMessage(message, other, pos);
                return;
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
                this.listFieldSchema.mergeListsAt(message, other, offset);
                return;
            case 50:
                SchemaUtil.mergeMap(this.mapFieldSchema, message, other, offset);
                return;
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
                if (isOneofPresent(other, number, pos)) {
                    UnsafeUtil.putObject(message, offset, UnsafeUtil.getObject(other, offset));
                    setOneofPresent(message, number, pos);
                    return;
                }
                return;
            case 60:
                mergeOneofMessage(message, other, pos);
                return;
            case 61:
            case 62:
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
                if (isOneofPresent(other, number, pos)) {
                    UnsafeUtil.putObject(message, offset, UnsafeUtil.getObject(other, offset));
                    setOneofPresent(message, number, pos);
                    return;
                }
                return;
            case 68:
                mergeOneofMessage(message, other, pos);
                return;
            default:
                return;
        }
    }

    private void mergeMessage(T message, T other, int pos) {
        int typeAndOffset = typeAndOffsetAt(pos);
        long offset = offset(typeAndOffset);
        if (!isFieldPresent(other, pos)) {
            return;
        }
        Object mine = UnsafeUtil.getObject(message, offset);
        Object theirs = UnsafeUtil.getObject(other, offset);
        if (mine != null && theirs != null) {
            Object merged = Internal.mergeMessage(mine, theirs);
            UnsafeUtil.putObject(message, offset, merged);
            setFieldPresent(message, pos);
        } else if (theirs != null) {
            UnsafeUtil.putObject(message, offset, theirs);
            setFieldPresent(message, pos);
        }
    }

    private void mergeOneofMessage(T message, T other, int pos) {
        int typeAndOffset = typeAndOffsetAt(pos);
        int number = numberAt(pos);
        long offset = offset(typeAndOffset);
        if (!isOneofPresent(other, number, pos)) {
            return;
        }
        Object mine = UnsafeUtil.getObject(message, offset);
        Object theirs = UnsafeUtil.getObject(other, offset);
        if (mine != null && theirs != null) {
            Object merged = Internal.mergeMessage(mine, theirs);
            UnsafeUtil.putObject(message, offset, merged);
            setOneofPresent(message, number, pos);
        } else if (theirs != null) {
            UnsafeUtil.putObject(message, offset, theirs);
            setOneofPresent(message, number, pos);
        }
    }

    @Override // com.android.framework.protobuf.Schema
    public int getSerializedSize(T message) {
        return this.proto3 ? getSerializedSizeProto3(message) : getSerializedSizeProto2(message);
    }

    /* JADX DEBUG: Type inference failed for r6v2. Raw type applied. Possible types: com.android.framework.protobuf.UnknownFieldSchema<?, ?>, com.android.framework.protobuf.UnknownFieldSchema<UT, UB> */
    private int getSerializedSizeProto2(T message) {
        int currentPresenceFieldOffset;
        int size = 0;
        Unsafe unsafe = UNSAFE;
        int currentPresenceFieldOffset2 = -1;
        int currentPresenceField = 0;
        int i = 0;
        while (i < this.buffer.length) {
            int typeAndOffset = typeAndOffsetAt(i);
            int number = numberAt(i);
            int fieldType = type(typeAndOffset);
            int presenceMaskAndOffset = 0;
            int presenceMask = 0;
            if (fieldType > 17) {
                if (this.useCachedSizeField && fieldType >= FieldType.DOUBLE_LIST_PACKED.id() && fieldType <= FieldType.SINT64_LIST_PACKED.id()) {
                    presenceMaskAndOffset = this.buffer[i + 2] & 1048575;
                }
            } else {
                presenceMaskAndOffset = this.buffer[i + 2];
                int presenceFieldOffset = presenceMaskAndOffset & 1048575;
                presenceMask = 1 << (presenceMaskAndOffset >>> 20);
                if (presenceFieldOffset != currentPresenceFieldOffset2) {
                    currentPresenceFieldOffset2 = presenceFieldOffset;
                    currentPresenceField = unsafe.getInt(message, presenceFieldOffset);
                }
            }
            long offset = offset(typeAndOffset);
            switch (fieldType) {
                case 0:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset3 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset3 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeDoubleSize(number, 0.0d);
                        break;
                    }
                case 1:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset4 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset4 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeFloatSize(number, 0.0f);
                        break;
                    }
                case 2:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset5 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset5 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeInt64Size(number, unsafe.getLong(message, offset));
                        break;
                    }
                case 3:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset6 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset6 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeUInt64Size(number, unsafe.getLong(message, offset));
                        break;
                    }
                case 4:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset7 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset7 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeInt32Size(number, unsafe.getInt(message, offset));
                        break;
                    }
                case 5:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset8 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset8 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeFixed64Size(number, 0L);
                        break;
                    }
                case 6:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset9 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset9 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeFixed32Size(number, 0);
                        break;
                    }
                case 7:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset10 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset10 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeBoolSize(number, true);
                        break;
                    }
                case 8:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset11 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset11 == 0) {
                        break;
                    } else {
                        Object value = unsafe.getObject(message, offset);
                        if (value instanceof ByteString) {
                            size += CodedOutputStream.computeBytesSize(number, (ByteString) value);
                            break;
                        } else {
                            size += CodedOutputStream.computeStringSize(number, (String) value);
                            break;
                        }
                    }
                case 9:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset12 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset12 == 0) {
                        break;
                    } else {
                        size += SchemaUtil.computeSizeMessage(number, unsafe.getObject(message, offset), getMessageFieldSchema(i));
                        break;
                    }
                case 10:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset13 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset13 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeBytesSize(number, (ByteString) unsafe.getObject(message, offset));
                        break;
                    }
                case 11:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset14 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset14 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeUInt32Size(number, unsafe.getInt(message, offset));
                        break;
                    }
                case 12:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset15 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset15 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeEnumSize(number, unsafe.getInt(message, offset));
                        break;
                    }
                case 13:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset16 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset16 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeSFixed32Size(number, 0);
                        break;
                    }
                case 14:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset17 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset17 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeSFixed64Size(number, 0L);
                        break;
                    }
                case 15:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset18 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset18 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeSInt32Size(number, unsafe.getInt(message, offset));
                        break;
                    }
                case 16:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset19 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset19 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeSInt64Size(number, unsafe.getLong(message, offset));
                        break;
                    }
                case 17:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int currentPresenceFieldOffset20 = currentPresenceField & presenceMask;
                    if (currentPresenceFieldOffset20 == 0) {
                        break;
                    } else {
                        size += CodedOutputStream.computeGroupSize(number, (MessageLite) unsafe.getObject(message, offset), getMessageFieldSchema(i));
                        break;
                    }
                case 18:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeFixed64List(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 19:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeFixed32List(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 20:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeInt64List(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 21:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeUInt64List(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 22:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeInt32List(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 23:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeFixed64List(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 24:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeFixed32List(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 25:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeBoolList(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 26:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeStringList(number, (List) unsafe.getObject(message, offset));
                    break;
                case 27:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeMessageList(number, (List) unsafe.getObject(message, offset), getMessageFieldSchema(i));
                    break;
                case 28:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeByteStringList(number, (List) unsafe.getObject(message, offset));
                    break;
                case 29:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeUInt32List(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 30:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeEnumList(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 31:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeFixed32List(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 32:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeFixed64List(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 33:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeSInt32List(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 34:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeSInt64List(number, (List) unsafe.getObject(message, offset), false);
                    break;
                case 35:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize = SchemaUtil.computeSizeFixed64ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize) + fieldSize;
                        break;
                    } else {
                        break;
                    }
                case 36:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize2 = SchemaUtil.computeSizeFixed32ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize2 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize2);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize2) + fieldSize2;
                        break;
                    } else {
                        break;
                    }
                case 37:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize3 = SchemaUtil.computeSizeInt64ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize3 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize3);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize3) + fieldSize3;
                        break;
                    } else {
                        break;
                    }
                case 38:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize4 = SchemaUtil.computeSizeUInt64ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize4 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize4);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize4) + fieldSize4;
                        break;
                    } else {
                        break;
                    }
                case 39:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize5 = SchemaUtil.computeSizeInt32ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize5 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize5);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize5) + fieldSize5;
                        break;
                    } else {
                        break;
                    }
                case 40:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize6 = SchemaUtil.computeSizeFixed64ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize6 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize6);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize6) + fieldSize6;
                        break;
                    } else {
                        break;
                    }
                case 41:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize7 = SchemaUtil.computeSizeFixed32ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize7 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize7);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize7) + fieldSize7;
                        break;
                    } else {
                        break;
                    }
                case 42:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize8 = SchemaUtil.computeSizeBoolListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize8 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize8);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize8) + fieldSize8;
                        break;
                    } else {
                        break;
                    }
                case 43:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize9 = SchemaUtil.computeSizeUInt32ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize9 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize9);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize9) + fieldSize9;
                        break;
                    } else {
                        break;
                    }
                case 44:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize10 = SchemaUtil.computeSizeEnumListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize10 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize10);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize10) + fieldSize10;
                        break;
                    } else {
                        break;
                    }
                case 45:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize11 = SchemaUtil.computeSizeFixed32ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize11 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize11);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize11) + fieldSize11;
                        break;
                    } else {
                        break;
                    }
                case 46:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize12 = SchemaUtil.computeSizeFixed64ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize12 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize12);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize12) + fieldSize12;
                        break;
                    } else {
                        break;
                    }
                case 47:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize13 = SchemaUtil.computeSizeSInt32ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize13 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize13);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize13) + fieldSize13;
                        break;
                    } else {
                        break;
                    }
                case 48:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    int fieldSize14 = SchemaUtil.computeSizeSInt64ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize14 > 0) {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, presenceMaskAndOffset, fieldSize14);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize14) + fieldSize14;
                        break;
                    } else {
                        break;
                    }
                case 49:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += SchemaUtil.computeSizeGroupList(number, (List) unsafe.getObject(message, offset), getMessageFieldSchema(i));
                    break;
                case 50:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    size += this.mapFieldSchema.getSerializedSize(number, unsafe.getObject(message, offset), getMapFieldDefaultEntry(i));
                    break;
                case 51:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeDoubleSize(number, 0.0d);
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 52:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeFloatSize(number, 0.0f);
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 53:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeInt64Size(number, oneofLongAt(message, offset));
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 54:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeUInt64Size(number, oneofLongAt(message, offset));
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 55:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeInt32Size(number, oneofIntAt(message, offset));
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 56:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeFixed64Size(number, 0L);
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 57:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeFixed32Size(number, 0);
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 58:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeBoolSize(number, true);
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 59:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        Object value2 = unsafe.getObject(message, offset);
                        if (value2 instanceof ByteString) {
                            size += CodedOutputStream.computeBytesSize(number, (ByteString) value2);
                        } else {
                            size += CodedOutputStream.computeStringSize(number, (String) value2);
                        }
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 60:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += SchemaUtil.computeSizeMessage(number, unsafe.getObject(message, offset), getMessageFieldSchema(i));
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 61:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeBytesSize(number, (ByteString) unsafe.getObject(message, offset));
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 62:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeUInt32Size(number, oneofIntAt(message, offset));
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 63:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeEnumSize(number, oneofIntAt(message, offset));
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 64:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeSFixed32Size(number, 0);
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 65:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeSFixed64Size(number, 0L);
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 66:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeSInt32Size(number, oneofIntAt(message, offset));
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 67:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeSInt64Size(number, oneofLongAt(message, offset));
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                case 68:
                    if (!isOneofPresent(message, number, i)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    } else {
                        size += CodedOutputStream.computeGroupSize(number, (MessageLite) unsafe.getObject(message, offset), getMessageFieldSchema(i));
                        currentPresenceFieldOffset = currentPresenceFieldOffset2;
                        break;
                    }
                default:
                    currentPresenceFieldOffset = currentPresenceFieldOffset2;
                    break;
            }
            i += 3;
            currentPresenceFieldOffset2 = currentPresenceFieldOffset;
        }
        int size2 = size + getUnknownFieldsSerializedSize(this.unknownFieldSchema, message);
        if (this.hasExtensions) {
            return size2 + this.extensionSchema.getExtensions(message).getSerializedSize();
        }
        return size2;
    }

    /* JADX DEBUG: Type inference failed for r4v2. Raw type applied. Possible types: com.android.framework.protobuf.UnknownFieldSchema<?, ?>, com.android.framework.protobuf.UnknownFieldSchema<UT, UB> */
    private int getSerializedSizeProto3(T message) {
        int cachedSizeOffset;
        Unsafe unsafe = UNSAFE;
        int size = 0;
        for (int i = 0; i < this.buffer.length; i += 3) {
            int typeAndOffset = typeAndOffsetAt(i);
            int fieldType = type(typeAndOffset);
            int number = numberAt(i);
            long offset = offset(typeAndOffset);
            if (fieldType >= FieldType.DOUBLE_LIST_PACKED.id() && fieldType <= FieldType.SINT64_LIST_PACKED.id()) {
                cachedSizeOffset = this.buffer[i + 2] & 1048575;
            } else {
                cachedSizeOffset = 0;
            }
            switch (fieldType) {
                case 0:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeDoubleSize(number, 0.0d);
                        break;
                    } else {
                        break;
                    }
                case 1:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeFloatSize(number, 0.0f);
                        break;
                    } else {
                        break;
                    }
                case 2:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeInt64Size(number, UnsafeUtil.getLong(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 3:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeUInt64Size(number, UnsafeUtil.getLong(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 4:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeInt32Size(number, UnsafeUtil.getInt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 5:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeFixed64Size(number, 0L);
                        break;
                    } else {
                        break;
                    }
                case 6:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeFixed32Size(number, 0);
                        break;
                    } else {
                        break;
                    }
                case 7:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeBoolSize(number, true);
                        break;
                    } else {
                        break;
                    }
                case 8:
                    if (isFieldPresent(message, i)) {
                        Object value = UnsafeUtil.getObject(message, offset);
                        if (value instanceof ByteString) {
                            size += CodedOutputStream.computeBytesSize(number, (ByteString) value);
                            break;
                        } else {
                            size += CodedOutputStream.computeStringSize(number, (String) value);
                            break;
                        }
                    } else {
                        break;
                    }
                case 9:
                    if (isFieldPresent(message, i)) {
                        size += SchemaUtil.computeSizeMessage(number, UnsafeUtil.getObject(message, offset), getMessageFieldSchema(i));
                        break;
                    } else {
                        break;
                    }
                case 10:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeBytesSize(number, (ByteString) UnsafeUtil.getObject(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 11:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeUInt32Size(number, UnsafeUtil.getInt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 12:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeEnumSize(number, UnsafeUtil.getInt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 13:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeSFixed32Size(number, 0);
                        break;
                    } else {
                        break;
                    }
                case 14:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeSFixed64Size(number, 0L);
                        break;
                    } else {
                        break;
                    }
                case 15:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeSInt32Size(number, UnsafeUtil.getInt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 16:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeSInt64Size(number, UnsafeUtil.getLong(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 17:
                    if (isFieldPresent(message, i)) {
                        size += CodedOutputStream.computeGroupSize(number, (MessageLite) UnsafeUtil.getObject(message, offset), getMessageFieldSchema(i));
                        break;
                    } else {
                        break;
                    }
                case 18:
                    size += SchemaUtil.computeSizeFixed64List(number, listAt(message, offset), false);
                    break;
                case 19:
                    size += SchemaUtil.computeSizeFixed32List(number, listAt(message, offset), false);
                    break;
                case 20:
                    size += SchemaUtil.computeSizeInt64List(number, listAt(message, offset), false);
                    break;
                case 21:
                    size += SchemaUtil.computeSizeUInt64List(number, listAt(message, offset), false);
                    break;
                case 22:
                    size += SchemaUtil.computeSizeInt32List(number, listAt(message, offset), false);
                    break;
                case 23:
                    size += SchemaUtil.computeSizeFixed64List(number, listAt(message, offset), false);
                    break;
                case 24:
                    size += SchemaUtil.computeSizeFixed32List(number, listAt(message, offset), false);
                    break;
                case 25:
                    size += SchemaUtil.computeSizeBoolList(number, listAt(message, offset), false);
                    break;
                case 26:
                    size += SchemaUtil.computeSizeStringList(number, listAt(message, offset));
                    break;
                case 27:
                    size += SchemaUtil.computeSizeMessageList(number, listAt(message, offset), getMessageFieldSchema(i));
                    break;
                case 28:
                    size += SchemaUtil.computeSizeByteStringList(number, listAt(message, offset));
                    break;
                case 29:
                    size += SchemaUtil.computeSizeUInt32List(number, listAt(message, offset), false);
                    break;
                case 30:
                    size += SchemaUtil.computeSizeEnumList(number, listAt(message, offset), false);
                    break;
                case 31:
                    size += SchemaUtil.computeSizeFixed32List(number, listAt(message, offset), false);
                    break;
                case 32:
                    size += SchemaUtil.computeSizeFixed64List(number, listAt(message, offset), false);
                    break;
                case 33:
                    size += SchemaUtil.computeSizeSInt32List(number, listAt(message, offset), false);
                    break;
                case 34:
                    size += SchemaUtil.computeSizeSInt64List(number, listAt(message, offset), false);
                    break;
                case 35:
                    int fieldSize = SchemaUtil.computeSizeFixed64ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize) + fieldSize;
                        break;
                    }
                case 36:
                    int fieldSize2 = SchemaUtil.computeSizeFixed32ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize2 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize2);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize2) + fieldSize2;
                        break;
                    }
                case 37:
                    int fieldSize3 = SchemaUtil.computeSizeInt64ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize3 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize3);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize3) + fieldSize3;
                        break;
                    }
                case 38:
                    int fieldSize4 = SchemaUtil.computeSizeUInt64ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize4 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize4);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize4) + fieldSize4;
                        break;
                    }
                case 39:
                    int fieldSize5 = SchemaUtil.computeSizeInt32ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize5 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize5);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize5) + fieldSize5;
                        break;
                    }
                case 40:
                    int fieldSize6 = SchemaUtil.computeSizeFixed64ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize6 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize6);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize6) + fieldSize6;
                        break;
                    }
                case 41:
                    int fieldSize7 = SchemaUtil.computeSizeFixed32ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize7 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize7);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize7) + fieldSize7;
                        break;
                    }
                case 42:
                    int fieldSize8 = SchemaUtil.computeSizeBoolListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize8 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize8);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize8) + fieldSize8;
                        break;
                    }
                case 43:
                    int fieldSize9 = SchemaUtil.computeSizeUInt32ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize9 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize9);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize9) + fieldSize9;
                        break;
                    }
                case 44:
                    int fieldSize10 = SchemaUtil.computeSizeEnumListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize10 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize10);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize10) + fieldSize10;
                        break;
                    }
                case 45:
                    int fieldSize11 = SchemaUtil.computeSizeFixed32ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize11 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize11);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize11) + fieldSize11;
                        break;
                    }
                case 46:
                    int fieldSize12 = SchemaUtil.computeSizeFixed64ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize12 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize12);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize12) + fieldSize12;
                        break;
                    }
                case 47:
                    int fieldSize13 = SchemaUtil.computeSizeSInt32ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize13 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize13);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize13) + fieldSize13;
                        break;
                    }
                case 48:
                    int fieldSize14 = SchemaUtil.computeSizeSInt64ListNoTag((List) unsafe.getObject(message, offset));
                    if (fieldSize14 <= 0) {
                        break;
                    } else {
                        if (this.useCachedSizeField) {
                            unsafe.putInt(message, cachedSizeOffset, fieldSize14);
                        }
                        size += CodedOutputStream.computeTagSize(number) + CodedOutputStream.computeUInt32SizeNoTag(fieldSize14) + fieldSize14;
                        break;
                    }
                case 49:
                    size += SchemaUtil.computeSizeGroupList(number, listAt(message, offset), getMessageFieldSchema(i));
                    break;
                case 50:
                    size += this.mapFieldSchema.getSerializedSize(number, UnsafeUtil.getObject(message, offset), getMapFieldDefaultEntry(i));
                    break;
                case 51:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeDoubleSize(number, 0.0d);
                        break;
                    } else {
                        break;
                    }
                case 52:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeFloatSize(number, 0.0f);
                        break;
                    } else {
                        break;
                    }
                case 53:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeInt64Size(number, oneofLongAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 54:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeUInt64Size(number, oneofLongAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 55:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeInt32Size(number, oneofIntAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 56:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeFixed64Size(number, 0L);
                        break;
                    } else {
                        break;
                    }
                case 57:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeFixed32Size(number, 0);
                        break;
                    } else {
                        break;
                    }
                case 58:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeBoolSize(number, true);
                        break;
                    } else {
                        break;
                    }
                case 59:
                    if (isOneofPresent(message, number, i)) {
                        Object value2 = UnsafeUtil.getObject(message, offset);
                        if (value2 instanceof ByteString) {
                            size += CodedOutputStream.computeBytesSize(number, (ByteString) value2);
                            break;
                        } else {
                            size += CodedOutputStream.computeStringSize(number, (String) value2);
                            break;
                        }
                    } else {
                        break;
                    }
                case 60:
                    if (isOneofPresent(message, number, i)) {
                        size += SchemaUtil.computeSizeMessage(number, UnsafeUtil.getObject(message, offset), getMessageFieldSchema(i));
                        break;
                    } else {
                        break;
                    }
                case 61:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeBytesSize(number, (ByteString) UnsafeUtil.getObject(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 62:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeUInt32Size(number, oneofIntAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 63:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeEnumSize(number, oneofIntAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 64:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeSFixed32Size(number, 0);
                        break;
                    } else {
                        break;
                    }
                case 65:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeSFixed64Size(number, 0L);
                        break;
                    } else {
                        break;
                    }
                case 66:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeSInt32Size(number, oneofIntAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 67:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeSInt64Size(number, oneofLongAt(message, offset));
                        break;
                    } else {
                        break;
                    }
                case 68:
                    if (isOneofPresent(message, number, i)) {
                        size += CodedOutputStream.computeGroupSize(number, (MessageLite) UnsafeUtil.getObject(message, offset), getMessageFieldSchema(i));
                        break;
                    } else {
                        break;
                    }
            }
        }
        return size + getUnknownFieldsSerializedSize(this.unknownFieldSchema, message);
    }

    private <UT, UB> int getUnknownFieldsSerializedSize(UnknownFieldSchema<UT, UB> schema, T message) {
        UT unknowns = schema.getFromMessage(message);
        return schema.getSerializedSize(unknowns);
    }

    private static List<?> listAt(Object message, long offset) {
        return (List) UnsafeUtil.getObject(message, offset);
    }

    @Override // com.android.framework.protobuf.Schema
    public void writeTo(T message, Writer writer) throws IOException {
        if (writer.fieldOrder() == Writer.FieldOrder.DESCENDING) {
            writeFieldsInDescendingOrder(message, writer);
        } else if (this.proto3) {
            writeFieldsInAscendingOrderProto3(message, writer);
        } else {
            writeFieldsInAscendingOrderProto2(message, writer);
        }
    }

    /* JADX DEBUG: Type inference failed for r7v3. Raw type applied. Possible types: com.android.framework.protobuf.UnknownFieldSchema<?, ?>, com.android.framework.protobuf.UnknownFieldSchema<UT, UB> */
    private void writeFieldsInAscendingOrderProto2(T message, Writer writer) throws IOException {
        Map.Entry nextExtension;
        int currentPresenceFieldOffset;
        Iterator<? extends Map.Entry<?, ?>> extensionIterator = null;
        Map.Entry nextExtension2 = null;
        if (this.hasExtensions) {
            FieldSet<?> extensions = this.extensionSchema.getExtensions(message);
            if (!extensions.isEmpty()) {
                extensionIterator = extensions.iterator();
                nextExtension2 = extensionIterator.next();
            }
        }
        int currentPresenceFieldOffset2 = -1;
        int currentPresenceField = 0;
        int bufferLength = this.buffer.length;
        Unsafe unsafe = UNSAFE;
        int pos = 0;
        while (pos < bufferLength) {
            int typeAndOffset = typeAndOffsetAt(pos);
            int number = numberAt(pos);
            int fieldType = type(typeAndOffset);
            int presenceMask = 0;
            Map.Entry nextExtension3 = nextExtension2;
            if (!this.proto3 && fieldType <= 17) {
                int presenceMaskAndOffset = this.buffer[pos + 2];
                int presenceFieldOffset = 1048575 & presenceMaskAndOffset;
                if (presenceFieldOffset != currentPresenceFieldOffset2) {
                    currentPresenceField = unsafe.getInt(message, presenceFieldOffset);
                    currentPresenceFieldOffset2 = presenceFieldOffset;
                }
                presenceMask = 1 << (presenceMaskAndOffset >>> 20);
                nextExtension = nextExtension3;
            } else {
                nextExtension = nextExtension3;
            }
            while (nextExtension != null && this.extensionSchema.extensionNumber(nextExtension) <= number) {
                this.extensionSchema.serializeExtension(writer, nextExtension);
                nextExtension = extensionIterator.hasNext() ? extensionIterator.next() : null;
            }
            Map.Entry nextExtension4 = nextExtension;
            int currentPresenceFieldOffset3 = currentPresenceFieldOffset2;
            long offset = offset(typeAndOffset);
            int bufferLength2 = bufferLength;
            switch (fieldType) {
                case 0:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeDouble(number, doubleAt(message, offset));
                        break;
                    }
                case 1:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeFloat(number, floatAt(message, offset));
                        break;
                    }
                case 2:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeInt64(number, unsafe.getLong(message, offset));
                        break;
                    }
                case 3:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeUInt64(number, unsafe.getLong(message, offset));
                        break;
                    }
                case 4:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeInt32(number, unsafe.getInt(message, offset));
                        break;
                    }
                case 5:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeFixed64(number, unsafe.getLong(message, offset));
                        break;
                    }
                case 6:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeFixed32(number, unsafe.getInt(message, offset));
                        break;
                    }
                case 7:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeBool(number, booleanAt(message, offset));
                        break;
                    }
                case 8:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writeString(number, unsafe.getObject(message, offset), writer);
                        break;
                    }
                case 9:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        Object value = unsafe.getObject(message, offset);
                        writer.writeMessage(number, value, getMessageFieldSchema(pos));
                        break;
                    }
                case 10:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeBytes(number, (ByteString) unsafe.getObject(message, offset));
                        break;
                    }
                case 11:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeUInt32(number, unsafe.getInt(message, offset));
                        break;
                    }
                case 12:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeEnum(number, unsafe.getInt(message, offset));
                        break;
                    }
                case 13:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeSFixed32(number, unsafe.getInt(message, offset));
                        break;
                    }
                case 14:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeSFixed64(number, unsafe.getLong(message, offset));
                        break;
                    }
                case 15:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeSInt32(number, unsafe.getInt(message, offset));
                        break;
                    }
                case 16:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeSInt64(number, unsafe.getLong(message, offset));
                        break;
                    }
                case 17:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if ((currentPresenceField & presenceMask) == 0) {
                        break;
                    } else {
                        writer.writeGroup(number, unsafe.getObject(message, offset), getMessageFieldSchema(pos));
                        break;
                    }
                case 18:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset4 = numberAt(pos);
                    SchemaUtil.writeDoubleList(currentPresenceFieldOffset4, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 19:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset5 = numberAt(pos);
                    SchemaUtil.writeFloatList(currentPresenceFieldOffset5, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 20:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset6 = numberAt(pos);
                    SchemaUtil.writeInt64List(currentPresenceFieldOffset6, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 21:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset7 = numberAt(pos);
                    SchemaUtil.writeUInt64List(currentPresenceFieldOffset7, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 22:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset8 = numberAt(pos);
                    SchemaUtil.writeInt32List(currentPresenceFieldOffset8, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 23:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset9 = numberAt(pos);
                    SchemaUtil.writeFixed64List(currentPresenceFieldOffset9, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 24:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset10 = numberAt(pos);
                    SchemaUtil.writeFixed32List(currentPresenceFieldOffset10, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 25:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset11 = numberAt(pos);
                    SchemaUtil.writeBoolList(currentPresenceFieldOffset11, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 26:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeStringList(numberAt(pos), (List) unsafe.getObject(message, offset), writer);
                    break;
                case 27:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeMessageList(numberAt(pos), (List) unsafe.getObject(message, offset), writer, getMessageFieldSchema(pos));
                    break;
                case 28:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeBytesList(numberAt(pos), (List) unsafe.getObject(message, offset), writer);
                    break;
                case 29:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset12 = numberAt(pos);
                    SchemaUtil.writeUInt32List(currentPresenceFieldOffset12, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 30:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset13 = numberAt(pos);
                    SchemaUtil.writeEnumList(currentPresenceFieldOffset13, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 31:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset14 = numberAt(pos);
                    SchemaUtil.writeSFixed32List(currentPresenceFieldOffset14, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 32:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset15 = numberAt(pos);
                    SchemaUtil.writeSFixed64List(currentPresenceFieldOffset15, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 33:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset16 = numberAt(pos);
                    SchemaUtil.writeSInt32List(currentPresenceFieldOffset16, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 34:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    int currentPresenceFieldOffset17 = numberAt(pos);
                    SchemaUtil.writeSInt64List(currentPresenceFieldOffset17, (List) unsafe.getObject(message, offset), writer, false);
                    break;
                case 35:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeDoubleList(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 36:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeFloatList(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 37:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeInt64List(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 38:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeUInt64List(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 39:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeInt32List(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 40:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeFixed64List(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 41:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeFixed32List(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 42:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeBoolList(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 43:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeUInt32List(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 44:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeEnumList(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 45:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeSFixed32List(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 46:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeSFixed64List(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 47:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeSInt32List(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 48:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeSInt64List(numberAt(pos), (List) unsafe.getObject(message, offset), writer, true);
                    break;
                case 49:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    SchemaUtil.writeGroupList(numberAt(pos), (List) unsafe.getObject(message, offset), writer, getMessageFieldSchema(pos));
                    break;
                case 50:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    writeMapHelper(writer, number, unsafe.getObject(message, offset), pos);
                    break;
                case 51:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeDouble(number, oneofDoubleAt(message, offset));
                        break;
                    }
                case 52:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeFloat(number, oneofFloatAt(message, offset));
                        break;
                    }
                case 53:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeInt64(number, oneofLongAt(message, offset));
                        break;
                    }
                case 54:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeUInt64(number, oneofLongAt(message, offset));
                        break;
                    }
                case 55:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeInt32(number, oneofIntAt(message, offset));
                        break;
                    }
                case 56:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeFixed64(number, oneofLongAt(message, offset));
                        break;
                    }
                case 57:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeFixed32(number, oneofIntAt(message, offset));
                        break;
                    }
                case 58:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeBool(number, oneofBooleanAt(message, offset));
                        break;
                    }
                case 59:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writeString(number, unsafe.getObject(message, offset), writer);
                        break;
                    }
                case 60:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        Object value2 = unsafe.getObject(message, offset);
                        writer.writeMessage(number, value2, getMessageFieldSchema(pos));
                        break;
                    }
                case 61:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeBytes(number, (ByteString) unsafe.getObject(message, offset));
                        break;
                    }
                case 62:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeUInt32(number, oneofIntAt(message, offset));
                        break;
                    }
                case 63:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeEnum(number, oneofIntAt(message, offset));
                        break;
                    }
                case 64:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeSFixed32(number, oneofIntAt(message, offset));
                        break;
                    }
                case 65:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeSFixed64(number, oneofLongAt(message, offset));
                        break;
                    }
                case 66:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeSInt32(number, oneofIntAt(message, offset));
                        break;
                    }
                case 67:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    if (!isOneofPresent(message, number, pos)) {
                        break;
                    } else {
                        writer.writeSInt64(number, oneofLongAt(message, offset));
                        break;
                    }
                case 68:
                    if (!isOneofPresent(message, number, pos)) {
                        currentPresenceFieldOffset = currentPresenceFieldOffset3;
                        break;
                    } else {
                        currentPresenceFieldOffset = currentPresenceFieldOffset3;
                        writer.writeGroup(number, unsafe.getObject(message, offset), getMessageFieldSchema(pos));
                        break;
                    }
                default:
                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                    break;
            }
            pos += 3;
            currentPresenceFieldOffset2 = currentPresenceFieldOffset;
            nextExtension2 = nextExtension4;
            bufferLength = bufferLength2;
        }
        while (nextExtension2 != null) {
            this.extensionSchema.serializeExtension(writer, nextExtension2);
            nextExtension2 = extensionIterator.hasNext() ? extensionIterator.next() : null;
        }
        writeUnknownInMessageTo(this.unknownFieldSchema, message, writer);
    }

    /* JADX DEBUG: Type inference failed for r3v2. Raw type applied. Possible types: com.android.framework.protobuf.UnknownFieldSchema<?, ?>, com.android.framework.protobuf.UnknownFieldSchema<UT, UB> */
    private void writeFieldsInAscendingOrderProto3(T message, Writer writer) throws IOException {
        Iterator<? extends Map.Entry<?, ?>> extensionIterator = null;
        Map.Entry nextExtension = null;
        if (this.hasExtensions) {
            FieldSet<?> extensions = this.extensionSchema.getExtensions(message);
            if (!extensions.isEmpty()) {
                extensionIterator = extensions.iterator();
                nextExtension = extensionIterator.next();
            }
        }
        int bufferLength = this.buffer.length;
        for (int pos = 0; pos < bufferLength; pos += 3) {
            int typeAndOffset = typeAndOffsetAt(pos);
            int number = numberAt(pos);
            while (nextExtension != null && this.extensionSchema.extensionNumber(nextExtension) <= number) {
                this.extensionSchema.serializeExtension(writer, nextExtension);
                nextExtension = extensionIterator.hasNext() ? extensionIterator.next() : null;
            }
            switch (type(typeAndOffset)) {
                case 0:
                    if (isFieldPresent(message, pos)) {
                        writer.writeDouble(number, doubleAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 1:
                    if (isFieldPresent(message, pos)) {
                        writer.writeFloat(number, floatAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 2:
                    if (isFieldPresent(message, pos)) {
                        writer.writeInt64(number, longAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 3:
                    if (isFieldPresent(message, pos)) {
                        writer.writeUInt64(number, longAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 4:
                    if (isFieldPresent(message, pos)) {
                        writer.writeInt32(number, intAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 5:
                    if (isFieldPresent(message, pos)) {
                        writer.writeFixed64(number, longAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 6:
                    if (isFieldPresent(message, pos)) {
                        writer.writeFixed32(number, intAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 7:
                    if (isFieldPresent(message, pos)) {
                        writer.writeBool(number, booleanAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 8:
                    if (isFieldPresent(message, pos)) {
                        writeString(number, UnsafeUtil.getObject(message, offset(typeAndOffset)), writer);
                        break;
                    } else {
                        break;
                    }
                case 9:
                    if (isFieldPresent(message, pos)) {
                        Object value = UnsafeUtil.getObject(message, offset(typeAndOffset));
                        writer.writeMessage(number, value, getMessageFieldSchema(pos));
                        break;
                    } else {
                        break;
                    }
                case 10:
                    if (isFieldPresent(message, pos)) {
                        writer.writeBytes(number, (ByteString) UnsafeUtil.getObject(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 11:
                    if (isFieldPresent(message, pos)) {
                        writer.writeUInt32(number, intAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 12:
                    if (isFieldPresent(message, pos)) {
                        writer.writeEnum(number, intAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 13:
                    if (isFieldPresent(message, pos)) {
                        writer.writeSFixed32(number, intAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 14:
                    if (isFieldPresent(message, pos)) {
                        writer.writeSFixed64(number, longAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 15:
                    if (isFieldPresent(message, pos)) {
                        writer.writeSInt32(number, intAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 16:
                    if (isFieldPresent(message, pos)) {
                        writer.writeSInt64(number, longAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 17:
                    if (isFieldPresent(message, pos)) {
                        writer.writeGroup(number, UnsafeUtil.getObject(message, offset(typeAndOffset)), getMessageFieldSchema(pos));
                        break;
                    } else {
                        break;
                    }
                case 18:
                    SchemaUtil.writeDoubleList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 19:
                    SchemaUtil.writeFloatList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 20:
                    SchemaUtil.writeInt64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 21:
                    SchemaUtil.writeUInt64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 22:
                    SchemaUtil.writeInt32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 23:
                    SchemaUtil.writeFixed64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 24:
                    SchemaUtil.writeFixed32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 25:
                    SchemaUtil.writeBoolList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 26:
                    SchemaUtil.writeStringList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer);
                    break;
                case 27:
                    SchemaUtil.writeMessageList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, getMessageFieldSchema(pos));
                    break;
                case 28:
                    SchemaUtil.writeBytesList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer);
                    break;
                case 29:
                    SchemaUtil.writeUInt32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 30:
                    SchemaUtil.writeEnumList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 31:
                    SchemaUtil.writeSFixed32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 32:
                    SchemaUtil.writeSFixed64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 33:
                    SchemaUtil.writeSInt32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 34:
                    SchemaUtil.writeSInt64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                    break;
                case 35:
                    SchemaUtil.writeDoubleList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 36:
                    SchemaUtil.writeFloatList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 37:
                    SchemaUtil.writeInt64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 38:
                    SchemaUtil.writeUInt64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 39:
                    SchemaUtil.writeInt32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 40:
                    SchemaUtil.writeFixed64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 41:
                    SchemaUtil.writeFixed32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 42:
                    SchemaUtil.writeBoolList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 43:
                    SchemaUtil.writeUInt32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 44:
                    SchemaUtil.writeEnumList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 45:
                    SchemaUtil.writeSFixed32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 46:
                    SchemaUtil.writeSFixed64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 47:
                    SchemaUtil.writeSInt32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 48:
                    SchemaUtil.writeSInt64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                    break;
                case 49:
                    SchemaUtil.writeGroupList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, getMessageFieldSchema(pos));
                    break;
                case 50:
                    writeMapHelper(writer, number, UnsafeUtil.getObject(message, offset(typeAndOffset)), pos);
                    break;
                case 51:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeDouble(number, oneofDoubleAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 52:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeFloat(number, oneofFloatAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 53:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeInt64(number, oneofLongAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 54:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeUInt64(number, oneofLongAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 55:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeInt32(number, oneofIntAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 56:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeFixed64(number, oneofLongAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 57:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeFixed32(number, oneofIntAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 58:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeBool(number, oneofBooleanAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 59:
                    if (isOneofPresent(message, number, pos)) {
                        writeString(number, UnsafeUtil.getObject(message, offset(typeAndOffset)), writer);
                        break;
                    } else {
                        break;
                    }
                case 60:
                    if (isOneofPresent(message, number, pos)) {
                        Object value2 = UnsafeUtil.getObject(message, offset(typeAndOffset));
                        writer.writeMessage(number, value2, getMessageFieldSchema(pos));
                        break;
                    } else {
                        break;
                    }
                case 61:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeBytes(number, (ByteString) UnsafeUtil.getObject(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 62:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeUInt32(number, oneofIntAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 63:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeEnum(number, oneofIntAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 64:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeSFixed32(number, oneofIntAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 65:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeSFixed64(number, oneofLongAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 66:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeSInt32(number, oneofIntAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 67:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeSInt64(number, oneofLongAt(message, offset(typeAndOffset)));
                        break;
                    } else {
                        break;
                    }
                case 68:
                    if (isOneofPresent(message, number, pos)) {
                        writer.writeGroup(number, UnsafeUtil.getObject(message, offset(typeAndOffset)), getMessageFieldSchema(pos));
                        break;
                    } else {
                        break;
                    }
            }
        }
        while (nextExtension != null) {
            this.extensionSchema.serializeExtension(writer, nextExtension);
            nextExtension = extensionIterator.hasNext() ? extensionIterator.next() : null;
        }
        writeUnknownInMessageTo(this.unknownFieldSchema, message, writer);
    }

    /* JADX DEBUG: Type inference failed for r0v0. Raw type applied. Possible types: com.android.framework.protobuf.UnknownFieldSchema<?, ?>, com.android.framework.protobuf.UnknownFieldSchema<UT, UB> */
    private void writeFieldsInDescendingOrder(T message, Writer writer) throws IOException {
        writeUnknownInMessageTo(this.unknownFieldSchema, message, writer);
        Iterator<? extends Map.Entry<?, ?>> extensionIterator = null;
        Map.Entry nextExtension = null;
        if (this.hasExtensions) {
            FieldSet<?> extensions = this.extensionSchema.getExtensions(message);
            if (!extensions.isEmpty()) {
                extensionIterator = extensions.descendingIterator();
                nextExtension = extensionIterator.next();
            }
        }
        int pos = this.buffer.length;
        while (true) {
            pos -= 3;
            if (pos >= 0) {
                int typeAndOffset = typeAndOffsetAt(pos);
                int number = numberAt(pos);
                while (nextExtension != null && this.extensionSchema.extensionNumber(nextExtension) > number) {
                    this.extensionSchema.serializeExtension(writer, nextExtension);
                    nextExtension = extensionIterator.hasNext() ? extensionIterator.next() : null;
                }
                switch (type(typeAndOffset)) {
                    case 0:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeDouble(number, doubleAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 1:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeFloat(number, floatAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 2:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeInt64(number, longAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 3:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeUInt64(number, longAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 4:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeInt32(number, intAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 5:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeFixed64(number, longAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 6:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeFixed32(number, intAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 7:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeBool(number, booleanAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 8:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writeString(number, UnsafeUtil.getObject(message, offset(typeAndOffset)), writer);
                            break;
                        }
                    case 9:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            Object value = UnsafeUtil.getObject(message, offset(typeAndOffset));
                            writer.writeMessage(number, value, getMessageFieldSchema(pos));
                            break;
                        }
                    case 10:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeBytes(number, (ByteString) UnsafeUtil.getObject(message, offset(typeAndOffset)));
                            break;
                        }
                    case 11:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeUInt32(number, intAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 12:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeEnum(number, intAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 13:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeSFixed32(number, intAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 14:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeSFixed64(number, longAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 15:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeSInt32(number, intAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 16:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeSInt64(number, longAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 17:
                        if (!isFieldPresent(message, pos)) {
                            break;
                        } else {
                            writer.writeGroup(number, UnsafeUtil.getObject(message, offset(typeAndOffset)), getMessageFieldSchema(pos));
                            break;
                        }
                    case 18:
                        SchemaUtil.writeDoubleList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 19:
                        SchemaUtil.writeFloatList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 20:
                        SchemaUtil.writeInt64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 21:
                        SchemaUtil.writeUInt64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 22:
                        SchemaUtil.writeInt32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 23:
                        SchemaUtil.writeFixed64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 24:
                        SchemaUtil.writeFixed32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 25:
                        SchemaUtil.writeBoolList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 26:
                        SchemaUtil.writeStringList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer);
                        break;
                    case 27:
                        SchemaUtil.writeMessageList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, getMessageFieldSchema(pos));
                        break;
                    case 28:
                        SchemaUtil.writeBytesList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer);
                        break;
                    case 29:
                        SchemaUtil.writeUInt32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 30:
                        SchemaUtil.writeEnumList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 31:
                        SchemaUtil.writeSFixed32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 32:
                        SchemaUtil.writeSFixed64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 33:
                        SchemaUtil.writeSInt32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 34:
                        SchemaUtil.writeSInt64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, false);
                        break;
                    case 35:
                        SchemaUtil.writeDoubleList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 36:
                        SchemaUtil.writeFloatList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 37:
                        SchemaUtil.writeInt64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 38:
                        SchemaUtil.writeUInt64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 39:
                        SchemaUtil.writeInt32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 40:
                        SchemaUtil.writeFixed64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 41:
                        SchemaUtil.writeFixed32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 42:
                        SchemaUtil.writeBoolList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 43:
                        SchemaUtil.writeUInt32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 44:
                        SchemaUtil.writeEnumList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 45:
                        SchemaUtil.writeSFixed32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 46:
                        SchemaUtil.writeSFixed64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 47:
                        SchemaUtil.writeSInt32List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 48:
                        SchemaUtil.writeSInt64List(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, true);
                        break;
                    case 49:
                        SchemaUtil.writeGroupList(numberAt(pos), (List) UnsafeUtil.getObject(message, offset(typeAndOffset)), writer, getMessageFieldSchema(pos));
                        break;
                    case 50:
                        writeMapHelper(writer, number, UnsafeUtil.getObject(message, offset(typeAndOffset)), pos);
                        break;
                    case 51:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeDouble(number, oneofDoubleAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 52:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeFloat(number, oneofFloatAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 53:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeInt64(number, oneofLongAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 54:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeUInt64(number, oneofLongAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 55:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeInt32(number, oneofIntAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 56:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeFixed64(number, oneofLongAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 57:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeFixed32(number, oneofIntAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 58:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeBool(number, oneofBooleanAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 59:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writeString(number, UnsafeUtil.getObject(message, offset(typeAndOffset)), writer);
                            break;
                        }
                    case 60:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            Object value2 = UnsafeUtil.getObject(message, offset(typeAndOffset));
                            writer.writeMessage(number, value2, getMessageFieldSchema(pos));
                            break;
                        }
                    case 61:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeBytes(number, (ByteString) UnsafeUtil.getObject(message, offset(typeAndOffset)));
                            break;
                        }
                    case 62:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeUInt32(number, oneofIntAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 63:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeEnum(number, oneofIntAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 64:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeSFixed32(number, oneofIntAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 65:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeSFixed64(number, oneofLongAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 66:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeSInt32(number, oneofIntAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 67:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeSInt64(number, oneofLongAt(message, offset(typeAndOffset)));
                            break;
                        }
                    case 68:
                        if (!isOneofPresent(message, number, pos)) {
                            break;
                        } else {
                            writer.writeGroup(number, UnsafeUtil.getObject(message, offset(typeAndOffset)), getMessageFieldSchema(pos));
                            break;
                        }
                }
            } else {
                while (nextExtension != null) {
                    this.extensionSchema.serializeExtension(writer, nextExtension);
                    nextExtension = extensionIterator.hasNext() ? extensionIterator.next() : null;
                }
                return;
            }
        }
    }

    private <K, V> void writeMapHelper(Writer writer, int number, Object mapField, int pos) throws IOException {
        if (mapField != null) {
            writer.writeMap(number, this.mapFieldSchema.forMapMetadata(getMapFieldDefaultEntry(pos)), this.mapFieldSchema.forMapData(mapField));
        }
    }

    private <UT, UB> void writeUnknownInMessageTo(UnknownFieldSchema<UT, UB> schema, T message, Writer writer) throws IOException {
        schema.writeTo(schema.getFromMessage(message), writer);
    }

    /* JADX DEBUG: Type inference failed for r1v0. Raw type applied. Possible types: com.android.framework.protobuf.UnknownFieldSchema<?, ?>, com.android.framework.protobuf.UnknownFieldSchema<UT, UB> */
    /* JADX DEBUG: Type inference failed for r2v0. Raw type applied. Possible types: com.android.framework.protobuf.ExtensionSchema<?>, com.android.framework.protobuf.ExtensionSchema<ET extends com.android.framework.protobuf.FieldSet$FieldDescriptorLite<ET>> */
    @Override // com.android.framework.protobuf.Schema
    public void mergeFrom(T message, Reader reader, ExtensionRegistryLite extensionRegistry) throws IOException {
        if (extensionRegistry == null) {
            throw new NullPointerException();
        }
        mergeFromHelper(this.unknownFieldSchema, this.extensionSchema, message, reader, extensionRegistry);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4306=5, 4324=13, 4325=6, 4326=6, 4328=6, 4329=6] */
    /* JADX DEBUG: Multi-variable search result rejected for r19v0, resolved type: com.android.framework.protobuf.UnknownFieldSchema<UT, UB> */
    /* JADX WARN: Code restructure failed: missing block: B:247:?, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x0091, code lost:
        r1 = r18.checkInitializedCount;
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x0095, code lost:
        if (r1 >= r18.repeatedFieldOffsetStart) goto L199;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x0097, code lost:
        r13 = filterMapUnknownEnumValues(r21, r18.intArray[r1], r13, r19);
        r1 = r1 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x00a2, code lost:
        if (r13 == null) goto L203;
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x00a4, code lost:
        r19.setBuilderToMessage(r21, r13);
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x00a7, code lost:
        return;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:175:0x06ac A[Catch: all -> 0x06f1, TRY_LEAVE, TryCatch #0 {all -> 0x06f1, blocks: (B:27:0x0066, B:28:0x006e, B:30:0x0077, B:34:0x0082, B:35:0x0087, B:46:0x00ab, B:48:0x00b0, B:52:0x00ba, B:161:0x0685, B:173:0x06a6, B:175:0x06ac, B:185:0x06cb, B:186:0x06d0, B:55:0x00c1, B:56:0x00d6, B:57:0x00ec, B:58:0x0102, B:59:0x0118, B:60:0x012e, B:62:0x0138, B:65:0x013f, B:66:0x0148, B:67:0x0159, B:68:0x016f, B:69:0x0180, B:71:0x0187, B:73:0x01b6, B:72:0x01a3, B:74:0x01bc, B:75:0x01c5, B:76:0x01db, B:77:0x01f1, B:78:0x0207, B:79:0x021d, B:80:0x0233, B:81:0x0249, B:82:0x025f, B:83:0x0275, B:88:0x028e, B:90:0x02a0, B:93:0x02ac, B:94:0x02bf, B:95:0x02d2, B:96:0x02e5, B:97:0x02f8, B:98:0x0315, B:99:0x0328, B:100:0x033b, B:101:0x034e, B:102:0x0361, B:103:0x0374, B:104:0x0387, B:105:0x039a, B:106:0x03ad, B:107:0x03c0, B:108:0x03d3, B:109:0x03e6, B:110:0x03f9, B:111:0x040c, B:112:0x0429, B:113:0x043c, B:114:0x044f, B:119:0x0470, B:120:0x0475, B:121:0x0485, B:122:0x0495, B:123:0x04a5, B:124:0x04b5, B:125:0x04c5, B:126:0x04d5, B:127:0x04e5, B:128:0x04f5, B:130:0x04fd, B:131:0x051a, B:132:0x052f, B:133:0x0540, B:134:0x0551, B:135:0x0562, B:136:0x0573, B:138:0x057e, B:141:0x0585, B:142:0x058d, B:143:0x0599, B:144:0x05aa, B:145:0x05bb, B:147:0x05c3, B:148:0x05e0, B:149:0x05f5, B:150:0x05fe, B:151:0x060f, B:152:0x0620, B:153:0x0631, B:154:0x0641, B:155:0x0651, B:156:0x0661, B:157:0x0671), top: B:207:0x0066 }] */
    /* JADX WARN: Removed duplicated region for block: B:184:0x06c9  */
    /* JADX WARN: Removed duplicated region for block: B:203:0x0700 A[LOOP:5: B:201:0x06fc->B:203:0x0700, LOOP_END] */
    /* JADX WARN: Removed duplicated region for block: B:205:0x070d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private <UT, UB, ET extends FieldSet.FieldDescriptorLite<ET>> void mergeFromHelper(UnknownFieldSchema<UT, UB> unknownFieldSchema, ExtensionSchema<ET> extensionSchema, T message, Reader reader, ExtensionRegistryLite extensionRegistry) throws IOException {
        Throwable th;
        int i;
        Object obj = null;
        FieldSet<ET> extensions = null;
        while (true) {
            try {
                int number = reader.getFieldNumber();
                int pos = positionForFieldNumber(number);
                if (pos >= 0) {
                    FieldSet<ET> extensions2 = extensions;
                    int typeAndOffset = typeAndOffsetAt(pos);
                    try {
                        switch (type(typeAndOffset)) {
                            case 0:
                                UnsafeUtil.putDouble(message, offset(typeAndOffset), reader.readDouble());
                                setFieldPresent(message, pos);
                                break;
                            case 1:
                                UnsafeUtil.putFloat(message, offset(typeAndOffset), reader.readFloat());
                                setFieldPresent(message, pos);
                                break;
                            case 2:
                                UnsafeUtil.putLong(message, offset(typeAndOffset), reader.readInt64());
                                setFieldPresent(message, pos);
                                break;
                            case 3:
                                UnsafeUtil.putLong(message, offset(typeAndOffset), reader.readUInt64());
                                setFieldPresent(message, pos);
                                break;
                            case 4:
                                UnsafeUtil.putInt(message, offset(typeAndOffset), reader.readInt32());
                                setFieldPresent(message, pos);
                                break;
                            case 5:
                                UnsafeUtil.putLong(message, offset(typeAndOffset), reader.readFixed64());
                                setFieldPresent(message, pos);
                                break;
                            case 6:
                                UnsafeUtil.putInt(message, offset(typeAndOffset), reader.readFixed32());
                                setFieldPresent(message, pos);
                                break;
                            case 7:
                                UnsafeUtil.putBoolean(message, offset(typeAndOffset), reader.readBool());
                                setFieldPresent(message, pos);
                                break;
                            case 8:
                                readString(message, typeAndOffset, reader);
                                setFieldPresent(message, pos);
                                break;
                            case 9:
                                if (isFieldPresent(message, pos)) {
                                    Object mergedResult = Internal.mergeMessage(UnsafeUtil.getObject(message, offset(typeAndOffset)), reader.readMessageBySchemaWithCheck(getMessageFieldSchema(pos), extensionRegistry));
                                    UnsafeUtil.putObject(message, offset(typeAndOffset), mergedResult);
                                    break;
                                } else {
                                    UnsafeUtil.putObject(message, offset(typeAndOffset), reader.readMessageBySchemaWithCheck(getMessageFieldSchema(pos), extensionRegistry));
                                    setFieldPresent(message, pos);
                                    break;
                                }
                            case 10:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), reader.readBytes());
                                setFieldPresent(message, pos);
                                break;
                            case 11:
                                UnsafeUtil.putInt(message, offset(typeAndOffset), reader.readUInt32());
                                setFieldPresent(message, pos);
                                break;
                            case 12:
                                int enumValue = reader.readEnum();
                                Internal.EnumVerifier enumVerifier = getEnumFieldVerifier(pos);
                                if (enumVerifier != null && !enumVerifier.isInRange(enumValue)) {
                                    obj = SchemaUtil.storeUnknownEnum(number, enumValue, obj, unknownFieldSchema);
                                    break;
                                }
                                UnsafeUtil.putInt(message, offset(typeAndOffset), enumValue);
                                setFieldPresent(message, pos);
                                break;
                            case 13:
                                UnsafeUtil.putInt(message, offset(typeAndOffset), reader.readSFixed32());
                                setFieldPresent(message, pos);
                                break;
                            case 14:
                                UnsafeUtil.putLong(message, offset(typeAndOffset), reader.readSFixed64());
                                setFieldPresent(message, pos);
                                break;
                            case 15:
                                UnsafeUtil.putInt(message, offset(typeAndOffset), reader.readSInt32());
                                setFieldPresent(message, pos);
                                break;
                            case 16:
                                UnsafeUtil.putLong(message, offset(typeAndOffset), reader.readSInt64());
                                setFieldPresent(message, pos);
                                break;
                            case 17:
                                if (isFieldPresent(message, pos)) {
                                    Object mergedResult2 = Internal.mergeMessage(UnsafeUtil.getObject(message, offset(typeAndOffset)), reader.readGroupBySchemaWithCheck(getMessageFieldSchema(pos), extensionRegistry));
                                    UnsafeUtil.putObject(message, offset(typeAndOffset), mergedResult2);
                                    break;
                                } else {
                                    UnsafeUtil.putObject(message, offset(typeAndOffset), reader.readGroupBySchemaWithCheck(getMessageFieldSchema(pos), extensionRegistry));
                                    setFieldPresent(message, pos);
                                    break;
                                }
                            case 18:
                                reader.readDoubleList(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 19:
                                reader.readFloatList(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 20:
                                reader.readInt64List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 21:
                                reader.readUInt64List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 22:
                                reader.readInt32List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 23:
                                reader.readFixed64List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 24:
                                reader.readFixed32List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 25:
                                reader.readBoolList(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 26:
                                readStringList(message, typeAndOffset, reader);
                                break;
                            case 27:
                                readMessageList(message, typeAndOffset, reader, getMessageFieldSchema(pos), extensionRegistry);
                                break;
                            case 28:
                                reader.readBytesList(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 29:
                                reader.readUInt32List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 30:
                                List<Integer> enumList = this.listFieldSchema.mutableListAt(message, offset(typeAndOffset));
                                reader.readEnumList(enumList);
                                obj = SchemaUtil.filterUnknownEnumList(number, enumList, getEnumFieldVerifier(pos), obj, unknownFieldSchema);
                                break;
                            case 31:
                                reader.readSFixed32List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 32:
                                reader.readSFixed64List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 33:
                                reader.readSInt32List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 34:
                                reader.readSInt64List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 35:
                                reader.readDoubleList(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 36:
                                reader.readFloatList(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 37:
                                reader.readInt64List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 38:
                                reader.readUInt64List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 39:
                                reader.readInt32List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 40:
                                reader.readFixed64List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 41:
                                reader.readFixed32List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 42:
                                reader.readBoolList(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 43:
                                reader.readUInt32List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 44:
                                List<Integer> enumList2 = this.listFieldSchema.mutableListAt(message, offset(typeAndOffset));
                                reader.readEnumList(enumList2);
                                obj = SchemaUtil.filterUnknownEnumList(number, enumList2, getEnumFieldVerifier(pos), obj, unknownFieldSchema);
                                break;
                            case 45:
                                reader.readSFixed32List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 46:
                                reader.readSFixed64List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 47:
                                reader.readSInt32List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 48:
                                reader.readSInt64List(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
                                break;
                            case 49:
                                try {
                                    readGroupList(message, offset(typeAndOffset), reader, getMessageFieldSchema(pos), extensionRegistry);
                                } catch (InvalidProtocolBufferException.InvalidWireTypeException e) {
                                    if (!unknownFieldSchema.shouldDiscardUnknownFields(reader)) {
                                    }
                                    extensions = extensions2;
                                }
                                break;
                            case 50:
                                mergeMap(message, pos, getMapFieldDefaultEntry(pos), extensionRegistry, reader);
                                break;
                            case 51:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Double.valueOf(reader.readDouble()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 52:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Float.valueOf(reader.readFloat()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 53:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Long.valueOf(reader.readInt64()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 54:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Long.valueOf(reader.readUInt64()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 55:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Integer.valueOf(reader.readInt32()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 56:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Long.valueOf(reader.readFixed64()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 57:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Integer.valueOf(reader.readFixed32()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 58:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Boolean.valueOf(reader.readBool()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 59:
                                readString(message, typeAndOffset, reader);
                                setOneofPresent(message, number, pos);
                                break;
                            case 60:
                                if (isOneofPresent(message, number, pos)) {
                                    Object mergedResult3 = Internal.mergeMessage(UnsafeUtil.getObject(message, offset(typeAndOffset)), reader.readMessageBySchemaWithCheck(getMessageFieldSchema(pos), extensionRegistry));
                                    UnsafeUtil.putObject(message, offset(typeAndOffset), mergedResult3);
                                } else {
                                    UnsafeUtil.putObject(message, offset(typeAndOffset), reader.readMessageBySchemaWithCheck(getMessageFieldSchema(pos), extensionRegistry));
                                    setFieldPresent(message, pos);
                                }
                                setOneofPresent(message, number, pos);
                                break;
                            case 61:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), reader.readBytes());
                                setOneofPresent(message, number, pos);
                                break;
                            case 62:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Integer.valueOf(reader.readUInt32()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 63:
                                int enumValue2 = reader.readEnum();
                                Internal.EnumVerifier enumVerifier2 = getEnumFieldVerifier(pos);
                                if (enumVerifier2 != null && !enumVerifier2.isInRange(enumValue2)) {
                                    obj = SchemaUtil.storeUnknownEnum(number, enumValue2, obj, unknownFieldSchema);
                                    break;
                                }
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Integer.valueOf(enumValue2));
                                setOneofPresent(message, number, pos);
                                break;
                            case 64:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Integer.valueOf(reader.readSFixed32()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 65:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Long.valueOf(reader.readSFixed64()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 66:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Integer.valueOf(reader.readSInt32()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 67:
                                UnsafeUtil.putObject(message, offset(typeAndOffset), Long.valueOf(reader.readSInt64()));
                                setOneofPresent(message, number, pos);
                                break;
                            case 68:
                                try {
                                    UnsafeUtil.putObject(message, offset(typeAndOffset), reader.readGroupBySchemaWithCheck(getMessageFieldSchema(pos), extensionRegistry));
                                    setOneofPresent(message, number, pos);
                                } catch (InvalidProtocolBufferException.InvalidWireTypeException e2) {
                                    if (!unknownFieldSchema.shouldDiscardUnknownFields(reader)) {
                                    }
                                    extensions = extensions2;
                                }
                                break;
                            default:
                                if (obj == null) {
                                    try {
                                        obj = unknownFieldSchema.newBuilder();
                                    } catch (InvalidProtocolBufferException.InvalidWireTypeException e3) {
                                        if (!unknownFieldSchema.shouldDiscardUnknownFields(reader)) {
                                            if (obj == null) {
                                                obj = unknownFieldSchema.getBuilderFromMessage(message);
                                            }
                                            if (!unknownFieldSchema.mergeOneFieldFrom(obj, reader)) {
                                                for (int i2 = this.checkInitializedCount; i2 < this.repeatedFieldOffsetStart; i2++) {
                                                    obj = filterMapUnknownEnumValues(message, this.intArray[i2], obj, unknownFieldSchema);
                                                }
                                                if (obj != null) {
                                                    unknownFieldSchema.setBuilderToMessage(message, obj);
                                                    return;
                                                }
                                                return;
                                            }
                                        } else if (!reader.skipField()) {
                                            for (int i3 = this.checkInitializedCount; i3 < this.repeatedFieldOffsetStart; i3++) {
                                                obj = filterMapUnknownEnumValues(message, this.intArray[i3], obj, unknownFieldSchema);
                                            }
                                            if (obj != null) {
                                                unknownFieldSchema.setBuilderToMessage(message, obj);
                                                return;
                                            }
                                            return;
                                        }
                                        extensions = extensions2;
                                    }
                                }
                                if (!unknownFieldSchema.mergeOneFieldFrom(obj, reader)) {
                                    for (int i4 = this.checkInitializedCount; i4 < this.repeatedFieldOffsetStart; i4++) {
                                        obj = filterMapUnknownEnumValues(message, this.intArray[i4], obj, unknownFieldSchema);
                                    }
                                    if (obj != null) {
                                        unknownFieldSchema.setBuilderToMessage(message, obj);
                                        return;
                                    }
                                    return;
                                }
                        }
                    } catch (InvalidProtocolBufferException.InvalidWireTypeException e4) {
                    }
                    extensions = extensions2;
                } else if (number == Integer.MAX_VALUE) {
                    for (int i5 = this.checkInitializedCount; i5 < this.repeatedFieldOffsetStart; i5++) {
                        obj = filterMapUnknownEnumValues(message, this.intArray[i5], obj, unknownFieldSchema);
                    }
                    if (obj != null) {
                        unknownFieldSchema.setBuilderToMessage(message, obj);
                        return;
                    }
                    return;
                } else {
                    Object extension = !this.hasExtensions ? null : extensionSchema.findExtensionByNumber(extensionRegistry, this.defaultInstance, number);
                    if (extension != null) {
                        if (extensions == null) {
                            try {
                                extensions = extensionSchema.getMutableExtensions(message);
                            } catch (Throwable th2) {
                                th = th2;
                                while (i < this.repeatedFieldOffsetStart) {
                                }
                                if (obj != null) {
                                }
                                throw th;
                            }
                        }
                        FieldSet<ET> extensions3 = extensions;
                        try {
                            obj = extensionSchema.parseExtension(reader, extension, extensionRegistry, extensions, obj, unknownFieldSchema);
                            extensions = extensions3;
                        } catch (Throwable th3) {
                            th = th3;
                            for (i = this.checkInitializedCount; i < this.repeatedFieldOffsetStart; i++) {
                                obj = filterMapUnknownEnumValues(message, this.intArray[i], obj, unknownFieldSchema);
                            }
                            if (obj != null) {
                                unknownFieldSchema.setBuilderToMessage(message, obj);
                            }
                            throw th;
                        }
                    } else {
                        FieldSet<ET> extensions4 = extensions;
                        if (!unknownFieldSchema.shouldDiscardUnknownFields(reader)) {
                            if (obj == null) {
                                obj = unknownFieldSchema.getBuilderFromMessage(message);
                            }
                            if (unknownFieldSchema.mergeOneFieldFrom(obj, reader)) {
                                extensions = extensions4;
                            }
                        } else if (reader.skipField()) {
                            extensions = extensions4;
                        }
                    }
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static UnknownFieldSetLite getMutableUnknownFields(Object message) {
        UnknownFieldSetLite unknownFields = ((GeneratedMessageLite) message).unknownFields;
        if (unknownFields == UnknownFieldSetLite.getDefaultInstance()) {
            UnknownFieldSetLite unknownFields2 = UnknownFieldSetLite.newInstance();
            ((GeneratedMessageLite) message).unknownFields = unknownFields2;
            return unknownFields2;
        }
        return unknownFields;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.framework.protobuf.MessageSchema$1  reason: invalid class name */
    /* loaded from: classes4.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$google$protobuf$WireFormat$FieldType;

        static {
            int[] iArr = new int[WireFormat.FieldType.values().length];
            $SwitchMap$com$google$protobuf$WireFormat$FieldType = iArr;
            try {
                iArr[WireFormat.FieldType.BOOL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.BYTES.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.DOUBLE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.FIXED32.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.SFIXED32.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.FIXED64.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.SFIXED64.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.FLOAT.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.ENUM.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.INT32.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.UINT32.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.INT64.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.UINT64.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.MESSAGE.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.SINT32.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.SINT64.ordinal()] = 16;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$com$google$protobuf$WireFormat$FieldType[WireFormat.FieldType.STRING.ordinal()] = 17;
            } catch (NoSuchFieldError e17) {
            }
        }
    }

    private int decodeMapEntryValue(byte[] data, int position, int limit, WireFormat.FieldType fieldType, Class<?> messageType, ArrayDecoders.Registers registers) throws IOException {
        switch (AnonymousClass1.$SwitchMap$com$google$protobuf$WireFormat$FieldType[fieldType.ordinal()]) {
            case 1:
                int position2 = ArrayDecoders.decodeVarint64(data, position, registers);
                registers.object1 = Boolean.valueOf(registers.long1 != 0);
                return position2;
            case 2:
                return ArrayDecoders.decodeBytes(data, position, registers);
            case 3:
                registers.object1 = Double.valueOf(ArrayDecoders.decodeDouble(data, position));
                return position + 8;
            case 4:
            case 5:
                registers.object1 = Integer.valueOf(ArrayDecoders.decodeFixed32(data, position));
                return position + 4;
            case 6:
            case 7:
                registers.object1 = Long.valueOf(ArrayDecoders.decodeFixed64(data, position));
                return position + 8;
            case 8:
                registers.object1 = Float.valueOf(ArrayDecoders.decodeFloat(data, position));
                return position + 4;
            case 9:
            case 10:
            case 11:
                int position3 = ArrayDecoders.decodeVarint32(data, position, registers);
                registers.object1 = Integer.valueOf(registers.int1);
                return position3;
            case 12:
            case 13:
                int position4 = ArrayDecoders.decodeVarint64(data, position, registers);
                registers.object1 = Long.valueOf(registers.long1);
                return position4;
            case 14:
                return ArrayDecoders.decodeMessageField(Protobuf.getInstance().schemaFor((Class) messageType), data, position, limit, registers);
            case 15:
                int position5 = ArrayDecoders.decodeVarint32(data, position, registers);
                registers.object1 = Integer.valueOf(CodedInputStream.decodeZigZag32(registers.int1));
                return position5;
            case 16:
                int position6 = ArrayDecoders.decodeVarint64(data, position, registers);
                registers.object1 = Long.valueOf(CodedInputStream.decodeZigZag64(registers.long1));
                return position6;
            case 17:
                return ArrayDecoders.decodeStringRequireUtf8(data, position, registers);
            default:
                throw new RuntimeException("unsupported field type.");
        }
    }

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:39:0x001e */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:42:0x001e */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r13v1, types: [java.lang.Object] */
    /* JADX WARN: Type inference failed for: r13v2, types: [java.lang.Object] */
    /* JADX WARN: Type inference failed for: r13v3 */
    /* JADX WARN: Type inference failed for: r14v1, types: [java.lang.Object] */
    /* JADX WARN: Type inference failed for: r14v2, types: [java.lang.Object] */
    /* JADX WARN: Type inference failed for: r14v3 */
    /* JADX WARN: Type inference failed for: r23v0, types: [java.util.Map, java.util.Map<K, V>] */
    private <K, V> int decodeMapEntry(byte[] data, int position, int limit, MapEntryLite.Metadata<K, V> metadata, Map<K, V> map, ArrayDecoders.Registers registers) throws IOException {
        int tag;
        int position2;
        int position3;
        int length;
        int position4 = ArrayDecoders.decodeVarint32(data, position, registers);
        int wireType = registers.int1;
        if (wireType < 0 || wireType > limit - position4) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        int end = position4 + wireType;
        K key = metadata.defaultKey;
        K key2 = key;
        V value = metadata.defaultValue;
        while (position4 < end) {
            int position5 = position4 + 1;
            int tag2 = data[position4];
            if (tag2 >= 0) {
                tag = tag2;
                position2 = position5;
            } else {
                int position6 = ArrayDecoders.decodeVarint32(tag2, data, position5, registers);
                tag = registers.int1;
                position2 = position6;
            }
            int fieldNumber = tag >>> 3;
            int wireType2 = tag & 7;
            switch (fieldNumber) {
                case 1:
                    position3 = position2;
                    length = wireType;
                    if (wireType2 != metadata.keyType.getWireType()) {
                        break;
                    } else {
                        position4 = decodeMapEntryValue(data, position3, limit, metadata.keyType, null, registers);
                        key2 = registers.object1;
                        wireType = length;
                        continue;
                    }
                case 2:
                    if (wireType2 != metadata.valueType.getWireType()) {
                        position3 = position2;
                        length = wireType;
                        break;
                    } else {
                        int length2 = wireType;
                        position4 = decodeMapEntryValue(data, position2, limit, metadata.valueType, metadata.defaultValue.getClass(), registers);
                        value = registers.object1;
                        wireType = length2;
                        continue;
                    }
                default:
                    position3 = position2;
                    length = wireType;
                    break;
            }
            position4 = ArrayDecoders.skipField(tag, data, position3, limit, registers);
            wireType = length;
        }
        if (position4 == end) {
            map.put(key2, value);
            return end;
        }
        throw InvalidProtocolBufferException.parseFailure();
    }

    private int parseRepeatedField(T message, byte[] data, int position, int limit, int tag, int number, int wireType, int bufferPosition, long typeAndOffset, int fieldType, long fieldOffset, ArrayDecoders.Registers registers) throws IOException {
        Internal.ProtobufList<?> list;
        int position2;
        Unsafe unsafe = UNSAFE;
        Internal.ProtobufList<?> list2 = (Internal.ProtobufList) unsafe.getObject(message, fieldOffset);
        if (list2.isModifiable()) {
            list = list2;
        } else {
            int size = list2.size();
            Internal.ProtobufList<?> list3 = list2.mutableCopyWithCapacity(size == 0 ? 10 : size * 2);
            unsafe.putObject(message, fieldOffset, list3);
            list = list3;
        }
        switch (fieldType) {
            case 18:
            case 35:
                Internal.ProtobufList<?> list4 = list;
                if (wireType == 2) {
                    return ArrayDecoders.decodePackedDoubleList(data, position, list4, registers);
                }
                if (wireType == 1) {
                    return ArrayDecoders.decodeDoubleList(tag, data, position, limit, list4, registers);
                }
                break;
            case 19:
            case 36:
                Internal.ProtobufList<?> list5 = list;
                if (wireType == 2) {
                    return ArrayDecoders.decodePackedFloatList(data, position, list5, registers);
                }
                if (wireType == 5) {
                    return ArrayDecoders.decodeFloatList(tag, data, position, limit, list5, registers);
                }
                break;
            case 20:
            case 21:
            case 37:
            case 38:
                Internal.ProtobufList<?> list6 = list;
                if (wireType == 2) {
                    return ArrayDecoders.decodePackedVarint64List(data, position, list6, registers);
                }
                if (wireType == 0) {
                    return ArrayDecoders.decodeVarint64List(tag, data, position, limit, list6, registers);
                }
                break;
            case 22:
            case 29:
            case 39:
            case 43:
                Internal.ProtobufList<?> list7 = list;
                if (wireType == 2) {
                    return ArrayDecoders.decodePackedVarint32List(data, position, list7, registers);
                }
                if (wireType == 0) {
                    return ArrayDecoders.decodeVarint32List(tag, data, position, limit, list7, registers);
                }
                break;
            case 23:
            case 32:
            case 40:
            case 46:
                Internal.ProtobufList<?> list8 = list;
                if (wireType == 2) {
                    return ArrayDecoders.decodePackedFixed64List(data, position, list8, registers);
                }
                if (wireType == 1) {
                    return ArrayDecoders.decodeFixed64List(tag, data, position, limit, list8, registers);
                }
                break;
            case 24:
            case 31:
            case 41:
            case 45:
                Internal.ProtobufList<?> list9 = list;
                if (wireType == 2) {
                    return ArrayDecoders.decodePackedFixed32List(data, position, list9, registers);
                }
                if (wireType == 5) {
                    return ArrayDecoders.decodeFixed32List(tag, data, position, limit, list9, registers);
                }
                break;
            case 25:
            case 42:
                Internal.ProtobufList<?> list10 = list;
                if (wireType == 2) {
                    return ArrayDecoders.decodePackedBoolList(data, position, list10, registers);
                }
                if (wireType == 0) {
                    return ArrayDecoders.decodeBoolList(tag, data, position, limit, list10, registers);
                }
                break;
            case 26:
                Internal.ProtobufList<?> list11 = list;
                if (wireType == 2) {
                    if ((typeAndOffset & 536870912) == 0) {
                        return ArrayDecoders.decodeStringList(tag, data, position, limit, list11, registers);
                    }
                    return ArrayDecoders.decodeStringListRequireUtf8(tag, data, position, limit, list11, registers);
                }
                break;
            case 27:
                Internal.ProtobufList<?> list12 = list;
                if (wireType == 2) {
                    return ArrayDecoders.decodeMessageList(getMessageFieldSchema(bufferPosition), tag, data, position, limit, list12, registers);
                }
                break;
            case 28:
                Internal.ProtobufList<?> list13 = list;
                if (wireType == 2) {
                    return ArrayDecoders.decodeBytesList(tag, data, position, limit, list13, registers);
                }
                break;
            case 30:
            case 44:
                Internal.ProtobufList<?> list14 = list;
                if (wireType == 2) {
                    position2 = ArrayDecoders.decodePackedVarint32List(data, position, list14, registers);
                } else if (wireType != 0) {
                    break;
                } else {
                    position2 = ArrayDecoders.decodeVarint32List(tag, data, position, limit, list14, registers);
                }
                UnknownFieldSetLite unknownFields = ((GeneratedMessageLite) message).unknownFields;
                if (unknownFields == UnknownFieldSetLite.getDefaultInstance()) {
                    unknownFields = null;
                }
                UnknownFieldSetLite unknownFields2 = (UnknownFieldSetLite) SchemaUtil.filterUnknownEnumList(number, (List<Integer>) list14, getEnumFieldVerifier(bufferPosition), unknownFields, (UnknownFieldSchema<UT, UnknownFieldSetLite>) this.unknownFieldSchema);
                if (unknownFields2 != null) {
                    ((GeneratedMessageLite) message).unknownFields = unknownFields2;
                    return position2;
                }
                return position2;
            case 33:
            case 47:
                Internal.ProtobufList<?> list15 = list;
                if (wireType == 2) {
                    return ArrayDecoders.decodePackedSInt32List(data, position, list15, registers);
                }
                if (wireType != 0) {
                    break;
                } else {
                    return ArrayDecoders.decodeSInt32List(tag, data, position, limit, list15, registers);
                }
            case 34:
            case 48:
                Internal.ProtobufList<?> list16 = list;
                if (wireType == 2) {
                    return ArrayDecoders.decodePackedSInt64List(data, position, list16, registers);
                }
                if (wireType != 0) {
                    break;
                } else {
                    return ArrayDecoders.decodeSInt64List(tag, data, position, limit, list16, registers);
                }
            case 49:
                if (wireType != 3) {
                    break;
                } else {
                    return ArrayDecoders.decodeGroupList(getMessageFieldSchema(bufferPosition), tag, data, position, limit, list, registers);
                }
        }
        return position;
    }

    private <K, V> int parseMapField(T message, byte[] data, int position, int limit, int bufferPosition, long fieldOffset, ArrayDecoders.Registers registers) throws IOException {
        Object mapField;
        Unsafe unsafe = UNSAFE;
        Object mapDefaultEntry = getMapFieldDefaultEntry(bufferPosition);
        Object mapField2 = unsafe.getObject(message, fieldOffset);
        if (!this.mapFieldSchema.isImmutable(mapField2)) {
            mapField = mapField2;
        } else {
            Object mapField3 = this.mapFieldSchema.newMapField(mapDefaultEntry);
            this.mapFieldSchema.mergeFrom(mapField3, mapField2);
            unsafe.putObject(message, fieldOffset, mapField3);
            mapField = mapField3;
        }
        return decodeMapEntry(data, position, limit, this.mapFieldSchema.forMapMetadata(mapDefaultEntry), this.mapFieldSchema.forMutableMapData(mapField), registers);
    }

    private int parseOneofField(T message, byte[] data, int position, int limit, int tag, int number, int wireType, int typeAndOffset, int fieldType, long fieldOffset, int bufferPosition, ArrayDecoders.Registers registers) throws IOException {
        int position2;
        Unsafe unsafe;
        Unsafe unsafe2 = UNSAFE;
        long oneofCaseOffset = this.buffer[bufferPosition + 2] & 1048575;
        Object obj = null;
        switch (fieldType) {
            case 51:
                position2 = position;
                if (wireType == 1) {
                    unsafe2.putObject(message, fieldOffset, Double.valueOf(ArrayDecoders.decodeDouble(data, position)));
                    int position3 = position2 + 8;
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position3;
                }
                break;
            case 52:
                position2 = position;
                if (wireType == 5) {
                    unsafe2.putObject(message, fieldOffset, Float.valueOf(ArrayDecoders.decodeFloat(data, position)));
                    int position4 = position2 + 4;
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position4;
                }
                break;
            case 53:
            case 54:
                position2 = position;
                if (wireType == 0) {
                    int position5 = ArrayDecoders.decodeVarint64(data, position2, registers);
                    unsafe2.putObject(message, fieldOffset, Long.valueOf(registers.long1));
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position5;
                }
                break;
            case 55:
            case 62:
                position2 = position;
                if (wireType == 0) {
                    int position6 = ArrayDecoders.decodeVarint32(data, position2, registers);
                    unsafe2.putObject(message, fieldOffset, Integer.valueOf(registers.int1));
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position6;
                }
                break;
            case 56:
            case 65:
                position2 = position;
                if (wireType == 1) {
                    unsafe2.putObject(message, fieldOffset, Long.valueOf(ArrayDecoders.decodeFixed64(data, position)));
                    int position7 = position2 + 8;
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position7;
                }
                break;
            case 57:
            case 64:
                position2 = position;
                if (wireType == 5) {
                    unsafe2.putObject(message, fieldOffset, Integer.valueOf(ArrayDecoders.decodeFixed32(data, position)));
                    int position8 = position2 + 4;
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position8;
                }
                break;
            case 58:
                position2 = position;
                if (wireType == 0) {
                    int position9 = ArrayDecoders.decodeVarint64(data, position2, registers);
                    unsafe2.putObject(message, fieldOffset, Boolean.valueOf(registers.long1 != 0));
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position9;
                }
                break;
            case 59:
                position2 = position;
                if (wireType == 2) {
                    int position10 = ArrayDecoders.decodeVarint32(data, position2, registers);
                    int length = registers.int1;
                    if (length == 0) {
                        unsafe2.putObject(message, fieldOffset, "");
                    } else if ((typeAndOffset & 536870912) != 0 && !Utf8.isValidUtf8(data, position10, position10 + length)) {
                        throw InvalidProtocolBufferException.invalidUtf8();
                    } else {
                        String value = new String(data, position10, length, Internal.UTF_8);
                        unsafe2.putObject(message, fieldOffset, value);
                        position10 += length;
                    }
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position10;
                }
                break;
            case 60:
                position2 = position;
                if (wireType != 2) {
                    break;
                } else {
                    int position11 = ArrayDecoders.decodeMessageField(getMessageFieldSchema(bufferPosition), data, position2, limit, registers);
                    if (unsafe2.getInt(message, oneofCaseOffset) == number) {
                        obj = unsafe2.getObject(message, fieldOffset);
                    }
                    Object oldValue = obj;
                    if (oldValue == null) {
                        unsafe2.putObject(message, fieldOffset, registers.object1);
                    } else {
                        unsafe2.putObject(message, fieldOffset, Internal.mergeMessage(oldValue, registers.object1));
                    }
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position11;
                }
            case 61:
                position2 = position;
                if (wireType != 2) {
                    break;
                } else {
                    int position12 = ArrayDecoders.decodeBytes(data, position2, registers);
                    unsafe2.putObject(message, fieldOffset, registers.object1);
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position12;
                }
            case 63:
                position2 = position;
                if (wireType != 0) {
                    break;
                } else {
                    int position13 = ArrayDecoders.decodeVarint32(data, position2, registers);
                    int enumValue = registers.int1;
                    Internal.EnumVerifier enumVerifier = getEnumFieldVerifier(bufferPosition);
                    if (enumVerifier == null) {
                        unsafe = unsafe2;
                    } else if (enumVerifier.isInRange(enumValue)) {
                        unsafe = unsafe2;
                    } else {
                        unsafe = unsafe2;
                        getMutableUnknownFields(message).storeField(tag, Long.valueOf(enumValue));
                        return position13;
                    }
                    unsafe.putObject(message, fieldOffset, Integer.valueOf(enumValue));
                    unsafe.putInt(message, oneofCaseOffset, number);
                    return position13;
                }
            case 66:
                position2 = position;
                if (wireType != 0) {
                    break;
                } else {
                    int position14 = ArrayDecoders.decodeVarint32(data, position2, registers);
                    unsafe2.putObject(message, fieldOffset, Integer.valueOf(CodedInputStream.decodeZigZag32(registers.int1)));
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position14;
                }
            case 67:
                if (wireType != 0) {
                    position2 = position;
                    break;
                } else {
                    int position15 = ArrayDecoders.decodeVarint64(data, position, registers);
                    unsafe2.putObject(message, fieldOffset, Long.valueOf(CodedInputStream.decodeZigZag64(registers.long1)));
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position15;
                }
            case 68:
                if (wireType != 3) {
                    position2 = position;
                    break;
                } else {
                    int endTag = (tag & (-8)) | 4;
                    int position16 = ArrayDecoders.decodeGroupField(getMessageFieldSchema(bufferPosition), data, position, limit, endTag, registers);
                    if (unsafe2.getInt(message, oneofCaseOffset) == number) {
                        obj = unsafe2.getObject(message, fieldOffset);
                    }
                    Object oldValue2 = obj;
                    if (oldValue2 == null) {
                        unsafe2.putObject(message, fieldOffset, registers.object1);
                    } else {
                        unsafe2.putObject(message, fieldOffset, Internal.mergeMessage(oldValue2, registers.object1));
                    }
                    unsafe2.putInt(message, oneofCaseOffset, number);
                    return position16;
                }
            default:
                position2 = position;
                break;
        }
        return position2;
    }

    private Schema getMessageFieldSchema(int pos) {
        int index = (pos / 3) * 2;
        Schema schema = (Schema) this.objects[index];
        if (schema != null) {
            return schema;
        }
        Schema schema2 = Protobuf.getInstance().schemaFor((Class) ((Class) this.objects[index + 1]));
        this.objects[index] = schema2;
        return schema2;
    }

    private Object getMapFieldDefaultEntry(int pos) {
        return this.objects[(pos / 3) * 2];
    }

    private Internal.EnumVerifier getEnumFieldVerifier(int pos) {
        return (Internal.EnumVerifier) this.objects[((pos / 3) * 2) + 1];
    }

    /* JADX DEBUG: Type inference failed for r14v2. Raw type applied. Possible types: com.android.framework.protobuf.UnknownFieldSchema<?, ?>, com.android.framework.protobuf.UnknownFieldSchema<UT, UB> */
    /* JADX INFO: Access modifiers changed from: package-private */
    public int parseProto2Message(T message, byte[] data, int position, int limit, int endGroup, ArrayDecoders.Registers registers) throws IOException {
        Unsafe unsafe;
        int i;
        MessageSchema<T> messageSchema;
        T t;
        int tag;
        int position2;
        int pos;
        int tag2;
        int pos2;
        int position3;
        int currentPresenceField;
        ArrayDecoders.Registers registers2;
        int position4;
        int wireType;
        int currentPresenceFieldOffset;
        int currentPresenceFieldOffset2;
        int tag3;
        Unsafe unsafe2;
        MessageSchema<T> messageSchema2 = this;
        T t2 = message;
        byte[] bArr = data;
        int wireType2 = limit;
        int i2 = endGroup;
        ArrayDecoders.Registers registers3 = registers;
        Unsafe unsafe3 = UNSAFE;
        int tag4 = 0;
        int oldNumber = -1;
        int pos3 = 0;
        int currentPresenceFieldOffset3 = -1;
        int currentPresenceField2 = 0;
        int position5 = position;
        while (true) {
            if (position5 < wireType2) {
                int position6 = position5 + 1;
                int tag5 = bArr[position5];
                if (tag5 >= 0) {
                    tag = tag5;
                    position2 = position6;
                } else {
                    int position7 = ArrayDecoders.decodeVarint32(tag5, bArr, position6, registers3);
                    int tag6 = registers3.int1;
                    tag = tag6;
                    position2 = position7;
                }
                int position8 = tag >>> 3;
                int wireType3 = tag & 7;
                if (position8 > oldNumber) {
                    pos = messageSchema2.positionForFieldNumber(position8, pos3 / 3);
                } else {
                    int pos4 = messageSchema2.positionForFieldNumber(position8);
                    pos = pos4;
                }
                if (pos == -1) {
                    tag2 = tag;
                    pos2 = 0;
                    position3 = position2;
                    currentPresenceField = currentPresenceField2;
                    unsafe = unsafe3;
                } else {
                    int typeAndOffset = messageSchema2.buffer[pos + 1];
                    int fieldType = type(typeAndOffset);
                    long fieldOffset = offset(typeAndOffset);
                    int tag7 = tag;
                    if (fieldType <= 17) {
                        int presenceMaskAndOffset = messageSchema2.buffer[pos + 2];
                        int presenceMask = 1 << (presenceMaskAndOffset >>> 20);
                        int presenceFieldOffset = presenceMaskAndOffset & 1048575;
                        if (presenceFieldOffset == currentPresenceFieldOffset3) {
                            position4 = position2;
                        } else {
                            if (currentPresenceFieldOffset3 == -1) {
                                position4 = position2;
                            } else {
                                position4 = position2;
                                unsafe3.putInt(t2, currentPresenceFieldOffset3, currentPresenceField2);
                            }
                            currentPresenceFieldOffset3 = presenceFieldOffset;
                            currentPresenceField2 = unsafe3.getInt(t2, presenceFieldOffset);
                        }
                        switch (fieldType) {
                            case 0:
                                wireType = wireType3;
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                tag3 = tag7;
                                currentPresenceFieldOffset2 = position4;
                                unsafe2 = unsafe3;
                                bArr = data;
                                ArrayDecoders.Registers registers4 = registers3;
                                if (wireType == 1) {
                                    UnsafeUtil.putDouble(t2, fieldOffset, ArrayDecoders.decodeDouble(bArr, currentPresenceFieldOffset2));
                                    position5 = currentPresenceFieldOffset2 + 8;
                                    currentPresenceField2 |= presenceMask;
                                    wireType2 = limit;
                                    pos3 = pos;
                                    registers3 = registers4;
                                    oldNumber = position8;
                                    unsafe3 = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag4 = tag3;
                                    i2 = endGroup;
                                    break;
                                } else {
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                }
                            case 1:
                                wireType = wireType3;
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                tag3 = tag7;
                                currentPresenceFieldOffset2 = position4;
                                unsafe2 = unsafe3;
                                bArr = data;
                                ArrayDecoders.Registers registers5 = registers3;
                                if (wireType == 5) {
                                    UnsafeUtil.putFloat(t2, fieldOffset, ArrayDecoders.decodeFloat(bArr, currentPresenceFieldOffset2));
                                    position5 = currentPresenceFieldOffset2 + 4;
                                    currentPresenceField2 |= presenceMask;
                                    wireType2 = limit;
                                    pos3 = pos;
                                    registers3 = registers5;
                                    oldNumber = position8;
                                    unsafe3 = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag4 = tag3;
                                    i2 = endGroup;
                                    break;
                                } else {
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                }
                            case 2:
                            case 3:
                                wireType = wireType3;
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                bArr = data;
                                currentPresenceFieldOffset2 = position4;
                                ArrayDecoders.Registers registers6 = registers3;
                                if (wireType != 0) {
                                    tag3 = tag7;
                                    unsafe2 = unsafe3;
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                } else {
                                    int position9 = ArrayDecoders.decodeVarint64(bArr, currentPresenceFieldOffset2, registers6);
                                    unsafe3.putLong(message, fieldOffset, registers6.long1);
                                    currentPresenceField2 |= presenceMask;
                                    wireType2 = limit;
                                    pos3 = pos;
                                    position5 = position9;
                                    registers3 = registers6;
                                    oldNumber = position8;
                                    unsafe3 = unsafe3;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag4 = tag7;
                                    i2 = endGroup;
                                    break;
                                }
                            case 4:
                            case 11:
                                wireType = wireType3;
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                bArr = data;
                                currentPresenceFieldOffset2 = position4;
                                ArrayDecoders.Registers registers7 = registers3;
                                if (wireType == 0) {
                                    position5 = ArrayDecoders.decodeVarint32(bArr, currentPresenceFieldOffset2, registers7);
                                    unsafe3.putInt(t2, fieldOffset, registers7.int1);
                                    currentPresenceField2 |= presenceMask;
                                    wireType2 = limit;
                                    pos3 = pos;
                                    tag4 = tag7;
                                    registers3 = registers7;
                                    oldNumber = position8;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    i2 = endGroup;
                                    break;
                                } else {
                                    tag3 = tag7;
                                    unsafe2 = unsafe3;
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                }
                            case 5:
                            case 14:
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                currentPresenceFieldOffset2 = position4;
                                bArr = data;
                                ArrayDecoders.Registers registers8 = registers3;
                                if (wireType3 != 1) {
                                    wireType = wireType3;
                                    tag3 = tag7;
                                    unsafe2 = unsafe3;
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                } else {
                                    unsafe3.putLong(message, fieldOffset, ArrayDecoders.decodeFixed64(bArr, currentPresenceFieldOffset2));
                                    position5 = currentPresenceFieldOffset2 + 8;
                                    currentPresenceField2 |= presenceMask;
                                    wireType2 = limit;
                                    pos3 = pos;
                                    tag4 = tag7;
                                    registers3 = registers8;
                                    oldNumber = position8;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    i2 = endGroup;
                                    break;
                                }
                            case 6:
                            case 13:
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                currentPresenceFieldOffset2 = position4;
                                bArr = data;
                                ArrayDecoders.Registers registers9 = registers3;
                                if (wireType3 != 5) {
                                    wireType = wireType3;
                                    unsafe2 = unsafe3;
                                    tag3 = tag7;
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                } else {
                                    unsafe3.putInt(t2, fieldOffset, ArrayDecoders.decodeFixed32(bArr, currentPresenceFieldOffset2));
                                    position5 = currentPresenceFieldOffset2 + 4;
                                    currentPresenceField2 |= presenceMask;
                                    pos3 = pos;
                                    tag4 = tag7;
                                    registers3 = registers9;
                                    oldNumber = position8;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    i2 = endGroup;
                                    break;
                                }
                            case 7:
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                currentPresenceFieldOffset2 = position4;
                                bArr = data;
                                ArrayDecoders.Registers registers10 = registers3;
                                if (wireType3 != 0) {
                                    wireType = wireType3;
                                    unsafe2 = unsafe3;
                                    tag3 = tag7;
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                } else {
                                    int position10 = ArrayDecoders.decodeVarint64(bArr, currentPresenceFieldOffset2, registers10);
                                    UnsafeUtil.putBoolean(t2, fieldOffset, registers10.long1 != 0);
                                    currentPresenceField2 |= presenceMask;
                                    position5 = position10;
                                    pos3 = pos;
                                    tag4 = tag7;
                                    registers3 = registers10;
                                    oldNumber = position8;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    i2 = endGroup;
                                    break;
                                }
                            case 8:
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                currentPresenceFieldOffset2 = position4;
                                bArr = data;
                                ArrayDecoders.Registers registers11 = registers3;
                                if (wireType3 != 2) {
                                    wireType = wireType3;
                                    tag3 = tag7;
                                    unsafe2 = unsafe3;
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                } else {
                                    if ((536870912 & typeAndOffset) == 0) {
                                        position5 = ArrayDecoders.decodeString(bArr, currentPresenceFieldOffset2, registers11);
                                    } else {
                                        position5 = ArrayDecoders.decodeStringRequireUtf8(bArr, currentPresenceFieldOffset2, registers11);
                                    }
                                    unsafe3.putObject(t2, fieldOffset, registers11.object1);
                                    currentPresenceField2 |= presenceMask;
                                    pos3 = pos;
                                    tag4 = tag7;
                                    registers3 = registers11;
                                    oldNumber = position8;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    i2 = endGroup;
                                    break;
                                }
                            case 9:
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                currentPresenceFieldOffset2 = position4;
                                bArr = data;
                                ArrayDecoders.Registers registers12 = registers3;
                                if (wireType3 != 2) {
                                    wireType = wireType3;
                                    unsafe2 = unsafe3;
                                    tag3 = tag7;
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                } else {
                                    wireType2 = limit;
                                    position5 = ArrayDecoders.decodeMessageField(messageSchema2.getMessageFieldSchema(pos), bArr, currentPresenceFieldOffset2, wireType2, registers12);
                                    if ((currentPresenceField2 & presenceMask) == 0) {
                                        unsafe3.putObject(t2, fieldOffset, registers12.object1);
                                    } else {
                                        unsafe3.putObject(t2, fieldOffset, Internal.mergeMessage(unsafe3.getObject(t2, fieldOffset), registers12.object1));
                                    }
                                    currentPresenceField2 |= presenceMask;
                                    pos3 = pos;
                                    tag4 = tag7;
                                    registers3 = registers12;
                                    oldNumber = position8;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    i2 = endGroup;
                                    break;
                                }
                            case 10:
                                wireType = wireType3;
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                currentPresenceFieldOffset2 = position4;
                                bArr = data;
                                if (wireType == 2) {
                                    position5 = ArrayDecoders.decodeBytes(bArr, currentPresenceFieldOffset2, registers);
                                    unsafe3.putObject(t2, fieldOffset, registers.object1);
                                    currentPresenceField2 |= presenceMask;
                                    wireType2 = limit;
                                    pos3 = pos;
                                    tag4 = tag7;
                                    registers3 = registers;
                                    oldNumber = position8;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    i2 = endGroup;
                                    break;
                                } else {
                                    unsafe2 = unsafe3;
                                    tag3 = tag7;
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                }
                            case 12:
                                wireType = wireType3;
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                currentPresenceFieldOffset2 = position4;
                                bArr = data;
                                if (wireType != 0) {
                                    unsafe2 = unsafe3;
                                    tag3 = tag7;
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                } else {
                                    position5 = ArrayDecoders.decodeVarint32(bArr, currentPresenceFieldOffset2, registers3);
                                    int enumValue = registers3.int1;
                                    Internal.EnumVerifier enumVerifier = messageSchema2.getEnumFieldVerifier(pos);
                                    if (enumVerifier == null || enumVerifier.isInRange(enumValue)) {
                                        unsafe3.putInt(t2, fieldOffset, enumValue);
                                        currentPresenceField2 |= presenceMask;
                                        wireType2 = limit;
                                        i2 = endGroup;
                                        pos3 = pos;
                                        tag4 = tag7;
                                        oldNumber = position8;
                                        currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                        registers3 = registers;
                                        break;
                                    } else {
                                        getMutableUnknownFields(message).storeField(tag7, Long.valueOf(enumValue));
                                        wireType2 = limit;
                                        i2 = endGroup;
                                        pos3 = pos;
                                        tag4 = tag7;
                                        oldNumber = position8;
                                        currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                        registers3 = registers;
                                        break;
                                    }
                                }
                                break;
                            case 15:
                                wireType = wireType3;
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                bArr = data;
                                currentPresenceFieldOffset2 = position4;
                                if (wireType == 0) {
                                    position5 = ArrayDecoders.decodeVarint32(bArr, currentPresenceFieldOffset2, registers3);
                                    unsafe3.putInt(t2, fieldOffset, CodedInputStream.decodeZigZag32(registers3.int1));
                                    currentPresenceField2 |= presenceMask;
                                    wireType2 = limit;
                                    i2 = endGroup;
                                    pos3 = pos;
                                    oldNumber = position8;
                                    tag4 = tag7;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    break;
                                } else {
                                    tag3 = tag7;
                                    unsafe2 = unsafe3;
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                }
                            case 16:
                                wireType = wireType3;
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                currentPresenceFieldOffset2 = position4;
                                if (wireType == 0) {
                                    bArr = data;
                                    int position11 = ArrayDecoders.decodeVarint64(bArr, currentPresenceFieldOffset2, registers3);
                                    unsafe3.putLong(message, fieldOffset, CodedInputStream.decodeZigZag64(registers3.long1));
                                    currentPresenceField2 |= presenceMask;
                                    wireType2 = limit;
                                    i2 = endGroup;
                                    pos3 = pos;
                                    position5 = position11;
                                    oldNumber = position8;
                                    tag4 = tag7;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    break;
                                } else {
                                    tag3 = tag7;
                                    unsafe2 = unsafe3;
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                }
                            case 17:
                                if (wireType3 != 3) {
                                    wireType = wireType3;
                                    currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                    currentPresenceFieldOffset2 = position4;
                                    tag3 = tag7;
                                    unsafe2 = unsafe3;
                                    pos2 = pos;
                                    currentPresenceField = currentPresenceField2;
                                    position3 = currentPresenceFieldOffset2;
                                    unsafe = unsafe2;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                    tag2 = tag3;
                                    break;
                                } else {
                                    int endTag = (position8 << 3) | 4;
                                    int currentPresenceFieldOffset4 = currentPresenceFieldOffset3;
                                    position5 = ArrayDecoders.decodeGroupField(messageSchema2.getMessageFieldSchema(pos), data, position4, limit, endTag, registers);
                                    if ((currentPresenceField2 & presenceMask) == 0) {
                                        unsafe3.putObject(t2, fieldOffset, registers3.object1);
                                    } else {
                                        unsafe3.putObject(t2, fieldOffset, Internal.mergeMessage(unsafe3.getObject(t2, fieldOffset), registers3.object1));
                                    }
                                    currentPresenceField2 |= presenceMask;
                                    bArr = data;
                                    wireType2 = limit;
                                    i2 = endGroup;
                                    pos3 = pos;
                                    oldNumber = position8;
                                    tag4 = tag7;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset4;
                                    break;
                                }
                            default:
                                wireType = wireType3;
                                currentPresenceFieldOffset = currentPresenceFieldOffset3;
                                tag3 = tag7;
                                currentPresenceFieldOffset2 = position4;
                                unsafe2 = unsafe3;
                                pos2 = pos;
                                currentPresenceField = currentPresenceField2;
                                position3 = currentPresenceFieldOffset2;
                                unsafe = unsafe2;
                                currentPresenceFieldOffset3 = currentPresenceFieldOffset;
                                tag2 = tag3;
                                break;
                        }
                    } else {
                        int currentPresenceFieldOffset5 = currentPresenceFieldOffset3;
                        int currentPresenceFieldOffset6 = position2;
                        Unsafe unsafe4 = unsafe3;
                        bArr = data;
                        ArrayDecoders.Registers registers13 = registers3;
                        if (fieldType == 27) {
                            if (wireType3 != 2) {
                                pos2 = pos;
                                currentPresenceField = currentPresenceField2;
                                position3 = currentPresenceFieldOffset6;
                                unsafe = unsafe4;
                                tag2 = tag7;
                                currentPresenceFieldOffset3 = currentPresenceFieldOffset5;
                            } else {
                                Internal.ProtobufList<?> list = (Internal.ProtobufList) unsafe4.getObject(t2, fieldOffset);
                                if (!list.isModifiable()) {
                                    int size = list.size();
                                    list = list.mutableCopyWithCapacity(size == 0 ? 10 : size * 2);
                                    unsafe4.putObject(t2, fieldOffset, list);
                                }
                                position5 = ArrayDecoders.decodeMessageList(messageSchema2.getMessageFieldSchema(pos), tag7, data, currentPresenceFieldOffset6, limit, list, registers);
                                wireType2 = limit;
                                registers3 = registers13;
                                oldNumber = position8;
                                unsafe3 = unsafe4;
                                pos3 = pos;
                                currentPresenceFieldOffset3 = currentPresenceFieldOffset5;
                                tag4 = tag7;
                                i2 = endGroup;
                            }
                        } else {
                            pos2 = pos;
                            if (fieldType <= 49) {
                                currentPresenceField = currentPresenceField2;
                                unsafe = unsafe4;
                                tag2 = tag7;
                                position5 = parseRepeatedField(message, data, currentPresenceFieldOffset6, limit, tag7, position8, wireType3, pos2, typeAndOffset, fieldType, fieldOffset, registers);
                                if (position5 != currentPresenceFieldOffset6) {
                                    messageSchema2 = this;
                                    t2 = message;
                                    bArr = data;
                                    wireType2 = limit;
                                    i2 = endGroup;
                                    registers3 = registers;
                                    oldNumber = position8;
                                    tag4 = tag2;
                                    pos3 = pos2;
                                    currentPresenceField2 = currentPresenceField;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset5;
                                    unsafe3 = unsafe;
                                } else {
                                    position3 = position5;
                                    currentPresenceFieldOffset3 = currentPresenceFieldOffset5;
                                }
                            } else {
                                currentPresenceField = currentPresenceField2;
                                position3 = currentPresenceFieldOffset6;
                                unsafe = unsafe4;
                                tag2 = tag7;
                                if (fieldType == 50) {
                                    if (wireType3 == 2) {
                                        position5 = parseMapField(message, data, position3, limit, pos2, fieldOffset, registers);
                                        if (position5 != position3) {
                                            messageSchema2 = this;
                                            t2 = message;
                                            bArr = data;
                                            wireType2 = limit;
                                            i2 = endGroup;
                                            registers3 = registers;
                                            oldNumber = position8;
                                            tag4 = tag2;
                                            pos3 = pos2;
                                            currentPresenceField2 = currentPresenceField;
                                            currentPresenceFieldOffset3 = currentPresenceFieldOffset5;
                                            unsafe3 = unsafe;
                                        } else {
                                            position3 = position5;
                                            currentPresenceFieldOffset3 = currentPresenceFieldOffset5;
                                        }
                                    } else {
                                        currentPresenceFieldOffset3 = currentPresenceFieldOffset5;
                                    }
                                } else {
                                    position5 = parseOneofField(message, data, position3, limit, tag2, position8, wireType3, typeAndOffset, fieldType, fieldOffset, pos2, registers);
                                    if (position5 == position3) {
                                        position3 = position5;
                                        currentPresenceFieldOffset3 = currentPresenceFieldOffset5;
                                    } else {
                                        messageSchema2 = this;
                                        t2 = message;
                                        bArr = data;
                                        wireType2 = limit;
                                        i2 = endGroup;
                                        registers3 = registers;
                                        oldNumber = position8;
                                        tag4 = tag2;
                                        pos3 = pos2;
                                        currentPresenceField2 = currentPresenceField;
                                        currentPresenceFieldOffset3 = currentPresenceFieldOffset5;
                                        unsafe3 = unsafe;
                                    }
                                }
                            }
                        }
                    }
                }
                i = endGroup;
                int tag8 = tag2;
                if (tag8 == i && i != 0) {
                    messageSchema = this;
                    tag4 = tag8;
                    currentPresenceField2 = currentPresenceField;
                    position5 = position3;
                } else {
                    if (!this.hasExtensions) {
                        registers2 = registers;
                    } else {
                        registers2 = registers;
                        if (registers2.extensionRegistry != ExtensionRegistryLite.getEmptyRegistry()) {
                            position5 = ArrayDecoders.decodeExtensionOrUnknownField(tag8, data, position3, limit, message, this.defaultInstance, this.unknownFieldSchema, registers);
                            t2 = message;
                            wireType2 = limit;
                            tag4 = tag8;
                            messageSchema2 = this;
                            oldNumber = position8;
                            pos3 = pos2;
                            currentPresenceField2 = currentPresenceField;
                            unsafe3 = unsafe;
                            i2 = i;
                            registers3 = registers2;
                            bArr = data;
                        }
                    }
                    position5 = ArrayDecoders.decodeUnknownField(tag8, data, position3, limit, getMutableUnknownFields(message), registers);
                    t2 = message;
                    wireType2 = limit;
                    tag4 = tag8;
                    messageSchema2 = this;
                    oldNumber = position8;
                    pos3 = pos2;
                    currentPresenceField2 = currentPresenceField;
                    unsafe3 = unsafe;
                    i2 = i;
                    registers3 = registers2;
                    bArr = data;
                }
            } else {
                unsafe = unsafe3;
                i = i2;
                messageSchema = messageSchema2;
            }
        }
        if (currentPresenceFieldOffset3 == -1) {
            t = message;
        } else {
            t = message;
            unsafe.putInt(t, currentPresenceFieldOffset3, currentPresenceField2);
        }
        UnknownFieldSetLite unknownFields = null;
        for (int i3 = messageSchema.checkInitializedCount; i3 < messageSchema.repeatedFieldOffsetStart; i3++) {
            unknownFields = (UnknownFieldSetLite) messageSchema.filterMapUnknownEnumValues(t, messageSchema.intArray[i3], unknownFields, messageSchema.unknownFieldSchema);
        }
        if (unknownFields != null) {
            messageSchema.unknownFieldSchema.setBuilderToMessage(t, unknownFields);
        }
        if (i == 0) {
            if (position5 != limit) {
                throw InvalidProtocolBufferException.parseFailure();
            }
        } else if (position5 > limit || tag4 != i) {
            throw InvalidProtocolBufferException.parseFailure();
        }
        return position5;
    }

    /* JADX WARN: Multi-variable type inference failed */
    private int parseProto3Message(T message, byte[] data, int position, int limit, ArrayDecoders.Registers registers) throws IOException {
        int tag;
        int position2;
        int pos;
        int pos2;
        Unsafe unsafe;
        int wireType;
        int position3;
        MessageSchema<T> messageSchema = this;
        T t = message;
        byte[] bArr = data;
        int i = limit;
        ArrayDecoders.Registers registers2 = registers;
        Unsafe unsafe2 = UNSAFE;
        int pos3 = 0;
        int oldNumber = -1;
        int position4 = position;
        while (position4 < i) {
            int position5 = position4 + 1;
            int i2 = bArr[position4];
            if (i2 >= 0) {
                tag = i2;
                position2 = position5;
            } else {
                int position6 = ArrayDecoders.decodeVarint32(i2, bArr, position5, registers2);
                tag = registers2.int1;
                position2 = position6;
            }
            int number = tag >>> 3;
            int wireType2 = tag & 7;
            if (number > oldNumber) {
                pos = messageSchema.positionForFieldNumber(number, pos3 / 3);
            } else {
                int pos4 = messageSchema.positionForFieldNumber(number);
                pos = pos4;
            }
            if (pos == -1) {
                pos2 = 0;
                unsafe = unsafe2;
            } else {
                int typeAndOffset = messageSchema.buffer[pos + 1];
                int fieldType = type(typeAndOffset);
                long fieldOffset = offset(typeAndOffset);
                if (fieldType <= 17) {
                    switch (fieldType) {
                        case 0:
                            Unsafe unsafe3 = unsafe2;
                            if (wireType2 != 1) {
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe3;
                                pos2 = pos;
                                position2 = position3;
                                break;
                            } else {
                                UnsafeUtil.putDouble(t, fieldOffset, ArrayDecoders.decodeDouble(bArr, position2));
                                position4 = position2 + 8;
                                pos3 = pos;
                                oldNumber = number;
                                unsafe2 = unsafe3;
                                continue;
                            }
                        case 1:
                            Unsafe unsafe4 = unsafe2;
                            if (wireType2 != 5) {
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe4;
                                pos2 = pos;
                                position2 = position3;
                                break;
                            } else {
                                UnsafeUtil.putFloat(t, fieldOffset, ArrayDecoders.decodeFloat(bArr, position2));
                                position4 = position2 + 4;
                                pos3 = pos;
                                oldNumber = number;
                                unsafe2 = unsafe4;
                                continue;
                            }
                        case 2:
                        case 3:
                            if (wireType2 != 0) {
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe2;
                                pos2 = pos;
                                position2 = position3;
                                break;
                            } else {
                                int position7 = ArrayDecoders.decodeVarint64(bArr, position2, registers2);
                                unsafe2.putLong(message, fieldOffset, registers2.long1);
                                pos3 = pos;
                                position4 = position7;
                                oldNumber = number;
                                unsafe2 = unsafe2;
                                continue;
                            }
                        case 4:
                        case 11:
                            if (wireType2 != 0) {
                                pos2 = pos;
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe2;
                                position2 = position3;
                                break;
                            } else {
                                position4 = ArrayDecoders.decodeVarint32(bArr, position2, registers2);
                                unsafe2.putInt(t, fieldOffset, registers2.int1);
                                pos3 = pos;
                                oldNumber = number;
                                continue;
                            }
                        case 5:
                        case 14:
                            if (wireType2 != 1) {
                                pos2 = pos;
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe2;
                                position2 = position3;
                                break;
                            } else {
                                unsafe2.putLong(message, fieldOffset, ArrayDecoders.decodeFixed64(bArr, position2));
                                position4 = position2 + 8;
                                pos3 = pos;
                                oldNumber = number;
                                continue;
                            }
                        case 6:
                        case 13:
                            if (wireType2 != 5) {
                                pos2 = pos;
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe2;
                                position2 = position3;
                                break;
                            } else {
                                unsafe2.putInt(t, fieldOffset, ArrayDecoders.decodeFixed32(bArr, position2));
                                position4 = position2 + 4;
                                pos3 = pos;
                                oldNumber = number;
                                continue;
                            }
                        case 7:
                            if (wireType2 != 0) {
                                pos2 = pos;
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe2;
                                position2 = position3;
                                break;
                            } else {
                                int position8 = ArrayDecoders.decodeVarint64(bArr, position2, registers2);
                                UnsafeUtil.putBoolean(t, fieldOffset, registers2.long1 != 0);
                                position4 = position8;
                                pos3 = pos;
                                oldNumber = number;
                                continue;
                            }
                        case 8:
                            if (wireType2 != 2) {
                                pos2 = pos;
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe2;
                                position2 = position3;
                                break;
                            } else {
                                if ((536870912 & typeAndOffset) == 0) {
                                    position4 = ArrayDecoders.decodeString(bArr, position2, registers2);
                                } else {
                                    position4 = ArrayDecoders.decodeStringRequireUtf8(bArr, position2, registers2);
                                }
                                unsafe2.putObject(t, fieldOffset, registers2.object1);
                                pos3 = pos;
                                oldNumber = number;
                                continue;
                            }
                        case 9:
                            if (wireType2 != 2) {
                                pos2 = pos;
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe2;
                                position2 = position3;
                                break;
                            } else {
                                position4 = ArrayDecoders.decodeMessageField(messageSchema.getMessageFieldSchema(pos), bArr, position2, i, registers2);
                                Object oldValue = unsafe2.getObject(t, fieldOffset);
                                if (oldValue == null) {
                                    unsafe2.putObject(t, fieldOffset, registers2.object1);
                                } else {
                                    unsafe2.putObject(t, fieldOffset, Internal.mergeMessage(oldValue, registers2.object1));
                                }
                                pos3 = pos;
                                oldNumber = number;
                                continue;
                            }
                        case 10:
                            if (wireType2 != 2) {
                                pos2 = pos;
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe2;
                                position2 = position3;
                                break;
                            } else {
                                position4 = ArrayDecoders.decodeBytes(bArr, position2, registers2);
                                unsafe2.putObject(t, fieldOffset, registers2.object1);
                                pos3 = pos;
                                oldNumber = number;
                                continue;
                            }
                        case 12:
                            if (wireType2 != 0) {
                                pos2 = pos;
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe2;
                                position2 = position3;
                                break;
                            } else {
                                position4 = ArrayDecoders.decodeVarint32(bArr, position2, registers2);
                                unsafe2.putInt(t, fieldOffset, registers2.int1);
                                pos3 = pos;
                                oldNumber = number;
                                continue;
                            }
                        case 15:
                            if (wireType2 != 0) {
                                pos2 = pos;
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe2;
                                position2 = position3;
                                break;
                            } else {
                                position4 = ArrayDecoders.decodeVarint32(bArr, position2, registers2);
                                unsafe2.putInt(t, fieldOffset, CodedInputStream.decodeZigZag32(registers2.int1));
                                pos3 = pos;
                                oldNumber = number;
                                continue;
                            }
                        case 16:
                            if (wireType2 != 0) {
                                pos2 = pos;
                                wireType = wireType2;
                                position3 = position2;
                                unsafe = unsafe2;
                                position2 = position3;
                                break;
                            } else {
                                int position9 = ArrayDecoders.decodeVarint64(bArr, position2, registers2);
                                unsafe2.putLong(message, fieldOffset, CodedInputStream.decodeZigZag64(registers2.long1));
                                pos3 = pos;
                                position4 = position9;
                                oldNumber = number;
                                continue;
                            }
                        default:
                            wireType = wireType2;
                            position3 = position2;
                            unsafe = unsafe2;
                            pos2 = pos;
                            position2 = position3;
                            break;
                    }
                } else {
                    Unsafe unsafe5 = unsafe2;
                    if (fieldType == 27) {
                        if (wireType2 == 2) {
                            Internal.ProtobufList<?> list = (Internal.ProtobufList) unsafe5.getObject(t, fieldOffset);
                            if (!list.isModifiable()) {
                                int size = list.size();
                                list = list.mutableCopyWithCapacity(size == 0 ? 10 : size * 2);
                                unsafe5.putObject(t, fieldOffset, list);
                            }
                            position4 = ArrayDecoders.decodeMessageList(messageSchema.getMessageFieldSchema(pos), tag, data, position2, limit, list, registers);
                            messageSchema = this;
                            oldNumber = number;
                            unsafe2 = unsafe5;
                            pos3 = pos;
                        } else {
                            pos2 = pos;
                            wireType = wireType2;
                            unsafe = unsafe5;
                            position3 = position2;
                            position2 = position3;
                        }
                    } else {
                        pos2 = pos;
                        if (fieldType > 49) {
                            unsafe = unsafe5;
                            position3 = position2;
                            if (fieldType == 50) {
                                wireType = wireType2;
                                if (wireType == 2) {
                                    position4 = parseMapField(message, data, position3, limit, pos2, fieldOffset, registers);
                                    if (position4 != position3) {
                                        messageSchema = this;
                                        t = message;
                                        bArr = data;
                                        i = limit;
                                        registers2 = registers;
                                        oldNumber = number;
                                        pos3 = pos2;
                                        unsafe2 = unsafe;
                                    } else {
                                        position2 = position4;
                                    }
                                } else {
                                    position2 = position3;
                                }
                            } else {
                                position4 = parseOneofField(message, data, position3, limit, tag, number, wireType2, typeAndOffset, fieldType, fieldOffset, pos2, registers);
                                if (position4 == position3) {
                                    position2 = position4;
                                } else {
                                    messageSchema = this;
                                    t = message;
                                    bArr = data;
                                    i = limit;
                                    registers2 = registers;
                                    oldNumber = number;
                                    pos3 = pos2;
                                    unsafe2 = unsafe;
                                }
                            }
                        } else {
                            int oldPosition = position2;
                            unsafe = unsafe5;
                            position4 = parseRepeatedField(message, data, position2, limit, tag, number, wireType2, pos2, typeAndOffset, fieldType, fieldOffset, registers);
                            if (position4 != oldPosition) {
                                messageSchema = this;
                                t = message;
                                bArr = data;
                                i = limit;
                                registers2 = registers;
                                oldNumber = number;
                                pos3 = pos2;
                                unsafe2 = unsafe;
                            } else {
                                position2 = position4;
                            }
                        }
                    }
                }
            }
            position4 = ArrayDecoders.decodeUnknownField(tag, data, position2, limit, getMutableUnknownFields(message), registers);
            messageSchema = this;
            t = message;
            bArr = data;
            i = limit;
            registers2 = registers;
            oldNumber = number;
            pos3 = pos2;
            unsafe2 = unsafe;
        }
        if (position4 != limit) {
            throw InvalidProtocolBufferException.parseFailure();
        }
        return position4;
    }

    @Override // com.android.framework.protobuf.Schema
    public void mergeFrom(T message, byte[] data, int position, int limit, ArrayDecoders.Registers registers) throws IOException {
        if (this.proto3) {
            parseProto3Message(message, data, position, limit, registers);
        } else {
            parseProto2Message(message, data, position, limit, 0, registers);
        }
    }

    @Override // com.android.framework.protobuf.Schema
    public void makeImmutable(T message) {
        for (int i = this.checkInitializedCount; i < this.repeatedFieldOffsetStart; i++) {
            long offset = offset(typeAndOffsetAt(this.intArray[i]));
            Object mapField = UnsafeUtil.getObject(message, offset);
            if (mapField != null) {
                UnsafeUtil.putObject(message, offset, this.mapFieldSchema.toImmutable(mapField));
            }
        }
        int length = this.intArray.length;
        for (int i2 = this.repeatedFieldOffsetStart; i2 < length; i2++) {
            this.listFieldSchema.makeImmutableListAt(message, this.intArray[i2]);
        }
        this.unknownFieldSchema.makeImmutable(message);
        if (this.hasExtensions) {
            this.extensionSchema.makeImmutable(message);
        }
    }

    private final <K, V> void mergeMap(Object message, int pos, Object mapDefaultEntry, ExtensionRegistryLite extensionRegistry, Reader reader) throws IOException {
        long offset = offset(typeAndOffsetAt(pos));
        Object mapField = UnsafeUtil.getObject(message, offset);
        if (mapField == null) {
            mapField = this.mapFieldSchema.newMapField(mapDefaultEntry);
            UnsafeUtil.putObject(message, offset, mapField);
        } else if (this.mapFieldSchema.isImmutable(mapField)) {
            mapField = this.mapFieldSchema.newMapField(mapDefaultEntry);
            this.mapFieldSchema.mergeFrom(mapField, mapField);
            UnsafeUtil.putObject(message, offset, mapField);
        }
        reader.readMap(this.mapFieldSchema.forMutableMapData(mapField), this.mapFieldSchema.forMapMetadata(mapDefaultEntry), extensionRegistry);
    }

    /* JADX DEBUG: Type inference failed for r15v0. Raw type applied. Possible types: java.util.Map<?, ?>, java.util.Map<K, V> */
    private final <UT, UB> UB filterMapUnknownEnumValues(Object message, int pos, UB unknownFields, UnknownFieldSchema<UT, UB> unknownFieldSchema) {
        int fieldNumber = numberAt(pos);
        long offset = offset(typeAndOffsetAt(pos));
        Object mapField = UnsafeUtil.getObject(message, offset);
        if (mapField == null) {
            return unknownFields;
        }
        Internal.EnumVerifier enumVerifier = getEnumFieldVerifier(pos);
        if (enumVerifier == null) {
            return unknownFields;
        }
        return (UB) filterUnknownEnumMap(pos, fieldNumber, this.mapFieldSchema.forMutableMapData(mapField), enumVerifier, unknownFields, unknownFieldSchema);
    }

    private final <K, V, UT, UB> UB filterUnknownEnumMap(int pos, int number, Map<K, V> mapData, Internal.EnumVerifier enumVerifier, UB unknownFields, UnknownFieldSchema<UT, UB> unknownFieldSchema) {
        MapEntryLite.Metadata<?, ?> forMapMetadata = this.mapFieldSchema.forMapMetadata(getMapFieldDefaultEntry(pos));
        Iterator<Map.Entry<K, V>> it = mapData.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> entry = it.next();
            if (!enumVerifier.isInRange(((Integer) entry.getValue()).intValue())) {
                if (unknownFields == null) {
                    UB unknownFields2 = unknownFieldSchema.newBuilder();
                    unknownFields = unknownFields2;
                }
                int entrySize = MapEntryLite.computeSerializedSize(forMapMetadata, entry.getKey(), entry.getValue());
                ByteString.CodedBuilder codedBuilder = ByteString.newCodedBuilder(entrySize);
                CodedOutputStream codedOutput = codedBuilder.getCodedOutput();
                try {
                    MapEntryLite.writeTo(codedOutput, forMapMetadata, entry.getKey(), entry.getValue());
                    unknownFieldSchema.addLengthDelimited(unknownFields, number, codedBuilder.build());
                    it.remove();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return unknownFields;
    }

    @Override // com.android.framework.protobuf.Schema
    public final boolean isInitialized(T message) {
        int currentPresenceFieldOffset = -1;
        int currentPresenceField = 0;
        for (int i = 0; i < this.checkInitializedCount; i++) {
            int pos = this.intArray[i];
            int number = numberAt(pos);
            int typeAndOffset = typeAndOffsetAt(pos);
            int presenceMask = 0;
            if (!this.proto3) {
                int presenceMaskAndOffset = this.buffer[pos + 2];
                int presenceFieldOffset = 1048575 & presenceMaskAndOffset;
                presenceMask = 1 << (presenceMaskAndOffset >>> 20);
                if (presenceFieldOffset != currentPresenceFieldOffset) {
                    currentPresenceFieldOffset = presenceFieldOffset;
                    currentPresenceField = UNSAFE.getInt(message, presenceFieldOffset);
                }
            }
            if (isRequired(typeAndOffset) && !isFieldPresent(message, pos, currentPresenceField, presenceMask)) {
                return false;
            }
            switch (type(typeAndOffset)) {
                case 9:
                case 17:
                    if (isFieldPresent(message, pos, currentPresenceField, presenceMask) && !isInitialized(message, typeAndOffset, getMessageFieldSchema(pos))) {
                        return false;
                    }
                    break;
                case 27:
                case 49:
                    if (isListInitialized(message, typeAndOffset, pos)) {
                        break;
                    } else {
                        return false;
                    }
                case 50:
                    if (isMapInitialized(message, typeAndOffset, pos)) {
                        break;
                    } else {
                        return false;
                    }
                case 60:
                case 68:
                    if (isOneofPresent(message, number, pos) && !isInitialized(message, typeAndOffset, getMessageFieldSchema(pos))) {
                        return false;
                    }
                    break;
            }
        }
        return !this.hasExtensions || this.extensionSchema.getExtensions(message).isInitialized();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: com.android.framework.protobuf.Schema */
    /* JADX WARN: Multi-variable type inference failed */
    private static boolean isInitialized(Object message, int typeAndOffset, Schema schema) {
        Object nested = UnsafeUtil.getObject(message, offset(typeAndOffset));
        return schema.isInitialized(nested);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v1, resolved type: com.android.framework.protobuf.Schema */
    /* JADX WARN: Multi-variable type inference failed */
    private <N> boolean isListInitialized(Object message, int typeAndOffset, int pos) {
        List<N> list = (List) UnsafeUtil.getObject(message, offset(typeAndOffset));
        if (list.isEmpty()) {
            return true;
        }
        Schema schema = getMessageFieldSchema(pos);
        for (int i = 0; i < list.size(); i++) {
            N nested = list.get(i);
            if (!schema.isInitialized(nested)) {
                return false;
            }
        }
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r4v2 */
    /* JADX WARN: Type inference failed for: r4v4, types: [com.android.framework.protobuf.Schema] */
    /* JADX WARN: Type inference failed for: r4v6 */
    private boolean isMapInitialized(T message, int typeAndOffset, int pos) {
        Map<?, ?> map = this.mapFieldSchema.forMapData(UnsafeUtil.getObject(message, offset(typeAndOffset)));
        if (map.isEmpty()) {
            return true;
        }
        Object mapDefaultEntry = getMapFieldDefaultEntry(pos);
        MapEntryLite.Metadata<?, ?> metadata = this.mapFieldSchema.forMapMetadata(mapDefaultEntry);
        if (metadata.valueType.getJavaType() != WireFormat.JavaType.MESSAGE) {
            return true;
        }
        Schema<T> schema = 0;
        for (Object nested : map.values()) {
            if (schema == null) {
                schema = Protobuf.getInstance().schemaFor((Class) nested.getClass());
            }
            boolean isInitialized = schema.isInitialized(nested);
            schema = schema;
            if (!isInitialized) {
                return false;
            }
        }
        return true;
    }

    private void writeString(int fieldNumber, Object value, Writer writer) throws IOException {
        if (value instanceof String) {
            writer.writeString(fieldNumber, (String) value);
        } else {
            writer.writeBytes(fieldNumber, (ByteString) value);
        }
    }

    private void readString(Object message, int typeAndOffset, Reader reader) throws IOException {
        if (isEnforceUtf8(typeAndOffset)) {
            UnsafeUtil.putObject(message, offset(typeAndOffset), reader.readStringRequireUtf8());
        } else if (this.lite) {
            UnsafeUtil.putObject(message, offset(typeAndOffset), reader.readString());
        } else {
            UnsafeUtil.putObject(message, offset(typeAndOffset), reader.readBytes());
        }
    }

    private void readStringList(Object message, int typeAndOffset, Reader reader) throws IOException {
        if (isEnforceUtf8(typeAndOffset)) {
            reader.readStringListRequireUtf8(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
        } else {
            reader.readStringList(this.listFieldSchema.mutableListAt(message, offset(typeAndOffset)));
        }
    }

    private <E> void readMessageList(Object message, int typeAndOffset, Reader reader, Schema<E> schema, ExtensionRegistryLite extensionRegistry) throws IOException {
        long offset = offset(typeAndOffset);
        reader.readMessageList(this.listFieldSchema.mutableListAt(message, offset), schema, extensionRegistry);
    }

    private <E> void readGroupList(Object message, long offset, Reader reader, Schema<E> schema, ExtensionRegistryLite extensionRegistry) throws IOException {
        reader.readGroupList(this.listFieldSchema.mutableListAt(message, offset), schema, extensionRegistry);
    }

    private int numberAt(int pos) {
        return this.buffer[pos];
    }

    private int typeAndOffsetAt(int pos) {
        return this.buffer[pos + 1];
    }

    private int presenceMaskAndOffsetAt(int pos) {
        return this.buffer[pos + 2];
    }

    private static int type(int value) {
        return (FIELD_TYPE_MASK & value) >>> 20;
    }

    private static boolean isRequired(int value) {
        return (268435456 & value) != 0;
    }

    private static boolean isEnforceUtf8(int value) {
        return (536870912 & value) != 0;
    }

    private static long offset(int value) {
        return 1048575 & value;
    }

    private static <T> double doubleAt(T message, long offset) {
        return UnsafeUtil.getDouble(message, offset);
    }

    private static <T> float floatAt(T message, long offset) {
        return UnsafeUtil.getFloat(message, offset);
    }

    private static <T> int intAt(T message, long offset) {
        return UnsafeUtil.getInt(message, offset);
    }

    private static <T> long longAt(T message, long offset) {
        return UnsafeUtil.getLong(message, offset);
    }

    private static <T> boolean booleanAt(T message, long offset) {
        return UnsafeUtil.getBoolean(message, offset);
    }

    private static <T> double oneofDoubleAt(T message, long offset) {
        return ((Double) UnsafeUtil.getObject(message, offset)).doubleValue();
    }

    private static <T> float oneofFloatAt(T message, long offset) {
        return ((Float) UnsafeUtil.getObject(message, offset)).floatValue();
    }

    private static <T> int oneofIntAt(T message, long offset) {
        return ((Integer) UnsafeUtil.getObject(message, offset)).intValue();
    }

    private static <T> long oneofLongAt(T message, long offset) {
        return ((Long) UnsafeUtil.getObject(message, offset)).longValue();
    }

    private static <T> boolean oneofBooleanAt(T message, long offset) {
        return ((Boolean) UnsafeUtil.getObject(message, offset)).booleanValue();
    }

    private boolean arePresentForEquals(T message, T other, int pos) {
        return isFieldPresent(message, pos) == isFieldPresent(other, pos);
    }

    private boolean isFieldPresent(T message, int pos, int presenceField, int presenceMask) {
        if (this.proto3) {
            return isFieldPresent(message, pos);
        }
        return (presenceField & presenceMask) != 0;
    }

    private boolean isFieldPresent(T message, int pos) {
        if (this.proto3) {
            int typeAndOffset = typeAndOffsetAt(pos);
            long offset = offset(typeAndOffset);
            switch (type(typeAndOffset)) {
                case 0:
                    return UnsafeUtil.getDouble(message, offset) != 0.0d;
                case 1:
                    return UnsafeUtil.getFloat(message, offset) != 0.0f;
                case 2:
                    return UnsafeUtil.getLong(message, offset) != 0;
                case 3:
                    return UnsafeUtil.getLong(message, offset) != 0;
                case 4:
                    return UnsafeUtil.getInt(message, offset) != 0;
                case 5:
                    return UnsafeUtil.getLong(message, offset) != 0;
                case 6:
                    return UnsafeUtil.getInt(message, offset) != 0;
                case 7:
                    return UnsafeUtil.getBoolean(message, offset);
                case 8:
                    Object value = UnsafeUtil.getObject(message, offset);
                    if (value instanceof String) {
                        return true ^ ((String) value).isEmpty();
                    }
                    if (value instanceof ByteString) {
                        return true ^ ByteString.EMPTY.equals(value);
                    }
                    throw new IllegalArgumentException();
                case 9:
                    return UnsafeUtil.getObject(message, offset) != null;
                case 10:
                    return !ByteString.EMPTY.equals(UnsafeUtil.getObject(message, offset));
                case 11:
                    return UnsafeUtil.getInt(message, offset) != 0;
                case 12:
                    return UnsafeUtil.getInt(message, offset) != 0;
                case 13:
                    return UnsafeUtil.getInt(message, offset) != 0;
                case 14:
                    return UnsafeUtil.getLong(message, offset) != 0;
                case 15:
                    return UnsafeUtil.getInt(message, offset) != 0;
                case 16:
                    return UnsafeUtil.getLong(message, offset) != 0;
                case 17:
                    return UnsafeUtil.getObject(message, offset) != null;
                default:
                    throw new IllegalArgumentException();
            }
        }
        int presenceMaskAndOffset = presenceMaskAndOffsetAt(pos);
        int presenceMask = 1 << (presenceMaskAndOffset >>> 20);
        return (UnsafeUtil.getInt(message, (long) (1048575 & presenceMaskAndOffset)) & presenceMask) != 0;
    }

    private void setFieldPresent(T message, int pos) {
        if (this.proto3) {
            return;
        }
        int presenceMaskAndOffset = presenceMaskAndOffsetAt(pos);
        int presenceMask = 1 << (presenceMaskAndOffset >>> 20);
        long presenceFieldOffset = 1048575 & presenceMaskAndOffset;
        UnsafeUtil.putInt(message, presenceFieldOffset, UnsafeUtil.getInt(message, presenceFieldOffset) | presenceMask);
    }

    private boolean isOneofPresent(T message, int fieldNumber, int pos) {
        int presenceMaskAndOffset = presenceMaskAndOffsetAt(pos);
        return UnsafeUtil.getInt(message, (long) (1048575 & presenceMaskAndOffset)) == fieldNumber;
    }

    private boolean isOneofCaseEqual(T message, T other, int pos) {
        int presenceMaskAndOffset = presenceMaskAndOffsetAt(pos);
        return UnsafeUtil.getInt(message, (long) (presenceMaskAndOffset & 1048575)) == UnsafeUtil.getInt(other, (long) (1048575 & presenceMaskAndOffset));
    }

    private void setOneofPresent(T message, int fieldNumber, int pos) {
        int presenceMaskAndOffset = presenceMaskAndOffsetAt(pos);
        UnsafeUtil.putInt(message, 1048575 & presenceMaskAndOffset, fieldNumber);
    }

    private int positionForFieldNumber(int number) {
        if (number >= this.minFieldNumber && number <= this.maxFieldNumber) {
            return slowPositionForFieldNumber(number, 0);
        }
        return -1;
    }

    private int positionForFieldNumber(int number, int min) {
        if (number >= this.minFieldNumber && number <= this.maxFieldNumber) {
            return slowPositionForFieldNumber(number, min);
        }
        return -1;
    }

    private int slowPositionForFieldNumber(int number, int min) {
        int max = (this.buffer.length / 3) - 1;
        while (min <= max) {
            int mid = (max + min) >>> 1;
            int pos = mid * 3;
            int midFieldNumber = numberAt(pos);
            if (number == midFieldNumber) {
                return pos;
            }
            if (number < midFieldNumber) {
                max = mid - 1;
            } else {
                min = mid + 1;
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSchemaSize() {
        return this.buffer.length * 3;
    }
}
