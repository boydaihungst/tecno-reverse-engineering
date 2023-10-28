package com.android.server;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.FastImmutableArraySet;
import android.util.LogPrinter;
import android.util.MutableInt;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.FastPrintWriter;
import com.android.server.pm.Computer;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.snapshot.PackageDataSnapshot;
import com.android.server.voiceinteraction.DatabaseHelper;
import defpackage.CompanionAppsPermissions;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
/* loaded from: classes.dex */
public abstract class IntentResolver<F, R> {
    private static final boolean DEBUG = false;
    private static final String TAG = "IntentResolver";
    private static final boolean localLOGV = false;
    private static final boolean localVerificationLOGV = false;
    private static final Comparator mResolvePrioritySorter = new Comparator() { // from class: com.android.server.IntentResolver.1
        @Override // java.util.Comparator
        public int compare(Object o1, Object o2) {
            int q1 = ((IntentFilter) o1).getPriority();
            int q2 = ((IntentFilter) o2).getPriority();
            if (q1 > q2) {
                return -1;
            }
            return q1 < q2 ? 1 : 0;
        }
    };
    protected final ArraySet<F> mFilters = new ArraySet<>();
    private final ArrayMap<String, F[]> mTypeToFilter = new ArrayMap<>();
    private final ArrayMap<String, F[]> mBaseTypeToFilter = new ArrayMap<>();
    private final ArrayMap<String, F[]> mWildTypeToFilter = new ArrayMap<>();
    private final ArrayMap<String, F[]> mSchemeToFilter = new ArrayMap<>();
    private final ArrayMap<String, F[]> mActionToFilter = new ArrayMap<>();
    private final ArrayMap<String, F[]> mTypedActionToFilter = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: protected */
    public abstract IntentFilter getIntentFilter(F f);

    /* JADX INFO: Access modifiers changed from: protected */
    public abstract boolean isPackageForFilter(String str, F f);

    protected abstract F[] newArray(int i);

    public void addFilter(PackageDataSnapshot snapshot, F f) {
        IntentFilter intentFilter = getIntentFilter(f);
        this.mFilters.add(f);
        int numS = register_intent_filter(f, intentFilter.schemesIterator(), this.mSchemeToFilter, "      Scheme: ");
        int numT = register_mime_types(f, "      Type: ");
        if (numS == 0 && numT == 0) {
            register_intent_filter(f, intentFilter.actionsIterator(), this.mActionToFilter, "      Action: ");
        }
        if (numT != 0) {
            register_intent_filter(f, intentFilter.actionsIterator(), this.mTypedActionToFilter, "      TypedAction: ");
        }
    }

    public static boolean filterEquals(IntentFilter f1, IntentFilter f2) {
        int s1 = f1.countActions();
        int s2 = f2.countActions();
        if (s1 != s2) {
            return false;
        }
        for (int i = 0; i < s1; i++) {
            if (!f2.hasAction(f1.getAction(i))) {
                return false;
            }
        }
        int s12 = f1.countCategories();
        int s22 = f2.countCategories();
        if (s12 != s22) {
            return false;
        }
        for (int i2 = 0; i2 < s12; i2++) {
            if (!f2.hasCategory(f1.getCategory(i2))) {
                return false;
            }
        }
        int s13 = f1.countDataTypes();
        int s23 = f2.countDataTypes();
        if (s13 != s23) {
            return false;
        }
        for (int i3 = 0; i3 < s13; i3++) {
            if (!f2.hasExactDataType(f1.getDataType(i3))) {
                return false;
            }
        }
        int s14 = f1.countDataSchemes();
        int s24 = f2.countDataSchemes();
        if (s14 != s24) {
            return false;
        }
        for (int i4 = 0; i4 < s14; i4++) {
            if (!f2.hasDataScheme(f1.getDataScheme(i4))) {
                return false;
            }
        }
        int s15 = f1.countDataAuthorities();
        int s25 = f2.countDataAuthorities();
        if (s15 != s25) {
            return false;
        }
        for (int i5 = 0; i5 < s15; i5++) {
            if (!f2.hasDataAuthority(f1.getDataAuthority(i5))) {
                return false;
            }
        }
        int s16 = f1.countDataPaths();
        int s26 = f2.countDataPaths();
        if (s16 != s26) {
            return false;
        }
        for (int i6 = 0; i6 < s16; i6++) {
            if (!f2.hasDataPath(f1.getDataPath(i6))) {
                return false;
            }
        }
        int s17 = f1.countDataSchemeSpecificParts();
        int s27 = f2.countDataSchemeSpecificParts();
        if (s17 != s27) {
            return false;
        }
        for (int i7 = 0; i7 < s17; i7++) {
            if (!f2.hasDataSchemeSpecificPart(f1.getDataSchemeSpecificPart(i7))) {
                return false;
            }
        }
        return true;
    }

