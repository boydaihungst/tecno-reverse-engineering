package com.android.server.power;

import android.text.TextUtils;
import android.util.Slog;
import com.android.server.UiModeManagerService;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class WakeLockLog {
    private static final boolean DEBUG = false;
    private static final int FLAG_ACQUIRE_CAUSES_WAKEUP = 16;
    private static final int FLAG_ON_AFTER_RELEASE = 8;
    private static final int FLAG_SYSTEM_WAKELOCK = 32;
    private static final int LEVEL_DOZE_WAKE_LOCK = 6;
    private static final int LEVEL_DRAW_WAKE_LOCK = 7;
    private static final int LEVEL_FULL_WAKE_LOCK = 2;
    private static final int LEVEL_PARTIAL_WAKE_LOCK = 1;
    private static final int LEVEL_PROXIMITY_SCREEN_OFF_WAKE_LOCK = 5;
    private static final int LEVEL_SCREEN_BRIGHT_WAKE_LOCK = 4;
    private static final int LEVEL_SCREEN_DIM_WAKE_LOCK = 3;
    private static final int LEVEL_UNKNOWN = 0;
    private static final int LOG_SIZE = 10240;
    private static final int LOG_SIZE_MIN = 10;
    private static final int MASK_LOWER_6_BITS = 63;
    private static final int MASK_LOWER_7_BITS = 127;
    private static final int MAX_LOG_ENTRY_BYTE_SIZE = 9;
    private static final String TAG = "PowerManagerService.WLLog";
    private static final int TAG_DATABASE_SIZE = 128;
    private static final int TAG_DATABASE_SIZE_MAX = 128;
    private static final int TYPE_ACQUIRE = 1;
    private static final int TYPE_RELEASE = 2;
    private static final int TYPE_TIME_RESET = 0;
    private final SimpleDateFormat mDumpsysDateFormat;
    private final Injector mInjector;
    private final Object mLock;
    private final TheLog mLog;
    private final TagDatabase mTagDatabase;
    private static final String[] LEVEL_TO_STRING = {UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN, "partial", "full", "screen-dim", "screen-bright", "prox", "doze", "draw"};
    private static final String[] REDUCED_TAG_PREFIXES = {"*job*/", "*gms_scheduler*/", "IntentOp:"};
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

    /* JADX INFO: Access modifiers changed from: package-private */
    public WakeLockLog() {
        this(new Injector());
    }

    WakeLockLog(Injector injector) {
        this.mLock = new Object();
        this.mInjector = injector;
        TagDatabase tagDatabase = new TagDatabase(injector);
        this.mTagDatabase = tagDatabase;
        EntryByteTranslator translator = new EntryByteTranslator(tagDatabase);
        this.mLog = new TheLog(injector, translator, tagDatabase);
        this.mDumpsysDateFormat = injector.getDateFormat();
    }

    public void onWakeLockAcquired(String tag, int ownerUid, int flags) {
        onWakeLockEvent(1, tag, ownerUid, flags);
    }

    public void onWakeLockReleased(String tag, int ownerUid) {
        onWakeLockEvent(2, tag, ownerUid, 0);
    }

    public void dump(PrintWriter pw) {
        dump(pw, false);
    }

    void dump(PrintWriter pw, boolean includeTagDb) {
        try {
            synchronized (this.mLock) {
                pw.println("Wake Lock Log");
                LogEntry tempEntry = new LogEntry();
                Iterator<LogEntry> iterator = this.mLog.getAllItems(tempEntry);
                int numEvents = 0;
                int numResets = 0;
                while (iterator.hasNext()) {
                    LogEntry entry = iterator.next();
                    if (entry != null) {
                        if (entry.type == 0) {
                            numResets++;
                        } else {
                            numEvents++;
                            entry.dump(pw, this.mDumpsysDateFormat);
                        }
                    }
                }
                pw.println("  -");
                pw.println("  Events: " + numEvents + ", Time-Resets: " + numResets);
                pw.println("  Buffer, Bytes used: " + this.mLog.getUsedBufferSize());
                if (includeTagDb) {
                    pw.println("  " + this.mTagDatabase);
                }
            }
        } catch (Exception e) {
            pw.println("Exception dumping wake-lock log: " + e.toString());
        }
    }

    private void onWakeLockEvent(int eventType, String tag, int ownerUid, int flags) {
        int translatedFlags;
        if (tag == null) {
            Slog.w(TAG, "Insufficient data to log wakelock [tag: " + tag + ", ownerUid: " + ownerUid + ", flags: 0x" + Integer.toHexString(flags));
            return;
        }
        long time = this.mInjector.currentTimeMillis();
        if (eventType == 1) {
            translatedFlags = translateFlagsFromPowerManager(flags);
        } else {
            translatedFlags = 0;
        }
        handleWakeLockEventInternal(eventType, tagNameReducer(tag), ownerUid, translatedFlags, time);
    }

    private void handleWakeLockEventInternal(int eventType, String tag, int ownerUid, int flags, long time) {
        synchronized (this.mLock) {
            TagData tagData = this.mTagDatabase.findOrCreateTag(tag, ownerUid, true);
            this.mLog.addEntry(new LogEntry(time, eventType, tagData, flags));
        }
    }

    int translateFlagsFromPowerManager(int flags) {
        int newFlags = 0;
        switch (65535 & flags) {
            case 1:
                newFlags = 1;
                break;
            case 6:
                newFlags = 3;
                break;
            case 10:
                newFlags = 4;
                break;
            case 26:
                newFlags = 2;
                break;
            case 32:
                newFlags = 5;
                break;
            case 64:
                newFlags = 6;
                break;
            case 128:
                newFlags = 7;
                break;
            default:
                Slog.w(TAG, "Unsupported lock level for logging, flags: " + flags);
                break;
        }
        if ((268435456 & flags) != 0) {
            newFlags |= 16;
        }
        if ((536870912 & flags) != 0) {
            newFlags |= 8;
        }
        if ((Integer.MIN_VALUE & flags) != 0) {
            return newFlags | 32;
        }
        return newFlags;
    }

    private String tagNameReducer(String tag) {
        if (tag == null) {
            return null;
        }
        String reduciblePrefix = null;
        String[] strArr = REDUCED_TAG_PREFIXES;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            String reducedTagPrefix = strArr[i];
            if (!tag.startsWith(reducedTagPrefix)) {
                i++;
            } else {
                reduciblePrefix = reducedTagPrefix;
                break;
            }
        }
        if (reduciblePrefix != null) {
            StringBuilder sb = new StringBuilder();
            sb.append((CharSequence) tag, 0, reduciblePrefix.length());
            int end = Math.max(tag.lastIndexOf(SliceClientPermissions.SliceAuthority.DELIMITER), tag.lastIndexOf("."));
            boolean printNext = true;
            int index = sb.length();
            while (index < end) {
                char c = tag.charAt(index);
                boolean isMarker = c == '.' || c == '/';
                if (isMarker || printNext) {
                    sb.append(c);
                }
                printNext = isMarker;
                index++;
            }
            sb.append(tag.substring(index));
            return sb.toString();
        }
        return tag;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class LogEntry {
        public int flags;
        public TagData tag;
        public long time;
        public int type;

        LogEntry() {
        }

        LogEntry(long time, int type, TagData tag, int flags) {
            set(time, type, tag, flags);
        }

        public void set(long time, int type, TagData tag, int flags) {
            this.time = time;
            this.type = type;
            this.tag = tag;
            this.flags = flags;
        }

        public void dump(PrintWriter pw, SimpleDateFormat dateFormat) {
            pw.println("  " + toStringInternal(dateFormat));
        }

        public String toString() {
            return toStringInternal(WakeLockLog.DATE_FORMAT);
        }

        private String toStringInternal(SimpleDateFormat dateFormat) {
            StringBuilder sb = new StringBuilder();
            if (this.type == 0) {
                return dateFormat.format(new Date(this.time)) + " - RESET";
            }
            StringBuilder append = sb.append(dateFormat.format(new Date(this.time))).append(" - ");
            TagData tagData = this.tag;
            StringBuilder append2 = append.append(tagData == null ? "---" : Integer.valueOf(tagData.ownerUid)).append(" - ").append(this.type == 1 ? "ACQ" : "REL").append(" ");
            TagData tagData2 = this.tag;
            append2.append(tagData2 == null ? "UNKNOWN" : tagData2.tag);
            if (this.type == 1) {
                sb.append(" (");
                flagsToString(sb);
                sb.append(")");
            }
            return sb.toString();
        }

        private void flagsToString(StringBuilder sb) {
            sb.append(WakeLockLog.LEVEL_TO_STRING[this.flags & 7]);
            if ((this.flags & 8) == 8) {
                sb.append(",on-after-release");
            }
            if ((this.flags & 16) == 16) {
                sb.append(",acq-causes-wake");
            }
            if ((this.flags & 32) == 32) {
                sb.append(",system-wakelock");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class EntryByteTranslator {
        static final int ERROR_TIME_IS_NEGATIVE = -1;
        static final int ERROR_TIME_TOO_LARGE = -2;
        private final TagDatabase mTagDatabase;

        EntryByteTranslator(TagDatabase tagDatabase) {
            this.mTagDatabase = tagDatabase;
        }

        LogEntry fromBytes(byte[] bytes, long timeReference, LogEntry entryToReuse) {
            int type;
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            LogEntry entry = entryToReuse != null ? entryToReuse : new LogEntry();
            int type2 = (bytes[0] >> 6) & 3;
            if ((type2 & 2) != 2) {
                type = type2;
            } else {
                type = 2;
            }
            switch (type) {
                case 0:
                    if (bytes.length >= 9) {
                        long time = ((bytes[1] & 255) << 56) | ((bytes[2] & 255) << 48) | ((bytes[3] & 255) << 40) | ((bytes[4] & 255) << 32) | ((bytes[5] & 255) << 24) | ((bytes[6] & 255) << 16) | ((bytes[7] & 255) << 8) | (bytes[8] & 255);
                        entry.set(time, 0, null, 0);
                        return entry;
                    }
                    break;
                case 1:
                    if (bytes.length >= 3) {
                        int flags = bytes[0] & 63;
                        int tagIndex = bytes[1] & Byte.MAX_VALUE;
                        TagData tag = this.mTagDatabase.getTag(tagIndex);
                        long time2 = (bytes[2] & 255) + timeReference;
                        entry.set(time2, 1, tag, flags);
                        return entry;
                    }
                    break;
                case 2:
                    if (bytes.length >= 2) {
                        int tagIndex2 = bytes[0] & Byte.MAX_VALUE;
                        TagData tag2 = this.mTagDatabase.getTag(tagIndex2);
                        long time3 = (bytes[1] & 255) + timeReference;
                        entry.set(time3, 2, tag2, 0);
                        return entry;
                    }
                    break;
                default:
                    Slog.w(WakeLockLog.TAG, "Type not recognized [" + type + "]", new Exception());
                    break;
            }
            return null;
        }

        int toBytes(LogEntry entry, byte[] bytes, long timeReference) {
            int sizeNeeded;
            switch (entry.type) {
                case 0:
                    sizeNeeded = 9;
                    long time = entry.time;
                    if (bytes != null && bytes.length >= 9) {
                        bytes[0] = 0;
                        bytes[1] = (byte) ((time >> 56) & 255);
                        bytes[2] = (byte) ((time >> 48) & 255);
                        bytes[3] = (byte) ((time >> 40) & 255);
                        bytes[4] = (byte) ((time >> 32) & 255);
                        bytes[5] = (byte) ((time >> 24) & 255);
                        bytes[6] = (byte) ((time >> 16) & 255);
                        bytes[7] = (byte) ((time >> 8) & 255);
                        bytes[8] = (byte) (time & 255);
                        break;
                    }
                    break;
                case 1:
                    sizeNeeded = 3;
                    if (bytes != null && bytes.length >= 3) {
                        int relativeTime = getRelativeTime(timeReference, entry.time);
                        if (relativeTime < 0) {
                            return relativeTime;
                        }
                        bytes[0] = (byte) ((entry.flags & 63) | 64);
                        bytes[1] = (byte) this.mTagDatabase.getTagIndex(entry.tag);
                        bytes[2] = (byte) (relativeTime & 255);
                        break;
                    }
                    break;
                case 2:
                    sizeNeeded = 2;
                    if (bytes != null && bytes.length >= 2) {
                        int relativeTime2 = getRelativeTime(timeReference, entry.time);
                        if (relativeTime2 < 0) {
                            return relativeTime2;
                        }
                        bytes[0] = (byte) (this.mTagDatabase.getTagIndex(entry.tag) | 128);
                        bytes[1] = (byte) (relativeTime2 & 255);
                        break;
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown type " + entry);
            }
            return sizeNeeded;
        }

        private int getRelativeTime(long timeReference, long time) {
            if (time < timeReference) {
                return -1;
            }
            long relativeTime = time - timeReference;
            if (relativeTime > 255) {
                return -2;
            }
            return (int) relativeTime;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class TheLog {
        private final byte[] mBuffer;
        private final TagDatabase mTagDatabase;
        private final EntryByteTranslator mTranslator;
        private final byte[] mTempBuffer = new byte[9];
        private final byte[] mReadWriteTempBuffer = new byte[9];
        private int mStart = 0;
        private int mEnd = 0;
        private long mStartTime = 0;
        private long mLatestTime = 0;
        private long mChangeCount = 0;

        TheLog(Injector injector, EntryByteTranslator translator, TagDatabase tagDatabase) {
            int logSize = Math.max(injector.getLogSize(), 10);
            this.mBuffer = new byte[logSize];
            this.mTranslator = translator;
            this.mTagDatabase = tagDatabase;
            tagDatabase.setCallback(new TagDatabase.Callback() { // from class: com.android.server.power.WakeLockLog.TheLog.1
                @Override // com.android.server.power.WakeLockLog.TagDatabase.Callback
                public void onIndexRemoved(int index) {
                    TheLog.this.removeTagIndex(index);
                }
            });
        }

        int getUsedBufferSize() {
            return this.mBuffer.length - getAvailableSpace();
        }

        void addEntry(LogEntry entry) {
            if (isBufferEmpty()) {
                long j = entry.time;
                this.mLatestTime = j;
                this.mStartTime = j;
            }
            int size = this.mTranslator.toBytes(entry, this.mTempBuffer, this.mLatestTime);
            if (size == -1) {
                return;
            }
            if (size == -2) {
                addEntry(new LogEntry(entry.time, 0, null, 0));
                size = this.mTranslator.toBytes(entry, this.mTempBuffer, this.mLatestTime);
            }
            if (size > 9 || size <= 0) {
                Slog.w(WakeLockLog.TAG, "Log entry size is out of expected range: " + size);
            } else if (!makeSpace(size)) {
            } else {
                writeBytesAt(this.mEnd, this.mTempBuffer, size);
                this.mEnd = (this.mEnd + size) % this.mBuffer.length;
                this.mLatestTime = entry.time;
                TagDatabase.updateTagTime(entry.tag, entry.time);
                this.mChangeCount++;
            }
        }

        Iterator<LogEntry> getAllItems(final LogEntry tempEntry) {
            return new Iterator<LogEntry>() { // from class: com.android.server.power.WakeLockLog.TheLog.2
                private final long mChangeValue;
                private int mCurrent;
                private long mCurrentTimeReference;

                {
                    this.mCurrent = TheLog.this.mStart;
                    this.mCurrentTimeReference = TheLog.this.mStartTime;
                    this.mChangeValue = TheLog.this.mChangeCount;
                }

                @Override // java.util.Iterator
                public boolean hasNext() {
                    checkState();
                    return this.mCurrent != TheLog.this.mEnd;
                }

                /* JADX DEBUG: Method merged with bridge method */
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // java.util.Iterator
                public LogEntry next() {
                    checkState();
                    if (!hasNext()) {
                        throw new NoSuchElementException("No more entries left.");
                    }
                    LogEntry entry = TheLog.this.readEntryAt(this.mCurrent, this.mCurrentTimeReference, tempEntry);
                    int size = TheLog.this.mTranslator.toBytes(entry, null, TheLog.this.mStartTime);
                    this.mCurrent = (this.mCurrent + size) % TheLog.this.mBuffer.length;
                    this.mCurrentTimeReference = entry.time;
                    return entry;
                }

                public String toString() {
                    return "@" + this.mCurrent;
                }

                private void checkState() {
                    if (this.mChangeValue != TheLog.this.mChangeCount) {
                        throw new ConcurrentModificationException("Buffer modified, old change: " + this.mChangeValue + ", new change: " + TheLog.this.mChangeCount);
                    }
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void removeTagIndex(int tagIndex) {
            if (isBufferEmpty()) {
                return;
            }
            int readIndex = this.mStart;
            long timeReference = this.mStartTime;
            LogEntry reusableEntryInstance = new LogEntry();
            while (readIndex != this.mEnd) {
                LogEntry entry = readEntryAt(readIndex, timeReference, reusableEntryInstance);
                if (entry == null) {
                    Slog.w(WakeLockLog.TAG, "Entry is unreadable - Unexpected @ " + readIndex);
                    return;
                }
                if (entry.tag != null && entry.tag.index == tagIndex) {
                    entry.tag = null;
                    writeEntryAt(readIndex, entry, timeReference);
                }
                timeReference = entry.time;
                int entryByteSize = this.mTranslator.toBytes(entry, null, 0L);
                readIndex = (readIndex + entryByteSize) % this.mBuffer.length;
            }
        }

        private boolean makeSpace(int spaceNeeded) {
            if (this.mBuffer.length < spaceNeeded + 1) {
                return false;
            }
            while (getAvailableSpace() < spaceNeeded + 1) {
                removeOldestItem();
            }
            return true;
        }

        private int getAvailableSpace() {
            int i = this.mEnd;
            int i2 = this.mStart;
            return i > i2 ? this.mBuffer.length - (i - i2) : i < i2 ? i2 - i : this.mBuffer.length;
        }

        private void removeOldestItem() {
            if (isBufferEmpty()) {
                return;
            }
            LogEntry entry = readEntryAt(this.mStart, this.mStartTime, null);
            int size = this.mTranslator.toBytes(entry, null, this.mStartTime);
            this.mStart = (this.mStart + size) % this.mBuffer.length;
            this.mStartTime = entry.time;
            this.mChangeCount++;
        }

        private boolean isBufferEmpty() {
            return this.mStart == this.mEnd;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public LogEntry readEntryAt(int index, long timeReference, LogEntry entryToSet) {
            for (int i = 0; i < 9; i++) {
                byte[] bArr = this.mBuffer;
                int indexIntoMainBuffer = (index + i) % bArr.length;
                if (indexIntoMainBuffer == this.mEnd) {
                    break;
                }
                this.mReadWriteTempBuffer[i] = bArr[indexIntoMainBuffer];
            }
            return this.mTranslator.fromBytes(this.mReadWriteTempBuffer, timeReference, entryToSet);
        }

        private void writeEntryAt(int index, LogEntry entry, long timeReference) {
            int size = this.mTranslator.toBytes(entry, this.mReadWriteTempBuffer, timeReference);
            if (size > 0) {
                writeBytesAt(index, this.mReadWriteTempBuffer, size);
            }
        }

        private void writeBytesAt(int index, byte[] buffer, int size) {
            for (int i = 0; i < size; i++) {
                byte[] bArr = this.mBuffer;
                int indexIntoMainBuffer = (index + i) % bArr.length;
                bArr[indexIntoMainBuffer] = buffer[i];
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class TagDatabase {
        private final TagData[] mArray;
        private Callback mCallback;
        private final int mInvalidIndex;

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public interface Callback {
            void onIndexRemoved(int i);
        }

        TagDatabase(Injector injector) {
            int size = Math.min(injector.getTagDatabaseSize(), 128);
            this.mArray = new TagData[size - 1];
            this.mInvalidIndex = size - 1;
        }

        public String toString() {
            TagData[] tagDataArr;
            StringBuilder sb = new StringBuilder();
            sb.append("Tag Database: size(").append(this.mArray.length).append(")");
            int entries = 0;
            int byteEstimate = 0;
            int tagSize = 0;
            int tags = 0;
            for (TagData tagData : this.mArray) {
                byteEstimate += 8;
                if (tagData != null) {
                    entries++;
                    byteEstimate += tagData.getByteSize();
                    if (tagData.tag != null) {
                        tags++;
                        tagSize += tagData.tag.length();
                    }
                }
            }
            sb.append(", entries: ").append(entries);
            sb.append(", Bytes used: ").append(byteEstimate);
            return sb.toString();
        }

        public void setCallback(Callback callback) {
            this.mCallback = callback;
        }

        public TagData getTag(int index) {
            if (index >= 0) {
                TagData[] tagDataArr = this.mArray;
                if (index >= tagDataArr.length || index == this.mInvalidIndex) {
                    return null;
                }
                return tagDataArr[index];
            }
            return null;
        }

        public TagData getTag(String tag, int ownerUid) {
            return findOrCreateTag(tag, ownerUid, false);
        }

        public int getTagIndex(TagData tagData) {
            return tagData == null ? this.mInvalidIndex : tagData.index;
        }

        public TagData findOrCreateTag(String tagStr, int ownerUid, boolean shouldCreate) {
            Callback callback;
            int firstAvailable = -1;
            TagData oldest = null;
            int oldestIndex = -1;
            TagData tag = new TagData(tagStr, ownerUid);
            int i = 0;
            while (true) {
                TagData[] tagDataArr = this.mArray;
                if (i < tagDataArr.length) {
                    TagData current = tagDataArr[i];
                    if (tag.equals(current)) {
                        return current;
                    }
                    if (shouldCreate) {
                        if (current != null) {
                            if (oldest == null || current.lastUsedTime < oldest.lastUsedTime) {
                                oldestIndex = i;
                                oldest = current;
                            }
                        } else if (firstAvailable == -1) {
                            firstAvailable = i;
                        }
                    }
                    i++;
                } else if (!shouldCreate) {
                    return null;
                } else {
                    boolean useOldest = firstAvailable == -1;
                    if (useOldest && (callback = this.mCallback) != null) {
                        callback.onIndexRemoved(oldestIndex);
                    }
                    setToIndex(tag, firstAvailable != -1 ? firstAvailable : oldestIndex);
                    return tag;
                }
            }
        }

        public static void updateTagTime(TagData tag, long time) {
            if (tag != null) {
                tag.lastUsedTime = time;
            }
        }

        private void setToIndex(TagData tag, int index) {
            if (index >= 0) {
                TagData[] tagDataArr = this.mArray;
                if (index >= tagDataArr.length) {
                    return;
                }
                TagData current = tagDataArr[index];
                if (current != null) {
                    current.index = this.mInvalidIndex;
                }
                this.mArray[index] = tag;
                tag.index = index;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class TagData {
        public int index;
        public long lastUsedTime;
        public int ownerUid;
        public String tag;

        TagData(String tag, int ownerUid) {
            this.tag = tag;
            this.ownerUid = ownerUid;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof TagData) {
                TagData other = (TagData) o;
                return TextUtils.equals(this.tag, other.tag) && this.ownerUid == other.ownerUid;
            }
            return false;
        }

        public String toString() {
            return "[" + this.ownerUid + " ; " + this.tag + "]";
        }

        int getByteSize() {
            int bytes = 0 + 8;
            String str = this.tag;
            return bytes + (str == null ? 0 : str.length() * 2) + 4 + 4 + 8;
        }
    }

    /* loaded from: classes2.dex */
    public static class Injector {
        public int getTagDatabaseSize() {
            return 128;
        }

        public int getLogSize() {
            return WakeLockLog.LOG_SIZE;
        }

        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        public SimpleDateFormat getDateFormat() {
            return WakeLockLog.DATE_FORMAT;
        }
    }
}
