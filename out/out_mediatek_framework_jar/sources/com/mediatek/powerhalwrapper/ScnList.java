package com.mediatek.powerhalwrapper;
/* loaded from: classes.dex */
public class ScnList {
    public int handle;
    public int pid;
    public int uid;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ScnList(int handle, int pid, int uid) {
        this.handle = handle;
        this.pid = pid;
        this.uid = uid;
    }

    public int getpid() {
        return this.pid;
    }

    public void setpid(int pid) {
        this.pid = pid;
    }

    public int getuid() {
        return this.uid;
    }

    public void setPack_Name(int uid) {
        this.uid = uid;
    }

    public int gethandle() {
        return this.handle;
    }

    public void sethandle(int handle) {
        this.handle = handle;
    }
}
