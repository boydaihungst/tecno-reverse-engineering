package com.android.server.wm;

import android.content.Context;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import com.android.server.wm.LocalAnimationAdapter;
import defpackage.CompanionAppsPermissions;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class FadeAnimationController {
    protected final Context mContext;
    protected final DisplayContent mDisplayContent;

    public FadeAnimationController(DisplayContent displayContent) {
        this.mDisplayContent = displayContent;
        this.mContext = displayContent.mWmService.mContext;
    }

    public Animation getFadeInAnimation() {
        return AnimationUtils.loadAnimation(this.mContext, 17432576);
    }

    public Animation getFadeOutAnimation() {
        return AnimationUtils.loadAnimation(this.mContext, 17432577);
    }

    public void fadeWindowToken(boolean show, WindowToken windowToken, int animationType) {
        if (windowToken == null || windowToken.getParent() == null) {
            return;
        }
        Animation animation = show ? getFadeInAnimation() : getFadeOutAnimation();
        FadeAnimationAdapter animationAdapter = animation != null ? createAdapter(createAnimationSpec(animation), show, windowToken) : null;
        if (animationAdapter == null) {
            return;
        }
        windowToken.startAnimation(windowToken.getPendingTransaction(), animationAdapter, show, animationType, null);
    }

    protected FadeAnimationAdapter createAdapter(LocalAnimationAdapter.AnimationSpec animationSpec, boolean show, WindowToken windowToken) {
        return new FadeAnimationAdapter(animationSpec, windowToken.getSurfaceAnimationRunner(), show, windowToken);
    }

    protected LocalAnimationAdapter.AnimationSpec createAnimationSpec(final Animation animation) {
        return new LocalAnimationAdapter.AnimationSpec() { // from class: com.android.server.wm.FadeAnimationController.1
            final Transformation mTransformation = new Transformation();

            @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
            public boolean getShowWallpaper() {
                return true;
            }

            @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
            public long getDuration() {
                return animation.getDuration();
            }

            @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
            public void apply(SurfaceControl.Transaction t, SurfaceControl leash, long currentPlayTime) {
                this.mTransformation.clear();
                animation.getTransformation(currentPlayTime, this.mTransformation);
                t.setAlpha(leash, this.mTransformation.getAlpha());
            }

            @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
            public void dump(PrintWriter pw, String prefix) {
                pw.print(prefix);
                pw.println(animation);
            }

            @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
            public void dumpDebugInner(ProtoOutputStream proto) {
                long token = proto.start(1146756268033L);
                proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, animation.toString());
                proto.end(token);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public static class FadeAnimationAdapter extends LocalAnimationAdapter {
        protected final boolean mShow;
        protected final WindowToken mToken;

        /* JADX INFO: Access modifiers changed from: package-private */
        public FadeAnimationAdapter(LocalAnimationAdapter.AnimationSpec windowAnimationSpec, SurfaceAnimationRunner surfaceAnimationRunner, boolean show, WindowToken token) {
            super(windowAnimationSpec, surfaceAnimationRunner);
            this.mShow = show;
            this.mToken = token;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public boolean shouldDeferAnimationFinish(Runnable endDeferFinishCallback) {
            return !this.mShow;
        }
    }
}
