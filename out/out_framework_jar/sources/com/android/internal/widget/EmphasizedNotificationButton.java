package com.android.internal.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.BlendMode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.widget.Button;
import android.widget.RemoteViews;
import com.android.internal.R;
@RemoteViews.RemoteView
/* loaded from: classes4.dex */
public class EmphasizedNotificationButton extends Button {
    private final GradientDrawable mBackground;
    private boolean mPriority;
    private final RippleDrawable mRipple;

    public EmphasizedNotificationButton(Context context) {
        this(context, null);
    }

    public EmphasizedNotificationButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmphasizedNotificationButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EmphasizedNotificationButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        RippleDrawable rippleDrawable = (RippleDrawable) getBackground();
        this.mRipple = rippleDrawable;
        rippleDrawable.mutate();
        DrawableWrapper inset = (DrawableWrapper) rippleDrawable.getDrawable(0);
        this.mBackground = (GradientDrawable) inset.getDrawable();
    }

    @RemotableViewMethod
    public void setRippleColor(ColorStateList color) {
        this.mRipple.setColor(color);
        invalidate();
    }

    @RemotableViewMethod
    public void setButtonBackground(ColorStateList color) {
        this.mBackground.setColor(color);
        invalidate();
    }

    @RemotableViewMethod(asyncImpl = "setImageIconAsync")
    public void setImageIcon(Icon icon) {
        Drawable drawable = icon == null ? null : icon.loadDrawable(this.mContext);
        m7066xc5e7518d(drawable);
    }

    @RemotableViewMethod
    public Runnable setImageIconAsync(Icon icon) {
        final Drawable drawable = icon == null ? null : icon.loadDrawable(this.mContext);
        return new Runnable() { // from class: com.android.internal.widget.EmphasizedNotificationButton$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                EmphasizedNotificationButton.this.m7066xc5e7518d(drawable);
            }
        };
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: setImageDrawable */
    public void m7066xc5e7518d(Drawable drawable) {
        if (drawable != null) {
            drawable.mutate();
            drawable.setTintList(getTextColors());
            drawable.setTintBlendMode(BlendMode.SRC_IN);
            int iconSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_actions_icon_drawable_size);
            drawable.setBounds(0, 0, iconSize, iconSize);
        }
        setCompoundDrawablesRelative(drawable, null, null, null);
    }

    @RemotableViewMethod
    public void setIsPriority(boolean priority) {
        this.mPriority = priority;
    }

    public boolean isPriority() {
        return this.mPriority;
    }
}
