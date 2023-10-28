package com.android.internal.widget;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.android.internal.R;
@RemoteViews.RemoteView
/* loaded from: classes4.dex */
public class BigPictureNotificationImageView extends ImageView {
    private static final String TAG = BigPictureNotificationImageView.class.getSimpleName();
    private final int mMaximumDrawableHeight;
    private final int mMaximumDrawableWidth;

    public BigPictureNotificationImageView(Context context) {
        this(context, null, 0, 0);
    }

    public BigPictureNotificationImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public BigPictureNotificationImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BigPictureNotificationImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        boolean isLowRam = ActivityManager.isLowRamDeviceStatic();
        this.mMaximumDrawableWidth = context.getResources().getDimensionPixelSize(isLowRam ? R.dimen.notification_big_picture_max_width_low_ram : R.dimen.notification_big_picture_max_width);
        this.mMaximumDrawableHeight = context.getResources().getDimensionPixelSize(isLowRam ? R.dimen.notification_big_picture_max_height_low_ram : R.dimen.notification_big_picture_max_height);
    }

    @Override // android.widget.ImageView
    @RemotableViewMethod(asyncImpl = "setImageURIAsync")
    public void setImageURI(Uri uri) {
        setImageDrawable(loadImage(uri));
    }

    @Override // android.widget.ImageView
    public Runnable setImageURIAsync(Uri uri) {
        final Drawable drawable = loadImage(uri);
        return new Runnable() { // from class: com.android.internal.widget.BigPictureNotificationImageView$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BigPictureNotificationImageView.this.m7052x5237d8c2(drawable);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setImageURIAsync$0$com-android-internal-widget-BigPictureNotificationImageView  reason: not valid java name */
    public /* synthetic */ void m7052x5237d8c2(Drawable drawable) {
        setImageDrawable(drawable);
    }

    @Override // android.widget.ImageView
    @RemotableViewMethod(asyncImpl = "setImageIconAsync")
    public void setImageIcon(Icon icon) {
        setImageDrawable(loadImage(icon));
    }

    @Override // android.widget.ImageView
    public Runnable setImageIconAsync(Icon icon) {
        final Drawable drawable = loadImage(icon);
        return new Runnable() { // from class: com.android.internal.widget.BigPictureNotificationImageView$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                BigPictureNotificationImageView.this.m7051x386f61ea(drawable);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setImageIconAsync$1$com-android-internal-widget-BigPictureNotificationImageView  reason: not valid java name */
    public /* synthetic */ void m7051x386f61ea(Drawable drawable) {
        setImageDrawable(drawable);
    }

    private Drawable loadImage(Uri uri) {
        if (uri == null) {
            return null;
        }
        return LocalImageResolver.resolveImage(uri, this.mContext, this.mMaximumDrawableWidth, this.mMaximumDrawableHeight);
    }

    private Drawable loadImage(Icon icon) {
        if (icon == null) {
            return null;
        }
        Drawable drawable = LocalImageResolver.resolveImage(icon, this.mContext, this.mMaximumDrawableWidth, this.mMaximumDrawableHeight);
        if (drawable == null) {
            return icon.loadDrawable(this.mContext);
        }
        return drawable;
    }
}
