package com.android.server.accessibility;

import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Slog;
import android.view.MagnificationSpec;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.IAccessibilityInteractionConnection;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class ActionReplacingCallback extends IAccessibilityInteractionConnectionCallback.Stub {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "ActionReplacingCallback";
    private final IAccessibilityInteractionConnection mConnectionWithReplacementActions;
    private final int mInteractionId;
    AccessibilityNodeInfo mNodeFromOriginalWindow;
    AccessibilityNodeInfo mNodeWithReplacementActions;
    private final int mNodeWithReplacementActionsInteractionId;
    List<AccessibilityNodeInfo> mNodesFromOriginalWindow;
    List<AccessibilityNodeInfo> mPrefetchedNodesFromOriginalWindow;
    private boolean mReplacementNodeIsReadyOrFailed;
    private final IAccessibilityInteractionConnectionCallback mServiceCallback;
    private final Object mLock = new Object();
    boolean mSetFindNodeFromOriginalWindowCalled = false;
    boolean mSetFindNodesFromOriginalWindowCalled = false;
    boolean mSetPrefetchFromOriginalWindowCalled = false;

    public ActionReplacingCallback(IAccessibilityInteractionConnectionCallback serviceCallback, IAccessibilityInteractionConnection connectionWithReplacementActions, int interactionId, int interrogatingPid, long interrogatingTid) {
        this.mServiceCallback = serviceCallback;
        this.mConnectionWithReplacementActions = connectionWithReplacementActions;
        this.mInteractionId = interactionId;
        int i = interactionId + 1;
        this.mNodeWithReplacementActionsInteractionId = i;
        long identityToken = Binder.clearCallingIdentity();
        try {
            try {
                connectionWithReplacementActions.findAccessibilityNodeInfoByAccessibilityId(AccessibilityNodeInfo.ROOT_NODE_ID, (Region) null, i, this, 0, interrogatingPid, interrogatingTid, (MagnificationSpec) null, (float[]) null, (Bundle) null);
            } catch (RemoteException e) {
                this.mReplacementNodeIsReadyOrFailed = true;
            }
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    public void setFindAccessibilityNodeInfoResult(AccessibilityNodeInfo info, int interactionId) {
        synchronized (this.mLock) {
            if (interactionId == this.mInteractionId) {
                this.mNodeFromOriginalWindow = info;
                this.mSetFindNodeFromOriginalWindowCalled = true;
            } else if (interactionId == this.mNodeWithReplacementActionsInteractionId) {
                this.mNodeWithReplacementActions = info;
                this.mReplacementNodeIsReadyOrFailed = true;
            } else {
                Slog.e(LOG_TAG, "Callback with unexpected interactionId");
                return;
            }
            replaceInfoActionsAndCallServiceIfReady();
        }
    }

    public void setFindAccessibilityNodeInfosResult(List<AccessibilityNodeInfo> infos, int interactionId) {
        synchronized (this.mLock) {
            if (interactionId == this.mInteractionId) {
                this.mNodesFromOriginalWindow = infos;
                this.mSetFindNodesFromOriginalWindowCalled = true;
            } else if (interactionId == this.mNodeWithReplacementActionsInteractionId) {
                setNodeWithReplacementActionsFromList(infos);
                this.mReplacementNodeIsReadyOrFailed = true;
            } else {
                Slog.e(LOG_TAG, "Callback with unexpected interactionId");
                return;
            }
            replaceInfoActionsAndCallServiceIfReady();
        }
    }

    public void setPrefetchAccessibilityNodeInfoResult(List<AccessibilityNodeInfo> infos, int interactionId) throws RemoteException {
        synchronized (this.mLock) {
            if (interactionId == this.mInteractionId) {
                this.mPrefetchedNodesFromOriginalWindow = infos;
                this.mSetPrefetchFromOriginalWindowCalled = true;
                replaceInfoActionsAndCallServiceIfReady();
                return;
            }
            Slog.e(LOG_TAG, "Callback with unexpected interactionId");
        }
    }

    private void replaceInfoActionsAndCallServiceIfReady() {
        replaceInfoActionsAndCallService();
        replaceInfosActionsAndCallService();
        replacePrefetchInfosActionsAndCallService();
    }

    private void setNodeWithReplacementActionsFromList(List<AccessibilityNodeInfo> infos) {
        for (int i = 0; i < infos.size(); i++) {
            AccessibilityNodeInfo info = infos.get(i);
            if (info.getSourceNodeId() == AccessibilityNodeInfo.ROOT_NODE_ID) {
                this.mNodeWithReplacementActions = info;
            }
        }
    }

    public void setPerformAccessibilityActionResult(boolean succeeded, int interactionId) throws RemoteException {
        this.mServiceCallback.setPerformAccessibilityActionResult(succeeded, interactionId);
    }

    private void replaceInfoActionsAndCallService() {
        boolean doCallback;
        AccessibilityNodeInfo nodeToReturn;
        AccessibilityNodeInfo accessibilityNodeInfo;
        synchronized (this.mLock) {
            doCallback = this.mReplacementNodeIsReadyOrFailed && this.mSetFindNodeFromOriginalWindowCalled;
            if (doCallback && (accessibilityNodeInfo = this.mNodeFromOriginalWindow) != null) {
                replaceActionsOnInfoLocked(accessibilityNodeInfo);
                this.mSetFindNodeFromOriginalWindowCalled = false;
            }
            nodeToReturn = this.mNodeFromOriginalWindow;
        }
        if (doCallback) {
            try {
                this.mServiceCallback.setFindAccessibilityNodeInfoResult(nodeToReturn, this.mInteractionId);
            } catch (RemoteException e) {
            }
        }
    }

    private void replaceInfosActionsAndCallService() {
        boolean doCallback;
        List<AccessibilityNodeInfo> nodesToReturn = null;
        synchronized (this.mLock) {
            doCallback = this.mReplacementNodeIsReadyOrFailed && this.mSetFindNodesFromOriginalWindowCalled;
            if (doCallback) {
                nodesToReturn = replaceActionsLocked(this.mNodesFromOriginalWindow);
                this.mSetFindNodesFromOriginalWindowCalled = false;
            }
        }
        if (doCallback) {
            try {
                this.mServiceCallback.setFindAccessibilityNodeInfosResult(nodesToReturn, this.mInteractionId);
            } catch (RemoteException e) {
            }
        }
    }

    private void replacePrefetchInfosActionsAndCallService() {
        boolean doCallback;
        List<AccessibilityNodeInfo> nodesToReturn = null;
        synchronized (this.mLock) {
            doCallback = this.mReplacementNodeIsReadyOrFailed && this.mSetPrefetchFromOriginalWindowCalled;
            if (doCallback) {
                nodesToReturn = replaceActionsLocked(this.mPrefetchedNodesFromOriginalWindow);
                this.mSetPrefetchFromOriginalWindowCalled = false;
            }
        }
        if (doCallback) {
            try {
                this.mServiceCallback.setPrefetchAccessibilityNodeInfoResult(nodesToReturn, this.mInteractionId);
            } catch (RemoteException e) {
            }
        }
    }

    private List<AccessibilityNodeInfo> replaceActionsLocked(List<AccessibilityNodeInfo> infos) {
        if (infos != null) {
            for (int i = 0; i < infos.size(); i++) {
                replaceActionsOnInfoLocked(infos.get(i));
            }
        }
        if (infos == null) {
            return null;
        }
        return new ArrayList(infos);
    }

    private void replaceActionsOnInfoLocked(AccessibilityNodeInfo info) {
        AccessibilityNodeInfo accessibilityNodeInfo;
        info.removeAllActions();
        info.setClickable(false);
        info.setFocusable(false);
        info.setContextClickable(false);
        info.setScrollable(false);
        info.setLongClickable(false);
        info.setDismissable(false);
        if (info.getSourceNodeId() == AccessibilityNodeInfo.ROOT_NODE_ID && (accessibilityNodeInfo = this.mNodeWithReplacementActions) != null) {
            List<AccessibilityNodeInfo.AccessibilityAction> actions = accessibilityNodeInfo.getActionList();
            if (actions != null) {
                for (int j = 0; j < actions.size(); j++) {
                    info.addAction(actions.get(j));
                }
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS);
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            info.setClickable(this.mNodeWithReplacementActions.isClickable());
            info.setFocusable(this.mNodeWithReplacementActions.isFocusable());
            info.setContextClickable(this.mNodeWithReplacementActions.isContextClickable());
            info.setScrollable(this.mNodeWithReplacementActions.isScrollable());
            info.setLongClickable(this.mNodeWithReplacementActions.isLongClickable());
            info.setDismissable(this.mNodeWithReplacementActions.isDismissable());
        }
    }
}
