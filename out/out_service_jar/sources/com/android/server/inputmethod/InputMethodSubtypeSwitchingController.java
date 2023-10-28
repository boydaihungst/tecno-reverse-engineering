package com.android.server.inputmethod;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Printer;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import com.android.server.inputmethod.InputMethodUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class InputMethodSubtypeSwitchingController {
    private static final boolean DEBUG = false;
    private static final int NOT_A_SUBTYPE_ID = -1;
    private static final String TAG = InputMethodSubtypeSwitchingController.class.getSimpleName();
    private ControllerImpl mController;
    private final InputMethodUtils.InputMethodSettings mSettings;
    private InputMethodAndSubtypeList mSubtypeList;

    /* loaded from: classes.dex */
    public static class ImeSubtypeListItem implements Comparable<ImeSubtypeListItem> {
        public final CharSequence mImeName;
        public final InputMethodInfo mImi;
        public final boolean mIsSystemLanguage;
        public final boolean mIsSystemLocale;
        public final int mSubtypeId;
        public final CharSequence mSubtypeName;

        ImeSubtypeListItem(CharSequence imeName, CharSequence subtypeName, InputMethodInfo imi, int subtypeId, String subtypeLocale, String systemLocale) {
            this.mImeName = imeName;
            this.mSubtypeName = subtypeName;
            this.mImi = imi;
            this.mSubtypeId = subtypeId;
            boolean z = false;
            if (TextUtils.isEmpty(subtypeLocale)) {
                this.mIsSystemLocale = false;
                this.mIsSystemLanguage = false;
                return;
            }
            boolean equals = subtypeLocale.equals(systemLocale);
            this.mIsSystemLocale = equals;
            if (equals) {
                this.mIsSystemLanguage = true;
                return;
            }
            String systemLanguage = parseLanguageFromLocaleString(systemLocale);
            String subtypeLanguage = parseLanguageFromLocaleString(subtypeLocale);
            if (systemLanguage.length() >= 2 && systemLanguage.equals(subtypeLanguage)) {
                z = true;
            }
            this.mIsSystemLanguage = z;
        }

        private static String parseLanguageFromLocaleString(String locale) {
            int idx = locale.indexOf(95);
            if (idx < 0) {
                return locale;
            }
            return locale.substring(0, idx);
        }

        private static int compareNullableCharSequences(CharSequence c1, CharSequence c2) {
            boolean empty1 = TextUtils.isEmpty(c1);
            boolean empty2 = TextUtils.isEmpty(c2);
            if (empty1 || empty2) {
                return (empty1 ? 1 : 0) - (empty2 ? 1 : 0);
            }
            return c1.toString().compareTo(c2.toString());
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.lang.Comparable
        public int compareTo(ImeSubtypeListItem other) {
            int result = compareNullableCharSequences(this.mImeName, other.mImeName);
            if (result != 0) {
                return result;
            }
            int result2 = (this.mIsSystemLocale ? -1 : 0) - (other.mIsSystemLocale ? -1 : 0);
            if (result2 != 0) {
                return result2;
            }
            int result3 = (this.mIsSystemLanguage ? -1 : 0) - (other.mIsSystemLanguage ? -1 : 0);
            if (result3 != 0) {
                return result3;
            }
            int result4 = compareNullableCharSequences(this.mSubtypeName, other.mSubtypeName);
            if (result4 != 0) {
                return result4;
            }
            return this.mImi.getId().compareTo(other.mImi.getId());
        }

        public String toString() {
            return "ImeSubtypeListItem{mImeName=" + ((Object) this.mImeName) + " mSubtypeName=" + ((Object) this.mSubtypeName) + " mSubtypeId=" + this.mSubtypeId + " mIsSystemLocale=" + this.mIsSystemLocale + " mIsSystemLanguage=" + this.mIsSystemLanguage + "}";
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof ImeSubtypeListItem) {
                ImeSubtypeListItem that = (ImeSubtypeListItem) o;
                return Objects.equals(this.mImi, that.mImi) && this.mSubtypeId == that.mSubtypeId;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class InputMethodAndSubtypeList {
        private final Context mContext;
        private final PackageManager mPm;
        private final InputMethodUtils.InputMethodSettings mSettings;
        private final String mSystemLocaleStr;

        InputMethodAndSubtypeList(Context context, InputMethodUtils.InputMethodSettings settings) {
            this.mContext = context;
            this.mSettings = settings;
            this.mPm = context.getPackageManager();
            Locale locale = context.getResources().getConfiguration().locale;
            this.mSystemLocaleStr = locale != null ? locale.toString() : "";
        }

        public List<ImeSubtypeListItem> getSortedInputMethodAndSubtypeList(boolean includeAuxiliarySubtypes, boolean isScreenLocked, boolean forImeMenu) {
            boolean includeAuxiliarySubtypes2;
            ArrayList<InputMethodInfo> imis;
            boolean includeAuxiliarySubtypes3;
            ArrayList<InputMethodInfo> imis2;
            boolean includeAuxiliarySubtypes4;
            int j;
            int subtypeCount;
            ArrayList<InputMethodInfo> imis3 = this.mSettings.getEnabledInputMethodListLocked();
            if (imis3.isEmpty()) {
                return Collections.emptyList();
            }
            if (isScreenLocked && includeAuxiliarySubtypes) {
                includeAuxiliarySubtypes2 = false;
            } else {
                includeAuxiliarySubtypes2 = includeAuxiliarySubtypes;
            }
            ArrayList<ImeSubtypeListItem> imList = new ArrayList<>();
            int numImes = imis3.size();
            int i = 0;
            while (i < numImes) {
                InputMethodInfo imi = imis3.get(i);
                if (forImeMenu && !imi.shouldShowInInputMethodPicker()) {
                    imis = imis3;
                    includeAuxiliarySubtypes3 = includeAuxiliarySubtypes2;
                } else {
                    List<InputMethodSubtype> explicitlyOrImplicitlyEnabledSubtypeList = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi, true);
                    ArraySet<String> enabledSubtypeSet = new ArraySet<>();
                    for (InputMethodSubtype subtype : explicitlyOrImplicitlyEnabledSubtypeList) {
                        enabledSubtypeSet.add(String.valueOf(subtype.hashCode()));
                    }
                    CharSequence imeLabel = imi.loadLabel(this.mPm);
                    if (enabledSubtypeSet.size() > 0) {
                        int subtypeCount2 = imi.getSubtypeCount();
                        int j2 = 0;
                        while (j2 < subtypeCount2) {
                            InputMethodSubtype subtype2 = imi.getSubtypeAt(j2);
                            String subtypeHashCode = String.valueOf(subtype2.hashCode());
                            if (!enabledSubtypeSet.contains(subtypeHashCode)) {
                                imis2 = imis3;
                                includeAuxiliarySubtypes4 = includeAuxiliarySubtypes2;
                                j = j2;
                                subtypeCount = subtypeCount2;
                            } else if (includeAuxiliarySubtypes2 || !subtype2.isAuxiliary()) {
                                CharSequence subtypeLabel = subtype2.overridesImplicitlyEnabledSubtype() ? null : subtype2.getDisplayName(this.mContext, imi.getPackageName(), imi.getServiceInfo().applicationInfo);
                                imis2 = imis3;
                                includeAuxiliarySubtypes4 = includeAuxiliarySubtypes2;
                                j = j2;
                                subtypeCount = subtypeCount2;
                                imList.add(new ImeSubtypeListItem(imeLabel, subtypeLabel, imi, j2, subtype2.getLocale(), this.mSystemLocaleStr));
                                enabledSubtypeSet.remove(subtypeHashCode);
                            } else {
                                imis2 = imis3;
                                includeAuxiliarySubtypes4 = includeAuxiliarySubtypes2;
                                j = j2;
                                subtypeCount = subtypeCount2;
                            }
                            j2 = j + 1;
                            includeAuxiliarySubtypes2 = includeAuxiliarySubtypes4;
                            subtypeCount2 = subtypeCount;
                            imis3 = imis2;
                        }
                        imis = imis3;
                        includeAuxiliarySubtypes3 = includeAuxiliarySubtypes2;
                    } else {
                        imis = imis3;
                        includeAuxiliarySubtypes3 = includeAuxiliarySubtypes2;
                        imList.add(new ImeSubtypeListItem(imeLabel, null, imi, -1, null, this.mSystemLocaleStr));
                    }
                }
                i++;
                includeAuxiliarySubtypes2 = includeAuxiliarySubtypes3;
                imis3 = imis;
            }
            Collections.sort(imList);
            return imList;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int calculateSubtypeId(InputMethodInfo imi, InputMethodSubtype subtype) {
        if (subtype != null) {
            return InputMethodUtils.getSubtypeIdFromHashCode(imi, subtype.hashCode());
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class StaticRotationList {
        private final List<ImeSubtypeListItem> mImeSubtypeList;

        StaticRotationList(List<ImeSubtypeListItem> imeSubtypeList) {
            this.mImeSubtypeList = imeSubtypeList;
        }

        private int getIndex(InputMethodInfo imi, InputMethodSubtype subtype) {
            int currentSubtypeId = InputMethodSubtypeSwitchingController.calculateSubtypeId(imi, subtype);
            int numSubtypes = this.mImeSubtypeList.size();
            for (int i = 0; i < numSubtypes; i++) {
                ImeSubtypeListItem isli = this.mImeSubtypeList.get(i);
                if (imi.equals(isli.mImi) && isli.mSubtypeId == currentSubtypeId) {
                    return i;
                }
            }
            return -1;
        }

        public ImeSubtypeListItem getNextInputMethodLocked(boolean onlyCurrentIme, InputMethodInfo imi, InputMethodSubtype subtype) {
            int currentIndex;
            if (imi == null || this.mImeSubtypeList.size() <= 1 || (currentIndex = getIndex(imi, subtype)) < 0) {
                return null;
            }
            int numSubtypes = this.mImeSubtypeList.size();
            for (int offset = 1; offset < numSubtypes; offset++) {
                int candidateIndex = (currentIndex + offset) % numSubtypes;
                ImeSubtypeListItem candidate = this.mImeSubtypeList.get(candidateIndex);
                if (!onlyCurrentIme || imi.equals(candidate.mImi)) {
                    return candidate;
                }
            }
            return null;
        }

        protected void dump(Printer pw, String prefix) {
            int numSubtypes = this.mImeSubtypeList.size();
            for (int i = 0; i < numSubtypes; i++) {
                int rank = i;
                ImeSubtypeListItem item = this.mImeSubtypeList.get(i);
                pw.println(prefix + "rank=" + rank + " item=" + item);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DynamicRotationList {
        private static final String TAG = DynamicRotationList.class.getSimpleName();
        private final List<ImeSubtypeListItem> mImeSubtypeList;
        private final int[] mUsageHistoryOfSubtypeListItemIndex;

        private DynamicRotationList(List<ImeSubtypeListItem> imeSubtypeListItems) {
            this.mImeSubtypeList = imeSubtypeListItems;
            this.mUsageHistoryOfSubtypeListItemIndex = new int[imeSubtypeListItems.size()];
            int numSubtypes = imeSubtypeListItems.size();
            for (int i = 0; i < numSubtypes; i++) {
                this.mUsageHistoryOfSubtypeListItemIndex[i] = i;
            }
        }

        private int getUsageRank(InputMethodInfo imi, InputMethodSubtype subtype) {
            int currentSubtypeId = InputMethodSubtypeSwitchingController.calculateSubtypeId(imi, subtype);
            int numItems = this.mUsageHistoryOfSubtypeListItemIndex.length;
            for (int usageRank = 0; usageRank < numItems; usageRank++) {
                int subtypeListItemIndex = this.mUsageHistoryOfSubtypeListItemIndex[usageRank];
                ImeSubtypeListItem subtypeListItem = this.mImeSubtypeList.get(subtypeListItemIndex);
                if (subtypeListItem.mImi.equals(imi) && subtypeListItem.mSubtypeId == currentSubtypeId) {
                    return usageRank;
                }
            }
            return -1;
        }

        public void onUserAction(InputMethodInfo imi, InputMethodSubtype subtype) {
            int currentUsageRank = getUsageRank(imi, subtype);
            if (currentUsageRank <= 0) {
                return;
            }
            int[] iArr = this.mUsageHistoryOfSubtypeListItemIndex;
            int currentItemIndex = iArr[currentUsageRank];
            System.arraycopy(iArr, 0, iArr, 1, currentUsageRank);
            this.mUsageHistoryOfSubtypeListItemIndex[0] = currentItemIndex;
        }

        public ImeSubtypeListItem getNextInputMethodLocked(boolean onlyCurrentIme, InputMethodInfo imi, InputMethodSubtype subtype) {
            int currentUsageRank = getUsageRank(imi, subtype);
            if (currentUsageRank < 0) {
                return null;
            }
            int numItems = this.mUsageHistoryOfSubtypeListItemIndex.length;
            for (int i = 1; i < numItems; i++) {
                int subtypeListItemRank = (currentUsageRank + i) % numItems;
                int subtypeListItemIndex = this.mUsageHistoryOfSubtypeListItemIndex[subtypeListItemRank];
                ImeSubtypeListItem subtypeListItem = this.mImeSubtypeList.get(subtypeListItemIndex);
                if (!onlyCurrentIme || imi.equals(subtypeListItem.mImi)) {
                    return subtypeListItem;
                }
            }
            return null;
        }

        protected void dump(Printer pw, String prefix) {
            int i = 0;
            while (true) {
                int[] iArr = this.mUsageHistoryOfSubtypeListItemIndex;
                if (i < iArr.length) {
                    int rank = iArr[i];
                    ImeSubtypeListItem item = this.mImeSubtypeList.get(i);
                    pw.println(prefix + "rank=" + rank + " item=" + item);
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public static class ControllerImpl {
        private final DynamicRotationList mSwitchingAwareRotationList;
        private final StaticRotationList mSwitchingUnawareRotationList;

        public static ControllerImpl createFrom(ControllerImpl currentInstance, List<ImeSubtypeListItem> sortedEnabledItems) {
            StaticRotationList staticRotationList;
            DynamicRotationList dynamicRotationList;
            DynamicRotationList switchingAwareRotationList = null;
            List<ImeSubtypeListItem> switchingAwareImeSubtypes = filterImeSubtypeList(sortedEnabledItems, true);
            if (currentInstance != null && (dynamicRotationList = currentInstance.mSwitchingAwareRotationList) != null && Objects.equals(dynamicRotationList.mImeSubtypeList, switchingAwareImeSubtypes)) {
                switchingAwareRotationList = currentInstance.mSwitchingAwareRotationList;
            }
            if (switchingAwareRotationList == null) {
                switchingAwareRotationList = new DynamicRotationList(switchingAwareImeSubtypes);
            }
            StaticRotationList switchingUnawareRotationList = null;
            List<ImeSubtypeListItem> switchingUnawareImeSubtypes = filterImeSubtypeList(sortedEnabledItems, false);
            if (currentInstance != null && (staticRotationList = currentInstance.mSwitchingUnawareRotationList) != null && Objects.equals(staticRotationList.mImeSubtypeList, switchingUnawareImeSubtypes)) {
                switchingUnawareRotationList = currentInstance.mSwitchingUnawareRotationList;
            }
            if (switchingUnawareRotationList == null) {
                switchingUnawareRotationList = new StaticRotationList(switchingUnawareImeSubtypes);
            }
            return new ControllerImpl(switchingAwareRotationList, switchingUnawareRotationList);
        }

        private ControllerImpl(DynamicRotationList switchingAwareRotationList, StaticRotationList switchingUnawareRotationList) {
            this.mSwitchingAwareRotationList = switchingAwareRotationList;
            this.mSwitchingUnawareRotationList = switchingUnawareRotationList;
        }

        public ImeSubtypeListItem getNextInputMethod(boolean onlyCurrentIme, InputMethodInfo imi, InputMethodSubtype subtype) {
            if (imi == null) {
                return null;
            }
            if (imi.supportsSwitchingToNextInputMethod()) {
                return this.mSwitchingAwareRotationList.getNextInputMethodLocked(onlyCurrentIme, imi, subtype);
            }
            return this.mSwitchingUnawareRotationList.getNextInputMethodLocked(onlyCurrentIme, imi, subtype);
        }

        public void onUserActionLocked(InputMethodInfo imi, InputMethodSubtype subtype) {
            if (imi != null && imi.supportsSwitchingToNextInputMethod()) {
                this.mSwitchingAwareRotationList.onUserAction(imi, subtype);
            }
        }

        private static List<ImeSubtypeListItem> filterImeSubtypeList(List<ImeSubtypeListItem> items, boolean supportsSwitchingToNextInputMethod) {
            ArrayList<ImeSubtypeListItem> result = new ArrayList<>();
            int numItems = items.size();
            for (int i = 0; i < numItems; i++) {
                ImeSubtypeListItem item = items.get(i);
                if (item.mImi.supportsSwitchingToNextInputMethod() == supportsSwitchingToNextInputMethod) {
                    result.add(item);
                }
            }
            return result;
        }

        protected void dump(Printer pw) {
            pw.println("    mSwitchingAwareRotationList:");
            this.mSwitchingAwareRotationList.dump(pw, "      ");
            pw.println("    mSwitchingUnawareRotationList:");
            this.mSwitchingUnawareRotationList.dump(pw, "      ");
        }
    }

    private InputMethodSubtypeSwitchingController(InputMethodUtils.InputMethodSettings settings, Context context) {
        this.mSettings = settings;
        resetCircularListLocked(context);
    }

    public static InputMethodSubtypeSwitchingController createInstanceLocked(InputMethodUtils.InputMethodSettings settings, Context context) {
        return new InputMethodSubtypeSwitchingController(settings, context);
    }

    public void onUserActionLocked(InputMethodInfo imi, InputMethodSubtype subtype) {
        ControllerImpl controllerImpl = this.mController;
        if (controllerImpl == null) {
            return;
        }
        controllerImpl.onUserActionLocked(imi, subtype);
    }

    public void resetCircularListLocked(Context context) {
        InputMethodAndSubtypeList inputMethodAndSubtypeList = new InputMethodAndSubtypeList(context, this.mSettings);
        this.mSubtypeList = inputMethodAndSubtypeList;
        this.mController = ControllerImpl.createFrom(this.mController, inputMethodAndSubtypeList.getSortedInputMethodAndSubtypeList(false, false, false));
    }

    public ImeSubtypeListItem getNextInputMethodLocked(boolean onlyCurrentIme, InputMethodInfo imi, InputMethodSubtype subtype) {
        ControllerImpl controllerImpl = this.mController;
        if (controllerImpl == null) {
            return null;
        }
        return controllerImpl.getNextInputMethod(onlyCurrentIme, imi, subtype);
    }

    public List<ImeSubtypeListItem> getSortedInputMethodAndSubtypeListForImeMenuLocked(boolean includingAuxiliarySubtypes, boolean isScreenLocked) {
        return this.mSubtypeList.getSortedInputMethodAndSubtypeList(includingAuxiliarySubtypes, isScreenLocked, true);
    }

    public void dump(Printer pw) {
        ControllerImpl controllerImpl = this.mController;
        if (controllerImpl != null) {
            controllerImpl.dump(pw);
        } else {
            pw.println("    mController=null");
        }
    }
}
