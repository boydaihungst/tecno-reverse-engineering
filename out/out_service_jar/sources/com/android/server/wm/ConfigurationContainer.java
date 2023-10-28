package com.android.server.wm;

import android.app.WindowConfiguration;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.LocaleList;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.server.wm.ConfigurationContainer;
import java.io.PrintWriter;
import java.util.ArrayList;
/* loaded from: classes2.dex */
public abstract class ConfigurationContainer<E extends ConfigurationContainer> {
    static final int BOUNDS_CHANGE_NONE = 0;
    static final int BOUNDS_CHANGE_POSITION = 1;
    static final int BOUNDS_CHANGE_SIZE = 2;
    private boolean mHasOverrideConfiguration;
    private boolean mOSFullDialog;
    private Rect mReturnBounds = new Rect();
    private Configuration mRequestedOverrideConfiguration = new Configuration();
    private Configuration mResolvedOverrideConfiguration = new Configuration();
    private Configuration mFullConfiguration = new Configuration();
    private Configuration mMergedOverrideConfiguration = new Configuration();
    private ArrayList<ConfigurationContainerListener> mChangeListeners = new ArrayList<>();
    private final Configuration mRequestsTmpConfig = new Configuration();
    private final Configuration mResolvedTmpConfig = new Configuration();
    private final Rect mTmpRect = new Rect();

    protected abstract E getChildAt(int i);

    protected abstract int getChildCount();

    protected abstract ConfigurationContainer getParent();

    public Configuration getConfiguration() {
        return this.mFullConfiguration;
    }

    public void onConfigurationChanged(Configuration newParentConfig) {
        this.mResolvedTmpConfig.setTo(this.mResolvedOverrideConfiguration);
        resolveOverrideConfiguration(newParentConfig);
        this.mFullConfiguration.setTo(newParentConfig);
        this.mFullConfiguration.windowConfiguration.unsetAlwaysOnTop();
        this.mFullConfiguration.updateFrom(this.mResolvedOverrideConfiguration);
        if (isOSFullDialog()) {
            this.mFullConfiguration.windowConfiguration.setBounds(this.mFullConfiguration.windowConfiguration.getMaxBounds());
            this.mFullConfiguration.windowConfiguration.setAppBounds(this.mFullConfiguration.windowConfiguration.getMaxBounds());
        }
        onMergedOverrideConfigurationChanged();
        if (!this.mResolvedTmpConfig.equals(this.mResolvedOverrideConfiguration)) {
            for (int i = this.mChangeListeners.size() - 1; i >= 0; i--) {
                this.mChangeListeners.get(i).onRequestedOverrideConfigurationChanged(this.mResolvedOverrideConfiguration);
            }
        }
        for (int i2 = this.mChangeListeners.size() - 1; i2 >= 0; i2--) {
            this.mChangeListeners.get(i2).onMergedOverrideConfigurationChanged(this.mMergedOverrideConfiguration);
        }
        int i3 = getChildCount();
        for (int i4 = i3 - 1; i4 >= 0; i4--) {
            dispatchConfigurationToChild(getChildAt(i4), this.mFullConfiguration);
        }
    }

