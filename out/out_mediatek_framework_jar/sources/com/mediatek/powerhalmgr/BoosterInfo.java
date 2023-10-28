package com.mediatek.powerhalmgr;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
/* loaded from: classes.dex */
public class BoosterInfo implements Parcelable {
    public static final int BOOSTER_ACTION_ADD_BY_LINKINFO = 4;
    public static final int BOOSTER_ACTION_ADD_BY_UID = 1;
    public static final int BOOSTER_ACTION_BASE = 0;
    public static final int BOOSTER_ACTION_DEL_ALL = 7;
    public static final int BOOSTER_ACTION_DEL_BY_LINKINFO = 5;
    public static final int BOOSTER_ACTION_DEL_BY_LINKINFO_ALL = 6;
    public static final int BOOSTER_ACTION_DEL_BY_UID = 2;
    public static final int BOOSTER_ACTION_DEL_BY_UID_ALL = 3;
    public static final int BOOSTER_GROUP_D;
    public static final int BOOSTER_GROUP_MAX;
    private int mAction;
    private String mDstIp;
    private int mDstPort;
    private int mGroup;
    private String[] mMoreInfo;
    private int[] mMoreValue;
    private int mProto;
    private String mSrcIp;
    private int mSrcPort;
    private int mUid;
    public static int BOOSTER_GROUP_BASE = 0;
    public static final int BOOSTER_GROUP_A = 0 + 1;
    public static final int BOOSTER_GROUP_B = 0 + 2;
    public static final int BOOSTER_GROUP_C = 0 + 3;
    public static final Parcelable.Creator<BoosterInfo> CREATOR = new Parcelable.Creator<BoosterInfo>() { // from class: com.mediatek.powerhalmgr.BoosterInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public BoosterInfo createFromParcel(Parcel in) {
            return new BoosterInfo(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public BoosterInfo[] newArray(int size) {
            return new BoosterInfo[size];
        }
    };

    static {
        int i = 0 + 4;
        BOOSTER_GROUP_D = i;
        BOOSTER_GROUP_MAX = i;
    }

    public BoosterInfo(BoosterInfo info) {
        this(info.mGroup, info.mAction, info.mUid, info.mSrcIp, info.mDstIp, info.mSrcPort, info.mDstPort, info.mProto, info.mMoreInfo, info.mMoreValue);
    }

    public BoosterInfo(int group, int action, int uid) {
        this(group, action, uid, null, null, -1, -1, -1, null, null);
    }

    public BoosterInfo(int group, int action, String srcIp, String dstIp, int srcPort, int dstPort, int proto) {
        this(group, action, -1, srcIp, dstIp, srcPort, dstPort, proto, null, null);
    }

    public BoosterInfo(int group, int action) {
        this(group, action, -1, null, null, -1, -1, -1, null, null);
    }

    public BoosterInfo(int group, int action, int uid, String srcIp, String dstIp, int srcPort, int dstPort, int proto) {
        this(group, action, uid, srcIp, dstIp, srcPort, dstPort, proto, null, null);
    }

    public BoosterInfo(int group, int action, String[] sa, int[] ia) {
        this(group, action, -1, null, null, -1, -1, -1, sa, ia);
    }

    public BoosterInfo(int group, int action, int uid, String srcIp, String dstIp, int srcPort, int dstPort, int proto, String[] sa, int[] ia) {
        this.mGroup = -1;
        this.mAction = -1;
        this.mUid = -1;
        this.mSrcIp = null;
        this.mDstIp = null;
        this.mSrcPort = -1;
        this.mDstPort = -1;
        this.mProto = -1;
        this.mMoreInfo = null;
        this.mMoreValue = null;
        this.mGroup = group;
        this.mAction = action;
        this.mUid = uid;
        this.mSrcIp = srcIp;
        this.mDstIp = dstIp;
        this.mSrcPort = srcPort;
        this.mDstPort = dstPort;
        this.mProto = proto;
        this.mMoreInfo = sa;
        this.mMoreValue = ia;
    }

    private BoosterInfo(Parcel in) {
        this.mGroup = -1;
        this.mAction = -1;
        this.mUid = -1;
        this.mSrcIp = null;
        this.mDstIp = null;
        this.mSrcPort = -1;
        this.mDstPort = -1;
        this.mProto = -1;
        this.mMoreInfo = null;
        this.mMoreValue = null;
        readFromParcel(in);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public int getGroup() {
        return this.mGroup;
    }

    public int getAction() {
        return this.mAction;
    }

    public int getUid() {
        return this.mUid;
    }

    public String getSrcIp() {
        return this.mSrcIp;
    }

    public String getDstIp() {
        return this.mDstIp;
    }

    public int getSrcPort() {
        return this.mSrcPort;
    }

    public int getDstPort() {
        return this.mDstPort;
    }

    public int getProto() {
        return this.mProto;
    }

    public String[] getMoreInfo() {
        return this.mMoreInfo;
    }

    public int[] getMoreValue() {
        return this.mMoreValue;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mGroup);
        out.writeInt(this.mAction);
        out.writeInt(this.mUid);
        out.writeString(this.mSrcIp);
        out.writeString(this.mDstIp);
        out.writeInt(this.mSrcPort);
        out.writeInt(this.mDstPort);
        out.writeInt(this.mProto);
        out.writeStringArray(this.mMoreInfo);
        out.writeIntArray(this.mMoreValue);
    }

    private void readFromParcel(Parcel in) {
        this.mGroup = in.readInt();
        this.mAction = in.readInt();
        this.mUid = in.readInt();
        this.mSrcIp = in.readString();
        this.mDstIp = in.readString();
        this.mSrcPort = in.readInt();
        this.mDstPort = in.readInt();
        this.mProto = in.readInt();
        this.mMoreInfo = in.createStringArray();
        this.mMoreValue = in.createIntArray();
    }

    public String toString() {
        return "BoosterInfo(" + this.mGroup + "," + this.mAction + "," + this.mUid + "," + this.mSrcIp + "," + this.mDstIp + "," + this.mSrcPort + "," + this.mDstPort + "," + this.mProto + "," + Arrays.toString(this.mMoreInfo) + "," + Arrays.toString(this.mMoreValue) + ")";
    }
}
