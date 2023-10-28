package com.android.server.people.data;

import android.text.format.DateFormat;
import android.util.Range;
import android.util.Slog;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Function;
/* loaded from: classes2.dex */
public class EventIndex {
    private static final int RETENTION_DAYS = 63;
    private static final int TIME_SLOT_FOUR_HOURS = 1;
    private static final int TIME_SLOT_ONE_DAY = 0;
    private static final int TIME_SLOT_ONE_HOUR = 2;
    private static final int TIME_SLOT_TWO_MINUTES = 3;
    private static final int TIME_SLOT_TYPES_COUNT = 4;
    private final long[] mEventBitmaps;
    private final Injector mInjector;
    private long mLastUpdatedTime;
    private final Object mLock;
    private static final String TAG = EventIndex.class.getSimpleName();
    static final EventIndex EMPTY = new EventIndex();
    private static final List<Function<Long, Range<Long>>> TIME_SLOT_FACTORIES = Collections.unmodifiableList(Arrays.asList(new Function() { // from class: com.android.server.people.data.EventIndex$$ExternalSyntheticLambda0
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            Range createOneDayLongTimeSlot;
            createOneDayLongTimeSlot = EventIndex.createOneDayLongTimeSlot(((Long) obj).longValue());
            return createOneDayLongTimeSlot;
        }
    }, new Function() { // from class: com.android.server.people.data.EventIndex$$ExternalSyntheticLambda1
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            Range createFourHoursLongTimeSlot;
            createFourHoursLongTimeSlot = EventIndex.createFourHoursLongTimeSlot(((Long) obj).longValue());
            return createFourHoursLongTimeSlot;
        }
    }, new Function() { // from class: com.android.server.people.data.EventIndex$$ExternalSyntheticLambda2
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            Range createOneHourLongTimeSlot;
            createOneHourLongTimeSlot = EventIndex.createOneHourLongTimeSlot(((Long) obj).longValue());
            return createOneHourLongTimeSlot;
        }
    }, new Function() { // from class: com.android.server.people.data.EventIndex$$ExternalSyntheticLambda3
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            Range createTwoMinutesLongTimeSlot;
            createTwoMinutesLongTimeSlot = EventIndex.createTwoMinutesLongTimeSlot(((Long) obj).longValue());
            return createTwoMinutesLongTimeSlot;
        }
    }));

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    private @interface TimeSlotType {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static EventIndex combine(EventIndex lhs, EventIndex rhs) {
        long j = lhs.mLastUpdatedTime;
        long j2 = rhs.mLastUpdatedTime;
        EventIndex older = j < j2 ? lhs : rhs;
        EventIndex younger = j >= j2 ? lhs : rhs;
        EventIndex combined = new EventIndex(older);
        combined.updateEventBitmaps(younger.mLastUpdatedTime);
        for (int slotType = 0; slotType < 4; slotType++) {
            long[] jArr = combined.mEventBitmaps;
            jArr[slotType] = jArr[slotType] | younger.mEventBitmaps[slotType];
        }
        return combined;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public EventIndex() {
        this(new Injector());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public EventIndex(EventIndex from) {
        this(from.mInjector, from.mEventBitmaps, from.mLastUpdatedTime);
    }

    EventIndex(Injector injector) {
        this(injector, new long[]{0, 0, 0, 0}, injector.currentTimeMillis());
    }

    private EventIndex(Injector injector, long[] eventBitmaps, long lastUpdatedTime) {
        this.mLock = new Object();
        this.mInjector = injector;
        this.mEventBitmaps = Arrays.copyOf(eventBitmaps, 4);
        this.mLastUpdatedTime = lastUpdatedTime;
    }

    public Range<Long> getMostRecentActiveTimeSlot() {
        synchronized (this.mLock) {
            for (int slotType = 3; slotType >= 0; slotType--) {
                if (this.mEventBitmaps[slotType] != 0) {
                    Range<Long> lastTimeSlot = TIME_SLOT_FACTORIES.get(slotType).apply(Long.valueOf(this.mLastUpdatedTime));
                    int numberOfTrailingZeros = Long.numberOfTrailingZeros(this.mEventBitmaps[slotType]);
                    long offset = getDuration(lastTimeSlot) * numberOfTrailingZeros;
                    return Range.create(Long.valueOf(lastTimeSlot.getLower().longValue() - offset), Long.valueOf(lastTimeSlot.getUpper().longValue() - offset));
                }
            }
            return null;
        }
    }

    /* JADX DEBUG: Type inference failed for r3v2. Raw type applied. Possible types: java.util.List<android.util.Range<java.lang.Long>> */
    /* JADX WARN: Multi-variable type inference failed */
    public List<Range<Long>> getActiveTimeSlots() {
        List<Range<Long>> activeTimeSlots = new ArrayList<>();
        synchronized (this.mLock) {
            for (int slotType = 0; slotType < 4; slotType++) {
                activeTimeSlots = combineTimeSlotLists(activeTimeSlots, getActiveTimeSlotsForType(slotType));
            }
        }
        Collections.reverse(activeTimeSlots);
        return activeTimeSlots;
    }

    public boolean isEmpty() {
        synchronized (this.mLock) {
            for (int slotType = 0; slotType < 4; slotType++) {
                if (this.mEventBitmaps[slotType] != 0) {
                    return false;
                }
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addEvent(long eventTime) {
        if (EMPTY == this) {
            throw new IllegalStateException("EMPTY instance is immutable");
        }
        synchronized (this.mLock) {
            long currentTime = this.mInjector.currentTimeMillis();
            updateEventBitmaps(currentTime);
            for (int slotType = 0; slotType < 4; slotType++) {
                int offset = diffTimeSlots(slotType, eventTime, currentTime);
                if (offset < 64) {
                    long[] jArr = this.mEventBitmaps;
                    jArr[slotType] = jArr[slotType] | (1 << offset);
                }
            }
        }
    }

    void update() {
        updateEventBitmaps(this.mInjector.currentTimeMillis());
    }

    public String toString() {
        return "EventIndex {perDayEventBitmap=0b" + Long.toBinaryString(this.mEventBitmaps[0]) + ", perFourHoursEventBitmap=0b" + Long.toBinaryString(this.mEventBitmaps[1]) + ", perHourEventBitmap=0b" + Long.toBinaryString(this.mEventBitmaps[2]) + ", perTwoMinutesEventBitmap=0b" + Long.toBinaryString(this.mEventBitmaps[3]) + ", lastUpdatedTime=" + DateFormat.format("yyyy-MM-dd HH:mm:ss", this.mLastUpdatedTime) + "}";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EventIndex) {
            EventIndex other = (EventIndex) obj;
            return this.mLastUpdatedTime == other.mLastUpdatedTime && Arrays.equals(this.mEventBitmaps, other.mEventBitmaps);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Long.valueOf(this.mLastUpdatedTime), Integer.valueOf(Arrays.hashCode(this.mEventBitmaps)));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void writeToProto(ProtoOutputStream protoOutputStream) {
        long[] jArr;
        for (long bitmap : this.mEventBitmaps) {
            protoOutputStream.write(2211908157441L, bitmap);
        }
        protoOutputStream.write(1112396529666L, this.mLastUpdatedTime);
    }

    private void updateEventBitmaps(long currentTimeMillis) {
        for (int slotType = 0; slotType < 4; slotType++) {
            int offset = diffTimeSlots(slotType, this.mLastUpdatedTime, currentTimeMillis);
            if (offset < 64) {
                long[] jArr = this.mEventBitmaps;
                jArr[slotType] = jArr[slotType] << offset;
            } else {
                this.mEventBitmaps[slotType] = 0;
            }
        }
        long[] jArr2 = this.mEventBitmaps;
        long j = jArr2[0] << 1;
        jArr2[0] = j;
        jArr2[0] = j >>> 1;
        this.mLastUpdatedTime = currentTimeMillis;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static EventIndex readFromProto(ProtoInputStream protoInputStream) throws IOException {
        int bitmapIndex = 0;
        long[] eventBitmaps = new long[4];
        long lastUpdated = 0;
        while (protoInputStream.nextField() != -1) {
            switch (protoInputStream.getFieldNumber()) {
                case 1:
                    eventBitmaps[bitmapIndex] = protoInputStream.readLong(2211908157441L);
                    bitmapIndex++;
                    break;
                case 2:
                    lastUpdated = protoInputStream.readLong(1112396529666L);
                    break;
                default:
                    Slog.e(TAG, "Could not read undefined field: " + protoInputStream.getFieldNumber());
                    break;
            }
        }
        return new EventIndex(new Injector(), eventBitmaps, lastUpdated);
    }

    private static LocalDateTime toLocalDateTime(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), TimeZone.getDefault().toZoneId());
    }

    private static long toEpochMilli(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static long getDuration(Range<Long> timeSlot) {
        return timeSlot.getUpper().longValue() - timeSlot.getLower().longValue();
    }

    private static int diffTimeSlots(int timeSlotType, long fromTime, long toTime) {
        Function<Long, Range<Long>> timeSlotFactory = TIME_SLOT_FACTORIES.get(timeSlotType);
        Range<Long> fromSlot = timeSlotFactory.apply(Long.valueOf(fromTime));
        Range<Long> toSlot = timeSlotFactory.apply(Long.valueOf(toTime));
        return (int) ((toSlot.getLower().longValue() - fromSlot.getLower().longValue()) / getDuration(fromSlot));
    }

    private List<Range<Long>> getActiveTimeSlotsForType(int timeSlotType) {
        long eventBitmap = this.mEventBitmaps[timeSlotType];
        Range<Long> latestTimeSlot = TIME_SLOT_FACTORIES.get(timeSlotType).apply(Long.valueOf(this.mLastUpdatedTime));
        long startTime = latestTimeSlot.getLower().longValue();
        long duration = getDuration(latestTimeSlot);
        List<Range<Long>> timeSlots = new ArrayList<>();
        while (eventBitmap != 0) {
            int trailingZeros = Long.numberOfTrailingZeros(eventBitmap);
            if (trailingZeros > 0) {
                startTime -= trailingZeros * duration;
                eventBitmap >>>= trailingZeros;
            }
            if (eventBitmap != 0) {
                timeSlots.add(Range.create(Long.valueOf(startTime), Long.valueOf(startTime + duration)));
                startTime -= duration;
                eventBitmap >>>= 1;
            }
        }
        return timeSlots;
    }

    private static List<Range<Long>> combineTimeSlotLists(List<Range<Long>> longerSlots, List<Range<Long>> shorterSlots) {
        List<Range<Long>> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < longerSlots.size() && j < shorterSlots.size()) {
            Range<Long> longerSlot = longerSlots.get(i);
            Range<Long> shorterSlot = shorterSlots.get(j);
            if (longerSlot.contains(shorterSlot)) {
                result.add(shorterSlot);
                i++;
                j++;
            } else if (longerSlot.getLower().longValue() < shorterSlot.getLower().longValue()) {
                result.add(shorterSlot);
                j++;
            } else {
                result.add(longerSlot);
                i++;
            }
        }
        if (i < longerSlots.size()) {
            result.addAll(longerSlots.subList(i, longerSlots.size()));
        } else if (j < shorterSlots.size()) {
            result.addAll(shorterSlots.subList(j, shorterSlots.size()));
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Range<Long> createOneDayLongTimeSlot(long time) {
        LocalDateTime beginTime = toLocalDateTime(time).truncatedTo(ChronoUnit.DAYS);
        return Range.create(Long.valueOf(toEpochMilli(beginTime)), Long.valueOf(toEpochMilli(beginTime.plusDays(1L))));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Range<Long> createFourHoursLongTimeSlot(long time) {
        int hourOfDay = toLocalDateTime(time).getHour();
        LocalDateTime beginTime = toLocalDateTime(time).truncatedTo(ChronoUnit.HOURS).minusHours(hourOfDay % 4);
        return Range.create(Long.valueOf(toEpochMilli(beginTime)), Long.valueOf(toEpochMilli(beginTime.plusHours(4L))));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Range<Long> createOneHourLongTimeSlot(long time) {
        LocalDateTime beginTime = toLocalDateTime(time).truncatedTo(ChronoUnit.HOURS);
        return Range.create(Long.valueOf(toEpochMilli(beginTime)), Long.valueOf(toEpochMilli(beginTime.plusHours(1L))));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Range<Long> createTwoMinutesLongTimeSlot(long time) {
        int minuteOfHour = toLocalDateTime(time).getMinute();
        LocalDateTime beginTime = toLocalDateTime(time).truncatedTo(ChronoUnit.MINUTES).minusMinutes(minuteOfHour % 2);
        return Range.create(Long.valueOf(toEpochMilli(beginTime)), Long.valueOf(toEpochMilli(beginTime.plusMinutes(2L))));
    }

    /* loaded from: classes2.dex */
    static class Injector {
        Injector() {
        }

        long currentTimeMillis() {
            return System.currentTimeMillis();
        }
    }
}