    public static boolean intentMatchesFilter(IntentFilter filter, Intent intent, String resolvedType) {
        String reason;
        boolean debug = (intent.getFlags() & 8) != 0;
        Printer logPrinter = debug ? new LogPrinter(2, TAG, 3) : null;
        if (debug) {
            Slog.v(TAG, "Intent: " + intent);
            Slog.v(TAG, "Matching against filter: " + filter);
            filter.dump(logPrinter, "  ");
        }
        int match = filter.match(intent.getAction(), resolvedType, intent.getScheme(), intent.getData(), intent.getCategories(), TAG);
        if (match >= 0) {
            if (debug) {
                Slog.v(TAG, "Filter matched!  match=0x" + Integer.toHexString(match));
            }
            return true;
        }
        if (debug) {
            switch (match) {
                case -4:
                    reason = "category";
                    break;
                case -3:
                    reason = "action";
                    break;
                case -2:
                    reason = "data";
                    break;
                case -1:
                    reason = DatabaseHelper.SoundModelContract.KEY_TYPE;
                    break;
                default:
                    reason = "unknown reason";
                    break;
            }
            Slog.v(TAG, "Filter did not match: " + reason);
        }
        return false;
    }

    private ArrayList<F> collectFilters(F[] array, IntentFilter matching) {
        F cur;
        ArrayList<F> res = null;
        if (array != null) {
            for (int i = 0; i < array.length && (cur = array[i]) != null; i++) {
                if (filterEquals(getIntentFilter(cur), matching)) {
                    if (res == null) {
                        res = new ArrayList<>();
                    }
                    res.add(cur);
                }
            }
        }
        return res;
    }

    public ArrayList<F> findFilters(IntentFilter matching) {
        if (matching.countDataSchemes() == 1) {
            return collectFilters(this.mSchemeToFilter.get(matching.getDataScheme(0)), matching);
        }
        if (matching.countDataTypes() != 0 && matching.countActions() == 1) {
            return collectFilters(this.mTypedActionToFilter.get(matching.getAction(0)), matching);
        }
        if (matching.countDataTypes() == 0 && matching.countDataSchemes() == 0 && matching.countActions() == 1) {
            return collectFilters(this.mActionToFilter.get(matching.getAction(0)), matching);
        }
        ArrayList<F> res = null;
        Iterator<F> it = this.mFilters.iterator();
        while (it.hasNext()) {
            F cur = it.next();
            if (filterEquals(getIntentFilter(cur), matching)) {
                if (res == null) {
                    res = new ArrayList<>();
                }
                res.add(cur);
            }
        }
        return res;
    }

