package android.service.carrier;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes3.dex */
public final class MessagePdu implements Parcelable {
    public static final Parcelable.Creator<MessagePdu> CREATOR = new Parcelable.Creator<MessagePdu>() { // from class: android.service.carrier.MessagePdu.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MessagePdu createFromParcel(Parcel source) {
            List<byte[]> pduList;
            int size = source.readInt();
            if (size == -1) {
                pduList = null;
            } else {
                pduList = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    pduList.add(source.createByteArray());
                }
            }
            return new MessagePdu(pduList);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MessagePdu[] newArray(int size) {
            return new MessagePdu[size];
        }
    };
    private static final int NULL_LENGTH = -1;
    private final List<byte[]> mPduList;

    public MessagePdu(List<byte[]> pduList) {
        if (pduList == null || pduList.contains(null)) {
            throw new IllegalArgumentException("pduList must not be null or contain nulls");
        }
        this.mPduList = pduList;
    }

    public List<byte[]> getPdus() {
        return this.mPduList;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        List<byte[]> list = this.mPduList;
        if (list == null) {
            dest.writeInt(-1);
            return;
        }
        dest.writeInt(list.size());
        for (byte[] messagePdu : this.mPduList) {
            dest.writeByteArray(messagePdu);
        }
    }
}