    void dispatchConfigurationToChild(E child, Configuration config) {
        child.onConfigurationChanged(config);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resolveOverrideConfiguration(Configuration newParentConfig) {
        this.mResolvedOverrideConfiguration.setTo(this.mRequestedOverrideConfiguration);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasRequestedOverrideConfiguration() {
        return this.mHasOverrideConfiguration;
    }

    public Configuration getRequestedOverrideConfiguration() {
        return this.mRequestedOverrideConfiguration;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Configuration getResolvedOverrideConfiguration() {
        return this.mResolvedOverrideConfiguration;
    }

    public void onRequestedOverrideConfigurationChanged(Configuration overrideConfiguration) {
        updateRequestedOverrideConfiguration(overrideConfiguration);
        ConfigurationContainer parent = getParent();
        onConfigurationChanged(parent != null ? parent.getConfiguration() : Configuration.EMPTY);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateRequestedOverrideConfiguration(Configuration overrideConfiguration) {
        this.mHasOverrideConfiguration = !Configuration.EMPTY.equals(overrideConfiguration);
        this.mRequestedOverrideConfiguration.setTo(overrideConfiguration);
        Rect newBounds = this.mRequestedOverrideConfiguration.windowConfiguration.getBounds();
        if (this.mHasOverrideConfiguration && providesMaxBounds() && diffRequestedOverrideMaxBounds(newBounds) != 0) {
            this.mRequestedOverrideConfiguration.windowConfiguration.setMaxBounds(newBounds);
        }
    }

    public Configuration getMergedOverrideConfiguration() {
        return this.mMergedOverrideConfiguration;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onMergedOverrideConfigurationChanged() {
        ConfigurationContainer parent = getParent();
        if (parent != null) {
            this.mMergedOverrideConfiguration.setTo(parent.getMergedOverrideConfiguration());
            this.mMergedOverrideConfiguration.windowConfiguration.unsetAlwaysOnTop();
            this.mMergedOverrideConfiguration.updateFrom(this.mResolvedOverrideConfiguration);
            if ((parent instanceof Task) && SplitScreenHelper.getParentTaskInSplitScreenTaskStack((Task) parent) != null && this.mMergedOverrideConfiguration.windowConfiguration.getWindowingMode() != 6) {
                Slog.w("Configuration", "in split-screen, but windowing mode is not multi-window");
                this.mMergedOverrideConfiguration.windowConfiguration.setWindowingMode(parent.getMergedOverrideConfiguration().windowConfiguration.getWindowingMode());
            }
        } else {
            this.mMergedOverrideConfiguration.setTo(this.mResolvedOverrideConfiguration);
        }
        for (int i = getChildCount() - 1; i >= 0; i--) {
            ConfigurationContainer child = getChildAt(i);
            child.onMergedOverrideConfigurationChanged();
        }
    }

    public boolean matchParentBounds() {
        return getResolvedOverrideBounds().isEmpty();
    }

    public boolean equivalentRequestedOverrideBounds(Rect bounds) {
        return equivalentBounds(getRequestedOverrideBounds(), bounds);
    }

    public boolean equivalentRequestedOverrideMaxBounds(Rect bounds) {
        return equivalentBounds(getRequestedOverrideMaxBounds(), bounds);
    }

    public static boolean equivalentBounds(Rect bounds, Rect other) {
        return bounds == other || (bounds != null && (bounds.equals(other) || (bounds.isEmpty() && other == null))) || (other != null && other.isEmpty() && bounds == null);
    }

    public Rect getBounds() {
        this.mReturnBounds.set(getConfiguration().windowConfiguration.getBounds());
        return this.mReturnBounds;
    }

    public void getBounds(Rect outBounds) {
        outBounds.set(getBounds());
    }

    public Rect getMaxBounds() {
        this.mReturnBounds.set(getConfiguration().windowConfiguration.getMaxBounds());
        return this.mReturnBounds;
    }

    public void getPosition(Point out) {
        Rect bounds = getBounds();
        out.set(bounds.left, bounds.top);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getResolvedOverrideBounds() {
        this.mReturnBounds.set(getResolvedOverrideConfiguration().windowConfiguration.getBounds());
        return this.mReturnBounds;
    }

    public Rect getRequestedOverrideBounds() {
        this.mReturnBounds.set(getRequestedOverrideConfiguration().windowConfiguration.getBounds());
        return this.mReturnBounds;
    }

    public Rect getRequestedOverrideMaxBounds() {
        this.mReturnBounds.set(getRequestedOverrideConfiguration().windowConfiguration.getMaxBounds());
        return this.mReturnBounds;
    }

    public boolean hasOverrideBounds() {
        return !getRequestedOverrideBounds().isEmpty();
    }

    public void getRequestedOverrideBounds(Rect outBounds) {
        outBounds.set(getRequestedOverrideBounds());
    }

    public int setBounds(Rect bounds) {
        int boundsChange = diffRequestedOverrideBounds(bounds);
        boolean overrideMaxBounds = providesMaxBounds() && diffRequestedOverrideMaxBounds(bounds) != 0;
        if (boundsChange == 0 && !overrideMaxBounds) {
            return boundsChange;
        }
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setBounds(bounds);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
        return boundsChange;
    }

    public int setBounds(int left, int top, int right, int bottom) {
        this.mTmpRect.set(left, top, right, bottom);
        return setBounds(this.mTmpRect);
    }

    protected boolean providesMaxBounds() {
        return false;
    }

    int diffRequestedOverrideMaxBounds(Rect bounds) {
        if (equivalentRequestedOverrideMaxBounds(bounds)) {
            return 0;
        }
        int boundsChange = 0;
        Rect existingBounds = getRequestedOverrideMaxBounds();
        if (bounds == null || existingBounds.left != bounds.left || existingBounds.top != bounds.top) {
            boundsChange = 0 | 1;
        }
        if (bounds == null || existingBounds.width() != bounds.width() || existingBounds.height() != bounds.height()) {
            return boundsChange | 2;
        }
        return boundsChange;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int diffRequestedOverrideBounds(Rect bounds) {
        if (equivalentRequestedOverrideBounds(bounds)) {
            return 0;
        }
        int boundsChange = 0;
        Rect existingBounds = getRequestedOverrideBounds();
        if (bounds == null || existingBounds.left != bounds.left || existingBounds.top != bounds.top) {
            boundsChange = 0 | 1;
        }
        if (bounds == null || existingBounds.width() != bounds.width() || existingBounds.height() != bounds.height()) {
            return boundsChange | 2;
        }
        return boundsChange;
    }

    public WindowConfiguration getWindowConfiguration() {
        return this.mFullConfiguration.windowConfiguration;
    }

    public int getWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.getWindowingMode();
    }

    public int getRequestedOverrideWindowingMode() {
        return this.mRequestedOverrideConfiguration.windowConfiguration.getWindowingMode();
    }

    public void setWindowingMode(int windowingMode) {
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setWindowingMode(windowingMode);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setAlwaysOnTop(alwaysOnTop);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
    }

    void setDisplayWindowingMode(int windowingMode) {
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setDisplayWindowingMode(windowingMode);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
    }

    public boolean inMultiWindowMode() {
        int windowingMode = this.mFullConfiguration.windowConfiguration.getWindowingMode();
        return WindowConfiguration.inMultiWindowMode(windowingMode);
    }

    public boolean supportsSplitScreenWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.supportSplitScreenWindowingMode();
    }

    public boolean inPinnedWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.getWindowingMode() == 2;
    }

    public boolean inFreeformWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.getWindowingMode() == 5;
    }

    public int getActivityType() {
        return this.mFullConfiguration.windowConfiguration.getActivityType();
    }

    public void setActivityType(int activityType) {
        int currentActivityType = getActivityType();
        if (currentActivityType == activityType) {
            return;
        }
        if (currentActivityType != 0) {
            throw new IllegalStateException("Can't change activity type once set: " + this + " activityType=" + WindowConfiguration.activityTypeToString(activityType));
        }
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setActivityType(activityType);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
    }

    public boolean isActivityTypeHome() {
        return getActivityType() == 2;
    }

    public boolean isActivityTypeRecents() {
        return getActivityType() == 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final boolean isActivityTypeHomeOrRecents() {
        int activityType = getActivityType();
        return activityType == 2 || activityType == 3;
    }

    public boolean isActivityTypeAssistant() {
        return getActivityType() == 4;
    }

    public boolean applyAppSpecificConfig(Integer nightMode, LocaleList locales) {
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        boolean newNightModeSet = nightMode != null && setOverrideNightMode(this.mRequestsTmpConfig, nightMode.intValue());
        boolean newLocalesSet = locales != null && setOverrideLocales(this.mRequestsTmpConfig, locales);
        if (newNightModeSet || newLocalesSet) {
            onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
        }
        return newNightModeSet || newLocalesSet;
    }

    private boolean setOverrideNightMode(Configuration requestsTmpConfig, int nightMode) {
        int currentUiMode = this.mRequestedOverrideConfiguration.uiMode;
        int currentNightMode = currentUiMode & 48;
        int validNightMode = nightMode & 48;
        if (currentNightMode == validNightMode) {
            return false;
        }
        requestsTmpConfig.uiMode = (currentUiMode & (-49)) | validNightMode;
        return true;
    }

    private boolean setOverrideLocales(Configuration requestsTmpConfig, LocaleList overrideLocales) {
        if (this.mRequestedOverrideConfiguration.getLocales().equals(overrideLocales)) {
            return false;
        }
        requestsTmpConfig.setLocales(overrideLocales);
        requestsTmpConfig.userSetLocale = true;
        return true;
    }

    public boolean isActivityTypeDream() {
        return getActivityType() == 5;
    }

    public boolean isActivityTypeStandard() {
        return getActivityType() == 1;
    }

    public boolean isActivityTypeStandardOrUndefined() {
        int activityType = getActivityType();
        return activityType == 1 || activityType == 0;
    }

    public static boolean isCompatibleActivityType(int currentType, int otherType) {
        if (currentType == otherType) {
            return true;
        }
        if (currentType == 4) {
            return false;
        }
        return currentType == 0 || otherType == 0;
    }

    public boolean isCompatible(int windowingMode, int activityType) {
        int thisActivityType = getActivityType();
        int thisWindowingMode = getWindowingMode();
        boolean sameActivityType = thisActivityType == activityType;
        boolean sameWindowingMode = thisWindowingMode == windowingMode;
        if (sameActivityType && sameWindowingMode) {
            return true;
        }
        if ((activityType != 0 && activityType != 1) || !isActivityTypeStandardOrUndefined()) {
            return sameActivityType;
        }
        return sameWindowingMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerConfigurationChangeListener(ConfigurationContainerListener listener) {
        registerConfigurationChangeListener(listener, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerConfigurationChangeListener(ConfigurationContainerListener listener, boolean shouldDispatchConfig) {
        if (this.mChangeListeners.contains(listener)) {
            return;
        }
        this.mChangeListeners.add(listener);
        if (shouldDispatchConfig) {
            listener.onRequestedOverrideConfigurationChanged(this.mResolvedOverrideConfiguration);
            listener.onMergedOverrideConfigurationChanged(this.mMergedOverrideConfiguration);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterConfigurationChangeListener(ConfigurationContainerListener listener) {
        this.mChangeListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean containsListener(ConfigurationContainerListener listener) {
        return this.mChangeListeners.contains(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onParentChanged(ConfigurationContainer newParent, ConfigurationContainer oldParent) {
        if (newParent != null) {
            onConfigurationChanged(newParent.mFullConfiguration);
            onMergedOverrideConfigurationChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        long token = proto.start(fieldId);
        if (logLevel == 0 || this.mHasOverrideConfiguration) {
            this.mRequestedOverrideConfiguration.dumpDebug(proto, 1146756268033L, logLevel == 2);
        }
        if (logLevel == 0) {
            this.mFullConfiguration.dumpDebug(proto, 1146756268034L, false);
            this.mMergedOverrideConfiguration.dumpDebug(proto, 1146756268035L, false);
        }
        if (logLevel == 1) {
            dumpDebugWindowingMode(proto);
        }
        proto.end(token);
    }

    private void dumpDebugWindowingMode(ProtoOutputStream proto) {
        long fullConfigToken = proto.start(1146756268034L);
        long windowConfigToken = proto.start(1146756268051L);
        int windowingMode = this.mFullConfiguration.windowConfiguration.getWindowingMode();
        proto.write(1120986464258L, windowingMode);
        proto.end(windowConfigToken);
        proto.end(fullConfigToken);
    }

    public void dumpChildrenNames(PrintWriter pw, String prefix) {
        String childPrefix = prefix + " ";
        pw.println(getName() + " type=" + WindowConfiguration.activityTypeToString(getActivityType()) + " mode=" + WindowConfiguration.windowingModeToString(getWindowingMode()) + " override-mode=" + WindowConfiguration.windowingModeToString(getRequestedOverrideWindowingMode()) + " requested-bounds=" + getRequestedOverrideBounds().toShortString() + " bounds=" + getBounds().toShortString());
        for (int i = getChildCount() - 1; i >= 0; i--) {
            E cc = getChildAt(i);
            pw.print(childPrefix + "#" + i + " ");
            cc.dumpChildrenNames(pw, childPrefix);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getName() {
        return toString();
    }

    public boolean isAlwaysOnTop() {
        return this.mFullConfiguration.windowConfiguration.isAlwaysOnTop();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasChild() {
        return getChildCount() > 0;
    }

    public void setMultiWindowMode(int multiWindowMode) {
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setMultiWindowMode(multiWindowMode);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
    }

    public void setMultiWindowId(int multiWindowId) {
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setMultiWindowId(multiWindowId);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
    }

    public void setInLargeScreen(int inLargeScreen) {
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setInLargeScreen(inLargeScreen);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
    }

    public int getMultiWindowMode() {
        return this.mFullConfiguration.windowConfiguration.getMultiWindowingMode();
    }

    public void setOSFullDialog() {
        this.mOSFullDialog = true;
    }

    public boolean isOSFullDialog() {
        return this.mOSFullDialog;
    }
}
