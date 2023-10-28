package com.android.internal.os;

import android.os.BatteryStats;
import android.os.Parcel;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import com.android.internal.util.ParseUtils;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/* loaded from: classes4.dex */
public class BatteryStatsHistory {
    private static final boolean DEBUG = false;
    public static final String FILE_SUFFIX = ".bin";
    public static final String HISTORY_DIR = "battery-history";
    private static final int MIN_FREE_SPACE = 104857600;
    private static final String TAG = "BatteryStatsHistory";
    private AtomicFile mActiveFile;
    private int mCurrentFileIndex;
    private Parcel mCurrentParcel;
    private int mCurrentParcelEnd;
    private final List<Integer> mFileNumbers;
    private final Parcel mHistoryBuffer;
    private final File mHistoryDir;
    private List<Parcel> mHistoryParcels;
    private int mParcelIndex;
    private int mRecordCount;
    private final BatteryStatsImpl mStats;

    public BatteryStatsHistory(BatteryStatsImpl stats, File systemDir, Parcel historyBuffer) {
        ArrayList arrayList = new ArrayList();
        this.mFileNumbers = arrayList;
        this.mHistoryParcels = null;
        this.mRecordCount = 0;
        this.mParcelIndex = 0;
        this.mStats = stats;
        this.mHistoryBuffer = historyBuffer;
        File file = new File(systemDir, HISTORY_DIR);
        this.mHistoryDir = file;
        file.mkdirs();
        if (!file.exists()) {
            Slog.wtf(TAG, "HistoryDir does not exist:" + file.getPath());
        }
        final ArraySet arraySet = new ArraySet();
        file.listFiles(new FilenameFilter() { // from class: com.android.internal.os.BatteryStatsHistory.1
            @Override // java.io.FilenameFilter
            public boolean accept(File dir, String name) {
                int b = name.lastIndexOf(BatteryStatsHistory.FILE_SUFFIX);
                if (b <= 0) {
                    return false;
                }
                Integer c = Integer.valueOf(ParseUtils.parseInt(name.substring(0, b), -1));
                if (c.intValue() == -1) {
                    return false;
                }
                arraySet.add(c);
                return true;
            }
        });
        if (!arraySet.isEmpty()) {
            arrayList.addAll(arraySet);
            Collections.sort(arrayList);
            setActiveFile(((Integer) arrayList.get(arrayList.size() - 1)).intValue());
            return;
        }
        arrayList.add(0);
        setActiveFile(0);
    }

    public BatteryStatsHistory(Parcel historyBuffer) {
        this.mFileNumbers = new ArrayList();
        this.mHistoryParcels = null;
        this.mRecordCount = 0;
        this.mParcelIndex = 0;
        this.mStats = null;
        this.mHistoryDir = null;
        this.mHistoryBuffer = historyBuffer;
    }

    public File getHistoryDirectory() {
        return this.mHistoryDir;
    }

    private void setActiveFile(int fileNumber) {
        this.mActiveFile = getFile(fileNumber);
    }

    private AtomicFile getFile(int num) {
        return new AtomicFile(new File(this.mHistoryDir, num + FILE_SUFFIX));
    }

    public void startNextFile() {
        if (this.mStats == null) {
            Slog.wtf(TAG, "mStats should not be null when writing history");
        } else if (this.mFileNumbers.isEmpty()) {
            Slog.wtf(TAG, "mFileNumbers should never be empty");
        } else {
            List<Integer> list = this.mFileNumbers;
            int next = list.get(list.size() - 1).intValue() + 1;
            this.mFileNumbers.add(Integer.valueOf(next));
            setActiveFile(next);
            if (!hasFreeDiskSpace()) {
                int oldest = this.mFileNumbers.remove(0).intValue();
                getFile(oldest).delete();
            }
            while (this.mFileNumbers.size() > this.mStats.mConstants.MAX_HISTORY_FILES) {
                int oldest2 = this.mFileNumbers.get(0).intValue();
                getFile(oldest2).delete();
                this.mFileNumbers.remove(0);
            }
        }
    }

    public void resetAllFiles() {
        for (Integer i : this.mFileNumbers) {
            getFile(i.intValue()).delete();
        }
        this.mFileNumbers.clear();
        this.mFileNumbers.add(0);
        setActiveFile(0);
    }

    public boolean startIteratingHistory() {
        this.mRecordCount = 0;
        this.mCurrentFileIndex = 0;
        this.mCurrentParcel = null;
        this.mCurrentParcelEnd = 0;
        this.mParcelIndex = 0;
        return true;
    }

    public void finishIteratingHistory() {
        Parcel parcel = this.mHistoryBuffer;
        parcel.setDataPosition(parcel.dataSize());
    }

