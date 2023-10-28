package android.net.util;

import android.net.KeepalivePacketData;
import android.net.NattKeepalivePacketData;
import android.net.NattKeepalivePacketDataParcelable;
import android.net.TcpKeepalivePacketData;
import android.net.TcpKeepalivePacketDataParcelable;
import android.os.Build;
import android.util.Log;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/* loaded from: classes.dex */
public final class KeepalivePacketDataUtil {
    private static final int IPV4_HEADER_LENGTH = 20;
    private static final int IPV6_HEADER_LENGTH = 40;
    private static final String TAG = KeepalivePacketDataUtil.class.getSimpleName();

    public static NattKeepalivePacketDataParcelable toStableParcelable(NattKeepalivePacketData pkt) {
        NattKeepalivePacketDataParcelable parcel = new NattKeepalivePacketDataParcelable();
        InetAddress srcAddress = pkt.getSrcAddress();
        InetAddress dstAddress = pkt.getDstAddress();
        parcel.srcAddress = srcAddress.getAddress();
        parcel.srcPort = pkt.getSrcPort();
        parcel.dstAddress = dstAddress.getAddress();
        parcel.dstPort = pkt.getDstPort();
        return parcel;
    }

    public static TcpKeepalivePacketDataParcelable toStableParcelable(TcpKeepalivePacketData pkt) {
        TcpKeepalivePacketDataParcelable parcel = new TcpKeepalivePacketDataParcelable();
        InetAddress srcAddress = pkt.getSrcAddress();
        InetAddress dstAddress = pkt.getDstAddress();
        parcel.srcAddress = srcAddress.getAddress();
        parcel.srcPort = pkt.getSrcPort();
        parcel.dstAddress = dstAddress.getAddress();
        parcel.dstPort = pkt.getDstPort();
        parcel.seq = pkt.getTcpSeq();
        parcel.ack = pkt.getTcpAck();
        parcel.rcvWnd = pkt.getTcpWindow();
        parcel.rcvWndScale = pkt.getTcpWindowScale();
        parcel.tos = pkt.getIpTos();
        parcel.ttl = pkt.getIpTtl();
        return parcel;
    }

    @Deprecated
    public static TcpKeepalivePacketDataParcelable parseTcpKeepalivePacketData(KeepalivePacketData data) {
        if (data == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT > 30) {
            Log.wtf(TAG, "parseTcpKeepalivePacketData should not be used after R, use TcpKeepalivePacketData instead.");
        }
        ByteBuffer buffer = ByteBuffer.wrap(data.getPacket());
        buffer.order(ByteOrder.BIG_ENDIAN);
        try {
            int tcpSeq = buffer.getInt(24);
            int tcpAck = buffer.getInt(28);
            int wndSize = buffer.getShort(34);
            int ipTos = buffer.get(1);
            int ttl = buffer.get(8);
            TcpKeepalivePacketDataParcelable p = new TcpKeepalivePacketDataParcelable();
            p.srcAddress = data.getSrcAddress().getAddress();
            p.srcPort = data.getSrcPort();
            p.dstAddress = data.getDstAddress().getAddress();
            p.dstPort = data.getDstPort();
            p.seq = tcpSeq;
            p.ack = tcpAck;
            p.rcvWnd = wndSize;
            p.rcvWndScale = 0;
            p.tos = ipTos;
            p.ttl = ttl;
            return p;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}
