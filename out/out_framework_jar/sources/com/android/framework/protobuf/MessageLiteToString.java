package com.android.framework.protobuf;

import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.telecom.Logging.Session;
import com.android.framework.protobuf.GeneratedMessageLite;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes4.dex */
public final class MessageLiteToString {
    private static final String BUILDER_LIST_SUFFIX = "OrBuilderList";
    private static final String BYTES_SUFFIX = "Bytes";
    private static final String LIST_SUFFIX = "List";
    private static final String MAP_SUFFIX = "Map";

    MessageLiteToString() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String toString(MessageLite messageLite, String commentString) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("# ").append(commentString);
        reflectivePrintWithIndent(messageLite, buffer, 0);
        return buffer.toString();
    }

    private static void reflectivePrintWithIndent(MessageLite messageLite, StringBuilder buffer, int indent) {
        Method[] declaredMethods;
        Map<String, Method> nameToNoArgMethod;
        boolean hasValue;
        Map<String, Method> nameToNoArgMethod2 = new HashMap<>();
        Map<String, Method> nameToMethod = new HashMap<>();
        Set<String> getters = new TreeSet<>();
        int i = 0;
        for (Method method : messageLite.getClass().getDeclaredMethods()) {
            nameToMethod.put(method.getName(), method);
            if (method.getParameterTypes().length == 0) {
                nameToNoArgMethod2.put(method.getName(), method);
                if (method.getName().startsWith("get")) {
                    getters.add(method.getName());
                }
            }
        }
        for (String getter : getters) {
            String suffix = getter.replaceFirst("get", "");
            if (suffix.endsWith(LIST_SUFFIX) && !suffix.endsWith(BUILDER_LIST_SUFFIX) && !suffix.equals(LIST_SUFFIX)) {
                String camelCase = suffix.substring(i, 1).toLowerCase() + suffix.substring(1, suffix.length() - LIST_SUFFIX.length());
                Method listMethod = nameToNoArgMethod2.get(getter);
                if (listMethod != null && listMethod.getReturnType().equals(List.class)) {
                    printField(buffer, indent, camelCaseToSnakeCase(camelCase), GeneratedMessageLite.invokeOrDie(listMethod, messageLite, new Object[i]));
                }
            }
            if (suffix.endsWith(MAP_SUFFIX) && !suffix.equals(MAP_SUFFIX)) {
                String camelCase2 = suffix.substring(i, 1).toLowerCase() + suffix.substring(1, suffix.length() - MAP_SUFFIX.length());
                Method mapMethod = nameToNoArgMethod2.get(getter);
                if (mapMethod != null && mapMethod.getReturnType().equals(Map.class) && !mapMethod.isAnnotationPresent(Deprecated.class) && Modifier.isPublic(mapMethod.getModifiers())) {
                    printField(buffer, indent, camelCaseToSnakeCase(camelCase2), GeneratedMessageLite.invokeOrDie(mapMethod, messageLite, new Object[i]));
                }
            }
            Method setter = nameToMethod.get("set" + suffix);
            if (setter != null && (!suffix.endsWith(BYTES_SUFFIX) || !nameToNoArgMethod2.containsKey("get" + suffix.substring(i, suffix.length() - BYTES_SUFFIX.length())))) {
                String camelCase3 = suffix.substring(i, 1).toLowerCase() + suffix.substring(1);
                Method getMethod = nameToNoArgMethod2.get("get" + suffix);
                Method hasMethod = nameToNoArgMethod2.get("has" + suffix);
                if (getMethod != null) {
                    Object value = GeneratedMessageLite.invokeOrDie(getMethod, messageLite, new Object[i]);
                    if (hasMethod == null) {
                        nameToNoArgMethod = nameToNoArgMethod2;
                        hasValue = !isDefaultValue(value) ? 1 : i;
                    } else {
                        nameToNoArgMethod = nameToNoArgMethod2;
                        hasValue = ((Boolean) GeneratedMessageLite.invokeOrDie(hasMethod, messageLite, new Object[i])).booleanValue();
                    }
                    if (hasValue == 0) {
                        nameToNoArgMethod2 = nameToNoArgMethod;
                        i = 0;
                    } else {
                        printField(buffer, indent, camelCaseToSnakeCase(camelCase3), value);
                        nameToNoArgMethod2 = nameToNoArgMethod;
                        i = 0;
                    }
                } else {
                    i = 0;
                }
            }
        }
        if (messageLite instanceof GeneratedMessageLite.ExtendableMessage) {
            Iterator<Map.Entry<GeneratedMessageLite.ExtensionDescriptor, Object>> iter = ((GeneratedMessageLite.ExtendableMessage) messageLite).extensions.iterator();
            while (iter.hasNext()) {
                Map.Entry<GeneratedMessageLite.ExtensionDescriptor, Object> entry = iter.next();
                printField(buffer, indent, NavigationBarInflaterView.SIZE_MOD_START + entry.getKey().getNumber() + NavigationBarInflaterView.SIZE_MOD_END, entry.getValue());
            }
        }
        if (((GeneratedMessageLite) messageLite).unknownFields != null) {
            ((GeneratedMessageLite) messageLite).unknownFields.printWithIndent(buffer, indent);
        }
    }

    private static boolean isDefaultValue(Object o) {
        if (o instanceof Boolean) {
            return !((Boolean) o).booleanValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue() == 0;
        } else if (o instanceof Float) {
            return ((Float) o).floatValue() == 0.0f;
        } else if (o instanceof Double) {
            return ((Double) o).doubleValue() == 0.0d;
        } else if (o instanceof String) {
            return o.equals("");
        } else {
            if (o instanceof ByteString) {
                return o.equals(ByteString.EMPTY);
            }
            return o instanceof MessageLite ? o == ((MessageLite) o).getDefaultInstanceForType() : (o instanceof Enum) && ((Enum) o).ordinal() == 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final void printField(StringBuilder buffer, int indent, String name, Object object) {
        if (object instanceof List) {
            List<?> list = (List) object;
            Iterator<?> it = list.iterator();
            while (it.hasNext()) {
                printField(buffer, indent, name, it.next());
            }
        } else if (object instanceof Map) {
            Map<?, ?> map = (Map) object;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                printField(buffer, indent, name, entry);
            }
        } else {
            buffer.append('\n');
            for (int i = 0; i < indent; i++) {
                buffer.append(' ');
            }
            buffer.append(name);
            if (object instanceof String) {
                buffer.append(": \"").append(TextFormatEscaper.escapeText((String) object)).append('\"');
            } else if (object instanceof ByteString) {
                buffer.append(": \"").append(TextFormatEscaper.escapeBytes((ByteString) object)).append('\"');
            } else if (object instanceof GeneratedMessageLite) {
                buffer.append(" {");
                reflectivePrintWithIndent((GeneratedMessageLite) object, buffer, indent + 2);
                buffer.append("\n");
                for (int i2 = 0; i2 < indent; i2++) {
                    buffer.append(' ');
                }
                buffer.append("}");
            } else if (object instanceof Map.Entry) {
                buffer.append(" {");
                Map.Entry<?, ?> entry2 = (Map.Entry) object;
                printField(buffer, indent + 2, "key", entry2.getKey());
                printField(buffer, indent + 2, "value", entry2.getValue());
                buffer.append("\n");
                for (int i3 = 0; i3 < indent; i3++) {
                    buffer.append(' ');
                }
                buffer.append("}");
            } else {
                buffer.append(": ").append(object.toString());
            }
        }
    }

    private static final String camelCaseToSnakeCase(String camelCase) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char ch = camelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                builder.append(Session.SESSION_SEPARATION_CHAR_CHILD);
            }
            builder.append(Character.toLowerCase(ch));
        }
        return builder.toString();
    }
}
