package com.android.internal.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.android.internal.R;
import java.util.Objects;
import java.util.function.Consumer;
@RemoteViews.RemoteView
/* loaded from: classes4.dex */
public class CachingIconView extends ImageView {
    private int mBackgroundColor;
    private int mDesiredVisibility;
    private boolean mForceHidden;
    private int mIconColor;
    private int mIconHeight;
    private int mIconWidth;
    private boolean mInternalSetDrawable;
    private String mLastPackage;
    private int mLastResId;
    private int mMaxDrawableHeight;
    private int mMaxDrawableWidth;
    private Consumer<Boolean> mOnForceHiddenChangedListener;
    private Consumer<Integer> mOnVisibilityChangedListener;
    private boolean mRoundCorner;
    private boolean mSupportCircle;
    private boolean mSupportConversationBadgeIconCorner;
    private boolean mWillBeForceHidden;

    public CachingIconView(Context context) {
        this(context, null, 0, 0);
    }

    public CachingIconView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public CachingIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CachingIconView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mMaxDrawableWidth = -1;
        this.mMaxDrawableHeight = -1;
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs == null) {
            return;
        }
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CachingIconView, defStyleAttr, defStyleRes);
        this.mMaxDrawableWidth = ta.getDimensionPixelSize(0, -1);
        this.mMaxDrawableHeight = ta.getDimensionPixelSize(1, -1);
        this.mRoundCorner = ta.getBoolean(4, false);
        this.mSupportCircle = ta.getBoolean(3, false);
        this.mSupportConversationBadgeIconCorner = ta.getBoolean(2, false);
        if (!this.mRoundCorner) {
            this.mRoundCorner = "gamemode_notification_icon".equals(ta.getString(44));
        }
        if (this.mRoundCorner) {
            setClipToOutline(true);
            setOutlineProvider(new ViewOutlineProvider() { // from class: com.android.internal.widget.CachingIconView.1
                @Override // android.view.ViewOutlineProvider
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), CachingIconView.this.dp2px(6.0f));
                }
            });
        }
        ta.recycle();
    }

    @Override // android.widget.ImageView
    @RemotableViewMethod(asyncImpl = "setImageIconAsync")
    public void setImageIcon(Icon icon) {
        if (!testAndSetCache(icon)) {
            this.mInternalSetDrawable = true;
            Drawable drawable = loadSizeRestrictedIcon(icon);
            if (drawable == null) {
                super.setImageIcon(icon);
            } else {
                super.setImageDrawable(drawable);
            }
            this.mInternalSetDrawable = false;
        }
    }

    private Drawable loadSizeRestrictedIcon(Icon icon) {
        return LocalImageResolver.resolveImage(icon, getContext(), this.mMaxDrawableWidth, this.mMaxDrawableHeight);
    }

    @Override // android.widget.ImageView
    public Runnable setImageIconAsync(Icon icon) {
        resetCache();
        final Drawable drawable = loadSizeRestrictedIcon(icon);
        if (drawable != null) {
            return new Runnable() { // from class: com.android.internal.widget.CachingIconView$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    CachingIconView.this.m7054x6a9a444f(drawable);
                }
            };
        }
        return super.setImageIconAsync(icon);
    }

    @Override // android.widget.ImageView
    @RemotableViewMethod(asyncImpl = "setImageResourceAsync")
    public void setImageResource(int resId) {
        if (!testAndSetCache(resId)) {
            this.mInternalSetDrawable = true;
            Drawable drawable = loadSizeRestrictedDrawable(resId);
            if (drawable == null) {
                super.setImageResource(resId);
            } else {
                super.setImageDrawable(drawable);
            }
            this.mInternalSetDrawable = false;
        }
    }

    private Drawable loadSizeRestrictedDrawable(int resId) {
        return LocalImageResolver.resolveImage(resId, getContext(), this.mMaxDrawableWidth, this.mMaxDrawableHeight);
    }

    @Override // android.widget.ImageView
    public Runnable setImageResourceAsync(int resId) {
        resetCache();
        final Drawable drawable = loadSizeRestrictedDrawable(resId);
        if (drawable != null) {
            return new Runnable() { // from class: com.android.internal.widget.CachingIconView$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    CachingIconView.this.m7055x8243841b(drawable);
                }
            };
        }
        return super.setImageResourceAsync(resId);
    }

    @Override // android.widget.ImageView
    @RemotableViewMethod(asyncImpl = "setImageURIAsync")
    public void setImageURI(Uri uri) {
        resetCache();
        Drawable drawable = loadSizeRestrictedUri(uri);
        if (drawable == null) {
            super.setImageURI(uri);
            return;
        }
        this.mInternalSetDrawable = true;
        super.setImageDrawable(drawable);
        this.mInternalSetDrawable = false;
    }

    private Drawable loadSizeRestrictedUri(Uri uri) {
        return LocalImageResolver.resolveImage(uri, getContext(), this.mMaxDrawableWidth, this.mMaxDrawableHeight);
    }

    @Override // android.widget.ImageView
    public Runnable setImageURIAsync(Uri uri) {
        resetCache();
        final Drawable drawable = loadSizeRestrictedUri(uri);
        if (drawable == null) {
            return super.setImageURIAsync(uri);
        }
        return new Runnable() { // from class: com.android.internal.widget.CachingIconView$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                CachingIconView.this.m7056x2ffdb2aa(drawable);
            }
        };
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.widget.ImageView, android.inputmethodservice.navigationbar.ButtonInterface
    /* renamed from: setImageDrawable */
    public void m7056x2ffdb2aa(Drawable drawable) {
        if (!this.mInternalSetDrawable) {
            resetCache();
        }
        super.setImageDrawable(drawable);
    }

    @Override // android.widget.ImageView
    @RemotableViewMethod
    public void setImageBitmap(Bitmap bm) {
        resetCache();
        super.setImageBitmap(bm);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.View
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resetCache();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.View
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mIconWidth = getWidth();
        this.mIconHeight = getHeight();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.widget.ImageView, android.view.View
    public void onDraw(Canvas canvas) {
        Path path = new Path();
        if (this.mSupportConversationBadgeIconCorner) {
            int roundSize = dp2px(4.0f);
            path.moveTo(roundSize, 0.0f);
            path.lineTo(this.mIconWidth - roundSize, 0.0f);
            int i = this.mIconWidth;
            path.quadTo(i, 0.0f, i, roundSize);
            path.lineTo(this.mIconWidth, this.mIconHeight - roundSize);
            int i2 = this.mIconWidth;
            int i3 = this.mIconHeight;
            path.quadTo(i2, i3, i2 - roundSize, i3);
            path.lineTo(roundSize, this.mIconHeight);
            int i4 = this.mIconHeight;
            path.quadTo(0.0f, i4, 0.0f, i4 - roundSize);
            path.lineTo(0.0f, roundSize);
            path.quadTo(0.0f, 0.0f, roundSize, 0.0f);
            canvas.clipPath(path);
        }
        if (this.mSupportCircle) {
            int i5 = this.mIconWidth;
            path.addCircle(i5 / 2, this.mIconHeight / 2, i5 / 2, Path.Direction.CW);
            canvas.clipPath(path);
        }
        super.onDraw(canvas);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp2px(float dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(1, dp, displayMetrics);
    }

    private synchronized boolean testAndSetCache(Icon icon) {
        boolean isCached = false;
        if (icon != null) {
            if (icon.getType() == 2) {
                String iconPackage = normalizeIconPackage(icon);
                if (this.mLastResId != 0 && icon.getResId() == this.mLastResId && Objects.equals(iconPackage, this.mLastPackage)) {
                    isCached = true;
                }
                this.mLastPackage = iconPackage;
                this.mLastResId = icon.getResId();
                return isCached;
            }
        }
        resetCache();
        return false;
    }

    private synchronized boolean testAndSetCache(int resId) {
        boolean isCached;
        if (resId != 0) {
            try {
                int i = this.mLastResId;
                if (i != 0) {
                    isCached = resId == i && this.mLastPackage == null;
                    this.mLastPackage = null;
                    this.mLastResId = resId;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        isCached = false;
        this.mLastPackage = null;
        this.mLastResId = resId;
        return isCached;
    }

    private String normalizeIconPackage(Icon icon) {
        if (icon == null) {
            return null;
        }
        String pkg = icon.getResPackage();
        if (TextUtils.isEmpty(pkg) || pkg.equals(this.mContext.getPackageName())) {
            return null;
        }
        return pkg;
    }

    private synchronized void resetCache() {
        this.mLastResId = 0;
        this.mLastPackage = null;
    }

    public void setForceHidden(boolean forceHidden) {
        if (forceHidden != this.mForceHidden) {
            this.mForceHidden = forceHidden;
            this.mWillBeForceHidden = false;
            updateVisibility();
            Consumer<Boolean> consumer = this.mOnForceHiddenChangedListener;
            if (consumer != null) {
                consumer.accept(Boolean.valueOf(forceHidden));
            }
        }
    }

    @Override // android.widget.ImageView, android.view.View
    @RemotableViewMethod
    public void setVisibility(int visibility) {
        this.mDesiredVisibility = visibility;
        updateVisibility();
    }

    private void updateVisibility() {
        int visibility = this.mDesiredVisibility;
        if (visibility == 0 && this.mForceHidden) {
            visibility = 4;
        }
        Consumer<Integer> consumer = this.mOnVisibilityChangedListener;
        if (consumer != null) {
            consumer.accept(Integer.valueOf(visibility));
        }
        super.setVisibility(visibility);
    }

    public void setOnVisibilityChangedListener(Consumer<Integer> listener) {
        this.mOnVisibilityChangedListener = listener;
    }

    public void setOnForceHiddenChangedListener(Consumer<Boolean> listener) {
        this.mOnForceHiddenChangedListener = listener;
    }

    public boolean isForceHidden() {
        return this.mForceHidden;
    }

    @Override // android.view.View
    @RemotableViewMethod
    public void setBackgroundColor(int color) {
        this.mBackgroundColor = color;
    }

    @RemotableViewMethod
    public void setOriginalIconColor(int color) {
        this.mIconColor = color;
        Drawable background = getBackground();
        Drawable icon = getDrawable();
        boolean hasColor = color != 1;
        if (background == null) {
            if (hasColor && icon != null) {
                icon.mutate().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            }
        } else if (hasColor && icon != null) {
            icon.mutate().setColorFilter(this.mBackgroundColor, PorterDuff.Mode.SRC_ATOP);
        }
    }

    public void setGrayedOut(boolean grayedOut) {
        Drawable drawable = getBackground();
        if (drawable == null) {
            drawable = getDrawable();
        }
        ColoredIconHelper.applyGrayTint(this.mContext, drawable, grayedOut, this.mIconColor);
    }

    public int getOriginalIconColor() {
        return this.mIconColor;
    }

    public boolean willBeForceHidden() {
        return this.mWillBeForceHidden;
    }

    public void setWillBeForceHidden(boolean forceHidden) {
        this.mWillBeForceHidden = forceHidden;
    }

    public int getMaxDrawableWidth() {
        return this.mMaxDrawableWidth;
    }

    public int getMaxDrawableHeight() {
        return this.mMaxDrawableHeight;
    }
}
