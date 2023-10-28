package com.android.server.backup.params;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.restore.ActiveRestoreSession;
import com.android.server.backup.transport.TransportConnection;
/* loaded from: classes.dex */
public class RestoreGetSetsParams {
    public final OnTaskFinishedListener listener;
    public final TransportConnection mTransportConnection;
    public final IBackupManagerMonitor monitor;
    public final IRestoreObserver observer;
    public final ActiveRestoreSession session;

    public RestoreGetSetsParams(TransportConnection _transportConnection, ActiveRestoreSession _session, IRestoreObserver _observer, IBackupManagerMonitor _monitor, OnTaskFinishedListener _listener) {
        this.mTransportConnection = _transportConnection;
        this.session = _session;
        this.observer = _observer;
        this.monitor = _monitor;
        this.listener = _listener;
    }
}