    public void removeFilter(F f) {
        removeFilterInternal(f);
        this.mFilters.remove(f);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void removeFilterInternal(F f) {
        IntentFilter intentFilter = getIntentFilter(f);
        int numS = unregister_intent_filter(f, intentFilter.schemesIterator(), this.mSchemeToFilter, "      Scheme: ");
        int numT = unregister_mime_types(f, "      Type: ");
        if (numS == 0 && numT == 0) {
            unregister_intent_filter(f, intentFilter.actionsIterator(), this.mActionToFilter, "      Action: ");
        }
        if (numT != 0) {
            unregister_intent_filter(f, intentFilter.actionsIterator(), this.mTypedActionToFilter, "      TypedAction: ");
        }
    }

    boolean dumpMap(PrintWriter out, String titlePrefix, String title, String prefix, ArrayMap<String, F[]> map, String packageName, boolean printFilter, boolean collapseDuplicates) {
        String str;
        ArrayMap<Object, MutableInt> found;
        F filter;
        String str2;
        boolean printedSomething;
        Printer printer;
        boolean filter2;
        Printer printer2;
        F filter3;
        boolean printedHeader;
        boolean printedSomething2;
        String str3;
        IntentResolver<F, R> intentResolver = this;
        PrintWriter printWriter = out;
        ArrayMap<String, F[]> arrayMap = map;
        String str4 = "  ";
        String eprefix = prefix + "  ";
        String fprefix = prefix + "    ";
        ArrayMap<Object, MutableInt> found2 = new ArrayMap<>();
        int mapi = 0;
        Printer printer3 = null;
        boolean printedSomething3 = false;
        String title2 = title;
        while (mapi < map.size()) {
            F[] a = arrayMap.valueAt(mapi);
            int N = a.length;
            boolean printedHeader2 = false;
            if (!collapseDuplicates || printFilter) {
                str = str4;
                found = found2;
                int i = 0;
                title2 = title2;
                printer3 = printer3;
                boolean printedHeader3 = false;
                printedSomething3 = printedSomething3;
                while (i < N) {
                    F filter4 = a[i];
                    if (filter4 != null) {
                        if (packageName != null) {
                            filter = filter4;
                            if (!intentResolver.isPackageForFilter(packageName, filter)) {
                                str2 = str;
                                i++;
                                intentResolver = this;
                                arrayMap = map;
                                str = str2;
                                printWriter = out;
                            }
                        } else {
                            filter = filter4;
                        }
                        if (title2 != null) {
                            out.print(titlePrefix);
                            printWriter.println(title2);
                            title2 = null;
                        }
                        if (!printedHeader3) {
                            printWriter.print(eprefix);
                            printWriter.print(arrayMap.keyAt(mapi));
                            printWriter.println(":");
                            printedHeader3 = true;
                        }
                        intentResolver.dumpFilter(printWriter, fprefix, filter);
                        if (!printFilter) {
                            str2 = str;
                            printedSomething3 = true;
                        } else {
                            if (printer3 == null) {
                                printer3 = new PrintWriterPrinter(printWriter);
                            }
                            str2 = str;
                            intentResolver.getIntentFilter(filter).dump(printer3, fprefix + str2);
                            printedSomething3 = true;
                        }
                        i++;
                        intentResolver = this;
                        arrayMap = map;
                        str = str2;
                        printWriter = out;
                    }
                }
            } else {
                found2.clear();
                String title3 = title2;
                int i2 = 0;
                while (true) {
                    if (i2 < N) {
                        F filter5 = a[i2];
                        if (filter5 == null) {
                            str = str4;
                            printedSomething = printedSomething3;
                            printer = printer3;
                            filter2 = printedHeader2;
                            break;
                        }
                        if (packageName != null) {
                            printer2 = printer3;
                            filter3 = filter5;
                            if (!intentResolver.isPackageForFilter(packageName, filter3)) {
                                str3 = str4;
                                printedSomething2 = printedSomething3;
                                printedHeader = printedHeader2;
                                i2++;
                                printer3 = printer2;
                                printedHeader2 = printedHeader;
                                printedSomething3 = printedSomething2;
                                str4 = str3;
                            }
                        } else {
                            printer2 = printer3;
                            filter3 = filter5;
                        }
                        printedHeader = printedHeader2;
                        Object label = intentResolver.filterToLabel(filter3);
                        int index = found2.indexOfKey(label);
                        printedSomething2 = printedSomething3;
                        if (index < 0) {
                            str3 = str4;
                            found2.put(label, new MutableInt(1));
                        } else {
                            str3 = str4;
                            found2.valueAt(index).value++;
                        }
                        i2++;
                        printer3 = printer2;
                        printedHeader2 = printedHeader;
                        printedSomething3 = printedSomething2;
                        str4 = str3;
                    } else {
                        str = str4;
                        printedSomething = printedSomething3;
                        printer = printer3;
                        filter2 = printedHeader2;
                        break;
                    }
                }
                int i3 = 0;
                title2 = title3;
                boolean printedHeader4 = filter2;
                printedSomething3 = printedSomething;
                while (i3 < found2.size()) {
                    if (title2 != null) {
                        out.print(titlePrefix);
                        printWriter.println(title2);
                        title2 = null;
                    }
                    if (!printedHeader4) {
                        printWriter.print(eprefix);
                        printWriter.print(arrayMap.keyAt(mapi));
                        printWriter.println(":");
                        printedHeader4 = true;
                    }
                    printedSomething3 = true;
                    intentResolver.dumpFilterLabel(printWriter, fprefix, found2.keyAt(i3), found2.valueAt(i3).value);
                    i3++;
                    found2 = found2;
                }
                found = found2;
                printer3 = printer;
            }
            mapi++;
            intentResolver = this;
            arrayMap = map;
            str4 = str;
            found2 = found;
            printWriter = out;
        }
        return printedSomething3;
    }

    void writeProtoMap(ProtoOutputStream proto, long fieldId, ArrayMap<String, F[]> map) {
        F[] valueAt;
        int N = map.size();
        for (int mapi = 0; mapi < N; mapi++) {
            long token = proto.start(fieldId);
            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, map.keyAt(mapi));
            for (F f : map.valueAt(mapi)) {
                if (f != null) {
                    proto.write(2237677961218L, f.toString());
                }
            }
            proto.end(token);
        }
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        writeProtoMap(proto, CompanionAppsPermissions.APP_PERMISSIONS, this.mTypeToFilter);
        writeProtoMap(proto, 2246267895810L, this.mBaseTypeToFilter);
        writeProtoMap(proto, 2246267895811L, this.mWildTypeToFilter);
        writeProtoMap(proto, 2246267895812L, this.mSchemeToFilter);
        writeProtoMap(proto, 2246267895813L, this.mActionToFilter);
        writeProtoMap(proto, 2246267895814L, this.mTypedActionToFilter);
        proto.end(token);
    }