    public Parcel getNextParcel(BatteryStats.HistoryItem out) {
        if (this.mRecordCount == 0) {
            out.clear();
        }
        this.mRecordCount++;
        Parcel parcel = this.mCurrentParcel;
        if (parcel != null) {
            if (parcel.dataPosition() < this.mCurrentParcelEnd) {
                return this.mCurrentParcel;
            }
            Parcel parcel2 = this.mHistoryBuffer;
            Parcel parcel3 = this.mCurrentParcel;
            if (parcel2 == parcel3) {
                return null;
            }
            List<Parcel> list = this.mHistoryParcels;
            if (list == null || !list.contains(parcel3)) {
                this.mCurrentParcel.recycle();
            }
        }
        while (this.mCurrentFileIndex < this.mFileNumbers.size() - 1) {
            this.mCurrentParcel = null;
            this.mCurrentParcelEnd = 0;
            Parcel p = Parcel.obtain();
            List<Integer> list2 = this.mFileNumbers;
            int i = this.mCurrentFileIndex;
            this.mCurrentFileIndex = i + 1;
            AtomicFile file = getFile(list2.get(i).intValue());
            if (readFileToParcel(p, file)) {
                int bufSize = p.readInt();
                int curPos = p.dataPosition();
                int i2 = curPos + bufSize;
                this.mCurrentParcelEnd = i2;
                this.mCurrentParcel = p;
                if (curPos < i2) {
                    return p;
                }
            } else {
                p.recycle();
            }
        }
        if (this.mHistoryParcels != null) {
            while (this.mParcelIndex < this.mHistoryParcels.size()) {
                List<Parcel> list3 = this.mHistoryParcels;
                int i3 = this.mParcelIndex;
                this.mParcelIndex = i3 + 1;
                Parcel p2 = list3.get(i3);
                if (skipHead(p2)) {
                    int bufSize2 = p2.readInt();
                    int curPos2 = p2.dataPosition();
                    int i4 = curPos2 + bufSize2;
                    this.mCurrentParcelEnd = i4;
                    this.mCurrentParcel = p2;
                    if (curPos2 < i4) {
                        return p2;
                    }
                }
            }
        }
        if (this.mHistoryBuffer.dataSize() <= 0) {
            return null;
        }
        this.mHistoryBuffer.setDataPosition(0);
        Parcel parcel4 = this.mHistoryBuffer;
        this.mCurrentParcel = parcel4;
        this.mCurrentParcelEnd = parcel4.dataSize();
        return this.mCurrentParcel;
    }

    public boolean readFileToParcel(Parcel out, AtomicFile file) {
        try {
            SystemClock.uptimeMillis();
            byte[] raw = file.readFully();
            out.unmarshall(raw, 0, raw.length);
            out.setDataPosition(0);
            return skipHead(out);
        } catch (Exception e) {
            Slog.e(TAG, "Error reading file " + file.getBaseFile().getPath(), e);
            return false;
        }
    }

    private boolean skipHead(Parcel p) {
        p.setDataPosition(0);
        int version = p.readInt();
        if (version != 208) {
            return false;
        }
        p.readLong();
        return true;
    }

    public void writeToParcel(Parcel out) {
        writeToParcel(out, false);
    }

    public void writeToBatteryUsageStatsParcel(Parcel out) {
        out.writeBlob(this.mHistoryBuffer.marshall());
        writeToParcel(out, true);
    }

    private void writeToParcel(Parcel out, boolean useBlobs) {
        SystemClock.uptimeMillis();
        out.writeInt(this.mFileNumbers.size() - 1);
        for (int i = 0; i < this.mFileNumbers.size() - 1; i++) {
            AtomicFile file = getFile(this.mFileNumbers.get(i).intValue());
            byte[] raw = new byte[0];
            try {
                raw = file.readFully();
            } catch (Exception e) {
                Slog.e(TAG, "Error reading file " + file.getBaseFile().getPath(), e);
            }
            if (useBlobs) {
                out.writeBlob(raw);
            } else {
                out.writeByteArray(raw);
            }
        }
    }

    public static BatteryStatsHistory createFromBatteryUsageStatsParcel(Parcel in) {
        byte[] historyBlob = in.readBlob();
        Parcel historyBuffer = Parcel.obtain();
        historyBuffer.unmarshall(historyBlob, 0, historyBlob.length);
        BatteryStatsHistory history = new BatteryStatsHistory(historyBuffer);
        history.readFromParcel(in, true);
        return history;
    }

    public void readFromParcel(Parcel in) {
        readFromParcel(in, false);
    }

    private void readFromParcel(Parcel in, boolean useBlobs) {
        SystemClock.uptimeMillis();
        this.mHistoryParcels = new ArrayList();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            byte[] temp = useBlobs ? in.readBlob() : in.createByteArray();
            if (temp != null && temp.length != 0) {
                Parcel p = Parcel.obtain();
                p.unmarshall(temp, 0, temp.length);
                p.setDataPosition(0);
                this.mHistoryParcels.add(p);
            }
        }
    }

    private boolean hasFreeDiskSpace() {
        StatFs stats = new StatFs(this.mHistoryDir.getAbsolutePath());
        return stats.getAvailableBytes() > 104857600;
    }

    public List<Integer> getFilesNumbers() {
        return this.mFileNumbers;
    }

    public AtomicFile getActiveFile() {
        return this.mActiveFile;
    }

    public int getHistoryUsedSize() {
        int ret = 0;
        for (int i = 0; i < this.mFileNumbers.size() - 1; i++) {
            ret = (int) (ret + getFile(this.mFileNumbers.get(i).intValue()).getBaseFile().length());
        }
        int ret2 = ret + this.mHistoryBuffer.dataSize();
        if (this.mHistoryParcels != null) {
            for (int i2 = 0; i2 < this.mHistoryParcels.size(); i2++) {
                ret2 += this.mHistoryParcels.get(i2).dataSize();
            }
        }
        return ret2;
    }
}
