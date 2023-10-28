package android.app.timezone;
/* loaded from: classes.dex */
final class Utils {
    private Utils() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int validateVersion(String type, int version) {
        if (version < 0 || version > 999) {
            throw new IllegalArgumentException("Invalid " + type + " version=" + version);
        }
        return version;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String validateRulesVersion(String type, String rulesVersion) {
        validateNotNull(type, rulesVersion);
        if (rulesVersion.isEmpty()) {
            throw new IllegalArgumentException(type + " must not be empty");
        }
        return rulesVersion;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static <T> T validateNotNull(String type, T object) {
        if (object == null) {
            throw new NullPointerException(type + " == null");
        }
        return object;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static <T> T validateConditionalNull(boolean requireNotNull, String type, T object) {
        if (requireNotNull) {
            return (T) validateNotNull(type, object);
        }
        return (T) validateNull(type, object);
    }

    static <T> T validateNull(String type, T object) {
        if (object != null) {
            throw new IllegalArgumentException(type + " != null");
        }
        return null;
    }
}
