package com.android.server.autofill;

import android.app.assist.AssistStructure;
import android.content.ComponentName;
import android.metrics.LogMaker;
import android.service.autofill.Dataset;
import android.service.autofill.InternalSanitizer;
import android.service.autofill.SaveInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.view.WindowManager;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import com.android.internal.util.ArrayUtils;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
/* loaded from: classes.dex */
public final class Helper {
    private static final String TAG = "AutofillHelper";
    public static boolean sDebug = false;
    public static boolean sVerbose = false;
    public static Boolean sFullScreenMode = null;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface ViewNodeFilter {
        boolean matches(AssistStructure.ViewNode viewNode);
    }

    private Helper() {
        throw new UnsupportedOperationException("contains static members only");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AutofillId[] toArray(ArraySet<AutofillId> set) {
        if (set == null) {
            return null;
        }
        AutofillId[] array = new AutofillId[set.size()];
        for (int i = 0; i < set.size(); i++) {
            array[i] = set.valueAt(i);
        }
        return array;
    }

    public static String paramsToString(WindowManager.LayoutParams params) {
        StringBuilder builder = new StringBuilder(25);
        params.dumpDimensions(builder);
        return builder.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ArrayMap<AutofillId, AutofillValue> getFields(Dataset dataset) {
        ArrayList<AutofillId> ids = dataset.getFieldIds();
        ArrayList<AutofillValue> values = dataset.getFieldValues();
        int size = ids == null ? 0 : ids.size();
        ArrayMap<AutofillId, AutofillValue> fields = new ArrayMap<>(size);
        for (int i = 0; i < size; i++) {
            fields.put(ids.get(i), values.get(i));
        }
        return fields;
    }

    private static LogMaker newLogMaker(int category, String servicePackageName, int sessionId, boolean compatMode) {
        LogMaker log = new LogMaker(category).addTaggedData(908, servicePackageName).addTaggedData(1456, Integer.toString(sessionId));
        if (compatMode) {
            log.addTaggedData(1414, 1);
        }
        return log;
    }

    public static LogMaker newLogMaker(int category, String packageName, String servicePackageName, int sessionId, boolean compatMode) {
        return newLogMaker(category, servicePackageName, sessionId, compatMode).setPackageName(packageName);
    }

    public static LogMaker newLogMaker(int category, ComponentName componentName, String servicePackageName, int sessionId, boolean compatMode) {
        ComponentName sanitizedComponentName = new ComponentName(componentName.getPackageName(), "");
        return newLogMaker(category, servicePackageName, sessionId, compatMode).setComponentName(sanitizedComponentName);
    }

    public static void printlnRedactedText(PrintWriter pw, CharSequence text) {
        if (text == null) {
            pw.println("null");
            return;
        }
        pw.print(text.length());
        pw.println("_chars");
    }

    public static AssistStructure.ViewNode findViewNodeByAutofillId(AssistStructure structure, final AutofillId autofillId) {
        return findViewNode(structure, new ViewNodeFilter() { // from class: com.android.server.autofill.Helper$$ExternalSyntheticLambda1
            @Override // com.android.server.autofill.Helper.ViewNodeFilter
            public final boolean matches(AssistStructure.ViewNode viewNode) {
                boolean equals;
                equals = autofillId.equals(viewNode.getAutofillId());
                return equals;
            }
        });
    }

    private static AssistStructure.ViewNode findViewNode(AssistStructure structure, ViewNodeFilter filter) {
        ArrayDeque<AssistStructure.ViewNode> nodesToProcess = new ArrayDeque<>();
        int numWindowNodes = structure.getWindowNodeCount();
        for (int i = 0; i < numWindowNodes; i++) {
            nodesToProcess.add(structure.getWindowNodeAt(i).getRootViewNode());
        }
        while (!nodesToProcess.isEmpty()) {
            AssistStructure.ViewNode node = nodesToProcess.removeFirst();
            if (filter.matches(node)) {
                return node;
            }
            for (int i2 = 0; i2 < node.getChildCount(); i2++) {
                nodesToProcess.addLast(node.getChildAt(i2));
            }
        }
        return null;
    }

    public static AssistStructure.ViewNode sanitizeUrlBar(AssistStructure structure, final String[] urlBarIds) {
        AssistStructure.ViewNode urlBarNode = findViewNode(structure, new ViewNodeFilter() { // from class: com.android.server.autofill.Helper$$ExternalSyntheticLambda0
            @Override // com.android.server.autofill.Helper.ViewNodeFilter
            public final boolean matches(AssistStructure.ViewNode viewNode) {
                boolean contains;
                contains = ArrayUtils.contains(urlBarIds, viewNode.getIdEntry());
                return contains;
            }
        });
        if (urlBarNode != null) {
            String domain = urlBarNode.getText().toString();
            if (domain.isEmpty()) {
                if (sDebug) {
                    Slog.d(TAG, "sanitizeUrlBar(): empty on " + urlBarNode.getIdEntry());
                    return null;
                }
                return null;
            }
            urlBarNode.setWebDomain(domain);
            if (sDebug) {
                Slog.d(TAG, "sanitizeUrlBar(): id=" + urlBarNode.getIdEntry() + ", domain=" + urlBarNode.getWebDomain());
            }
        }
        return urlBarNode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getNumericValue(LogMaker log, int tag) {
        Object value = log.getTaggedData(tag);
        if (!(value instanceof Number)) {
            return 0;
        }
        return ((Number) value).intValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ArrayList<AutofillId> getAutofillIds(AssistStructure structure, boolean autofillableOnly) {
        ArrayList<AutofillId> ids = new ArrayList<>();
        int size = structure.getWindowNodeCount();
        for (int i = 0; i < size; i++) {
            AssistStructure.WindowNode node = structure.getWindowNodeAt(i);
            addAutofillableIds(node.getRootViewNode(), ids, autofillableOnly);
        }
        return ids;
    }

    private static void addAutofillableIds(AssistStructure.ViewNode node, ArrayList<AutofillId> ids, boolean autofillableOnly) {
        if (!autofillableOnly || node.getAutofillType() != 0) {
            ids.add(node.getAutofillId());
        }
        int size = node.getChildCount();
        for (int i = 0; i < size; i++) {
            AssistStructure.ViewNode child = node.getChildAt(i);
            addAutofillableIds(child, ids, autofillableOnly);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ArrayMap<AutofillId, InternalSanitizer> createSanitizers(SaveInfo saveInfo) {
        InternalSanitizer[] sanitizerKeys;
        if (saveInfo == null || (sanitizerKeys = saveInfo.getSanitizerKeys()) == null) {
            return null;
        }
        int size = sanitizerKeys.length;
        ArrayMap<AutofillId, InternalSanitizer> sanitizers = new ArrayMap<>(size);
        if (sDebug) {
            Slog.d(TAG, "Service provided " + size + " sanitizers");
        }
        AutofillId[][] sanitizerValues = saveInfo.getSanitizerValues();
        for (int i = 0; i < size; i++) {
            InternalSanitizer sanitizer = sanitizerKeys[i];
            AutofillId[] ids = sanitizerValues[i];
            if (sDebug) {
                Slog.d(TAG, "sanitizer #" + i + " (" + sanitizer + ") for ids " + Arrays.toString(ids));
            }
            for (AutofillId id : ids) {
                sanitizers.put(id, sanitizer);
            }
        }
        return sanitizers;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean containsCharsInOrder(String s1, String s2) {
        char[] charArray;
        int prevIndex = -1;
        for (char ch : s2.toCharArray()) {
            int index = TextUtils.indexOf(s1, ch, prevIndex + 1);
            if (index == -1) {
                return false;
            }
            prevIndex = index;
        }
        return true;
    }
}
