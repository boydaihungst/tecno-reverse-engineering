package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityShortcutInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteCallbackList;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseIntArray;
import android.view.accessibility.IAccessibilityManagerClient;
import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.server.accessibility.AccessibilityManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AccessibilityUserState {
    private static final String LOG_TAG = AccessibilityUserState.class.getSimpleName();
    private boolean mAccessibilityFocusOnlyInActiveWindow;
    private boolean mBindInstantServiceAllowed;
    private Context mContext;
    private int mFocusColor;
    private final int mFocusColorDefaultValue;
    private int mFocusStrokeWidth;
    private final int mFocusStrokeWidthDefaultValue;
    private boolean mIsAudioDescriptionByDefaultRequested;
    private boolean mIsAutoclickEnabled;
    private boolean mIsDisplayMagnificationEnabled;
    private boolean mIsFilterKeyEventsEnabled;
    private boolean mIsPerformGesturesEnabled;
    private boolean mIsTextHighContrastEnabled;
    private boolean mIsTouchExplorationEnabled;
    private boolean mRequestMultiFingerGestures;
    private boolean mRequestTwoFingerPassthrough;
    private boolean mSendMotionEventsEnabled;
    private ComponentName mServiceChangingSoftKeyboardMode;
    private boolean mServiceHandlesDoubleTap;
    private final ServiceInfoChangeListener mServiceInfoChangeListener;
    private final boolean mSupportWindowMagnification;
    private String mTargetAssignedToAccessibilityButton;
    final int mUserId;
    private int mUserInteractiveUiTimeout;
    private int mUserNonInteractiveUiTimeout;
    final RemoteCallbackList<IAccessibilityManagerClient> mUserClients = new RemoteCallbackList<>();
    final ArrayList<AccessibilityServiceConnection> mBoundServices = new ArrayList<>();
    final Map<ComponentName, AccessibilityServiceConnection> mComponentNameToServiceMap = new HashMap();
    final List<AccessibilityServiceInfo> mInstalledServices = new ArrayList();
    final List<AccessibilityShortcutInfo> mInstalledShortcuts = new ArrayList();
    final Set<ComponentName> mBindingServices = new HashSet();
    final Set<ComponentName> mCrashedServices = new HashSet();
    final Set<ComponentName> mEnabledServices = new HashSet();
    final Set<ComponentName> mTouchExplorationGrantedServices = new HashSet();
    final ArraySet<String> mAccessibilityShortcutKeyTargets = new ArraySet<>();
    final ArraySet<String> mAccessibilityButtonTargets = new ArraySet<>();
    private int mNonInteractiveUiTimeout = 0;
    private int mInteractiveUiTimeout = 0;
    private int mLastSentClientState = -1;
    private final SparseIntArray mMagnificationModes = new SparseIntArray();
    private int mMagnificationCapabilities = 1;
    private boolean mMagnificationFollowTypingEnabled = true;
    private int mSoftKeyboardShowMode = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface ServiceInfoChangeListener {
        void onServiceInfoChangedLocked(AccessibilityUserState accessibilityUserState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isValidMagnificationModeLocked(int displayId) {
        int mode = getMagnificationModeLocked(displayId);
        return (this.mSupportWindowMagnification || mode != 2) && (this.mMagnificationCapabilities & mode) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AccessibilityUserState(int userId, Context context, ServiceInfoChangeListener serviceInfoChangeListener) {
        boolean z = false;
        this.mUserId = userId;
        this.mContext = context;
        this.mServiceInfoChangeListener = serviceInfoChangeListener;
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(17104908);
        this.mFocusStrokeWidthDefaultValue = dimensionPixelSize;
        int color = this.mContext.getResources().getColor(17170559);
        this.mFocusColorDefaultValue = color;
        this.mFocusStrokeWidth = dimensionPixelSize;
        this.mFocusColor = color;
        if (this.mContext.getResources().getBoolean(17891701) && this.mContext.getPackageManager().hasSystemFeature("android.software.window_magnification")) {
            z = true;
        }
        this.mSupportWindowMagnification = z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isHandlingAccessibilityEventsLocked() {
        return (this.mBoundServices.isEmpty() && this.mBindingServices.isEmpty()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSwitchToAnotherUserLocked() {
        unbindAllServicesLocked();
        this.mBoundServices.clear();
        this.mBindingServices.clear();
        this.mCrashedServices.clear();
        this.mLastSentClientState = -1;
        this.mNonInteractiveUiTimeout = 0;
        this.mInteractiveUiTimeout = 0;
        this.mEnabledServices.clear();
        this.mTouchExplorationGrantedServices.clear();
        this.mAccessibilityShortcutKeyTargets.clear();
        this.mAccessibilityButtonTargets.clear();
        this.mTargetAssignedToAccessibilityButton = null;
        this.mIsTouchExplorationEnabled = false;
        this.mServiceHandlesDoubleTap = false;
        this.mRequestMultiFingerGestures = false;
        this.mRequestTwoFingerPassthrough = false;
        this.mSendMotionEventsEnabled = false;
        this.mIsDisplayMagnificationEnabled = false;
        this.mIsAutoclickEnabled = false;
        this.mUserNonInteractiveUiTimeout = 0;
        this.mUserInteractiveUiTimeout = 0;
        this.mMagnificationModes.clear();
        this.mFocusStrokeWidth = this.mFocusStrokeWidthDefaultValue;
        this.mFocusColor = this.mFocusColorDefaultValue;
        this.mMagnificationFollowTypingEnabled = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addServiceLocked(AccessibilityServiceConnection serviceConnection) {
        if (!this.mBoundServices.contains(serviceConnection)) {
            serviceConnection.onAdded();
            this.mBoundServices.add(serviceConnection);
            this.mComponentNameToServiceMap.put(serviceConnection.getComponentName(), serviceConnection);
            this.mServiceInfoChangeListener.onServiceInfoChangedLocked(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeServiceLocked(AccessibilityServiceConnection serviceConnection) {
        this.mBoundServices.remove(serviceConnection);
        serviceConnection.onRemoved();
        ComponentName componentName = this.mServiceChangingSoftKeyboardMode;
        if (componentName != null && componentName.equals(serviceConnection.getServiceInfo().getComponentName())) {
            setSoftKeyboardModeLocked(0, null);
        }
        this.mComponentNameToServiceMap.clear();
        for (int i = 0; i < this.mBoundServices.size(); i++) {
            AccessibilityServiceConnection boundClient = this.mBoundServices.get(i);
            this.mComponentNameToServiceMap.put(boundClient.getComponentName(), boundClient);
        }
        this.mServiceInfoChangeListener.onServiceInfoChangedLocked(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void serviceDisconnectedLocked(AccessibilityServiceConnection serviceConnection) {
        removeServiceLocked(serviceConnection);
        this.mCrashedServices.add(serviceConnection.getComponentName());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setSoftKeyboardModeLocked(int newMode, ComponentName requester) {
        if (newMode != 0 && newMode != 1 && newMode != 2) {
            Slog.w(LOG_TAG, "Invalid soft keyboard mode");
            return false;
        }
        int i = this.mSoftKeyboardShowMode;
        if (i == newMode) {
            return true;
        }
        if (newMode == 2) {
            if (hasUserOverriddenHardKeyboardSetting()) {
                return false;
            }
            if (getSoftKeyboardValueFromSettings() != 2) {
                setOriginalHardKeyboardValue(getSecureIntForUser("show_ime_with_hard_keyboard", 0, this.mUserId) != 0);
            }
            putSecureIntForUser("show_ime_with_hard_keyboard", 1, this.mUserId);
        } else if (i == 2) {
            putSecureIntForUser("show_ime_with_hard_keyboard", getOriginalHardKeyboardValue() ? 1 : 0, this.mUserId);
        }
        saveSoftKeyboardValueToSettings(newMode);
        this.mSoftKeyboardShowMode = newMode;
        this.mServiceChangingSoftKeyboardMode = requester;
        for (int i2 = this.mBoundServices.size() - 1; i2 >= 0; i2--) {
            AccessibilityServiceConnection service = this.mBoundServices.get(i2);
            service.notifySoftKeyboardShowModeChangedLocked(this.mSoftKeyboardShowMode);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSoftKeyboardShowModeLocked() {
        return this.mSoftKeyboardShowMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reconcileSoftKeyboardModeWithSettingsLocked() {
        boolean showWithHardKeyboardSettings = getSecureIntForUser("show_ime_with_hard_keyboard", 0, this.mUserId) != 0;
        if (this.mSoftKeyboardShowMode == 2 && !showWithHardKeyboardSettings) {
            setSoftKeyboardModeLocked(0, null);
            setUserOverridesHardKeyboardSetting();
        }
        if (getSoftKeyboardValueFromSettings() != this.mSoftKeyboardShowMode) {
            Slog.e(LOG_TAG, "Show IME setting inconsistent with internal state. Overwriting");
            setSoftKeyboardModeLocked(0, null);
            putSecureIntForUser("accessibility_soft_keyboard_mode", 0, this.mUserId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getBindInstantServiceAllowedLocked() {
        return this.mBindInstantServiceAllowed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBindInstantServiceAllowedLocked(boolean allowed) {
        this.mBindInstantServiceAllowed = allowed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<ComponentName> getBindingServicesLocked() {
        return this.mBindingServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<ComponentName> getCrashedServicesLocked() {
        return this.mCrashedServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<ComponentName> getEnabledServicesLocked() {
        return this.mEnabledServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateCrashedServicesIfNeededLocked() {
        int count = this.mInstalledServices.size();
        for (int i = 0; i < count; i++) {
            AccessibilityServiceInfo installedService = this.mInstalledServices.get(i);
            ComponentName componentName = ComponentName.unflattenFromString(installedService.getId());
            if (this.mCrashedServices.contains(componentName) && !this.mEnabledServices.contains(componentName)) {
                this.mCrashedServices.remove(componentName);
            }
        }
    }

    List<AccessibilityServiceConnection> getBoundServicesLocked() {
        return this.mBoundServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getClientStateLocked(boolean isUiAutomationRunning, int traceClientState) {
        int clientState = 0;
        boolean a11yEnabled = isUiAutomationRunning || isHandlingAccessibilityEventsLocked();
        if (a11yEnabled) {
            clientState = 0 | 1;
        }
        if (a11yEnabled && this.mIsTouchExplorationEnabled) {
            clientState = clientState | 2 | 8 | 16;
        }
        if (this.mIsTextHighContrastEnabled) {
            clientState |= 4;
        }
        if (this.mIsAudioDescriptionByDefaultRequested) {
            clientState |= 4096;
        }
        return clientState | traceClientState;
    }

    private void setUserOverridesHardKeyboardSetting() {
        int softKeyboardSetting = getSecureIntForUser("accessibility_soft_keyboard_mode", 0, this.mUserId);
        putSecureIntForUser("accessibility_soft_keyboard_mode", 1073741824 | softKeyboardSetting, this.mUserId);
    }

    private boolean hasUserOverriddenHardKeyboardSetting() {
        int softKeyboardSetting = getSecureIntForUser("accessibility_soft_keyboard_mode", 0, this.mUserId);
        return (1073741824 & softKeyboardSetting) != 0;
    }

    private void setOriginalHardKeyboardValue(boolean originalHardKeyboardValue) {
        int oldSoftKeyboardSetting = getSecureIntForUser("accessibility_soft_keyboard_mode", 0, this.mUserId);
        int newSoftKeyboardSetting = (originalHardKeyboardValue ? 536870912 : 0) | ((-536870913) & oldSoftKeyboardSetting);
        putSecureIntForUser("accessibility_soft_keyboard_mode", newSoftKeyboardSetting, this.mUserId);
    }

    private void saveSoftKeyboardValueToSettings(int softKeyboardShowMode) {
        int oldSoftKeyboardSetting = getSecureIntForUser("accessibility_soft_keyboard_mode", 0, this.mUserId);
        int newSoftKeyboardSetting = (oldSoftKeyboardSetting & (-4)) | softKeyboardShowMode;
        putSecureIntForUser("accessibility_soft_keyboard_mode", newSoftKeyboardSetting, this.mUserId);
    }

    private int getSoftKeyboardValueFromSettings() {
        return getSecureIntForUser("accessibility_soft_keyboard_mode", 0, this.mUserId) & 3;
    }

    private boolean getOriginalHardKeyboardValue() {
        return (getSecureIntForUser("accessibility_soft_keyboard_mode", 0, this.mUserId) & 536870912) != 0;
    }

    private void unbindAllServicesLocked() {
        List<AccessibilityServiceConnection> services = this.mBoundServices;
        for (int count = services.size(); count > 0; count--) {
            services.get(0).unbindLocked();
        }
    }

    private int getSecureIntForUser(String key, int def, int userId) {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), key, def, userId);
    }

    private void putSecureIntForUser(String key, int value, int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), key, value, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.append("User state[");
        pw.println();
        pw.append("     attributes:{id=").append((CharSequence) String.valueOf(this.mUserId));
        pw.append(", touchExplorationEnabled=").append((CharSequence) String.valueOf(this.mIsTouchExplorationEnabled));
        pw.append(", serviceHandlesDoubleTap=").append((CharSequence) String.valueOf(this.mServiceHandlesDoubleTap));
        pw.append(", requestMultiFingerGestures=").append((CharSequence) String.valueOf(this.mRequestMultiFingerGestures));
        pw.append(", requestTwoFingerPassthrough=").append((CharSequence) String.valueOf(this.mRequestTwoFingerPassthrough));
        pw.append(", sendMotionEventsEnabled").append((CharSequence) String.valueOf(this.mSendMotionEventsEnabled));
        pw.append(", displayMagnificationEnabled=").append((CharSequence) String.valueOf(this.mIsDisplayMagnificationEnabled));
        pw.append(", autoclickEnabled=").append((CharSequence) String.valueOf(this.mIsAutoclickEnabled));
        pw.append(", nonInteractiveUiTimeout=").append((CharSequence) String.valueOf(this.mNonInteractiveUiTimeout));
        pw.append(", interactiveUiTimeout=").append((CharSequence) String.valueOf(this.mInteractiveUiTimeout));
        pw.append(", installedServiceCount=").append((CharSequence) String.valueOf(this.mInstalledServices.size()));
        pw.append(", magnificationModes=").append((CharSequence) String.valueOf(this.mMagnificationModes));
        pw.append(", magnificationCapabilities=").append((CharSequence) String.valueOf(this.mMagnificationCapabilities));
        pw.append(", audioDescriptionByDefaultEnabled=").append((CharSequence) String.valueOf(this.mIsAudioDescriptionByDefaultRequested));
        pw.append(", magnificationFollowTypingEnabled=").append((CharSequence) String.valueOf(this.mMagnificationFollowTypingEnabled));
        pw.append("}");
        pw.println();
        pw.append("     shortcut key:{");
        int size = this.mAccessibilityShortcutKeyTargets.size();
        for (int i = 0; i < size; i++) {
            String componentId = this.mAccessibilityShortcutKeyTargets.valueAt(i);
            pw.append((CharSequence) componentId);
            if (i + 1 < size) {
                pw.append(", ");
            }
        }
        pw.println("}");
        pw.append("     button:{");
        int size2 = this.mAccessibilityButtonTargets.size();
        for (int i2 = 0; i2 < size2; i2++) {
            String componentId2 = this.mAccessibilityButtonTargets.valueAt(i2);
            pw.append((CharSequence) componentId2);
            if (i2 + 1 < size2) {
                pw.append(", ");
            }
        }
        pw.println("}");
        pw.append("     button target:{").append((CharSequence) this.mTargetAssignedToAccessibilityButton);
        pw.println("}");
        pw.append("     Bound services:{");
        int serviceCount = this.mBoundServices.size();
        for (int j = 0; j < serviceCount; j++) {
            if (j > 0) {
                pw.append(", ");
                pw.println();
                pw.append("                     ");
            }
            AccessibilityServiceConnection service = this.mBoundServices.get(j);
            service.dump(fd, pw, args);
        }
        pw.println("}");
        pw.append("     Enabled services:{");
        Iterator<ComponentName> it = this.mEnabledServices.iterator();
        if (it.hasNext()) {
            ComponentName componentName = it.next();
            pw.append((CharSequence) componentName.toShortString());
            while (it.hasNext()) {
                ComponentName componentName2 = it.next();
                pw.append(", ");
                pw.append((CharSequence) componentName2.toShortString());
            }
        }
        pw.println("}");
        pw.append("     Binding services:{");
        Iterator<ComponentName> it2 = this.mBindingServices.iterator();
        if (it2.hasNext()) {
            ComponentName componentName3 = it2.next();
            pw.append((CharSequence) componentName3.toShortString());
            while (it2.hasNext()) {
                ComponentName componentName4 = it2.next();
                pw.append(", ");
                pw.append((CharSequence) componentName4.toShortString());
            }
        }
        pw.println("}");
        pw.append("     Crashed services:{");
        Iterator<ComponentName> it3 = this.mCrashedServices.iterator();
        if (it3.hasNext()) {
            ComponentName componentName5 = it3.next();
            pw.append((CharSequence) componentName5.toShortString());
            while (it3.hasNext()) {
                ComponentName componentName6 = it3.next();
                pw.append(", ");
                pw.append((CharSequence) componentName6.toShortString());
            }
        }
        pw.println("}");
        pw.println("     Client list info:{");
        this.mUserClients.dump(pw, "          Client list ");
        pw.println("          Registered clients:{");
        for (int i3 = 0; i3 < this.mUserClients.getRegisteredCallbackCount(); i3++) {
            AccessibilityManagerService.Client client = (AccessibilityManagerService.Client) this.mUserClients.getRegisteredCallbackCookie(i3);
            pw.append((CharSequence) Arrays.toString(client.mPackageNames));
        }
        pw.println("}]");
    }

    public boolean isAutoclickEnabledLocked() {
        return this.mIsAutoclickEnabled;
    }

    public void setAutoclickEnabledLocked(boolean enabled) {
        this.mIsAutoclickEnabled = enabled;
    }

    public boolean isDisplayMagnificationEnabledLocked() {
        return this.mIsDisplayMagnificationEnabled;
    }

    public void setDisplayMagnificationEnabledLocked(boolean enabled) {
        this.mIsDisplayMagnificationEnabled = enabled;
    }

    public boolean isFilterKeyEventsEnabledLocked() {
        return this.mIsFilterKeyEventsEnabled;
    }

    public void setFilterKeyEventsEnabledLocked(boolean enabled) {
        this.mIsFilterKeyEventsEnabled = enabled;
    }

    public int getInteractiveUiTimeoutLocked() {
        return this.mInteractiveUiTimeout;
    }

    public void setInteractiveUiTimeoutLocked(int timeout) {
        this.mInteractiveUiTimeout = timeout;
    }

    public int getLastSentClientStateLocked() {
        return this.mLastSentClientState;
    }

    public void setLastSentClientStateLocked(int state) {
        this.mLastSentClientState = state;
    }

    public boolean isShortcutMagnificationEnabledLocked() {
        return this.mAccessibilityShortcutKeyTargets.contains("com.android.server.accessibility.MagnificationController") || this.mAccessibilityButtonTargets.contains("com.android.server.accessibility.MagnificationController");
    }

    public int getMagnificationModeLocked(int displayId) {
        int mode = this.mMagnificationModes.get(displayId, 0);
        if (mode == 0) {
            setMagnificationModeLocked(displayId, 1);
            return 1;
        }
        return mode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getMagnificationCapabilitiesLocked() {
        return this.mMagnificationCapabilities;
    }

    public void setMagnificationCapabilitiesLocked(int capabilities) {
        this.mMagnificationCapabilities = capabilities;
    }

    public void setMagnificationFollowTypingEnabled(boolean enabled) {
        this.mMagnificationFollowTypingEnabled = enabled;
    }

    public boolean isMagnificationFollowTypingEnabled() {
        return this.mMagnificationFollowTypingEnabled;
    }

    public void setMagnificationModeLocked(int displayId, int mode) {
        this.mMagnificationModes.put(displayId, mode);
    }

    public void disableShortcutMagnificationLocked() {
        this.mAccessibilityShortcutKeyTargets.remove("com.android.server.accessibility.MagnificationController");
        this.mAccessibilityButtonTargets.remove("com.android.server.accessibility.MagnificationController");
    }

    public ArraySet<String> getShortcutTargetsLocked(int shortcutType) {
        if (shortcutType == 1) {
            return this.mAccessibilityShortcutKeyTargets;
        }
        if (shortcutType == 0) {
            return this.mAccessibilityButtonTargets;
        }
        return null;
    }

    public boolean isShortcutTargetInstalledLocked(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        if ("com.android.server.accessibility.MagnificationController".equals(name)) {
            return true;
        }
        ComponentName componentName = ComponentName.unflattenFromString(name);
        if (componentName == null) {
            return false;
        }
        if (!AccessibilityShortcutController.getFrameworkShortcutFeaturesMap().containsKey(componentName) && getInstalledServiceInfoLocked(componentName) == null) {
            for (int i = 0; i < this.mInstalledShortcuts.size(); i++) {
                if (this.mInstalledShortcuts.get(i).getComponentName().equals(componentName)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public boolean removeShortcutTargetLocked(int shortcutType, final ComponentName target) {
        return getShortcutTargetsLocked(shortcutType).removeIf(new Predicate() { // from class: com.android.server.accessibility.AccessibilityUserState$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AccessibilityUserState.lambda$removeShortcutTargetLocked$0(target, (String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeShortcutTargetLocked$0(ComponentName target, String name) {
        ComponentName componentName;
        if (name == null || (componentName = ComponentName.unflattenFromString(name)) == null) {
            return false;
        }
        return componentName.equals(target);
    }

    public AccessibilityServiceInfo getInstalledServiceInfoLocked(ComponentName componentName) {
        for (int i = 0; i < this.mInstalledServices.size(); i++) {
            AccessibilityServiceInfo serviceInfo = this.mInstalledServices.get(i);
            if (serviceInfo.getComponentName().equals(componentName)) {
                return serviceInfo;
            }
        }
        return null;
    }

    public AccessibilityServiceConnection getServiceConnectionLocked(ComponentName componentName) {
        return this.mComponentNameToServiceMap.get(componentName);
    }

    public int getNonInteractiveUiTimeoutLocked() {
        return this.mNonInteractiveUiTimeout;
    }

    public void setNonInteractiveUiTimeoutLocked(int timeout) {
        this.mNonInteractiveUiTimeout = timeout;
    }

    public boolean isPerformGesturesEnabledLocked() {
        return this.mIsPerformGesturesEnabled;
    }

    public void setPerformGesturesEnabledLocked(boolean enabled) {
        this.mIsPerformGesturesEnabled = enabled;
    }

    public boolean isAccessibilityFocusOnlyInActiveWindow() {
        return this.mAccessibilityFocusOnlyInActiveWindow;
    }

    public void setAccessibilityFocusOnlyInActiveWindow(boolean enabled) {
        this.mAccessibilityFocusOnlyInActiveWindow = enabled;
    }

    public ComponentName getServiceChangingSoftKeyboardModeLocked() {
        return this.mServiceChangingSoftKeyboardMode;
    }

    public void setServiceChangingSoftKeyboardModeLocked(ComponentName serviceChangingSoftKeyboardMode) {
        this.mServiceChangingSoftKeyboardMode = serviceChangingSoftKeyboardMode;
    }

    public boolean isTextHighContrastEnabledLocked() {
        return this.mIsTextHighContrastEnabled;
    }

    public void setTextHighContrastEnabledLocked(boolean enabled) {
        this.mIsTextHighContrastEnabled = enabled;
    }

    public boolean isAudioDescriptionByDefaultEnabledLocked() {
        return this.mIsAudioDescriptionByDefaultRequested;
    }

    public void setAudioDescriptionByDefaultEnabledLocked(boolean enabled) {
        this.mIsAudioDescriptionByDefaultRequested = enabled;
    }

    public boolean isTouchExplorationEnabledLocked() {
        return this.mIsTouchExplorationEnabled;
    }

    public void setTouchExplorationEnabledLocked(boolean enabled) {
        this.mIsTouchExplorationEnabled = enabled;
    }

    public boolean isServiceHandlesDoubleTapEnabledLocked() {
        return this.mServiceHandlesDoubleTap;
    }

    public void setServiceHandlesDoubleTapLocked(boolean enabled) {
        this.mServiceHandlesDoubleTap = enabled;
    }

    public boolean isMultiFingerGesturesEnabledLocked() {
        return this.mRequestMultiFingerGestures;
    }

    public void setMultiFingerGesturesLocked(boolean enabled) {
        this.mRequestMultiFingerGestures = enabled;
    }

    public boolean isTwoFingerPassthroughEnabledLocked() {
        return this.mRequestTwoFingerPassthrough;
    }

    public void setTwoFingerPassthroughLocked(boolean enabled) {
        this.mRequestTwoFingerPassthrough = enabled;
    }

    public boolean isSendMotionEventsEnabled() {
        return this.mSendMotionEventsEnabled;
    }

    public void setSendMotionEventsEnabled(boolean mode) {
        this.mSendMotionEventsEnabled = mode;
    }

    public int getUserInteractiveUiTimeoutLocked() {
        return this.mUserInteractiveUiTimeout;
    }

    public void setUserInteractiveUiTimeoutLocked(int timeout) {
        this.mUserInteractiveUiTimeout = timeout;
    }

    public int getUserNonInteractiveUiTimeoutLocked() {
        return this.mUserNonInteractiveUiTimeout;
    }

    public void setUserNonInteractiveUiTimeoutLocked(int timeout) {
        this.mUserNonInteractiveUiTimeout = timeout;
    }

    public String getTargetAssignedToAccessibilityButton() {
        return this.mTargetAssignedToAccessibilityButton;
    }

    public void setTargetAssignedToAccessibilityButton(String target) {
        this.mTargetAssignedToAccessibilityButton = target;
    }

    public static boolean doesShortcutTargetsStringContain(Collection<String> shortcutTargets, String targetName) {
        if (shortcutTargets == null || targetName == null) {
            return false;
        }
        if (shortcutTargets.contains(targetName)) {
            return true;
        }
        ComponentName targetComponentName = ComponentName.unflattenFromString(targetName);
        if (targetComponentName == null) {
            return false;
        }
        for (String stringName : shortcutTargets) {
            if (!TextUtils.isEmpty(stringName) && targetComponentName.equals(ComponentName.unflattenFromString(stringName))) {
                return true;
            }
        }
        return false;
    }

    public int getFocusStrokeWidthLocked() {
        return this.mFocusStrokeWidth;
    }

    public int getFocusColorLocked() {
        return this.mFocusColor;
    }

    public void setFocusAppearanceLocked(int strokeWidth, int color) {
        this.mFocusStrokeWidth = strokeWidth;
        this.mFocusColor = color;
    }
}
