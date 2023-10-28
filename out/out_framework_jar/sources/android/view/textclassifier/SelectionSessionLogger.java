package android.view.textclassifier;
/* loaded from: classes3.dex */
public final class SelectionSessionLogger {
    private static final String CLASSIFIER_ID = "androidtc";

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isPlatformLocalTextClassifierSmartSelection(String signature) {
        return "androidtc".equals(SignatureParser.getClassifierId(signature));
    }

    /* loaded from: classes3.dex */
    public static final class SignatureParser {
        static String getClassifierId(String signature) {
            int end;
            if (signature == null || (end = signature.indexOf("|")) < 0) {
                return "";
            }
            return signature.substring(0, end);
        }
    }
}