    public boolean dump(PrintWriter out, String title, String prefix, String packageName, boolean printFilter, boolean collapseDuplicates) {
        String innerPrefix = prefix + "  ";
        String sepPrefix = "\n" + prefix;
        String curPrefix = title + "\n" + prefix;
        if (dumpMap(out, curPrefix, "Full MIME Types:", innerPrefix, this.mTypeToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "Base MIME Types:", innerPrefix, this.mBaseTypeToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "Wild MIME Types:", innerPrefix, this.mWildTypeToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "Schemes:", innerPrefix, this.mSchemeToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "Non-Data Actions:", innerPrefix, this.mActionToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "MIME Typed Actions:", innerPrefix, this.mTypedActionToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        return curPrefix == sepPrefix;
    }

    /* loaded from: classes.dex */
    private class IteratorWrapper implements Iterator<F> {
        private F mCur;
        private final Iterator<F> mI;

        IteratorWrapper(Iterator<F> it) {
            this.mI = it;
        }

        @Override // java.util.Iterator
        public boolean hasNext() {
            return this.mI.hasNext();
        }

        @Override // java.util.Iterator
        public F next() {
            F next = this.mI.next();
            this.mCur = next;
            return next;
        }

        @Override // java.util.Iterator
        public void remove() {
            F f = this.mCur;
            if (f != null) {
                IntentResolver.this.removeFilterInternal(f);
            }
            this.mI.remove();
        }
    }

    public Iterator<F> filterIterator() {
        return new IteratorWrapper(this.mFilters.iterator());
    }

    public Set<F> filterSet() {
        return Collections.unmodifiableSet(this.mFilters);
    }

    public List<R> queryIntentFromList(Computer computer, Intent intent, String resolvedType, boolean defaultOnly, ArrayList<F[]> listCut, int userId, long customFlags) {
        ArrayList<R> resultList = new ArrayList<>();
        boolean debug = (intent.getFlags() & 8) != 0;
        FastImmutableArraySet<String> categories = getFastIntentCategories(intent);
        String scheme = intent.getScheme();
        int i = 0;
        for (int N = listCut.size(); i < N; N = N) {
            buildResolveList(computer, intent, categories, debug, defaultOnly, resolvedType, scheme, listCut.get(i), resultList, userId, customFlags);
            i++;
        }
        filterResults(resultList);
        sortResults(resultList);
        return resultList;
    }

    public List<R> queryIntent(PackageDataSnapshot snapshot, Intent intent, String resolvedType, boolean defaultOnly, int userId) {
        return queryIntent(snapshot, intent, resolvedType, defaultOnly, userId, 0L);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Removed duplicated region for block: B:46:0x019c  */
    /* JADX WARN: Removed duplicated region for block: B:50:0x01c4  */
    /* JADX WARN: Removed duplicated region for block: B:57:0x01df  */
    /* JADX WARN: Removed duplicated region for block: B:60:0x0205  */
    /* JADX WARN: Removed duplicated region for block: B:61:0x0222  */
    /* JADX WARN: Removed duplicated region for block: B:63:0x0227  */
    /* JADX WARN: Removed duplicated region for block: B:65:0x0243  */
    /* JADX WARN: Removed duplicated region for block: B:67:0x025f  */
    /* JADX WARN: Removed duplicated region for block: B:70:0x0283  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public final List<R> queryIntent(PackageDataSnapshot snapshot, Intent intent, String resolvedType, boolean defaultOnly, int userId, long customFlags) {
        F[] firstTypeCut;
        F[] secondTypeCut;
        F[] thirdTypeCut;
        F[] firstTypeCut2;
        F[] schemeCut;
        F[] firstTypeCut3;
        ArrayList<R> finalList;
        String str;
        F[] secondTypeCut2;
        String scheme = intent.getScheme();
        ArrayList<R> finalList2 = new ArrayList<>();
        boolean debug = (intent.getFlags() & 8) != 0;
        if (debug) {
            Slog.v(TAG, "Resolving type=" + resolvedType + " scheme=" + scheme + " defaultOnly=" + defaultOnly + " userId=" + userId + " of " + intent);
        }
        if (resolvedType == null) {
            firstTypeCut = null;
            secondTypeCut = null;
            thirdTypeCut = null;
        } else {
            int slashpos = resolvedType.indexOf(47);
            if (slashpos <= 0) {
                firstTypeCut = null;
                secondTypeCut = null;
                thirdTypeCut = null;
            } else {
                String baseType = resolvedType.substring(0, slashpos);
                if (!baseType.equals("*")) {
                    if (resolvedType.length() != slashpos + 2 || resolvedType.charAt(slashpos + 1) != '*') {
                        firstTypeCut2 = this.mTypeToFilter.get(resolvedType);
                        if (debug) {
                            Slog.v(TAG, "First type cut: " + Arrays.toString(firstTypeCut2));
                        }
                        secondTypeCut2 = this.mWildTypeToFilter.get(baseType);
                        if (debug) {
                            Slog.v(TAG, "Second type cut: " + Arrays.toString(secondTypeCut2));
                        }
                    } else {
                        firstTypeCut2 = this.mBaseTypeToFilter.get(baseType);
                        if (debug) {
                            Slog.v(TAG, "First type cut: " + Arrays.toString(firstTypeCut2));
                        }
                        secondTypeCut2 = this.mWildTypeToFilter.get(baseType);
                        if (debug) {
                            Slog.v(TAG, "Second type cut: " + Arrays.toString(secondTypeCut2));
                        }
                    }
                    F[] secondTypeCut3 = secondTypeCut2;
                    F[] thirdTypeCut2 = this.mWildTypeToFilter.get("*");
                    if (debug) {
                        Slog.v(TAG, "Third type cut: " + Arrays.toString(thirdTypeCut2));
                    }
                    secondTypeCut = secondTypeCut3;
                    thirdTypeCut = thirdTypeCut2;
                } else {
                    firstTypeCut = null;
                    secondTypeCut = null;
                    thirdTypeCut = null;
                    if (intent.getAction() != null) {
                        firstTypeCut2 = this.mTypedActionToFilter.get(intent.getAction());
                        if (debug) {
                            Slog.v(TAG, "Typed Action list: " + Arrays.toString(firstTypeCut2));
                        }
                    }
                }
                if (scheme != null) {
                    schemeCut = null;
                } else {
                    F[] schemeCut2 = this.mSchemeToFilter.get(scheme);
                    F[] schemeCut3 = schemeCut2;
                    if (debug) {
                        Slog.v(TAG, "Scheme list: " + Arrays.toString(schemeCut3));
                    }
                    schemeCut = schemeCut3;
                }
                if (resolvedType == null && scheme == null && intent.getAction() != null) {
                    F[] firstTypeCut4 = this.mActionToFilter.get(intent.getAction());
                    firstTypeCut2 = firstTypeCut4;
                    if (debug) {
                        Slog.v(TAG, "Action list: " + Arrays.toString(firstTypeCut2));
                    }
                }
                firstTypeCut3 = firstTypeCut2;
                FastImmutableArraySet<String> categories = getFastIntentCategories(intent);
                Computer computer = (Computer) snapshot;
                if (firstTypeCut3 == null) {
                    finalList = finalList2;
                    str = TAG;
                    buildResolveList(computer, intent, categories, debug, defaultOnly, resolvedType, scheme, firstTypeCut3, finalList2, userId, customFlags);
                } else {
                    finalList = finalList2;
                    str = TAG;
                }
                if (secondTypeCut != null) {
                    buildResolveList(computer, intent, categories, debug, defaultOnly, resolvedType, scheme, secondTypeCut, finalList, userId, customFlags);
                }
                if (thirdTypeCut != null) {
                    buildResolveList(computer, intent, categories, debug, defaultOnly, resolvedType, scheme, thirdTypeCut, finalList, userId, customFlags);
                }
                if (schemeCut != null) {
                    buildResolveList(computer, intent, categories, debug, defaultOnly, resolvedType, scheme, schemeCut, finalList, userId, customFlags);
                }
                ArrayList<R> finalList3 = finalList;
                filterResults(finalList3);
                sortResults(finalList3);
                if (debug) {
                    Slog.v(str, "Final result list:");
                    for (int i = 0; i < finalList3.size(); i++) {
                        Slog.v(str, "  " + finalList3.get(i));
                    }
                }
                return finalList3;
            }
        }
        firstTypeCut2 = firstTypeCut;
        if (scheme != null) {
        }
        if (resolvedType == null) {
            F[] firstTypeCut42 = this.mActionToFilter.get(intent.getAction());
            firstTypeCut2 = firstTypeCut42;
            if (debug) {
            }
        }
        firstTypeCut3 = firstTypeCut2;
        FastImmutableArraySet<String> categories2 = getFastIntentCategories(intent);
        Computer computer2 = (Computer) snapshot;
        if (firstTypeCut3 == null) {
        }
        if (secondTypeCut != null) {
        }
        if (thirdTypeCut != null) {
        }
        if (schemeCut != null) {
        }
        ArrayList<R> finalList32 = finalList;
        filterResults(finalList32);
        sortResults(finalList32);
        if (debug) {
        }
        return finalList32;
    }

    protected boolean allowFilterResult(F filter, List<R> dest) {
        return true;
    }

    protected boolean isFilterStopped(PackageStateInternal packageState, int userId) {
        return false;
    }

    protected boolean isFilterVerified(F filter) {
        return getIntentFilter(filter).isVerified();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: F */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Multi-variable type inference failed */
    public R newResult(Computer computer, F filter, int match, int userId, long customFlags) {
        return filter;
    }

    protected void sortResults(List<R> results) {
        Collections.sort(results, mResolvePrioritySorter);
    }

    protected void filterResults(List<R> results) {
    }

    protected void dumpFilter(PrintWriter out, String prefix, F filter) {
        out.print(prefix);
        out.println(filter);
    }

    protected Object filterToLabel(F filter) {
        return "IntentFilter";
    }

    protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
        out.print(prefix);
        out.print(label);
        out.print(": ");
        out.println(count);
    }

    private final void addFilter(ArrayMap<String, F[]> map, String name, F filter) {
        F[] array = map.get(name);
        if (array == null) {
            F[] array2 = newArray(2);
            map.put(name, array2);
            array2[0] = filter;
            return;
        }
        int N = array.length;
        int i = N;
        while (i > 0 && array[i - 1] == null) {
            i--;
        }
        if (i >= N) {
            F[] newa = newArray((N * 3) / 2);
            System.arraycopy(array, 0, newa, 0, N);
            newa[N] = filter;
            map.put(name, newa);
            return;
        }
        array[i] = filter;
    }

    private final int register_mime_types(F filter, String prefix) {
        Iterator<String> i = getIntentFilter(filter).typesIterator();
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            String name = i.next();
            num++;
            String baseName = name;
            int slashpos = name.indexOf(47);
            if (slashpos > 0) {
                baseName = name.substring(0, slashpos).intern();
            } else {
                name = name + "/*";
            }
            addFilter(this.mTypeToFilter, name, filter);
            if (slashpos > 0) {
                addFilter(this.mBaseTypeToFilter, baseName, filter);
            } else {
                addFilter(this.mWildTypeToFilter, baseName, filter);
            }
        }
        return num;
    }

    private final int unregister_mime_types(F filter, String prefix) {
        Iterator<String> i = getIntentFilter(filter).typesIterator();
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            String name = i.next();
            num++;
            String baseName = name;
            int slashpos = name.indexOf(47);
            if (slashpos > 0) {
                baseName = name.substring(0, slashpos).intern();
            } else {
                name = name + "/*";
            }
            remove_all_objects(this.mTypeToFilter, name, filter);
            if (slashpos > 0) {
                remove_all_objects(this.mBaseTypeToFilter, baseName, filter);
            } else {
                remove_all_objects(this.mWildTypeToFilter, baseName, filter);
            }
        }
        return num;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final int register_intent_filter(F filter, Iterator<String> i, ArrayMap<String, F[]> dest, String prefix) {
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            String name = i.next();
            num++;
            addFilter(dest, name, filter);
        }
        return num;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final int unregister_intent_filter(F filter, Iterator<String> i, ArrayMap<String, F[]> dest, String prefix) {
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            String name = i.next();
            num++;
            remove_all_objects(dest, name, filter);
        }
        return num;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r6v0, resolved type: com.android.server.IntentResolver<F, R> */
    /* JADX WARN: Multi-variable type inference failed */
    private final void remove_all_objects(ArrayMap<String, F[]> map, String name, F object) {
        F[] array = map.get(name);
        if (array != null) {
            int LAST = array.length - 1;
            while (LAST >= 0 && array[LAST] == null) {
                LAST--;
            }
            for (int idx = LAST; idx >= 0; idx--) {
                F arrayValue = array[idx];
                if (arrayValue != null && getIntentFilter(arrayValue) == getIntentFilter(object)) {
                    int remain = LAST - idx;
                    if (remain > 0) {
                        System.arraycopy(array, idx + 1, array, idx, remain);
                    }
                    array[LAST] = null;
                    LAST--;
                }
            }
            if (LAST < 0) {
                map.remove(name);
            } else if (LAST < array.length / 2) {
                F[] newa = newArray(LAST + 2);
                System.arraycopy(array, 0, newa, 0, LAST + 1);
                map.put(name, newa);
            }
        }
    }

    private static FastImmutableArraySet<String> getFastIntentCategories(Intent intent) {
        Set<String> categories = intent.getCategories();
        if (categories == null) {
            return null;
        }
        return new FastImmutableArraySet<>((String[]) categories.toArray(new String[categories.size()]));
    }

    /* JADX WARN: Code restructure failed: missing block: B:24:0x008c, code lost:
        if (isPackageForFilter(r12, r0) != false) goto L29;
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x008e, code lost:
        if (r26 == false) goto L27;
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x0090, code lost:
        android.util.Slog.v(com.android.server.IntentResolver.TAG, "  Filter is not from package " + r12 + "; skipping");
        r18 = r4;
        r20 = r5;
        r21 = r10;
        r10 = r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x00b6, code lost:
        r18 = r4;
        r20 = r5;
        r21 = r10;
        r10 = r6;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void buildResolveList(Computer computer, Intent intent, FastImmutableArraySet<String> categories, boolean debug, boolean defaultOnly, String resolvedType, String scheme, F[] src, List<R> dest, int userId, long customFlags) {
        Printer logPrinter;
        PrintWriter logPrintWriter;
        int i;
        int N;
        String action;
        PrintWriter logPrintWriter2;
        String reason;
        F[] fArr = src;
        String action2 = intent.getAction();
        Uri data = intent.getData();
        String packageName = intent.getPackage();
        boolean excludingStopped = intent.isExcludingStopped();
        if (debug) {
            Printer logPrinter2 = new LogPrinter(2, TAG, 3);
            logPrinter = logPrinter2;
            logPrintWriter = new FastPrintWriter(logPrinter2);
        } else {
            logPrinter = null;
            logPrintWriter = null;
        }
        int N2 = fArr != null ? fArr.length : 0;
        boolean hasNonDefaults = false;
        int i2 = 0;
        while (i2 < N2) {
            F filter = fArr[i2];
            if (filter != null) {
                if (debug) {
                    Slog.v(TAG, "Matching against filter " + filter);
                }
                if (excludingStopped && isFilterStopped(computer.getPackageStateInternal(packageName), userId)) {
                    if (debug) {
                        Slog.v(TAG, "  Filter's target is stopped; skipping");
                        i = i2;
                        N = N2;
                        action = action2;
                        logPrintWriter2 = logPrintWriter;
                    } else {
                        i = i2;
                        N = N2;
                        action = action2;
                        logPrintWriter2 = logPrintWriter;
                    }
                    i2 = i + 1;
                    fArr = src;
                    logPrintWriter = logPrintWriter2;
                    N2 = N;
                    action2 = action;
                }
                IntentFilter intentFilter = getIntentFilter(filter);
                if (!intentFilter.getAutoVerify()) {
                    i = i2;
                } else if (debug) {
                    Slog.v(TAG, "  Filter verified: " + isFilterVerified(filter));
                    int authorities = intentFilter.countDataAuthorities();
                    int z = 0;
                    while (z < authorities) {
                        Slog.v(TAG, "   " + intentFilter.getDataAuthority(z).getHost());
                        z++;
                        authorities = authorities;
                        i2 = i2;
                    }
                    i = i2;
                } else {
                    i = i2;
                }
                if (!allowFilterResult(filter, dest)) {
                    if (debug) {
                        Slog.v(TAG, "  Filter's target already added");
                        N = N2;
                        action = action2;
                        logPrintWriter2 = logPrintWriter;
                    } else {
                        N = N2;
                        action = action2;
                        logPrintWriter2 = logPrintWriter;
                    }
                } else {
                    String str = action2;
                    N = N2;
                    action = action2;
                    logPrintWriter2 = logPrintWriter;
                    int match = intentFilter.match(str, resolvedType, scheme, data, categories, TAG);
                    if (match >= 0) {
                        if (debug) {
                            Slog.v(TAG, "  Filter matched!  match=0x" + Integer.toHexString(match) + " hasDefault=" + intentFilter.hasCategory("android.intent.category.DEFAULT"));
                        }
                        if (!defaultOnly || intentFilter.hasCategory("android.intent.category.DEFAULT")) {
                            R oneResult = newResult(computer, filter, match, userId, customFlags);
                            if (debug) {
                                Slog.v(TAG, "    Created result: " + oneResult);
                            }
                            if (oneResult != null) {
                                dest.add(oneResult);
                                if (debug) {
                                    dumpFilter(logPrintWriter2, "    ", filter);
                                    logPrintWriter2.flush();
                                    intentFilter.dump(logPrinter, "    ");
                                }
                            }
                        } else {
                            hasNonDefaults = true;
                        }
                    } else if (debug) {
                        switch (match) {
                            case -4:
                                reason = "category";
                                break;
                            case -3:
                                reason = "action";
                                break;
                            case -2:
                                reason = "data";
                                break;
                            case -1:
                                reason = DatabaseHelper.SoundModelContract.KEY_TYPE;
                                break;
                            default:
                                reason = "unknown reason";
                                break;
                        }
                        Slog.v(TAG, "  Filter did not match: " + reason);
                    }
                }
                i2 = i + 1;
                fArr = src;
                logPrintWriter = logPrintWriter2;
                N2 = N;
                action2 = action;
            } else if (!debug && hasNonDefaults) {
                if (dest.size() == 0) {
                    Slog.v(TAG, "resolveIntent failed: found match, but none with CATEGORY_DEFAULT");
                    return;
                } else if (dest.size() > 1) {
                    Slog.v(TAG, "resolveIntent: multiple matches, only some with CATEGORY_DEFAULT");
                    return;
                } else {
                    return;
                }
            }
        }
        if (!debug) {
        }
    }

    protected F snapshot(F f) {
        return f;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r7v0, resolved type: com.android.server.IntentResolver<F, R> */
    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: android.util.ArrayMap<java.lang.String, F[]> */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Multi-variable type inference failed */
    public void copyInto(ArrayMap<String, F[]> l, ArrayMap<String, F[]> r) {
        int end = r.size();
        l.clear();
        l.ensureCapacity(end);
        for (int i = 0; i < end; i++) {
            F[] val = r.valueAt(i);
            String key = r.keyAt(i);
            Object[] copyOf = Arrays.copyOf(val, val.length);
            for (int j = 0; j < copyOf.length; j++) {
                copyOf[j] = snapshot(copyOf[j]);
            }
            l.put(key, copyOf);
        }
    }

    protected void copyInto(ArraySet<F> l, ArraySet<F> r) {
        l.clear();
        int end = r.size();
        l.ensureCapacity(end);
        for (int i = 0; i < end; i++) {
            l.append(snapshot(r.valueAt(i)));
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void copyFrom(IntentResolver orig) {
        copyInto(this.mFilters, orig.mFilters);
        copyInto(this.mTypeToFilter, orig.mTypeToFilter);
        copyInto(this.mBaseTypeToFilter, orig.mBaseTypeToFilter);
        copyInto(this.mWildTypeToFilter, orig.mWildTypeToFilter);
        copyInto(this.mSchemeToFilter, orig.mSchemeToFilter);
        copyInto(this.mActionToFilter, orig.mActionToFilter);
        copyInto(this.mTypedActionToFilter, orig.mTypedActionToFilter);
    }
}
