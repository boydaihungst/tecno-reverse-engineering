package android.content.om;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
/* loaded from: classes.dex */
public class OverlayManagerTransaction implements Iterable<Request>, Parcelable {
    public static final Parcelable.Creator<OverlayManagerTransaction> CREATOR = new Parcelable.Creator<OverlayManagerTransaction>() { // from class: android.content.om.OverlayManagerTransaction.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public OverlayManagerTransaction createFromParcel(Parcel source) {
            return new OverlayManagerTransaction(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public OverlayManagerTransaction[] newArray(int size) {
            return new OverlayManagerTransaction[size];
        }
    };
    private final List<Request> mRequests;

    OverlayManagerTransaction(List<Request> requests) {
        Preconditions.checkNotNull(requests);
        if (requests.contains(null)) {
            throw new IllegalArgumentException("null request");
        }
        this.mRequests = requests;
    }

    private OverlayManagerTransaction(Parcel source) {
        int size = source.readInt();
        this.mRequests = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            int request = source.readInt();
            OverlayIdentifier overlay = (OverlayIdentifier) source.readParcelable(null, OverlayIdentifier.class);
            int userId = source.readInt();
            Bundle extras = source.readBundle(null);
            this.mRequests.add(new Request(request, overlay, userId, extras));
        }
    }

    @Override // java.lang.Iterable
    public Iterator<Request> iterator() {
        return this.mRequests.iterator();
    }

    public String toString() {
        return String.format("OverlayManagerTransaction { mRequests = %s }", this.mRequests);
    }

    /* loaded from: classes.dex */
    public static class Request {
        public static final String BUNDLE_FABRICATED_OVERLAY = "fabricated_overlay";
        public static final int TYPE_REGISTER_FABRICATED = 2;
        public static final int TYPE_SET_DISABLED = 1;
        public static final int TYPE_SET_ENABLED = 0;
        public static final int TYPE_UNREGISTER_FABRICATED = 3;
        public final Bundle extras;
        public final OverlayIdentifier overlay;
        public final int type;
        public final int userId;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        @interface RequestType {
        }

        public Request(int type, OverlayIdentifier overlay, int userId) {
            this(type, overlay, userId, null);
        }

        public Request(int type, OverlayIdentifier overlay, int userId, Bundle extras) {
            this.type = type;
            this.overlay = overlay;
            this.userId = userId;
            this.extras = extras;
        }

        public String toString() {
            return String.format(Locale.US, "Request{type=0x%02x (%s), overlay=%s, userId=%d}", Integer.valueOf(this.type), typeToString(), this.overlay, Integer.valueOf(this.userId));
        }

        public String typeToString() {
            int i = this.type;
            switch (i) {
                case 0:
                    return "TYPE_SET_ENABLED";
                case 1:
                    return "TYPE_SET_DISABLED";
                case 2:
                    return "TYPE_REGISTER_FABRICATED";
                case 3:
                    return "TYPE_UNREGISTER_FABRICATED";
                default:
                    return String.format("TYPE_UNKNOWN (0x%02x)", Integer.valueOf(i));
            }
        }
    }

    /* loaded from: classes.dex */
    public static class Builder {
        private final List<Request> mRequests = new ArrayList();

        public Builder setEnabled(OverlayIdentifier overlay, boolean enable) {
            return setEnabled(overlay, enable, UserHandle.myUserId());
        }

        public Builder setEnabled(OverlayIdentifier overlay, boolean enable, int userId) {
            Preconditions.checkNotNull(overlay);
            int type = !enable ? 1 : 0;
            this.mRequests.add(new Request(type, overlay, userId));
            return this;
        }

        public Builder registerFabricatedOverlay(FabricatedOverlay overlay) {
            Bundle extras = new Bundle();
            extras.putParcelable(Request.BUNDLE_FABRICATED_OVERLAY, overlay.mOverlay);
            this.mRequests.add(new Request(2, overlay.getIdentifier(), -1, extras));
            return this;
        }

        public Builder unregisterFabricatedOverlay(OverlayIdentifier overlay) {
            this.mRequests.add(new Request(3, overlay, -1));
            return this;
        }

        public OverlayManagerTransaction build() {
            return new OverlayManagerTransaction(this.mRequests);
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        int size = this.mRequests.size();
        dest.writeInt(size);
        for (int i = 0; i < size; i++) {
            Request req = this.mRequests.get(i);
            dest.writeInt(req.type);
            dest.writeParcelable(req.overlay, flags);
            dest.writeInt(req.userId);
            dest.writeBundle(req.extras);
        }
    }
}
