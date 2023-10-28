package android.app.job;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public final class JobWorkItem implements Parcelable {
    public static final Parcelable.Creator<JobWorkItem> CREATOR = new Parcelable.Creator<JobWorkItem>() { // from class: android.app.job.JobWorkItem.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public JobWorkItem createFromParcel(Parcel in) {
            return new JobWorkItem(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public JobWorkItem[] newArray(int size) {
            return new JobWorkItem[size];
        }
    };
    int mDeliveryCount;
    Object mGrants;
    final Intent mIntent;
    private final long mMinimumChunkBytes;
    private final long mNetworkDownloadBytes;
    private final long mNetworkUploadBytes;
    int mWorkId;

    public JobWorkItem(Intent intent) {
        this(intent, -1L, -1L);
    }

    public JobWorkItem(Intent intent, long downloadBytes, long uploadBytes) {
        this(intent, downloadBytes, uploadBytes, -1L);
    }

    public JobWorkItem(Intent intent, long downloadBytes, long uploadBytes, long minimumChunkBytes) {
        long estimatedTransfer;
        if (minimumChunkBytes != -1 && minimumChunkBytes <= 0) {
            throw new IllegalArgumentException("Minimum chunk size must be positive");
        }
        if (uploadBytes == -1) {
            estimatedTransfer = downloadBytes;
        } else {
            estimatedTransfer = uploadBytes + (downloadBytes != -1 ? downloadBytes : 0L);
        }
        if (minimumChunkBytes != -1 && estimatedTransfer != -1 && minimumChunkBytes > estimatedTransfer) {
            throw new IllegalArgumentException("Minimum chunk size can't be greater than estimated network usage");
        }
        this.mIntent = intent;
        this.mNetworkDownloadBytes = downloadBytes;
        this.mNetworkUploadBytes = uploadBytes;
        this.mMinimumChunkBytes = minimumChunkBytes;
    }

    public Intent getIntent() {
        return this.mIntent;
    }

    public long getEstimatedNetworkDownloadBytes() {
        return this.mNetworkDownloadBytes;
    }

    public long getEstimatedNetworkUploadBytes() {
        return this.mNetworkUploadBytes;
    }

    public long getMinimumNetworkChunkBytes() {
        return this.mMinimumChunkBytes;
    }

    public int getDeliveryCount() {
        return this.mDeliveryCount;
    }

    public void bumpDeliveryCount() {
        this.mDeliveryCount++;
    }

    public void setWorkId(int id) {
        this.mWorkId = id;
    }

    public int getWorkId() {
        return this.mWorkId;
    }

    public void setGrants(Object grants) {
        this.mGrants = grants;
    }

    public Object getGrants() {
        return this.mGrants;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("JobWorkItem{id=");
        sb.append(this.mWorkId);
        sb.append(" intent=");
        sb.append(this.mIntent);
        if (this.mNetworkDownloadBytes != -1) {
            sb.append(" downloadBytes=");
            sb.append(this.mNetworkDownloadBytes);
        }
        if (this.mNetworkUploadBytes != -1) {
            sb.append(" uploadBytes=");
            sb.append(this.mNetworkUploadBytes);
        }
        if (this.mMinimumChunkBytes != -1) {
            sb.append(" minimumChunkBytes=");
            sb.append(this.mMinimumChunkBytes);
        }
        if (this.mDeliveryCount != 0) {
            sb.append(" dcount=");
            sb.append(this.mDeliveryCount);
        }
        sb.append("}");
        return sb.toString();
    }

    public void enforceValidity() {
        long estimatedTransfer;
        long j = this.mNetworkUploadBytes;
        if (j == -1) {
            estimatedTransfer = this.mNetworkDownloadBytes;
        } else {
            long j2 = this.mNetworkDownloadBytes;
            if (j2 == -1) {
                j2 = 0;
            }
            estimatedTransfer = j + j2;
        }
        long j3 = this.mMinimumChunkBytes;
        if (j3 != -1 && estimatedTransfer != -1 && j3 > estimatedTransfer) {
            throw new IllegalArgumentException("Minimum chunk size can't be greater than estimated network usage");
        }
        if (j3 != -1 && j3 <= 0) {
            throw new IllegalArgumentException("Minimum chunk size must be positive");
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        if (this.mIntent != null) {
            out.writeInt(1);
            this.mIntent.writeToParcel(out, 0);
        } else {
            out.writeInt(0);
        }
        out.writeLong(this.mNetworkDownloadBytes);
        out.writeLong(this.mNetworkUploadBytes);
        out.writeLong(this.mMinimumChunkBytes);
        out.writeInt(this.mDeliveryCount);
        out.writeInt(this.mWorkId);
    }

    JobWorkItem(Parcel in) {
        if (in.readInt() != 0) {
            this.mIntent = Intent.CREATOR.createFromParcel(in);
        } else {
            this.mIntent = null;
        }
        this.mNetworkDownloadBytes = in.readLong();
        this.mNetworkUploadBytes = in.readLong();
        this.mMinimumChunkBytes = in.readLong();
        this.mDeliveryCount = in.readInt();
        this.mWorkId = in.readInt();
    }
}
