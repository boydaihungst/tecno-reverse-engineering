package android.content.pm;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class BaseParceledListSlice<T> implements Parcelable {
    private List<T> mList;
    private static String TAG = "ParceledListSlice";
    private static boolean DEBUG = false;
    private static final int MAX_IPC_SIZE = IBinder.getSuggestedMaxIpcSizeBytes();
    private int mInlineCountLimit = Integer.MAX_VALUE;
    private boolean mHasBeenParceled = false;

    protected abstract Parcelable.Creator<?> readParcelableCreator(Parcel parcel, ClassLoader classLoader);

    protected abstract void writeElement(T t, Parcel parcel, int i);

    protected abstract void writeParcelableCreator(T t, Parcel parcel);

    public BaseParceledListSlice(List<T> list) {
        this.mList = list;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseParceledListSlice(Parcel p, ClassLoader loader) {
        int i = 0;
        int N = p.readInt();
        this.mList = new ArrayList(N);
        if (DEBUG) {
            Log.d(TAG, "Retrieving " + N + " items");
        }
        if (N <= 0) {
            return;
        }
        Parcelable.Creator<?> creator = readParcelableCreator(p, loader);
        Class<?> listElementClass = null;
        int i2 = 0;
        while (i2 < N && p.readInt() != 0) {
            listElementClass = readVerifyAndAddElement(creator, p, loader, listElementClass);
            if (DEBUG) {
                String str = TAG;
                StringBuilder append = new StringBuilder().append("Read inline #").append(i2).append(": ");
                List<T> list = this.mList;
                Log.d(str, append.append(list.get(list.size() - 1)).toString());
            }
            i2++;
        }
        if (i2 >= N) {
            return;
        }
        IBinder retriever = p.readStrongBinder();
        while (i2 < N) {
            if (DEBUG) {
                Log.d(TAG, "Reading more @" + i2 + " of " + N + ": retriever=" + retriever);
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInt(i2);
            try {
                retriever.transact(1, data, reply, i);
                while (i2 < N && reply.readInt() != 0) {
                    listElementClass = readVerifyAndAddElement(creator, reply, loader, listElementClass);
                    if (DEBUG) {
                        String str2 = TAG;
                        StringBuilder append2 = new StringBuilder().append("Read extra #").append(i2).append(": ");
                        List<T> list2 = this.mList;
                        Log.d(str2, append2.append(list2.get(list2.size() - 1)).toString());
                    }
                    i2++;
                }
                reply.recycle();
                data.recycle();
                i = 0;
            } catch (RemoteException e) {
                Log.w(TAG, "Failure retrieving array; only received " + i2 + " of " + N, e);
                return;
            }
        }
    }

    private Class<?> readVerifyAndAddElement(Parcelable.Creator<?> creator, Parcel p, ClassLoader loader, Class<?> listElementClass) {
        T parcelable = readCreator(creator, p, loader);
        if (listElementClass == null) {
            listElementClass = parcelable.getClass();
        } else {
            verifySameType(listElementClass, parcelable.getClass());
        }
        this.mList.add(parcelable);
        return listElementClass;
    }

    private T readCreator(Parcelable.Creator<?> creator, Parcel p, ClassLoader loader) {
        if (creator instanceof Parcelable.ClassLoaderCreator) {
            Parcelable.ClassLoaderCreator<?> classLoaderCreator = (Parcelable.ClassLoaderCreator) creator;
            return (T) classLoaderCreator.createFromParcel(p, loader);
        }
        return (T) creator.createFromParcel(p);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void verifySameType(Class<?> expected, Class<?> actual) {
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException("Can't unparcel type " + (actual == null ? null : actual.getName()) + " in list of type " + (expected != null ? expected.getName() : null));
        }
    }

    public List<T> getList() {
        return this.mList;
    }

    public void setInlineCountLimit(int maxCount) {
        this.mInlineCountLimit = maxCount;
    }

    /* JADX WARN: Code restructure failed: missing block: B:19:0x009d, code lost:
        r11.writeInt(0);
        r0 = new android.content.pm.BaseParceledListSlice.AnonymousClass1(r10);
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x00a7, code lost:
        if (android.content.pm.BaseParceledListSlice.DEBUG == false) goto L24;
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x00a9, code lost:
        android.util.Log.d(android.content.pm.BaseParceledListSlice.TAG, "Breaking @" + r5 + " of " + r1 + ": retriever=" + r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x00d5, code lost:
        r11.writeStrongBinder(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x00d8, code lost:
        return;
     */
    @Override // android.os.Parcelable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void writeToParcel(Parcel dest, final int flags) {
        if (this.mHasBeenParceled) {
            throw new IllegalStateException("Can't Parcel a ParceledListSlice more than once");
        }
        this.mHasBeenParceled = true;
        final int N = this.mList.size();
        dest.writeInt(N);
        if (DEBUG) {
            Log.d(TAG, "Writing " + N + " items");
        }
        if (N > 0) {
            final Class<?> listElementClass = this.mList.get(0).getClass();
            writeParcelableCreator(this.mList.get(0), dest);
            int i = 0;
            while (i < N && i < this.mInlineCountLimit && dest.dataSize() < MAX_IPC_SIZE) {
                dest.writeInt(1);
                T parcelable = this.mList.get(i);
                verifySameType(listElementClass, parcelable.getClass());
                writeElement(parcelable, dest, flags);
                if (DEBUG) {
                    Log.d(TAG, "Wrote inline #" + i + ": " + this.mList.get(i));
                }
                i++;
            }
        }
    }
}
