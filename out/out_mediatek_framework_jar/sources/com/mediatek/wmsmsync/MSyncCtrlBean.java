package com.mediatek.wmsmsync;

import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class MSyncCtrlBean {
    private List<ActivityBean> mActivityBeans;
    private float mFps;
    private float mImeFps;
    private String mPackageName;
    private boolean mSlideResponse;
    private float mVoiceFps;

    public String getPackageName() {
        return this.mPackageName;
    }

    public void setPackageName(String packageName) {
        this.mPackageName = packageName;
    }

    public List<ActivityBean> getActivityBeans() {
        if (this.mActivityBeans == null) {
            this.mActivityBeans = new ArrayList();
        }
        return this.mActivityBeans;
    }

    public void setActivityBeans(List<ActivityBean> activityBeans) {
        this.mActivityBeans = activityBeans;
    }

    public float getImeFps() {
        return this.mImeFps;
    }

    public void setImeFps(float imeFps) {
        this.mImeFps = imeFps;
    }

    public boolean isSlideResponse() {
        return this.mSlideResponse;
    }

    public void setSlideResponse(boolean slideResponse) {
        this.mSlideResponse = slideResponse;
    }

    public float getFps() {
        return this.mFps;
    }

    public void setFps(float defaultFps) {
        this.mFps = defaultFps;
    }

    public float getVoiceFps() {
        return this.mVoiceFps;
    }

    public void setVoiceFps(float voiceFps) {
        this.mVoiceFps = voiceFps;
    }

    /* loaded from: classes.dex */
    public static class ActivityBean {
        private float mFps;
        private float mImeFps;
        private String mName;
        private float mVoiceFps;

        public String toString() {
            return "ActivityBean{name='" + this.mName + "', fps=" + this.mFps + ", imeFps=" + this.mImeFps + ", voiceFps=" + this.mVoiceFps + '}';
        }

        public float getImeFps() {
            return this.mImeFps;
        }

        public void setImeFps(float imeFps) {
            this.mImeFps = imeFps;
        }

        public String getName() {
            return this.mName;
        }

        public void setName(String name) {
            this.mName = name;
        }

        public float getFps() {
            return this.mFps;
        }

        public void setFps(float fps) {
            this.mFps = fps;
        }

        public float getVoiceFps() {
            return this.mVoiceFps;
        }

        public void setVoiceFps(float voiceFps) {
            this.mVoiceFps = voiceFps;
        }
    }

    public String toString() {
        return "MSyncCtrlTableBean{packageName='" + this.mPackageName + "', activityBeans=" + this.mActivityBeans + ", slideResponse=" + this.mSlideResponse + ", defaultFps=" + this.mFps + ", imeFps=" + this.mImeFps + ", voiceFps=" + this.mVoiceFps + '}';
    }
}
