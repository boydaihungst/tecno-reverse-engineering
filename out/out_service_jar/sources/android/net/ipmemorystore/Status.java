package android.net.ipmemorystore;
/* loaded from: classes.dex */
public class Status {
    public static final int ERROR_DATABASE_CANNOT_BE_OPENED = -3;
    public static final int ERROR_GENERIC = -1;
    public static final int ERROR_ILLEGAL_ARGUMENT = -2;
    public static final int ERROR_STORAGE = -4;
    public static final int ERROR_UNKNOWN = -5;
    public static final int SUCCESS = 0;
    public final int resultCode;

    public Status(int resultCode) {
        this.resultCode = resultCode;
    }

    public Status(StatusParcelable parcelable) {
        this(parcelable.resultCode);
    }

    public StatusParcelable toParcelable() {
        StatusParcelable parcelable = new StatusParcelable();
        parcelable.resultCode = this.resultCode;
        return parcelable;
    }

    public boolean isSuccess() {
        return this.resultCode == 0;
    }

    public String toString() {
        switch (this.resultCode) {
            case -4:
                return "DATABASE STORAGE ERROR";
            case -3:
                return "DATABASE CANNOT BE OPENED";
            case -2:
                return "ILLEGAL ARGUMENT";
            case -1:
                return "GENERIC ERROR";
            case 0:
                return "SUCCESS";
            default:
                return "Unknown value ?!";
        }
    }
}
