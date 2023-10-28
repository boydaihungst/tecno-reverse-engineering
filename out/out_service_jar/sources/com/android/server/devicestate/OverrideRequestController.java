package com.android.server.devicestate;

import android.os.IBinder;
import android.util.Slog;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class OverrideRequestController {
    static final int STATUS_ACTIVE = 1;
    static final int STATUS_CANCELED = 2;
    static final int STATUS_UNKNOWN = 0;
    private static final String TAG = "OverrideRequestController";
    private final StatusChangeListener mListener;
    private OverrideRequest mRequest;
    private boolean mStickyRequest;
    private boolean mStickyRequestsAllowed;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface RequestStatus {
    }

    /* loaded from: classes.dex */
    public interface StatusChangeListener {
        void onStatusChanged(OverrideRequest overrideRequest, int i);
    }

    static String statusToString(int status) {
        switch (status) {
            case 0:
                return "UNKNOWN";
            case 1:
                return "ACTIVE";
            case 2:
                return "CANCELED";
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OverrideRequestController(StatusChangeListener listener) {
        this.mListener = listener;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setStickyRequestsAllowed(boolean stickyRequestsAllowed) {
        this.mStickyRequestsAllowed = stickyRequestsAllowed;
        if (!stickyRequestsAllowed) {
            cancelStickyRequest();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addRequest(OverrideRequest request) {
        OverrideRequest previousRequest = this.mRequest;
        this.mRequest = request;
        this.mListener.onStatusChanged(request, 1);
        if (previousRequest != null) {
            cancelRequestLocked(previousRequest);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelRequest(OverrideRequest request) {
        if (!hasRequest(request.getToken())) {
            return;
        }
        cancelCurrentRequestLocked();
    }

    void cancelStickyRequest() {
        if (this.mStickyRequest) {
            cancelCurrentRequestLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelOverrideRequest() {
        cancelCurrentRequestLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasRequest(IBinder token) {
        OverrideRequest overrideRequest = this.mRequest;
        return overrideRequest != null && token == overrideRequest.getToken();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleProcessDied(int pid) {
        OverrideRequest overrideRequest = this.mRequest;
        if (overrideRequest != null && overrideRequest.getPid() == pid) {
            if (this.mStickyRequestsAllowed) {
                this.mStickyRequest = true;
            } else {
                cancelCurrentRequestLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleBaseStateChanged() {
        OverrideRequest overrideRequest = this.mRequest;
        if (overrideRequest != null && (overrideRequest.getFlags() & 1) != 0) {
            cancelCurrentRequestLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleNewSupportedStates(int[] newSupportedStates) {
        OverrideRequest overrideRequest = this.mRequest;
        if (overrideRequest != null && !contains(newSupportedStates, overrideRequest.getRequestedState())) {
            cancelCurrentRequestLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpInternal(PrintWriter pw) {
        OverrideRequest overrideRequest = this.mRequest;
        boolean requestActive = overrideRequest != null;
        pw.println();
        pw.println("Override Request active: " + requestActive);
        if (requestActive) {
            pw.println("Request: mPid=" + overrideRequest.getPid() + ", mRequestedState=" + overrideRequest.getRequestedState() + ", mFlags=" + overrideRequest.getFlags() + ", mStatus=" + statusToString(1));
        }
    }

    private void cancelRequestLocked(OverrideRequest requestToCancel) {
        this.mListener.onStatusChanged(requestToCancel, 2);
    }

    private void cancelCurrentRequestLocked() {
        OverrideRequest overrideRequest = this.mRequest;
        if (overrideRequest == null) {
            Slog.w(TAG, "Attempted to cancel a null OverrideRequest");
            return;
        }
        this.mStickyRequest = false;
        this.mListener.onStatusChanged(overrideRequest, 2);
        this.mRequest = null;
    }

    private static boolean contains(int[] array, int value) {
        for (int i : array) {
            if (i == value) {
                return true;
            }
        }
        return false;
    }
}
