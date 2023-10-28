package android.view;

import android.app.WindowConfiguration;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayCutout;
import android.view.WindowInsets;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
/* loaded from: classes3.dex */
public class InsetsState implements Parcelable {
    public static final Parcelable.Creator<InsetsState> CREATOR = new Parcelable.Creator<InsetsState>() { // from class: android.view.InsetsState.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InsetsState createFromParcel(Parcel in) {
            return new InsetsState(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InsetsState[] newArray(int size) {
            return new InsetsState[size];
        }
    };
    static final int FIRST_TYPE = 0;
    static final int ISIDE_BOTTOM = 3;
    static final int ISIDE_FLOATING = 4;
    static final int ISIDE_LEFT = 0;
    static final int ISIDE_RIGHT = 2;
    static final int ISIDE_TOP = 1;
    static final int ISIDE_UNKNOWN = 5;
    public static final int ITYPE_BOTTOM_DISPLAY_CUTOUT = 14;
    public static final int ITYPE_BOTTOM_GESTURES = 4;
    public static final int ITYPE_BOTTOM_MANDATORY_GESTURES = 8;
    public static final int ITYPE_BOTTOM_TAPPABLE_ELEMENT = 18;
    public static final int ITYPE_CAPTION_BAR = 2;
    public static final int ITYPE_CLIMATE_BAR = 20;
    public static final int ITYPE_EXTRA_NAVIGATION_BAR = 21;
    public static final int ITYPE_IME = 19;
    public static final int ITYPE_INVALID = -1;
    public static final int ITYPE_LEFT_DISPLAY_CUTOUT = 11;
    public static final int ITYPE_LEFT_GESTURES = 5;
    public static final int ITYPE_LEFT_MANDATORY_GESTURES = 9;
    public static final int ITYPE_LEFT_TAPPABLE_ELEMENT = 15;
    public static final int ITYPE_LOCAL_NAVIGATION_BAR_1 = 22;
    public static final int ITYPE_LOCAL_NAVIGATION_BAR_2 = 23;
    public static final int ITYPE_NAVIGATION_BAR = 1;
    public static final int ITYPE_RIGHT_DISPLAY_CUTOUT = 13;
    public static final int ITYPE_RIGHT_GESTURES = 6;
    public static final int ITYPE_RIGHT_MANDATORY_GESTURES = 10;
    public static final int ITYPE_RIGHT_TAPPABLE_ELEMENT = 17;
    public static final int ITYPE_SHELF = 1;
    public static final int ITYPE_STATUS_BAR = 0;
    public static final int ITYPE_TOP_DISPLAY_CUTOUT = 12;
    public static final int ITYPE_TOP_GESTURES = 3;
    public static final int ITYPE_TOP_MANDATORY_GESTURES = 7;
    public static final int ITYPE_TOP_TAPPABLE_ELEMENT = 16;
    static final int LAST_TYPE = 23;
    public static final int SIZE = 24;
    private final DisplayCutout.ParcelableWrapper mDisplayCutout;
    private final Rect mDisplayFrame;
    private PrivacyIndicatorBounds mPrivacyIndicatorBounds;
    private final Rect mRoundedCornerFrame;
    private RoundedCorners mRoundedCorners;
    private final InsetsSource[] mSources;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface InternalInsetsSide {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface InternalInsetsType {
    }

    public InsetsState() {
        this.mSources = new InsetsSource[24];
        this.mDisplayFrame = new Rect();
        this.mDisplayCutout = new DisplayCutout.ParcelableWrapper();
        this.mRoundedCornerFrame = new Rect();
        this.mRoundedCorners = RoundedCorners.NO_ROUNDED_CORNERS;
        this.mPrivacyIndicatorBounds = new PrivacyIndicatorBounds();
    }

    public InsetsState(InsetsState copy) {
        this.mSources = new InsetsSource[24];
        this.mDisplayFrame = new Rect();
        this.mDisplayCutout = new DisplayCutout.ParcelableWrapper();
        this.mRoundedCornerFrame = new Rect();
        this.mRoundedCorners = RoundedCorners.NO_ROUNDED_CORNERS;
        this.mPrivacyIndicatorBounds = new PrivacyIndicatorBounds();
        set(copy);
    }

    public InsetsState(InsetsState copy, boolean copySources) {
        this.mSources = new InsetsSource[24];
        this.mDisplayFrame = new Rect();
        this.mDisplayCutout = new DisplayCutout.ParcelableWrapper();
        this.mRoundedCornerFrame = new Rect();
        this.mRoundedCorners = RoundedCorners.NO_ROUNDED_CORNERS;
        this.mPrivacyIndicatorBounds = new PrivacyIndicatorBounds();
        set(copy, copySources);
    }

    public WindowInsets calculateInsets(Rect frame, InsetsState ignoringVisibilityState, boolean isScreenRound, boolean alwaysConsumeSystemBars, int legacySoftInputMode, int legacyWindowFlags, int legacySystemUiFlags, int windowType, int windowingMode, SparseIntArray typeSideMap) {
        int compatInsetsTypes;
        InsetsSource ignoringVisibilitySource;
        Insets[] typeInsetsMap = new Insets[9];
        Insets[] typeMaxInsetsMap = new Insets[9];
        boolean[] typeVisibilityMap = new boolean[9];
        Rect relativeFrame = new Rect(frame);
        Rect relativeFrameMax = new Rect(frame);
        for (int type = 0; type <= 23; type++) {
            InsetsSource source = this.mSources[type];
            if (source == null) {
                int index = WindowInsets.Type.indexOf(toPublicType(type));
                if (typeInsetsMap[index] == null) {
                    typeInsetsMap[index] = Insets.NONE;
                }
            } else {
                processSource(source, relativeFrame, false, typeInsetsMap, typeSideMap, typeVisibilityMap);
                if (source.getType() != 19) {
                    if (ignoringVisibilityState != null) {
                        ignoringVisibilitySource = ignoringVisibilityState.getSource(type);
                    } else {
                        ignoringVisibilitySource = source;
                    }
                    if (ignoringVisibilitySource != null) {
                        processSource(ignoringVisibilitySource, relativeFrameMax, true, typeMaxInsetsMap, null, null);
                    }
                }
            }
        }
        int softInputAdjustMode = legacySoftInputMode & 240;
        int compatInsetsTypes2 = WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout();
        if (softInputAdjustMode == 16) {
            compatInsetsTypes2 |= WindowInsets.Type.ime();
        }
        if ((legacyWindowFlags & 1024) != 0) {
            compatInsetsTypes2 &= ~WindowInsets.Type.statusBars();
        }
        if (!clearsCompatInsets(windowType, legacyWindowFlags, windowingMode)) {
            compatInsetsTypes = compatInsetsTypes2;
        } else {
            compatInsetsTypes = 0;
        }
        return new WindowInsets(typeInsetsMap, typeMaxInsetsMap, typeVisibilityMap, isScreenRound, alwaysConsumeSystemBars, calculateRelativeCutout(frame), calculateRelativeRoundedCorners(frame), calculateRelativePrivacyIndicatorBounds(frame), compatInsetsTypes, (legacySystemUiFlags & 256) != 0);
    }

    private DisplayCutout calculateRelativeCutout(Rect frame) {
        DisplayCutout raw = this.mDisplayCutout.get();
        if (this.mDisplayFrame.equals(frame)) {
            return raw;
        }
        if (frame == null) {
            return DisplayCutout.NO_CUTOUT;
        }
        int insetLeft = frame.left - this.mDisplayFrame.left;
        int insetTop = frame.top - this.mDisplayFrame.top;
        int insetRight = this.mDisplayFrame.right - frame.right;
        int insetBottom = this.mDisplayFrame.bottom - frame.bottom;
        if (insetLeft >= raw.getSafeInsetLeft() && insetTop >= raw.getSafeInsetTop() && insetRight >= raw.getSafeInsetRight() && insetBottom >= raw.getSafeInsetBottom()) {
            return DisplayCutout.NO_CUTOUT;
        }
        return raw.inset(insetLeft, insetTop, insetRight, insetBottom);
    }

    private RoundedCorners calculateRelativeRoundedCorners(Rect frame) {
        InsetsSource[] insetsSourceArr;
        if (frame == null) {
            return RoundedCorners.NO_ROUNDED_CORNERS;
        }
        Rect roundedCornerFrame = new Rect(this.mRoundedCornerFrame);
        for (InsetsSource source : this.mSources) {
            if (source != null && source.getInsetsRoundedCornerFrame()) {
                Insets insets = source.calculateInsets(roundedCornerFrame, false);
                roundedCornerFrame.inset(insets);
            }
        }
        if (!roundedCornerFrame.isEmpty() && !roundedCornerFrame.equals(this.mDisplayFrame)) {
            return this.mRoundedCorners.insetWithFrame(frame, roundedCornerFrame);
        }
        if (this.mDisplayFrame.equals(frame)) {
            return this.mRoundedCorners;
        }
        int insetLeft = frame.left - this.mDisplayFrame.left;
        int insetTop = frame.top - this.mDisplayFrame.top;
        int insetRight = this.mDisplayFrame.right - frame.right;
        int insetBottom = this.mDisplayFrame.bottom - frame.bottom;
        return this.mRoundedCorners.inset(insetLeft, insetTop, insetRight, insetBottom);
    }

    private PrivacyIndicatorBounds calculateRelativePrivacyIndicatorBounds(Rect frame) {
        if (this.mDisplayFrame.equals(frame)) {
            return this.mPrivacyIndicatorBounds;
        }
        if (frame == null) {
            return null;
        }
        int insetLeft = frame.left - this.mDisplayFrame.left;
        int insetTop = frame.top - this.mDisplayFrame.top;
        int insetRight = this.mDisplayFrame.right - frame.right;
        int insetBottom = this.mDisplayFrame.bottom - frame.bottom;
        return this.mPrivacyIndicatorBounds.inset(insetLeft, insetTop, insetRight, insetBottom);
    }

    public Insets calculateInsets(Rect frame, int types, boolean ignoreVisibility) {
        Insets insets = Insets.NONE;
        for (int type = 0; type <= 23; type++) {
            InsetsSource source = this.mSources[type];
            if (source != null) {
                int publicType = toPublicType(type);
                if ((publicType & types) != 0) {
                    insets = Insets.max(source.calculateInsets(frame, ignoreVisibility), insets);
                }
            }
        }
        return insets;
    }

    public Insets calculateInsets(Rect frame, int types, InsetsVisibilities overrideVisibilities) {
        Insets insets = Insets.NONE;
        for (int type = 0; type <= 23; type++) {
            InsetsSource source = this.mSources[type];
            if (source != null) {
                int publicType = toPublicType(type);
                if ((publicType & types) != 0 && overrideVisibilities.getVisibility(type)) {
                    insets = Insets.max(source.calculateInsets(frame, true), insets);
                }
            }
        }
        return insets;
    }

    public Insets calculateVisibleInsets(Rect frame, int windowType, int windowingMode, int softInputMode, int windowFlags) {
        int visibleInsetsTypes;
        if (clearsCompatInsets(windowType, windowFlags, windowingMode)) {
            return Insets.NONE;
        }
        int softInputAdjustMode = softInputMode & 240;
        if (softInputAdjustMode != 48) {
            visibleInsetsTypes = WindowInsets.Type.systemBars() | WindowInsets.Type.ime();
        } else {
            visibleInsetsTypes = WindowInsets.Type.systemBars();
        }
        Insets insets = Insets.NONE;
        for (int type = 0; type <= 23; type++) {
            InsetsSource source = this.mSources[type];
            if (source != null) {
                int publicType = toPublicType(type);
                if ((publicType & visibleInsetsTypes) != 0) {
                    insets = Insets.max(source.calculateVisibleInsets(frame), insets);
                }
            }
        }
        return insets;
    }

    public int calculateUncontrollableInsetsFromFrame(Rect frame) {
        int blocked = 0;
        for (int type = 0; type <= 23; type++) {
            InsetsSource source = this.mSources[type];
            if (source != null && !canControlSide(frame, getInsetSide(source.calculateInsets(frame, true)))) {
                blocked |= toPublicType(type);
            }
        }
        return blocked;
    }

    private boolean canControlSide(Rect frame, int side) {
        switch (side) {
            case 0:
            case 2:
                return frame.left == this.mDisplayFrame.left && frame.right == this.mDisplayFrame.right;
            case 1:
            case 3:
                return frame.top == this.mDisplayFrame.top && frame.bottom == this.mDisplayFrame.bottom;
            case 4:
                return true;
            default:
                return false;
        }
    }

    private void processSource(InsetsSource source, Rect relativeFrame, boolean ignoreVisibility, Insets[] typeInsetsMap, SparseIntArray typeSideMap, boolean[] typeVisibilityMap) {
        Insets insets = source.calculateInsets(relativeFrame, ignoreVisibility);
        int type = toPublicType(source.getType());
        processSourceAsPublicType(source, typeInsetsMap, typeSideMap, typeVisibilityMap, insets, type);
        if (type == 32) {
            processSourceAsPublicType(source, typeInsetsMap, typeSideMap, typeVisibilityMap, insets, 16);
        }
        if (type == 4) {
            processSourceAsPublicType(source, typeInsetsMap, typeSideMap, typeVisibilityMap, insets, 16);
            processSourceAsPublicType(source, typeInsetsMap, typeSideMap, typeVisibilityMap, insets, 32);
            processSourceAsPublicType(source, typeInsetsMap, typeSideMap, typeVisibilityMap, insets, 64);
        }
    }

    private void processSourceAsPublicType(InsetsSource source, Insets[] typeInsetsMap, SparseIntArray typeSideMap, boolean[] typeVisibilityMap, Insets insets, int type) {
        int insetSide;
        int index = WindowInsets.Type.indexOf(type);
        Insets existing = typeInsetsMap[index];
        if (existing == null) {
            typeInsetsMap[index] = insets;
        } else {
            typeInsetsMap[index] = Insets.max(existing, insets);
        }
        if (typeVisibilityMap != null) {
            typeVisibilityMap[index] = source.isVisible();
        }
        if (typeSideMap != null && (insetSide = getInsetSide(insets)) != 5) {
            if (source.getType() == 1 && insetSide == 4) {
                insetSide = 3;
            }
            typeSideMap.put(source.getType(), insetSide);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getInsetSide(Insets insets) {
        if (Insets.NONE.equals(insets)) {
            return 4;
        }
        if (insets.left != 0) {
            return 0;
        }
        if (insets.top != 0) {
            return 1;
        }
        if (insets.right != 0) {
            return 2;
        }
        if (insets.bottom != 0) {
            return 3;
        }
        return 5;
    }

    public InsetsSource getSource(int type) {
        InsetsSource source = this.mSources[type];
        if (source != null) {
            return source;
        }
        InsetsSource source2 = new InsetsSource(type);
        this.mSources[type] = source2;
        return source2;
    }

    public InsetsSource peekSource(int type) {
        return this.mSources[type];
    }

    public boolean getSourceOrDefaultVisibility(int type) {
        InsetsSource source = this.mSources[type];
        return source != null ? source.isVisible() : getDefaultVisibility(type);
    }

    public void setDisplayFrame(Rect frame) {
        this.mDisplayFrame.set(frame);
    }

    public Rect getDisplayFrame() {
        return this.mDisplayFrame;
    }

    public void setDisplayCutout(DisplayCutout cutout) {
        this.mDisplayCutout.set(cutout);
    }

    public DisplayCutout getDisplayCutout() {
        return this.mDisplayCutout.get();
    }

    public void getDisplayCutoutSafe(Rect outBounds) {
        outBounds.set(-100000, -100000, 100000, 100000);
        DisplayCutout cutout = this.mDisplayCutout.get();
        Rect displayFrame = this.mDisplayFrame;
        if (!cutout.isEmpty()) {
            if (cutout.getSafeInsetLeft() > 0) {
                outBounds.left = displayFrame.left + cutout.getSafeInsetLeft();
            }
            if (cutout.getSafeInsetTop() > 0) {
                outBounds.top = displayFrame.top + cutout.getSafeInsetTop();
            }
            if (cutout.getSafeInsetRight() > 0) {
                outBounds.right = displayFrame.right - cutout.getSafeInsetRight();
            }
            if (cutout.getSafeInsetBottom() > 0) {
                outBounds.bottom = displayFrame.bottom - cutout.getSafeInsetBottom();
            }
        }
    }

    public void setRoundedCorners(RoundedCorners roundedCorners) {
        this.mRoundedCorners = roundedCorners;
    }

    public RoundedCorners getRoundedCorners() {
        return this.mRoundedCorners;
    }

    public void setRoundedCornerFrame(Rect frame) {
        this.mRoundedCornerFrame.set(frame);
    }

    public void setPrivacyIndicatorBounds(PrivacyIndicatorBounds bounds) {
        this.mPrivacyIndicatorBounds = bounds;
    }

    public PrivacyIndicatorBounds getPrivacyIndicatorBounds() {
        return this.mPrivacyIndicatorBounds;
    }

    public boolean removeSource(int type) {
        InsetsSource[] insetsSourceArr = this.mSources;
        if (insetsSourceArr[type] == null) {
            return false;
        }
        insetsSourceArr[type] = null;
        return true;
    }

    public void setSourceVisible(int type, boolean visible) {
        InsetsSource source = this.mSources[type];
        if (source != null) {
            source.setVisible(visible);
        }
    }

    public void scale(float scale) {
        this.mDisplayFrame.scale(scale);
        this.mDisplayCutout.scale(scale);
        this.mRoundedCorners = this.mRoundedCorners.scale(scale);
        this.mRoundedCornerFrame.scale(scale);
        this.mPrivacyIndicatorBounds = this.mPrivacyIndicatorBounds.scale(scale);
        for (int i = 0; i < 24; i++) {
            InsetsSource source = this.mSources[i];
            if (source != null) {
                source.getFrame().scale(scale);
                Rect visibleFrame = source.getVisibleFrame();
                if (visibleFrame != null) {
                    visibleFrame.scale(scale);
                }
            }
        }
    }

    public void set(InsetsState other) {
        set(other, false);
    }

    public void set(InsetsState other, boolean copySources) {
        this.mDisplayFrame.set(other.mDisplayFrame);
        this.mDisplayCutout.set(other.mDisplayCutout);
        this.mRoundedCorners = other.getRoundedCorners();
        this.mRoundedCornerFrame.set(other.mRoundedCornerFrame);
        this.mPrivacyIndicatorBounds = other.getPrivacyIndicatorBounds();
        if (copySources) {
            for (int i = 0; i < 24; i++) {
                InsetsSource source = other.mSources[i];
                this.mSources[i] = source != null ? new InsetsSource(source) : null;
            }
            return;
        }
        for (int i2 = 0; i2 < 24; i2++) {
            this.mSources[i2] = other.mSources[i2];
        }
    }

    public void set(InsetsState other, int types) {
        this.mDisplayFrame.set(other.mDisplayFrame);
        this.mDisplayCutout.set(other.mDisplayCutout);
        this.mRoundedCorners = other.getRoundedCorners();
        this.mRoundedCornerFrame.set(other.mRoundedCornerFrame);
        this.mPrivacyIndicatorBounds = other.getPrivacyIndicatorBounds();
        ArraySet<Integer> t = toInternalType(types);
        for (int i = t.size() - 1; i >= 0; i--) {
            int type = t.valueAt(i).intValue();
            this.mSources[type] = other.mSources[type];
        }
    }

    public void addSource(InsetsSource source) {
        this.mSources[source.getType()] = source;
    }

    public static boolean clearsCompatInsets(int windowType, int windowFlags, int windowingMode) {
        return ((windowFlags & 512) == 0 || windowType == 2013 || windowType == 2010 || WindowConfiguration.inMultiWindowMode(windowingMode)) ? false : true;
    }

    public static ArraySet<Integer> toInternalType(int types) {
        ArraySet<Integer> result = new ArraySet<>();
        if ((types & 1) != 0) {
            result.add(0);
            result.add(20);
        }
        if ((types & 2) != 0) {
            result.add(1);
            result.add(21);
            result.add(22);
            result.add(23);
        }
        if ((types & 4) != 0) {
            result.add(2);
        }
        if ((types & 16) != 0) {
            result.add(5);
            result.add(3);
            result.add(6);
            result.add(4);
        }
        if ((types & 32) != 0) {
            result.add(9);
            result.add(7);
            result.add(10);
            result.add(8);
        }
        if ((types & 128) != 0) {
            result.add(11);
            result.add(12);
            result.add(13);
            result.add(14);
        }
        if ((types & 8) != 0) {
            result.add(19);
        }
        return result;
    }

    public static int toPublicType(int type) {
        switch (type) {
            case 0:
            case 20:
                return 1;
            case 1:
            case 21:
            case 22:
            case 23:
                return 2;
            case 2:
                return 4;
            case 3:
            case 4:
            case 5:
            case 6:
                return 16;
            case 7:
            case 8:
            case 9:
            case 10:
                return 32;
            case 11:
            case 12:
            case 13:
            case 14:
                return 128;
            case 15:
            case 16:
            case 17:
            case 18:
                return 64;
            case 19:
                return 8;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    public static boolean getDefaultVisibility(int type) {
        return type != 19;
    }

    public static boolean containsType(int[] types, int type) {
        if (types == null) {
            return false;
        }
        for (int t : types) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    public void dump(String prefix, PrintWriter pw) {
        String newPrefix = prefix + "  ";
        pw.println(prefix + "InsetsState");
        pw.println(newPrefix + "mDisplayFrame=" + this.mDisplayFrame);
        pw.println(newPrefix + "mDisplayCutout=" + this.mDisplayCutout.get());
        pw.println(newPrefix + "mRoundedCorners=" + this.mRoundedCorners);
        pw.println(newPrefix + "mRoundedCornerFrame=" + this.mRoundedCornerFrame);
        pw.println(newPrefix + "mPrivacyIndicatorBounds=" + this.mPrivacyIndicatorBounds);
        for (int i = 0; i < 24; i++) {
            InsetsSource source = this.mSources[i];
            if (source != null) {
                source.dump(newPrefix + "  ", pw);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        InsetsSource source = this.mSources[19];
        if (source != null) {
            source.dumpDebug(proto, 2246267895809L);
        }
        this.mDisplayFrame.dumpDebug(proto, 1146756268034L);
        this.mDisplayCutout.get().dumpDebug(proto, 1146756268035L);
        proto.end(token);
    }

    public static String typeToString(int type) {
        switch (type) {
            case 0:
                return "ITYPE_STATUS_BAR";
            case 1:
                return "ITYPE_NAVIGATION_BAR";
            case 2:
                return "ITYPE_CAPTION_BAR";
            case 3:
                return "ITYPE_TOP_GESTURES";
            case 4:
                return "ITYPE_BOTTOM_GESTURES";
            case 5:
                return "ITYPE_LEFT_GESTURES";
            case 6:
                return "ITYPE_RIGHT_GESTURES";
            case 7:
                return "ITYPE_TOP_MANDATORY_GESTURES";
            case 8:
                return "ITYPE_BOTTOM_MANDATORY_GESTURES";
            case 9:
                return "ITYPE_LEFT_MANDATORY_GESTURES";
            case 10:
                return "ITYPE_RIGHT_MANDATORY_GESTURES";
            case 11:
                return "ITYPE_LEFT_DISPLAY_CUTOUT";
            case 12:
                return "ITYPE_TOP_DISPLAY_CUTOUT";
            case 13:
                return "ITYPE_RIGHT_DISPLAY_CUTOUT";
            case 14:
                return "ITYPE_BOTTOM_DISPLAY_CUTOUT";
            case 15:
                return "ITYPE_LEFT_TAPPABLE_ELEMENT";
            case 16:
                return "ITYPE_TOP_TAPPABLE_ELEMENT";
            case 17:
                return "ITYPE_RIGHT_TAPPABLE_ELEMENT";
            case 18:
                return "ITYPE_BOTTOM_TAPPABLE_ELEMENT";
            case 19:
                return "ITYPE_IME";
            case 20:
                return "ITYPE_CLIMATE_BAR";
            case 21:
                return "ITYPE_EXTRA_NAVIGATION_BAR";
            case 22:
                return "ITYPE_LOCAL_NAVIGATION_BAR_1";
            case 23:
                return "ITYPE_LOCAL_NAVIGATION_BAR_2";
            default:
                return "ITYPE_UNKNOWN_" + type;
        }
    }

    public boolean equals(Object o) {
        return equals(o, false, false);
    }

    public boolean equals(Object o, boolean excludingCaptionInsets, boolean excludeInvisibleImeFrames) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InsetsState state = (InsetsState) o;
        if (!this.mDisplayFrame.equals(state.mDisplayFrame) || !this.mDisplayCutout.equals(state.mDisplayCutout) || !this.mRoundedCorners.equals(state.mRoundedCorners) || !this.mRoundedCornerFrame.equals(state.mRoundedCornerFrame) || !this.mPrivacyIndicatorBounds.equals(state.mPrivacyIndicatorBounds)) {
            return false;
        }
        for (int i = 0; i < 24; i++) {
            if (!excludingCaptionInsets || i != 2) {
                InsetsSource source = this.mSources[i];
                InsetsSource otherSource = state.mSources[i];
                if ((source != null || otherSource != null) && (source == null || otherSource == null || !otherSource.equals(source, excludeInvisibleImeFrames))) {
                    return false;
                }
            }
        }
        return true;
    }

    public int hashCode() {
        return Objects.hash(this.mDisplayFrame, this.mDisplayCutout, Integer.valueOf(Arrays.hashCode(this.mSources)), this.mRoundedCorners, this.mPrivacyIndicatorBounds, this.mRoundedCornerFrame);
    }

    public InsetsState(Parcel in) {
        this.mSources = new InsetsSource[24];
        this.mDisplayFrame = new Rect();
        this.mDisplayCutout = new DisplayCutout.ParcelableWrapper();
        this.mRoundedCornerFrame = new Rect();
        this.mRoundedCorners = RoundedCorners.NO_ROUNDED_CORNERS;
        this.mPrivacyIndicatorBounds = new PrivacyIndicatorBounds();
        readFromParcel(in);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        this.mDisplayFrame.writeToParcel(dest, flags);
        this.mDisplayCutout.writeToParcel(dest, flags);
        dest.writeTypedArray(this.mSources, 0);
        dest.writeTypedObject(this.mRoundedCorners, flags);
        this.mRoundedCornerFrame.writeToParcel(dest, flags);
        dest.writeTypedObject(this.mPrivacyIndicatorBounds, flags);
    }

    public void readFromParcel(Parcel in) {
        this.mDisplayFrame.readFromParcel(in);
        this.mDisplayCutout.readFromParcel(in);
        in.readTypedArray(this.mSources, InsetsSource.CREATOR);
        this.mRoundedCorners = (RoundedCorners) in.readTypedObject(RoundedCorners.CREATOR);
        this.mRoundedCornerFrame.readFromParcel(in);
        this.mPrivacyIndicatorBounds = (PrivacyIndicatorBounds) in.readTypedObject(PrivacyIndicatorBounds.CREATOR);
    }

    public String toString() {
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < 24; i++) {
            InsetsSource source = this.mSources[i];
            if (source != null) {
                joiner.add(source.toString());
            }
        }
        return "InsetsState: {mDisplayFrame=" + this.mDisplayFrame + ", mDisplayCutout=" + this.mDisplayCutout + ", mRoundedCorners=" + this.mRoundedCorners + "  mRoundedCornerFrame=" + this.mRoundedCornerFrame + ", mPrivacyIndicatorBounds=" + this.mPrivacyIndicatorBounds + ", mSources= { " + joiner + " }";
    }
}
