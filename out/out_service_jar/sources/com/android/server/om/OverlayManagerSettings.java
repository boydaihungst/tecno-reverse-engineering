package com.android.server.om;

import android.content.om.OverlayIdentifier;
import android.content.om.OverlayInfo;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Pair;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.XmlUtils;
import com.android.server.om.OverlayManagerSettings;
import com.android.server.pm.PackageManagerService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public final class OverlayManagerSettings {
    private final ArrayList<SettingsItem> mItems = new ArrayList<>();

    public OverlayInfo init(OverlayIdentifier overlay, int userId, String targetPackageName, String targetOverlayableName, String baseCodePath, boolean isMutable, boolean isEnabled, int priority, String overlayCategory, boolean isFabricated) {
        remove(overlay, userId);
        SettingsItem item = new SettingsItem(overlay, userId, targetPackageName, targetOverlayableName, baseCodePath, -1, isEnabled, isMutable, priority, overlayCategory, isFabricated);
        insert(item);
        return item.getOverlayInfo();
    }

    public boolean remove(OverlayIdentifier overlay, int userId) {
        int idx = select(overlay, userId);
        if (idx < 0) {
            return false;
        }
        this.mItems.remove(idx);
        return true;
    }

    public OverlayInfo getOverlayInfo(OverlayIdentifier overlay, int userId) throws BadKeyException {
        int idx = select(overlay, userId);
        if (idx < 0) {
            throw new BadKeyException(overlay, userId);
        }
        return this.mItems.get(idx).getOverlayInfo();
    }

    public OverlayInfo getNullableOverlayInfo(OverlayIdentifier overlay, int userId) {
        int idx = select(overlay, userId);
        if (idx < 0) {
            return null;
        }
        return this.mItems.get(idx).getOverlayInfo();
    }

    public boolean setBaseCodePath(OverlayIdentifier overlay, int userId, String path) throws BadKeyException {
        int idx = select(overlay, userId);
        if (idx < 0) {
            throw new BadKeyException(overlay, userId);
        }
        return this.mItems.get(idx).setBaseCodePath(path);
    }

    public boolean setCategory(OverlayIdentifier overlay, int userId, String category) throws BadKeyException {
        int idx = select(overlay, userId);
        if (idx < 0) {
            throw new BadKeyException(overlay, userId);
        }
        return this.mItems.get(idx).setCategory(category);
    }

    public boolean getEnabled(OverlayIdentifier overlay, int userId) throws BadKeyException {
        int idx = select(overlay, userId);
        if (idx < 0) {
            throw new BadKeyException(overlay, userId);
        }
        return this.mItems.get(idx).isEnabled();
    }

    public boolean setEnabled(OverlayIdentifier overlay, int userId, boolean enable) throws BadKeyException {
        int idx = select(overlay, userId);
        if (idx < 0) {
            throw new BadKeyException(overlay, userId);
        }
        return this.mItems.get(idx).setEnabled(enable);
    }

    public int getState(OverlayIdentifier overlay, int userId) throws BadKeyException {
        int idx = select(overlay, userId);
        if (idx < 0) {
            throw new BadKeyException(overlay, userId);
        }
        return this.mItems.get(idx).getState();
    }

    public boolean setState(OverlayIdentifier overlay, int userId, int state) throws BadKeyException {
        int idx = select(overlay, userId);
        if (idx < 0) {
            throw new BadKeyException(overlay, userId);
        }
        return this.mItems.get(idx).setState(state);
    }

    public List<OverlayInfo> getOverlaysForTarget(String targetPackageName, int userId) {
        List<SettingsItem> items = selectWhereTarget(targetPackageName, userId);
        items.removeIf(new OverlayManagerSettings$$ExternalSyntheticLambda3());
        return CollectionUtils.map(items, new Function() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda4
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                OverlayInfo overlayInfo;
                overlayInfo = ((OverlayManagerSettings.SettingsItem) obj).getOverlayInfo();
                return overlayInfo;
            }
        });
    }

    public ArrayMap<String, List<OverlayInfo>> getOverlaysForUser(int userId) {
        List<SettingsItem> items = selectWhereUser(userId);
        items.removeIf(new OverlayManagerSettings$$ExternalSyntheticLambda3());
        ArrayMap<String, List<OverlayInfo>> targetInfos = new ArrayMap<>();
        int n = items.size();
        for (int i = 0; i < n; i++) {
            SettingsItem item = items.get(i);
            targetInfos.computeIfAbsent(item.mTargetPackageName, new Function() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda5
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return OverlayManagerSettings.lambda$getOverlaysForUser$1((String) obj);
                }
            }).add(item.getOverlayInfo());
        }
        return targetInfos;
    }

    public static /* synthetic */ List lambda$getOverlaysForUser$1(String String) {
        return new ArrayList();
    }

    public Set<String> getAllBaseCodePaths() {
        final Set<String> paths = new ArraySet<>();
        this.mItems.forEach(new Consumer() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda9
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                paths.add(((OverlayManagerSettings.SettingsItem) obj).mBaseCodePath);
            }
        });
        return paths;
    }

    public Set<Pair<OverlayIdentifier, String>> getAllIdentifiersAndBaseCodePaths() {
        final Set<Pair<OverlayIdentifier, String>> set = new ArraySet<>();
        this.mItems.forEach(new Consumer() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                set.add(new Pair(r2.mOverlay, ((OverlayManagerSettings.SettingsItem) obj).mBaseCodePath));
            }
        });
        return set;
    }

    public static /* synthetic */ boolean lambda$removeIf$4(Predicate predicate, int userId, OverlayInfo info) {
        return predicate.test(info) && info.userId == userId;
    }

    List<OverlayInfo> removeIf(final Predicate<OverlayInfo> predicate, final int userId) {
        return removeIf(new Predicate() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda6
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return OverlayManagerSettings.lambda$removeIf$4(predicate, userId, (OverlayInfo) obj);
            }
        });
    }

    public List<OverlayInfo> removeIf(Predicate<OverlayInfo> predicate) {
        List<OverlayInfo> removed = null;
        for (int i = this.mItems.size() - 1; i >= 0; i--) {
            OverlayInfo info = this.mItems.get(i).getOverlayInfo();
            if (predicate.test(info)) {
                this.mItems.remove(i);
                removed = CollectionUtils.add(removed, info);
            }
        }
        return CollectionUtils.emptyIfNull(removed);
    }

    public int[] getUsers() {
        return this.mItems.stream().mapToInt(new ToIntFunction() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda2
            @Override // java.util.function.ToIntFunction
            public final int applyAsInt(Object obj) {
                int userId;
                userId = ((OverlayManagerSettings.SettingsItem) obj).getUserId();
                return userId;
            }
        }).distinct().toArray();
    }

    public static boolean isImmutableFrameworkOverlay(SettingsItem item) {
        return !item.isMutable() && PackageManagerService.PLATFORM_PACKAGE_NAME.equals(item.getTargetPackageName());
    }

    public boolean removeUser(final int userId) {
        return this.mItems.removeIf(new Predicate() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return OverlayManagerSettings.lambda$removeUser$6(userId, (OverlayManagerSettings.SettingsItem) obj);
            }
        });
    }

    public static /* synthetic */ boolean lambda$removeUser$6(int userId, SettingsItem item) {
        if (item.getUserId() == userId) {
            return true;
        }
        return false;
    }

    public void setPriority(OverlayIdentifier overlay, int userId, int priority) throws BadKeyException {
        int moveIdx = select(overlay, userId);
        if (moveIdx < 0) {
            throw new BadKeyException(overlay, userId);
        }
        SettingsItem itemToMove = this.mItems.get(moveIdx);
        this.mItems.remove(moveIdx);
        itemToMove.setPriority(priority);
        insert(itemToMove);
    }

    public boolean setPriority(OverlayIdentifier overlay, OverlayIdentifier newOverlay, int userId) {
        int moveIdx;
        int parentIdx;
        if (!overlay.equals(newOverlay) && (moveIdx = select(overlay, userId)) >= 0 && (parentIdx = select(newOverlay, userId)) >= 0) {
            SettingsItem itemToMove = this.mItems.get(moveIdx);
            if (itemToMove.getTargetPackageName().equals(this.mItems.get(parentIdx).getTargetPackageName())) {
                this.mItems.remove(moveIdx);
                int newParentIdx = select(newOverlay, userId) + 1;
                this.mItems.add(newParentIdx, itemToMove);
                return moveIdx != newParentIdx;
            }
            return false;
        }
        return false;
    }

    public boolean setLowestPriority(OverlayIdentifier overlay, int userId) {
        int idx = select(overlay, userId);
        if (idx <= 0) {
            return false;
        }
        SettingsItem item = this.mItems.get(idx);
        this.mItems.remove(item);
        this.mItems.add(0, item);
        return true;
    }

    public boolean setHighestPriority(OverlayIdentifier overlay, int userId) {
        int idx = select(overlay, userId);
        if (idx < 0 || idx == this.mItems.size() - 1) {
            return false;
        }
        SettingsItem item = this.mItems.get(idx);
        this.mItems.remove(idx);
        this.mItems.add(item);
        return true;
    }

    private void insert(SettingsItem item) {
        int i = this.mItems.size() - 1;
        while (i >= 0) {
            SettingsItem parentItem = this.mItems.get(i);
            if (parentItem.mPriority <= item.getPriority()) {
                break;
            }
            i--;
        }
        this.mItems.add(i + 1, item);
    }

    public void dump(PrintWriter p, final DumpState dumpState) {
        Stream<SettingsItem> items = this.mItems.stream();
        if (dumpState.getUserId() != -1) {
            items = items.filter(new Predicate() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda12
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return OverlayManagerSettings.lambda$dump$7(DumpState.this, (OverlayManagerSettings.SettingsItem) obj);
                }
            });
        }
        if (dumpState.getPackageName() != null) {
            items = items.filter(new Predicate() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda13
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean equals;
                    equals = ((OverlayManagerSettings.SettingsItem) obj).mOverlay.getPackageName().equals(DumpState.this.getPackageName());
                    return equals;
                }
            });
        }
        if (dumpState.getOverlayName() != null) {
            items = items.filter(new Predicate() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda14
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean equals;
                    equals = ((OverlayManagerSettings.SettingsItem) obj).mOverlay.getOverlayName().equals(DumpState.this.getOverlayName());
                    return equals;
                }
            });
        }
        final IndentingPrintWriter pw = new IndentingPrintWriter(p, "  ");
        if (dumpState.getField() != null) {
            items.forEach(new Consumer() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda15
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    OverlayManagerSettings.this.m5244lambda$dump$10$comandroidserveromOverlayManagerSettings(pw, dumpState, (OverlayManagerSettings.SettingsItem) obj);
                }
            });
        } else {
            items.forEach(new Consumer() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda16
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    OverlayManagerSettings.this.m5245lambda$dump$11$comandroidserveromOverlayManagerSettings(pw, (OverlayManagerSettings.SettingsItem) obj);
                }
            });
        }
    }

    public static /* synthetic */ boolean lambda$dump$7(DumpState dumpState, SettingsItem item) {
        return item.mUserId == dumpState.getUserId();
    }

    /* renamed from: lambda$dump$10$com-android-server-om-OverlayManagerSettings */
    public /* synthetic */ void m5244lambda$dump$10$comandroidserveromOverlayManagerSettings(IndentingPrintWriter pw, DumpState dumpState, SettingsItem item) {
        dumpSettingsItemField(pw, item, dumpState.getField());
    }

    /* renamed from: dumpSettingsItem */
    public void m5245lambda$dump$11$comandroidserveromOverlayManagerSettings(IndentingPrintWriter pw, SettingsItem item) {
        pw.println(item.mOverlay + ":" + item.getUserId() + " {");
        pw.increaseIndent();
        pw.println("mPackageName...........: " + item.mOverlay.getPackageName());
        pw.println("mOverlayName...........: " + item.mOverlay.getOverlayName());
        pw.println("mUserId................: " + item.getUserId());
        pw.println("mTargetPackageName.....: " + item.getTargetPackageName());
        pw.println("mTargetOverlayableName.: " + item.getTargetOverlayableName());
        pw.println("mBaseCodePath..........: " + item.getBaseCodePath());
        pw.println("mState.................: " + OverlayInfo.stateToString(item.getState()));
        pw.println("mIsEnabled.............: " + item.isEnabled());
        pw.println("mIsMutable.............: " + item.isMutable());
        pw.println("mPriority..............: " + item.mPriority);
        pw.println("mCategory..............: " + item.mCategory);
        pw.println("mIsFabricated..........: " + item.mIsFabricated);
        pw.decreaseIndent();
        pw.println("}");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private void dumpSettingsItemField(IndentingPrintWriter pw, SettingsItem item, String field) {
        char c;
        switch (field.hashCode()) {
            case -1750736508:
                if (field.equals("targetoverlayablename")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -1248283232:
                if (field.equals("targetpackagename")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -1165461084:
                if (field.equals("priority")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -836029914:
                if (field.equals("userid")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -831052100:
                if (field.equals("ismutable")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -405989669:
                if (field.equals("overlayname")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 50511102:
                if (field.equals("category")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 109757585:
                if (field.equals("state")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 440941271:
                if (field.equals("isenabled")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 909712337:
                if (field.equals("packagename")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1693907299:
                if (field.equals("basecodepath")) {
                    c = 5;
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
                pw.println(item.mOverlay.getPackageName());
                return;
            case 1:
                pw.println(item.mOverlay.getOverlayName());
                return;
            case 2:
                pw.println(item.mUserId);
                return;
            case 3:
                pw.println(item.mTargetPackageName);
                return;
            case 4:
                pw.println(item.mTargetOverlayableName);
                return;
            case 5:
                pw.println(item.mBaseCodePath);
                return;
            case 6:
                pw.println(OverlayInfo.stateToString(item.mState));
                return;
            case 7:
                pw.println(item.mIsEnabled);
                return;
            case '\b':
                pw.println(item.mIsMutable);
                return;
            case '\t':
                pw.println(item.mPriority);
                return;
            case '\n':
                pw.println(item.mCategory);
                return;
            default:
                return;
        }
    }

    public void restore(InputStream is) throws IOException, XmlPullParserException {
        Serializer.restore(this.mItems, is);
    }

    public void persist(OutputStream os) throws IOException, XmlPullParserException {
        Serializer.persist(this.mItems, os);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class Serializer {
        private static final String ATTR_BASE_CODE_PATH = "baseCodePath";
        private static final String ATTR_CATEGORY = "category";
        private static final String ATTR_IS_ENABLED = "isEnabled";
        private static final String ATTR_IS_FABRICATED = "fabricated";
        private static final String ATTR_IS_STATIC = "isStatic";
        private static final String ATTR_OVERLAY_NAME = "overlayName";
        private static final String ATTR_PACKAGE_NAME = "packageName";
        private static final String ATTR_PRIORITY = "priority";
        private static final String ATTR_STATE = "state";
        private static final String ATTR_TARGET_OVERLAYABLE_NAME = "targetOverlayableName";
        private static final String ATTR_TARGET_PACKAGE_NAME = "targetPackageName";
        private static final String ATTR_USER_ID = "userId";
        private static final String ATTR_VERSION = "version";
        static final int CURRENT_VERSION = 4;
        private static final String TAG_ITEM = "item";
        private static final String TAG_OVERLAYS = "overlays";

        Serializer() {
        }

        public static void restore(ArrayList<SettingsItem> table, InputStream is) throws IOException, XmlPullParserException {
            table.clear();
            TypedXmlPullParser parser = Xml.resolvePullParser(is);
            XmlUtils.beginDocument(parser, TAG_OVERLAYS);
            int version = parser.getAttributeInt((String) null, ATTR_VERSION);
            if (version != 4) {
                upgrade(version);
            }
            int depth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, depth)) {
                if ("item".equals(parser.getName())) {
                    SettingsItem item = restoreRow(parser, depth + 1);
                    table.add(item);
                }
            }
        }

        private static void upgrade(int oldVersion) throws XmlPullParserException {
            switch (oldVersion) {
                case 0:
                case 1:
                case 2:
                    throw new XmlPullParserException("old version " + oldVersion + "; ignoring");
                case 3:
                    return;
                default:
                    throw new XmlPullParserException("unrecognized version " + oldVersion);
            }
        }

        private static SettingsItem restoreRow(TypedXmlPullParser parser, int depth) throws IOException, XmlPullParserException {
            OverlayIdentifier overlay = new OverlayIdentifier(XmlUtils.readStringAttribute(parser, "packageName"), XmlUtils.readStringAttribute(parser, ATTR_OVERLAY_NAME));
            int userId = parser.getAttributeInt((String) null, "userId");
            String targetPackageName = XmlUtils.readStringAttribute(parser, ATTR_TARGET_PACKAGE_NAME);
            String targetOverlayableName = XmlUtils.readStringAttribute(parser, ATTR_TARGET_OVERLAYABLE_NAME);
            String baseCodePath = XmlUtils.readStringAttribute(parser, ATTR_BASE_CODE_PATH);
            int state = parser.getAttributeInt((String) null, "state");
            boolean isEnabled = parser.getAttributeBoolean((String) null, ATTR_IS_ENABLED, false);
            boolean isStatic = parser.getAttributeBoolean((String) null, ATTR_IS_STATIC, false);
            int priority = parser.getAttributeInt((String) null, ATTR_PRIORITY);
            String category = XmlUtils.readStringAttribute(parser, ATTR_CATEGORY);
            boolean isFabricated = parser.getAttributeBoolean((String) null, ATTR_IS_FABRICATED, false);
            return new SettingsItem(overlay, userId, targetPackageName, targetOverlayableName, baseCodePath, state, isEnabled, !isStatic, priority, category, isFabricated);
        }

        public static void persist(ArrayList<SettingsItem> table, OutputStream os) throws IOException, XmlPullParserException {
            TypedXmlSerializer xml = Xml.resolveSerializer(os);
            xml.startDocument((String) null, true);
            xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xml.startTag((String) null, TAG_OVERLAYS);
            xml.attributeInt((String) null, ATTR_VERSION, 4);
            int n = table.size();
            for (int i = 0; i < n; i++) {
                SettingsItem item = table.get(i);
                persistRow(xml, item);
            }
            xml.endTag((String) null, TAG_OVERLAYS);
            xml.endDocument();
        }

        private static void persistRow(TypedXmlSerializer xml, SettingsItem item) throws IOException {
            xml.startTag((String) null, "item");
            XmlUtils.writeStringAttribute(xml, "packageName", item.mOverlay.getPackageName());
            XmlUtils.writeStringAttribute(xml, ATTR_OVERLAY_NAME, item.mOverlay.getOverlayName());
            xml.attributeInt((String) null, "userId", item.mUserId);
            XmlUtils.writeStringAttribute(xml, ATTR_TARGET_PACKAGE_NAME, item.mTargetPackageName);
            XmlUtils.writeStringAttribute(xml, ATTR_TARGET_OVERLAYABLE_NAME, item.mTargetOverlayableName);
            XmlUtils.writeStringAttribute(xml, ATTR_BASE_CODE_PATH, item.mBaseCodePath);
            xml.attributeInt((String) null, "state", item.mState);
            XmlUtils.writeBooleanAttribute(xml, ATTR_IS_ENABLED, item.mIsEnabled);
            XmlUtils.writeBooleanAttribute(xml, ATTR_IS_STATIC, !item.mIsMutable);
            xml.attributeInt((String) null, ATTR_PRIORITY, item.mPriority);
            XmlUtils.writeStringAttribute(xml, ATTR_CATEGORY, item.mCategory);
            XmlUtils.writeBooleanAttribute(xml, ATTR_IS_FABRICATED, item.mIsFabricated);
            xml.endTag((String) null, "item");
        }
    }

    /* loaded from: classes2.dex */
    public static final class SettingsItem {
        private String mBaseCodePath;
        private OverlayInfo mCache = null;
        private String mCategory;
        private boolean mIsEnabled;
        private boolean mIsFabricated;
        private boolean mIsMutable;
        private final OverlayIdentifier mOverlay;
        private int mPriority;
        private int mState;
        private final String mTargetOverlayableName;
        private final String mTargetPackageName;
        private final int mUserId;

        SettingsItem(OverlayIdentifier overlay, int userId, String targetPackageName, String targetOverlayableName, String baseCodePath, int state, boolean isEnabled, boolean isMutable, int priority, String category, boolean isFabricated) {
            this.mOverlay = overlay;
            this.mUserId = userId;
            this.mTargetPackageName = targetPackageName;
            this.mTargetOverlayableName = targetOverlayableName;
            this.mBaseCodePath = baseCodePath;
            this.mState = state;
            this.mIsEnabled = isEnabled;
            this.mCategory = category;
            this.mIsMutable = isMutable;
            this.mPriority = priority;
            this.mIsFabricated = isFabricated;
        }

        public String getTargetPackageName() {
            return this.mTargetPackageName;
        }

        public String getTargetOverlayableName() {
            return this.mTargetOverlayableName;
        }

        public int getUserId() {
            return this.mUserId;
        }

        public String getBaseCodePath() {
            return this.mBaseCodePath;
        }

        public boolean setBaseCodePath(String path) {
            if (!this.mBaseCodePath.equals(path)) {
                this.mBaseCodePath = path;
                invalidateCache();
                return true;
            }
            return false;
        }

        public int getState() {
            return this.mState;
        }

        public boolean setState(int state) {
            if (this.mState != state) {
                this.mState = state;
                invalidateCache();
                return true;
            }
            return false;
        }

        public boolean isEnabled() {
            return this.mIsEnabled;
        }

        public boolean setEnabled(boolean enable) {
            if (this.mIsMutable && this.mIsEnabled != enable) {
                this.mIsEnabled = enable;
                invalidateCache();
                return true;
            }
            return false;
        }

        public boolean setCategory(String category) {
            if (!Objects.equals(this.mCategory, category)) {
                this.mCategory = category == null ? null : category.intern();
                invalidateCache();
                return true;
            }
            return false;
        }

        public OverlayInfo getOverlayInfo() {
            if (this.mCache == null) {
                this.mCache = new OverlayInfo(this.mOverlay.getPackageName(), this.mOverlay.getOverlayName(), this.mTargetPackageName, this.mTargetOverlayableName, this.mCategory, this.mBaseCodePath, this.mState, this.mUserId, this.mPriority, this.mIsMutable, this.mIsFabricated);
            }
            return this.mCache;
        }

        public void setPriority(int priority) {
            this.mPriority = priority;
            invalidateCache();
        }

        private void invalidateCache() {
            this.mCache = null;
        }

        public boolean isMutable() {
            return this.mIsMutable;
        }

        public int getPriority() {
            return this.mPriority;
        }
    }

    private int select(OverlayIdentifier overlay, int userId) {
        int n = this.mItems.size();
        for (int i = 0; i < n; i++) {
            SettingsItem item = this.mItems.get(i);
            if (item.mUserId == userId && item.mOverlay.equals(overlay)) {
                return i;
            }
        }
        return -1;
    }

    private List<SettingsItem> selectWhereUser(final int userId) {
        List<SettingsItem> selectedItems = new ArrayList<>();
        if (userId == 999) {
            CollectionUtils.addIf(this.mItems, selectedItems, new Predicate() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda7
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return OverlayManagerSettings.lambda$selectWhereUser$12((OverlayManagerSettings.SettingsItem) obj);
                }
            });
        } else {
            CollectionUtils.addIf(this.mItems, selectedItems, new Predicate() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda8
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return OverlayManagerSettings.lambda$selectWhereUser$13(userId, (OverlayManagerSettings.SettingsItem) obj);
                }
            });
        }
        return selectedItems;
    }

    public static /* synthetic */ boolean lambda$selectWhereUser$12(SettingsItem i) {
        return i.mUserId == 0;
    }

    public static /* synthetic */ boolean lambda$selectWhereUser$13(int userId, SettingsItem i) {
        return i.mUserId == userId;
    }

    private List<SettingsItem> selectWhereOverlay(final String packageName, int userId) {
        List<SettingsItem> items = selectWhereUser(userId);
        items.removeIf(new Predicate() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda10
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return OverlayManagerSettings.lambda$selectWhereOverlay$14(packageName, (OverlayManagerSettings.SettingsItem) obj);
            }
        });
        return items;
    }

    public static /* synthetic */ boolean lambda$selectWhereOverlay$14(String packageName, SettingsItem i) {
        return !i.mOverlay.getPackageName().equals(packageName);
    }

    private List<SettingsItem> selectWhereTarget(final String targetPackageName, int userId) {
        List<SettingsItem> items = selectWhereUser(userId);
        items.removeIf(new Predicate() { // from class: com.android.server.om.OverlayManagerSettings$$ExternalSyntheticLambda11
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return OverlayManagerSettings.lambda$selectWhereTarget$15(targetPackageName, (OverlayManagerSettings.SettingsItem) obj);
            }
        });
        return items;
    }

    public static /* synthetic */ boolean lambda$selectWhereTarget$15(String targetPackageName, SettingsItem i) {
        return !i.getTargetPackageName().equals(targetPackageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class BadKeyException extends Exception {
        BadKeyException(OverlayIdentifier overlay, int userId) {
            super("Bad key '" + overlay + "' for user " + userId);
        }
    }
}
