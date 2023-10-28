package com.android.server.display;

import android.hardware.display.AmbientBrightnessDayStats;
import android.os.SystemClock;
import android.os.UserManager;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class AmbientBrightnessStatsTracker {
    static final float[] BUCKET_BOUNDARIES_FOR_NEW_STATS = {0.0f, 0.1f, 0.3f, 1.0f, 3.0f, 10.0f, 30.0f, 100.0f, 300.0f, 1000.0f, 3000.0f, 10000.0f};
    private static final boolean DEBUG = false;
    static final int MAX_DAYS_TO_TRACK = 7;
    private static final String TAG = "AmbientBrightnessStatsTracker";
    private final AmbientBrightnessStats mAmbientBrightnessStats;
    private float mCurrentAmbientBrightness;
    private int mCurrentUserId;
    private final Injector mInjector;
    private final Timer mTimer;
    private final UserManager mUserManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Clock {
        long elapsedTimeMillis();
    }

    public AmbientBrightnessStatsTracker(UserManager userManager, Injector injector) {
        this.mUserManager = userManager;
        if (injector != null) {
            this.mInjector = injector;
        } else {
            this.mInjector = new Injector();
        }
        this.mAmbientBrightnessStats = new AmbientBrightnessStats();
        this.mTimer = new Timer(new Clock() { // from class: com.android.server.display.AmbientBrightnessStatsTracker$$ExternalSyntheticLambda0
            @Override // com.android.server.display.AmbientBrightnessStatsTracker.Clock
            public final long elapsedTimeMillis() {
                return AmbientBrightnessStatsTracker.this.m3185x8a34b9ac();
            }
        });
        this.mCurrentAmbientBrightness = -1.0f;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-display-AmbientBrightnessStatsTracker  reason: not valid java name */
    public /* synthetic */ long m3185x8a34b9ac() {
        return this.mInjector.elapsedRealtimeMillis();
    }

    public synchronized void start() {
        this.mTimer.reset();
        this.mTimer.start();
    }

    public synchronized void stop() {
        if (this.mTimer.isRunning()) {
            this.mAmbientBrightnessStats.log(this.mCurrentUserId, this.mInjector.getLocalDate(), this.mCurrentAmbientBrightness, this.mTimer.totalDurationSec());
        }
        this.mTimer.reset();
        this.mCurrentAmbientBrightness = -1.0f;
    }

    public synchronized void add(int userId, float newAmbientBrightness) {
        if (this.mTimer.isRunning()) {
            int i = this.mCurrentUserId;
            if (userId == i) {
                this.mAmbientBrightnessStats.log(i, this.mInjector.getLocalDate(), this.mCurrentAmbientBrightness, this.mTimer.totalDurationSec());
            } else {
                this.mCurrentUserId = userId;
            }
            this.mTimer.reset();
            this.mTimer.start();
            this.mCurrentAmbientBrightness = newAmbientBrightness;
        }
    }

    public synchronized void writeStats(OutputStream stream) throws IOException {
        this.mAmbientBrightnessStats.writeToXML(stream);
    }

    public synchronized void readStats(InputStream stream) throws IOException {
        this.mAmbientBrightnessStats.readFromXML(stream);
    }

    public synchronized ArrayList<AmbientBrightnessDayStats> getUserStats(int userId) {
        return this.mAmbientBrightnessStats.getUserStats(userId);
    }

    public synchronized void dump(PrintWriter pw) {
        pw.println("AmbientBrightnessStats:");
        pw.print(this.mAmbientBrightnessStats);
    }

    /* loaded from: classes.dex */
    class AmbientBrightnessStats {
        private static final String ATTR_BUCKET_BOUNDARIES = "bucket-boundaries";
        private static final String ATTR_BUCKET_STATS = "bucket-stats";
        private static final String ATTR_LOCAL_DATE = "local-date";
        private static final String ATTR_USER = "user";
        private static final String TAG_AMBIENT_BRIGHTNESS_DAY_STATS = "ambient-brightness-day-stats";
        private static final String TAG_AMBIENT_BRIGHTNESS_STATS = "ambient-brightness-stats";
        private Map<Integer, Deque<AmbientBrightnessDayStats>> mStats = new HashMap();

        public AmbientBrightnessStats() {
        }

        public void log(int userId, LocalDate localDate, float ambientBrightness, float durationSec) {
            Deque<AmbientBrightnessDayStats> userStats = getOrCreateUserStats(this.mStats, userId);
            AmbientBrightnessDayStats dayStats = getOrCreateDayStats(userStats, localDate);
            dayStats.log(ambientBrightness, durationSec);
        }

        public ArrayList<AmbientBrightnessDayStats> getUserStats(int userId) {
            if (this.mStats.containsKey(Integer.valueOf(userId))) {
                return new ArrayList<>(this.mStats.get(Integer.valueOf(userId)));
            }
            return null;
        }

        public void writeToXML(OutputStream stream) throws IOException {
            TypedXmlSerializer out = Xml.resolveSerializer(stream);
            out.startDocument((String) null, true);
            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            LocalDate cutOffDate = AmbientBrightnessStatsTracker.this.mInjector.getLocalDate().minusDays(7L);
            out.startTag((String) null, TAG_AMBIENT_BRIGHTNESS_STATS);
            for (Map.Entry<Integer, Deque<AmbientBrightnessDayStats>> entry : this.mStats.entrySet()) {
                for (AmbientBrightnessDayStats userDayStats : entry.getValue()) {
                    int userSerialNumber = AmbientBrightnessStatsTracker.this.mInjector.getUserSerialNumber(AmbientBrightnessStatsTracker.this.mUserManager, entry.getKey().intValue());
                    if (userSerialNumber != -1 && userDayStats.getLocalDate().isAfter(cutOffDate)) {
                        out.startTag((String) null, TAG_AMBIENT_BRIGHTNESS_DAY_STATS);
                        out.attributeInt((String) null, ATTR_USER, userSerialNumber);
                        out.attribute((String) null, ATTR_LOCAL_DATE, userDayStats.getLocalDate().toString());
                        StringBuilder bucketBoundariesValues = new StringBuilder();
                        StringBuilder timeSpentValues = new StringBuilder();
                        for (int i = 0; i < userDayStats.getBucketBoundaries().length; i++) {
                            if (i > 0) {
                                bucketBoundariesValues.append(",");
                                timeSpentValues.append(",");
                            }
                            bucketBoundariesValues.append(userDayStats.getBucketBoundaries()[i]);
                            timeSpentValues.append(userDayStats.getStats()[i]);
                        }
                        out.attribute((String) null, ATTR_BUCKET_BOUNDARIES, bucketBoundariesValues.toString());
                        out.attribute((String) null, ATTR_BUCKET_STATS, timeSpentValues.toString());
                        out.endTag((String) null, TAG_AMBIENT_BRIGHTNESS_DAY_STATS);
                    }
                }
            }
            out.endTag((String) null, TAG_AMBIENT_BRIGHTNESS_STATS);
            out.endDocument();
            stream.flush();
        }

        /* JADX WARN: Code restructure failed: missing block: B:40:0x00dd, code lost:
            throw new java.io.IOException("Invalid brightness stats string.");
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void readFromXML(InputStream stream) throws IOException {
            int i;
            String str;
            String str2 = ",";
            try {
                Map<Integer, Deque<AmbientBrightnessDayStats>> parsedStats = new HashMap<>();
                TypedXmlPullParser parser = Xml.resolvePullParser(stream);
                while (true) {
                    int type = parser.next();
                    i = 1;
                    if (type == 1 || type == 2) {
                        break;
                    }
                }
                String tag = parser.getName();
                if (!TAG_AMBIENT_BRIGHTNESS_STATS.equals(tag)) {
                    throw new XmlPullParserException("Ambient brightness stats not found in tracker file " + tag);
                }
                LocalDate cutOffDate = AmbientBrightnessStatsTracker.this.mInjector.getLocalDate().minusDays(7L);
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == i || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    }
                    if (type2 == 3) {
                        str = str2;
                    } else if (type2 == 4) {
                        str = str2;
                    } else if (!TAG_AMBIENT_BRIGHTNESS_DAY_STATS.equals(parser.getName())) {
                        str = str2;
                    } else {
                        int userSerialNumber = parser.getAttributeInt((String) null, ATTR_USER);
                        LocalDate localDate = LocalDate.parse(parser.getAttributeValue((String) null, ATTR_LOCAL_DATE));
                        String[] bucketBoundaries = parser.getAttributeValue((String) null, ATTR_BUCKET_BOUNDARIES).split(str2);
                        String[] bucketStats = parser.getAttributeValue((String) null, ATTR_BUCKET_STATS).split(str2);
                        if (bucketBoundaries.length != bucketStats.length || bucketBoundaries.length < i) {
                            break;
                        }
                        float[] parsedBucketBoundaries = new float[bucketBoundaries.length];
                        float[] parsedBucketStats = new float[bucketStats.length];
                        for (int i2 = 0; i2 < bucketBoundaries.length; i2++) {
                            parsedBucketBoundaries[i2] = Float.parseFloat(bucketBoundaries[i2]);
                            parsedBucketStats[i2] = Float.parseFloat(bucketStats[i2]);
                        }
                        int userId = AmbientBrightnessStatsTracker.this.mInjector.getUserId(AmbientBrightnessStatsTracker.this.mUserManager, userSerialNumber);
                        if (userId == -1 || !localDate.isAfter(cutOffDate)) {
                            str = str2;
                        } else {
                            Deque<AmbientBrightnessDayStats> userStats = getOrCreateUserStats(parsedStats, userId);
                            str = str2;
                            userStats.offer(new AmbientBrightnessDayStats(localDate, parsedBucketBoundaries, parsedBucketStats));
                        }
                    }
                    str2 = str;
                    i = 1;
                }
                this.mStats = parsedStats;
            } catch (IOException | NullPointerException | NumberFormatException | DateTimeParseException | XmlPullParserException e) {
                throw new IOException("Failed to parse brightness stats file.", e);
            }
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<Integer, Deque<AmbientBrightnessDayStats>> entry : this.mStats.entrySet()) {
                for (AmbientBrightnessDayStats dayStats : entry.getValue()) {
                    builder.append("  ");
                    builder.append(entry.getKey()).append(" ");
                    builder.append(dayStats).append("\n");
                }
            }
            return builder.toString();
        }

        private Deque<AmbientBrightnessDayStats> getOrCreateUserStats(Map<Integer, Deque<AmbientBrightnessDayStats>> stats, int userId) {
            if (!stats.containsKey(Integer.valueOf(userId))) {
                stats.put(Integer.valueOf(userId), new ArrayDeque());
            }
            return stats.get(Integer.valueOf(userId));
        }

        private AmbientBrightnessDayStats getOrCreateDayStats(Deque<AmbientBrightnessDayStats> userStats, LocalDate localDate) {
            AmbientBrightnessDayStats lastBrightnessStats = userStats.peekLast();
            if (lastBrightnessStats != null && lastBrightnessStats.getLocalDate().equals(localDate)) {
                return lastBrightnessStats;
            }
            AmbientBrightnessDayStats dayStats = new AmbientBrightnessDayStats(localDate, AmbientBrightnessStatsTracker.BUCKET_BOUNDARIES_FOR_NEW_STATS);
            if (userStats.size() == 7) {
                userStats.poll();
            }
            userStats.offer(dayStats);
            return dayStats;
        }
    }

    /* loaded from: classes.dex */
    static class Timer {
        private final Clock clock;
        private long startTimeMillis;
        private boolean started;

        public Timer(Clock clock) {
            this.clock = clock;
        }

        public void reset() {
            this.started = false;
        }

        public void start() {
            if (!this.started) {
                this.startTimeMillis = this.clock.elapsedTimeMillis();
                this.started = true;
            }
        }

        public boolean isRunning() {
            return this.started;
        }

        public float totalDurationSec() {
            if (this.started) {
                return (float) ((this.clock.elapsedTimeMillis() - this.startTimeMillis) / 1000.0d);
            }
            return 0.0f;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        Injector() {
        }

        public long elapsedRealtimeMillis() {
            return SystemClock.elapsedRealtime();
        }

        public int getUserSerialNumber(UserManager userManager, int userId) {
            return userManager.getUserSerialNumber(userId);
        }

        public int getUserId(UserManager userManager, int userSerialNumber) {
            return userManager.getUserHandle(userSerialNumber);
        }

        public LocalDate getLocalDate() {
            return LocalDate.now();
        }
    }
}
