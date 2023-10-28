package android.content;

import android.app.ActivityThread;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;
import android.util.Log;
import android.view.contentcapture.ContentCaptureManager;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public final class ContentCaptureOptions implements Parcelable {
    public final int idleFlushingFrequencyMs;
    public final boolean lite;
    public final int logHistorySize;
    public final int loggingLevel;
    public final int maxBufferSize;
    public final int textChangeFlushingFrequencyMs;
    public final ArraySet<ComponentName> whitelistedComponents;
    private static final String TAG = ContentCaptureOptions.class.getSimpleName();
    public static final Parcelable.Creator<ContentCaptureOptions> CREATOR = new Parcelable.Creator<ContentCaptureOptions>() { // from class: android.content.ContentCaptureOptions.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ContentCaptureOptions createFromParcel(Parcel parcel) {
            boolean lite = parcel.readBoolean();
            int loggingLevel = parcel.readInt();
            if (lite) {
                return new ContentCaptureOptions(loggingLevel);
            }
            int maxBufferSize = parcel.readInt();
            int idleFlushingFrequencyMs = parcel.readInt();
            int textChangeFlushingFrequencyMs = parcel.readInt();
            int logHistorySize = parcel.readInt();
            return new ContentCaptureOptions(loggingLevel, maxBufferSize, idleFlushingFrequencyMs, textChangeFlushingFrequencyMs, logHistorySize, parcel.readArraySet(null));
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ContentCaptureOptions[] newArray(int size) {
            return new ContentCaptureOptions[size];
        }
    };

    public ContentCaptureOptions(int loggingLevel) {
        this(true, loggingLevel, 0, 0, 0, 0, null);
    }

    public ContentCaptureOptions(int loggingLevel, int maxBufferSize, int idleFlushingFrequencyMs, int textChangeFlushingFrequencyMs, int logHistorySize, ArraySet<ComponentName> whitelistedComponents) {
        this(false, loggingLevel, maxBufferSize, idleFlushingFrequencyMs, textChangeFlushingFrequencyMs, logHistorySize, whitelistedComponents);
    }

    public ContentCaptureOptions(ArraySet<ComponentName> whitelistedComponents) {
        this(2, 500, 5000, 1000, 10, whitelistedComponents);
    }

    private ContentCaptureOptions(boolean lite, int loggingLevel, int maxBufferSize, int idleFlushingFrequencyMs, int textChangeFlushingFrequencyMs, int logHistorySize, ArraySet<ComponentName> whitelistedComponents) {
        this.lite = lite;
        this.loggingLevel = loggingLevel;
        this.maxBufferSize = maxBufferSize;
        this.idleFlushingFrequencyMs = idleFlushingFrequencyMs;
        this.textChangeFlushingFrequencyMs = textChangeFlushingFrequencyMs;
        this.logHistorySize = logHistorySize;
        this.whitelistedComponents = whitelistedComponents;
    }

    public static ContentCaptureOptions forWhitelistingItself() {
        ActivityThread at = ActivityThread.currentActivityThread();
        if (at == null) {
            throw new IllegalStateException("No ActivityThread");
        }
        String packageName = at.getApplication().getPackageName();
        if (!"android.contentcaptureservice.cts".equals(packageName) && !"android.translation.cts".equals(packageName)) {
            Log.e(TAG, "forWhitelistingItself(): called by " + packageName);
            throw new SecurityException("Thou shall not pass!");
        }
        ContentCaptureOptions options = new ContentCaptureOptions((ArraySet<ComponentName>) null);
        Log.i(TAG, "forWhitelistingItself(" + packageName + "): " + options);
        return options;
    }

    public boolean isWhitelisted(Context context) {
        if (this.whitelistedComponents == null) {
            return true;
        }
        ContentCaptureManager.ContentCaptureClient client = context.getContentCaptureClient();
        if (client == null) {
            Log.w(TAG, "isWhitelisted(): no ContentCaptureClient on " + context);
            return false;
        }
        return this.whitelistedComponents.contains(client.contentCaptureClientGetComponentName());
    }

    public String toString() {
        if (this.lite) {
            return "ContentCaptureOptions [loggingLevel=" + this.loggingLevel + " (lite)]";
        }
        StringBuilder string = new StringBuilder("ContentCaptureOptions [");
        string.append("loggingLevel=").append(this.loggingLevel).append(", maxBufferSize=").append(this.maxBufferSize).append(", idleFlushingFrequencyMs=").append(this.idleFlushingFrequencyMs).append(", textChangeFlushingFrequencyMs=").append(this.textChangeFlushingFrequencyMs).append(", logHistorySize=").append(this.logHistorySize);
        if (this.whitelistedComponents != null) {
            string.append(", whitelisted=").append(this.whitelistedComponents);
        }
        return string.append(']').toString();
    }

    public void dumpShort(PrintWriter pw) {
        pw.print("logLvl=");
        pw.print(this.loggingLevel);
        if (this.lite) {
            pw.print(", lite");
            return;
        }
        pw.print(", bufferSize=");
        pw.print(this.maxBufferSize);
        pw.print(", idle=");
        pw.print(this.idleFlushingFrequencyMs);
        pw.print(", textIdle=");
        pw.print(this.textChangeFlushingFrequencyMs);
        pw.print(", logSize=");
        pw.print(this.logHistorySize);
        if (this.whitelistedComponents != null) {
            pw.print(", whitelisted=");
            pw.print(this.whitelistedComponents);
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeBoolean(this.lite);
        parcel.writeInt(this.loggingLevel);
        if (this.lite) {
            return;
        }
        parcel.writeInt(this.maxBufferSize);
        parcel.writeInt(this.idleFlushingFrequencyMs);
        parcel.writeInt(this.textChangeFlushingFrequencyMs);
        parcel.writeInt(this.logHistorySize);
        parcel.writeArraySet(this.whitelistedComponents);
    }
}
