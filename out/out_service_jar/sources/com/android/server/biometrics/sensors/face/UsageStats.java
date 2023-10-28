package com.android.server.biometrics.sensors.face;

import android.content.Context;
import android.hardware.face.FaceManager;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Iterator;
/* loaded from: classes.dex */
public class UsageStats {
    private static final int EVENT_LOG_SIZE = 100;
    private int mAcceptCount;
    private long mAcceptLatency;
    private Context mContext;
    private int mRejectCount;
    private long mRejectLatency;
    private ArrayDeque<AuthenticationEvent> mAuthenticationEvents = new ArrayDeque<>();
    private SparseIntArray mErrorCount = new SparseIntArray();
    private SparseLongArray mErrorLatency = new SparseLongArray();

    /* loaded from: classes.dex */
    public static final class AuthenticationEvent {
        private boolean mAuthenticated;
        private int mError;
        private long mLatency;
        private long mStartTime;
        private int mUser;
        private int mVendorError;

        public AuthenticationEvent(long startTime, long latency, boolean authenticated, int error, int vendorError, int user) {
            this.mStartTime = startTime;
            this.mLatency = latency;
            this.mAuthenticated = authenticated;
            this.mError = error;
            this.mVendorError = vendorError;
            this.mUser = user;
        }

        public String toString(Context context) {
            return "Start: " + this.mStartTime + "\tLatency: " + this.mLatency + "\tAuthenticated: " + this.mAuthenticated + "\tError: " + this.mError + "\tVendorCode: " + this.mVendorError + "\tUser: " + this.mUser + "\t" + FaceManager.getErrorString(context, this.mError, this.mVendorError);
        }
    }

    public UsageStats(Context context) {
        this.mContext = context;
    }

    public void addEvent(AuthenticationEvent event) {
        if (this.mAuthenticationEvents.size() >= 100) {
            this.mAuthenticationEvents.removeFirst();
        }
        this.mAuthenticationEvents.add(event);
        if (event.mAuthenticated) {
            this.mAcceptCount++;
            this.mAcceptLatency += event.mLatency;
        } else if (event.mError == 0) {
            this.mRejectCount++;
            this.mRejectLatency += event.mLatency;
        } else {
            this.mErrorCount.put(event.mError, this.mErrorCount.get(event.mError, 0) + 1);
            this.mErrorLatency.put(event.mError, this.mErrorLatency.get(event.mError, 0L) + event.mLatency);
        }
    }

    public void print(PrintWriter pw) {
        pw.println("Events since last reboot: " + this.mAuthenticationEvents.size());
        Iterator<AuthenticationEvent> it = this.mAuthenticationEvents.iterator();
        while (it.hasNext()) {
            AuthenticationEvent event = it.next();
            pw.println(event.toString(this.mContext));
        }
        StringBuilder append = new StringBuilder().append("Accept\tCount: ").append(this.mAcceptCount).append("\tLatency: ").append(this.mAcceptLatency).append("\tAverage: ");
        int i = this.mAcceptCount;
        pw.println(append.append(i > 0 ? this.mAcceptLatency / i : 0L).toString());
        StringBuilder append2 = new StringBuilder().append("Reject\tCount: ").append(this.mRejectCount).append("\tLatency: ").append(this.mRejectLatency).append("\tAverage: ");
        int i2 = this.mRejectCount;
        pw.println(append2.append(i2 > 0 ? this.mRejectLatency / i2 : 0L).toString());
        for (int i3 = 0; i3 < this.mErrorCount.size(); i3++) {
            int key = this.mErrorCount.keyAt(i3);
            int count = this.mErrorCount.get(i3);
            pw.println("Error" + key + "\tCount: " + count + "\tLatency: " + this.mErrorLatency.get(key, 0L) + "\tAverage: " + (count > 0 ? this.mErrorLatency.get(key, 0L) / count : 0L) + "\t" + FaceManager.getErrorString(this.mContext, key, 0));
        }
    }
}
