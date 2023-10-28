package com.android.internal;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import com.android.internal.IInternalViewLice;
import com.android.internal.app.ResolverActivity;
import com.android.internal.app.chooser.DisplayResolveInfo;
import com.android.internal.app.chooser.TargetInfo;
import com.android.internal.policy.DecorView;
import com.android.internal.policy.PhoneWindow;
import com.android.internal.util.ScreenshotHelper;
import com.android.internal.widget.DecorCaptionView;
import com.android.internal.widget.ResolverDrawerLayout;
import com.android.internal.widget.floatingtoolbar.FloatingToolbarPopup;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_FRAMEWORK)
/* loaded from: classes4.dex */
public interface IInternalViewLice {
    public static final LiceInfo<IInternalViewLice> sLiceInfo = new LiceInfo<>("com.transsion.internal.InternalViewLice", IInternalViewLice.class, new Supplier() { // from class: com.android.internal.IInternalViewLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IInternalViewLice.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements IInternalViewLice {
    }

    static IInternalViewLice Instance() {
        return sLiceInfo.getImpl();
    }

    default DecorCaptionView inflateDecorCaptionView(LayoutInflater inflater) {
        return (DecorCaptionView) inflater.inflate(R.layout.decor_caption, (ViewGroup) null);
    }

    default boolean setLightDecorCaptionShade(DecorCaptionView view) {
        return false;
    }

    default boolean setDarkDecorCaptionShade(DecorCaptionView view) {
        return false;
    }

    default Pair<View, View> setPhoneWindow(View view, PhoneWindow owner, boolean show) {
        owner.getDecorView().setOutlineProvider(ViewOutlineProvider.BOUNDS);
        View maximize = view.findViewById(R.id.maximize_window);
        View close = view.findViewById(R.id.close_window);
        return new Pair<>(maximize, close);
    }

    default ImageButton createOverflowButton(Context context) {
        return null;
    }

    default View createMenuItemButton(Context context) {
        return null;
    }

    default ViewGroup createContentContaine(Context context) {
        return null;
    }

    default void initAdChooserOnCreate(Context context) {
    }

    default void initMeidaChooserOnCreate() {
    }

    default void unBindAdOnDestroy() {
    }

    default int appliedThemeResId() {
        return 0;
    }

    default int getLayoutResource() {
        return 0;
    }

    default void onTrimMemory() {
    }

    default void setBottomLayoutMargin(Context context, ResolverDrawerLayout rdl, boolean isChooserActivity) {
    }

    default void setSystemUIVisibility(View view, int sysFlag, PhoneWindow phoneWindow, String pkg, int color) {
    }

    default int modifyDefaultNaviBarColor(PhoneWindow phoneWindow, int color, Drawable windowBackgroundDrawable, boolean windowIsTranslucent) {
        return color;
    }

    default void adjustLayoutParamsForWhiteNavigationBar(WindowManager.LayoutParams inOutParams, int color, Context context) {
    }

    default void modifySystemUiVisibility(PhoneWindow phoneWindow, int color, boolean forceColor, DecorView view) {
    }

    default List<DisplayResolveInfo> chooserSort(Context context, List<DisplayResolveInfo> groupedTargets) {
        return null;
    }

    default List<ResolverActivity.ResolvedComponentInfo> resolverSort(Context context, List<ResolverActivity.ResolvedComponentInfo> targets, String type) {
        return null;
    }

    default String getRecommendPkg(Context context, List<ResolverActivity.ResolvedComponentInfo> targets, String type) {
        return null;
    }

    default boolean takeScreenshot(Context context, int screenshotType, long timeoutMs, Handler handler, ScreenshotHelper.ScreenshotRequest screenshotRequest, Consumer<Uri> completionConsumer) {
        return false;
    }

    default boolean takeScreenshot(Context context, int screenshotType, long timeoutMs, int source, Handler handler, Bundle bundle) {
        return false;
    }

    default void startInMultiWindow(Context context, TargetInfo targetInfo, String sourcePkg) {
    }

    default void initShareProtect(Context context, Handler handler, Intent intent) {
    }

    default void onBindItemGroupViewHolder(View view) {
    }

    default void onStartSelected(Intent intent) {
    }

    default void onChooserActivityDestroy() {
    }

    default boolean isShareProtectActived() {
        return false;
    }

    default FloatingToolbarPopup createFloatingToolbarPopup(Context context, View parent) {
        return null;
    }

    default int onMenuItemCompare(MenuItem item1, MenuItem item2) {
        return 0;
    }

    default void onPopulateMenuWithItems(Menu menu, Context context, int flags) {
    }

    default boolean onTextContextMenuItem(int id, TextView textView) {
        return false;
    }

    default void onAddAssistMenuItem(MenuItem item, String textType, String selectedText, Context context, Map<MenuItem, View.OnClickListener> clickListenerMap) {
    }

    default int onUpdateColorViewInt(View view, int color, String phrase) {
        return color;
    }
}
