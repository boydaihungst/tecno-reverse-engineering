package com.android.server.soundtrigger_middleware;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
/* loaded from: classes2.dex */
class ObjectPrinter {
    public static final int kDefaultMaxCollectionLength = 16;

    ObjectPrinter() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String print(Object obj, int maxCollectionLength) {
        StringBuilder builder = new StringBuilder();
        print(builder, obj, maxCollectionLength);
        return builder.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void print(StringBuilder builder, Object obj, int maxCollectionLength) {
        try {
            if (obj == null) {
                builder.append("null");
            } else if (obj instanceof Boolean) {
                builder.append(obj);
            } else if (obj instanceof Number) {
                builder.append(obj);
            } else if (obj instanceof Character) {
                builder.append('\'');
                builder.append(obj);
                builder.append('\'');
            } else if (obj instanceof String) {
                builder.append('\"');
                builder.append(obj.toString());
                builder.append('\"');
            } else {
                Class cls = obj.getClass();
                if (Collection.class.isAssignableFrom(cls)) {
                    Collection collection = (Collection) obj;
                    builder.append("[ ");
                    int length = collection.size();
                    boolean isLong = false;
                    int i = 0;
                    Iterator it = collection.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        Object child = it.next();
                        if (i > 0) {
                            builder.append(", ");
                        }
                        if (i >= maxCollectionLength) {
                            isLong = true;
                            break;
                        } else {
                            print(builder, child, maxCollectionLength);
                            i++;
                        }
                    }
                    if (isLong) {
                        builder.append("... (+");
                        builder.append(length - maxCollectionLength);
                        builder.append(" entries)");
                    }
                    builder.append(" ]");
                } else if (Map.class.isAssignableFrom(cls)) {
                    Map<?, ?> map = (Map) obj;
                    builder.append("< ");
                    int length2 = map.size();
                    boolean isLong2 = false;
                    int i2 = 0;
                    Iterator<Map.Entry<?, ?>> it2 = map.entrySet().iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        }
                        Map.Entry<?, ?> child2 = it2.next();
                        if (i2 > 0) {
                            builder.append(", ");
                        }
                        if (i2 >= maxCollectionLength) {
                            isLong2 = true;
                            break;
                        }
                        print(builder, child2.getKey(), maxCollectionLength);
                        builder.append(": ");
                        print(builder, child2.getValue(), maxCollectionLength);
                        i2++;
                    }
                    if (isLong2) {
                        builder.append("... (+");
                        builder.append(length2 - maxCollectionLength);
                        builder.append(" entries)");
                    }
                    builder.append(" >");
                } else if (cls.isArray()) {
                    builder.append("[ ");
                    int length3 = Array.getLength(obj);
                    boolean isLong3 = false;
                    int i3 = 0;
                    while (true) {
                        if (i3 >= length3) {
                            break;
                        }
                        if (i3 > 0) {
                            builder.append(", ");
                        }
                        if (i3 >= maxCollectionLength) {
                            isLong3 = true;
                            break;
                        } else {
                            print(builder, Array.get(obj, i3), maxCollectionLength);
                            i3++;
                        }
                    }
                    if (isLong3) {
                        builder.append("... (+");
                        builder.append(length3 - maxCollectionLength);
                        builder.append(" entries)");
                    }
                    builder.append(" ]");
                } else {
                    builder.append(obj);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
