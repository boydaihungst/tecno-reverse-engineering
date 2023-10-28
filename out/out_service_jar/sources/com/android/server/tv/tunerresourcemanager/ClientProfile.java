package com.android.server.tv.tunerresourcemanager;

import java.util.HashSet;
import java.util.Set;
/* loaded from: classes2.dex */
public final class ClientProfile {
    public static final int INVALID_GROUP_ID = -1;
    public static final int INVALID_RESOURCE_ID = -1;
    private int mGroupId;
    private final int mId;
    private boolean mIsPriorityOverwritten;
    private int mNiceValue;
    private int mPrimaryUsingFrontendHandle;
    private int mPriority;
    private final int mProcessId;
    private Set<Integer> mShareFeClientIds;
    private final String mTvInputSessionId;
    private final int mUseCase;
    private int mUsingCasSystemId;
    private int mUsingCiCamId;
    private Set<Integer> mUsingFrontendHandles;
    private Set<Integer> mUsingLnbHandles;

    private ClientProfile(Builder builder) {
        this.mGroupId = -1;
        this.mPrimaryUsingFrontendHandle = -1;
        this.mUsingFrontendHandles = new HashSet();
        this.mShareFeClientIds = new HashSet();
        this.mUsingLnbHandles = new HashSet();
        this.mUsingCasSystemId = -1;
        this.mUsingCiCamId = -1;
        this.mIsPriorityOverwritten = false;
        this.mId = builder.mId;
        this.mTvInputSessionId = builder.mTvInputSessionId;
        this.mUseCase = builder.mUseCase;
        this.mProcessId = builder.mProcessId;
    }

    public int getId() {
        return this.mId;
    }

    public String getTvInputSessionId() {
        return this.mTvInputSessionId;
    }

    public int getUseCase() {
        return this.mUseCase;
    }

    public int getProcessId() {
        return this.mProcessId;
    }

    public boolean isPriorityOverwritten() {
        return this.mIsPriorityOverwritten;
    }

    public int getGroupId() {
        return this.mGroupId;
    }

    public int getPriority() {
        return this.mPriority - this.mNiceValue;
    }

    public void setGroupId(int groupId) {
        this.mGroupId = groupId;
    }

    public void setPriority(int priority) {
        if (priority < 0) {
            return;
        }
        this.mPriority = priority;
    }

    public void overwritePriority(int priority) {
        if (priority < 0) {
            return;
        }
        this.mIsPriorityOverwritten = true;
        this.mPriority = priority;
    }

    public void setNiceValue(int niceValue) {
        this.mNiceValue = niceValue;
    }

    public void useFrontend(int frontendHandle) {
        this.mUsingFrontendHandles.add(Integer.valueOf(frontendHandle));
    }

    public void setPrimaryFrontend(int frontendHandle) {
        this.mPrimaryUsingFrontendHandle = frontendHandle;
    }

    public int getPrimaryFrontend() {
        return this.mPrimaryUsingFrontendHandle;
    }

    public void shareFrontend(int clientId) {
        this.mShareFeClientIds.add(Integer.valueOf(clientId));
    }

    public void stopSharingFrontend(int clientId) {
        this.mShareFeClientIds.remove(Integer.valueOf(clientId));
    }

    public Set<Integer> getInUseFrontendHandles() {
        return this.mUsingFrontendHandles;
    }

    public Set<Integer> getShareFeClientIds() {
        return this.mShareFeClientIds;
    }

    public void releaseFrontend() {
        this.mUsingFrontendHandles.clear();
        this.mShareFeClientIds.clear();
        this.mPrimaryUsingFrontendHandle = -1;
    }

    public void useLnb(int lnbHandle) {
        this.mUsingLnbHandles.add(Integer.valueOf(lnbHandle));
    }

    public Set<Integer> getInUseLnbHandles() {
        return this.mUsingLnbHandles;
    }

    public void releaseLnb(int lnbHandle) {
        this.mUsingLnbHandles.remove(Integer.valueOf(lnbHandle));
    }

    public void useCas(int casSystemId) {
        this.mUsingCasSystemId = casSystemId;
    }

    public int getInUseCasSystemId() {
        return this.mUsingCasSystemId;
    }

    public void releaseCas() {
        this.mUsingCasSystemId = -1;
    }

    public void useCiCam(int ciCamId) {
        this.mUsingCiCamId = ciCamId;
    }

    public int getInUseCiCamId() {
        return this.mUsingCiCamId;
    }

    public void releaseCiCam() {
        this.mUsingCiCamId = -1;
    }

    public void reclaimAllResources() {
        this.mUsingFrontendHandles.clear();
        this.mShareFeClientIds.clear();
        this.mPrimaryUsingFrontendHandle = -1;
        this.mUsingLnbHandles.clear();
        this.mUsingCasSystemId = -1;
        this.mUsingCiCamId = -1;
    }

    public String toString() {
        return "ClientProfile[id=" + this.mId + ", tvInputSessionId=" + this.mTvInputSessionId + ", useCase=" + this.mUseCase + ", processId=" + this.mProcessId + "]";
    }

    /* loaded from: classes2.dex */
    public static class Builder {
        private final int mId;
        private int mProcessId;
        private String mTvInputSessionId;
        private int mUseCase;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(int id) {
            this.mId = id;
        }

        public Builder useCase(int useCase) {
            this.mUseCase = useCase;
            return this;
        }

        public Builder tvInputSessionId(String tvInputSessionId) {
            this.mTvInputSessionId = tvInputSessionId;
            return this;
        }

        public Builder processId(int processId) {
            this.mProcessId = processId;
            return this;
        }

        public ClientProfile build() {
            ClientProfile clientProfile = new ClientProfile(this);
            return clientProfile;
        }
    }
}
