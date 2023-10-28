package com.android.server.display;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ParceledListSlice;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.AmbientBrightnessDayStats;
import android.hardware.display.BrightnessChangeEvent;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.ColorDisplayManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.DisplayedContentSample;
import android.hardware.display.DisplayedContentSamplingAttributes;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.Settings;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.view.Display;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.RingBuffer;
import com.android.server.LocalServices;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class BrightnessTracker {
    private static final String AMBIENT_BRIGHTNESS_STATS_FILE = "ambient_brightness_stats.xml";
    private static final String ATTR_BATTERY_LEVEL = "batteryLevel";
    private static final String ATTR_COLOR_SAMPLE_DURATION = "colorSampleDuration";
    private static final String ATTR_COLOR_TEMPERATURE = "colorTemperature";
    private static final String ATTR_COLOR_VALUE_BUCKETS = "colorValueBuckets";
    private static final String ATTR_DEFAULT_CONFIG = "defaultConfig";
    private static final String ATTR_LAST_NITS = "lastNits";
    private static final String ATTR_LUX = "lux";
    private static final String ATTR_LUX_TIMESTAMPS = "luxTimestamps";
    private static final String ATTR_NIGHT_MODE = "nightMode";
    private static final String ATTR_NITS = "nits";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_POWER_SAVE = "powerSaveFactor";
    private static final String ATTR_REDUCE_BRIGHT_COLORS = "reduceBrightColors";
    private static final String ATTR_REDUCE_BRIGHT_COLORS_OFFSET = "reduceBrightColorsOffset";
    private static final String ATTR_REDUCE_BRIGHT_COLORS_STRENGTH = "reduceBrightColorsStrength";
    private static final String ATTR_TIMESTAMP = "timestamp";
    private static final String ATTR_UNIQUE_DISPLAY_ID = "uniqueDisplayId";
    private static final String ATTR_USER = "user";
    private static final String ATTR_USER_POINT = "userPoint";
    private static final int COLOR_SAMPLE_COMPONENT_MASK = 4;
    private static final String EVENTS_FILE = "brightness_events.xml";
    private static final int MAX_EVENTS = 100;
    private static final int MSG_BACKGROUND_START = 0;
    private static final int MSG_BRIGHTNESS_CHANGED = 1;
    private static final int MSG_BRIGHTNESS_CONFIG_CHANGED = 4;
    private static final int MSG_SENSOR_CHANGED = 5;
    private static final int MSG_START_SENSOR_LISTENER = 3;
    private static final int MSG_STOP_SENSOR_LISTENER = 2;
    static final String TAG = "BrightnessTracker";
    private static final String TAG_EVENT = "event";
    private static final String TAG_EVENTS = "events";
    private AmbientBrightnessStatsTracker mAmbientBrightnessStatsTracker;
    private final Handler mBgHandler;
    private BrightnessConfiguration mBrightnessConfiguration;
    private BroadcastReceiver mBroadcastReceiver;
    private boolean mColorSamplingEnabled;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private DisplayListener mDisplayListener;
    private boolean mEventsDirty;
    private float mFrameRate;
    private final Injector mInjector;
    private Sensor mLightSensor;
    private int mNoFramesToSample;
    private SensorListener mSensorListener;
    private boolean mSensorRegistered;
    private SettingsObserver mSettingsObserver;
    private boolean mStarted;
    private final UserManager mUserManager;
    private volatile boolean mWriteBrightnessTrackerStateScheduled;
    static final boolean DEBUG = SystemProperties.getBoolean("dbg.dms.brighttrack", false);
    private static final long MAX_EVENT_AGE = TimeUnit.DAYS.toMillis(30);
    private static final long LUX_EVENT_HORIZON = TimeUnit.SECONDS.toNanos(10);
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static final long COLOR_SAMPLE_DURATION = TimeUnit.SECONDS.toSeconds(10);
    private final Object mEventsLock = new Object();
    private RingBuffer<BrightnessChangeEvent> mEvents = new RingBuffer<>(BrightnessChangeEvent.class, 100);
    private int mCurrentUserId = -10000;
    private final Object mDataCollectionLock = new Object();
    private Deque<LightData> mLastSensorReadings = new ArrayDeque();
    private float mLastBatteryLevel = Float.NaN;
    private float mLastBrightness = -1.0f;

    public BrightnessTracker(Context context, Injector injector) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        if (injector != null) {
            this.mInjector = injector;
        } else {
            this.mInjector = new Injector();
        }
        this.mBgHandler = new TrackerHandler(this.mInjector.getBackgroundHandler().getLooper());
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
    }

    public void start(float initialBrightness) {
        if (DEBUG) {
            Slog.d(TAG, "Start");
        }
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        this.mBgHandler.obtainMessage(0, Float.valueOf(initialBrightness)).sendToTarget();
    }

    public void setBrightnessConfiguration(BrightnessConfiguration brightnessConfiguration) {
        this.mBgHandler.obtainMessage(4, brightnessConfiguration).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void backgroundStart(float initialBrightness) {
        if (DEBUG) {
            Slog.d(TAG, "Background start");
        }
        readEvents();
        readAmbientBrightnessStats();
        this.mSensorListener = new SensorListener();
        SettingsObserver settingsObserver = new SettingsObserver(this.mBgHandler);
        this.mSettingsObserver = settingsObserver;
        this.mInjector.registerBrightnessModeObserver(this.mContentResolver, settingsObserver);
        startSensorListener();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        Receiver receiver = new Receiver();
        this.mBroadcastReceiver = receiver;
        this.mInjector.registerReceiver(this.mContext, receiver, intentFilter);
        this.mInjector.scheduleIdleJob(this.mContext);
        synchronized (this.mDataCollectionLock) {
            this.mLastBrightness = initialBrightness;
            this.mStarted = true;
        }
        enableColorSampling();
    }

    void stop() {
        if (DEBUG) {
            Slog.d(TAG, "Stop");
        }
        this.mBgHandler.removeMessages(0);
        stopSensorListener();
        this.mInjector.unregisterSensorListener(this.mContext, this.mSensorListener);
        this.mInjector.unregisterBrightnessModeObserver(this.mContext, this.mSettingsObserver);
        this.mInjector.unregisterReceiver(this.mContext, this.mBroadcastReceiver);
        this.mInjector.cancelIdleJob(this.mContext);
        synchronized (this.mDataCollectionLock) {
            this.mStarted = false;
        }
        disableColorSampling();
    }

    public void onSwitchUser(int newUserId) {
        if (DEBUG) {
            Slog.d(TAG, "Used id updated from " + this.mCurrentUserId + " to " + newUserId);
        }
        this.mCurrentUserId = newUserId;
    }

    public ParceledListSlice<BrightnessChangeEvent> getEvents(int userId, boolean includePackage) {
        BrightnessChangeEvent[] events;
        synchronized (this.mEventsLock) {
            events = (BrightnessChangeEvent[]) this.mEvents.toArray();
        }
        int[] profiles = this.mInjector.getProfileIds(this.mUserManager, userId);
        Map<Integer, Boolean> toRedact = new HashMap<>();
        int i = 0;
        while (true) {
            boolean redact = true;
            if (i >= profiles.length) {
                break;
            }
            int profileId = profiles[i];
            if (includePackage && profileId == userId) {
                redact = false;
            }
            toRedact.put(Integer.valueOf(profiles[i]), Boolean.valueOf(redact));
            i++;
        }
        ArrayList<BrightnessChangeEvent> out = new ArrayList<>(events.length);
        for (int i2 = 0; i2 < events.length; i2++) {
            Boolean redact2 = toRedact.get(Integer.valueOf(events[i2].userId));
            if (redact2 != null) {
                if (!redact2.booleanValue()) {
                    out.add(events[i2]);
                } else {
                    BrightnessChangeEvent event = new BrightnessChangeEvent(events[i2], true);
                    out.add(event);
                }
            }
        }
        return new ParceledListSlice<>(out);
    }

    public void persistBrightnessTrackerState() {
        scheduleWriteBrightnessTrackerState();
    }

    public void notifyBrightnessChanged(float brightness, boolean userInitiated, float powerBrightnessFactor, boolean isUserSetBrightness, boolean isDefaultBrightnessConfig, String uniqueDisplayId) {
        if (DEBUG) {
            Slog.d(TAG, String.format("notifyBrightnessChanged(brightness=%f, userInitiated=%b)", Float.valueOf(brightness), Boolean.valueOf(userInitiated)));
        }
        Message m = this.mBgHandler.obtainMessage(1, userInitiated ? 1 : 0, 0, new BrightnessChangeValues(brightness, powerBrightnessFactor, isUserSetBrightness, isDefaultBrightnessConfig, this.mInjector.currentTimeMillis(), uniqueDisplayId));
        m.sendToTarget();
    }

    public void setLightSensor(Sensor lightSensor) {
        this.mBgHandler.obtainMessage(5, 0, 0, lightSensor).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleBrightnessChanged(float brightness, boolean userInitiated, float powerBrightnessFactor, boolean isUserSetBrightness, boolean isDefaultBrightnessConfig, long timestamp, String uniqueDisplayId) {
        DisplayedContentSample sample;
        synchronized (this.mDataCollectionLock) {
            if (this.mStarted) {
                float previousBrightness = this.mLastBrightness;
                this.mLastBrightness = brightness;
                if (userInitiated) {
                    BrightnessChangeEvent.Builder builder = new BrightnessChangeEvent.Builder();
                    builder.setBrightness(brightness);
                    builder.setTimeStamp(timestamp);
                    builder.setPowerBrightnessFactor(powerBrightnessFactor);
                    builder.setUserBrightnessPoint(isUserSetBrightness);
                    builder.setIsDefaultBrightnessConfig(isDefaultBrightnessConfig);
                    builder.setUniqueDisplayId(uniqueDisplayId);
                    int readingCount = this.mLastSensorReadings.size();
                    if (readingCount == 0) {
                        return;
                    }
                    float[] luxValues = new float[readingCount];
                    long[] luxTimestamps = new long[readingCount];
                    int pos = 0;
                    long currentTimeMillis = this.mInjector.currentTimeMillis();
                    long elapsedTimeNanos = this.mInjector.elapsedRealtimeNanos();
                    for (Iterator<LightData> it = this.mLastSensorReadings.iterator(); it.hasNext(); it = it) {
                        LightData reading = it.next();
                        luxValues[pos] = reading.lux;
                        luxTimestamps[pos] = currentTimeMillis - TimeUnit.NANOSECONDS.toMillis(elapsedTimeNanos - reading.timestamp);
                        pos++;
                    }
                    builder.setLuxValues(luxValues);
                    builder.setLuxTimestamps(luxTimestamps);
                    builder.setBatteryLevel(this.mLastBatteryLevel);
                    builder.setLastBrightness(previousBrightness);
                    try {
                        ActivityTaskManager.RootTaskInfo focusedTask = this.mInjector.getFocusedStack();
                        if (focusedTask != null && focusedTask.topActivity != null) {
                            builder.setUserId(focusedTask.userId);
                            builder.setPackageName(focusedTask.topActivity.getPackageName());
                            builder.setNightMode(this.mInjector.isNightDisplayActivated(this.mContext));
                            builder.setColorTemperature(this.mInjector.getNightDisplayColorTemperature(this.mContext));
                            builder.setReduceBrightColors(this.mInjector.isReduceBrightColorsActivated(this.mContext));
                            builder.setReduceBrightColorsStrength(this.mInjector.getReduceBrightColorsStrength(this.mContext));
                            builder.setReduceBrightColorsOffset(this.mInjector.getReduceBrightColorsOffsetFactor(this.mContext) * brightness);
                            if (this.mColorSamplingEnabled && (sample = this.mInjector.sampleColor(this.mNoFramesToSample)) != null && sample.getSampleComponent(DisplayedContentSample.ColorComponent.CHANNEL2) != null) {
                                float numMillis = (((float) sample.getNumFrames()) / this.mFrameRate) * 1000.0f;
                                builder.setColorValues(sample.getSampleComponent(DisplayedContentSample.ColorComponent.CHANNEL2), Math.round(numMillis));
                            }
                            BrightnessChangeEvent event = builder.build();
                            if (DEBUG) {
                                Slog.d(TAG, "Event " + event.brightness + " " + event.packageName);
                            }
                            synchronized (this.mEventsLock) {
                                this.mEventsDirty = true;
                                this.mEvents.append(event);
                            }
                        } else if (DEBUG) {
                            Slog.d(TAG, "Ignoring event due to null focusedTask.");
                        }
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSensorChanged(Sensor lightSensor) {
        if (this.mLightSensor != lightSensor) {
            this.mLightSensor = lightSensor;
            stopSensorListener();
            synchronized (this.mDataCollectionLock) {
                this.mLastSensorReadings.clear();
            }
            startSensorListener();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startSensorListener() {
        if (!this.mSensorRegistered && this.mLightSensor != null && this.mAmbientBrightnessStatsTracker != null && this.mInjector.isInteractive(this.mContext) && this.mInjector.isBrightnessModeAutomatic(this.mContentResolver)) {
            this.mAmbientBrightnessStatsTracker.start();
            this.mSensorRegistered = true;
            Injector injector = this.mInjector;
            injector.registerSensorListener(this.mContext, this.mSensorListener, this.mLightSensor, injector.getBackgroundHandler());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopSensorListener() {
        if (this.mSensorRegistered) {
            this.mAmbientBrightnessStatsTracker.stop();
            this.mInjector.unregisterSensorListener(this.mContext, this.mSensorListener);
            this.mSensorRegistered = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleWriteBrightnessTrackerState() {
        if (!this.mWriteBrightnessTrackerStateScheduled) {
            this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.BrightnessTracker$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    BrightnessTracker.this.m3225x622fb9f();
                }
            });
            this.mWriteBrightnessTrackerStateScheduled = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleWriteBrightnessTrackerState$0$com-android-server-display-BrightnessTracker  reason: not valid java name */
    public /* synthetic */ void m3225x622fb9f() {
        this.mWriteBrightnessTrackerStateScheduled = false;
        writeEvents();
        writeAmbientBrightnessStats();
    }

    private void writeEvents() {
        synchronized (this.mEventsLock) {
            if (this.mEventsDirty) {
                AtomicFile writeTo = this.mInjector.getFile(EVENTS_FILE);
                if (writeTo == null) {
                    return;
                }
                if (this.mEvents.isEmpty()) {
                    if (writeTo.exists()) {
                        writeTo.delete();
                    }
                    this.mEventsDirty = false;
                } else {
                    FileOutputStream output = null;
                    try {
                        output = writeTo.startWrite();
                        writeEventsLocked(output);
                        writeTo.finishWrite(output);
                        this.mEventsDirty = false;
                    } catch (IOException e) {
                        writeTo.failWrite(output);
                        Slog.e(TAG, "Failed to write change mEvents.", e);
                    }
                }
            }
        }
    }

    private void writeAmbientBrightnessStats() {
        AtomicFile writeTo = this.mInjector.getFile(AMBIENT_BRIGHTNESS_STATS_FILE);
        if (writeTo == null) {
            return;
        }
        FileOutputStream output = null;
        try {
            output = writeTo.startWrite();
            this.mAmbientBrightnessStatsTracker.writeStats(output);
            writeTo.finishWrite(output);
        } catch (IOException e) {
            writeTo.failWrite(output);
            Slog.e(TAG, "Failed to write ambient brightness stats.", e);
        }
    }

    private void readEvents() {
        synchronized (this.mEventsLock) {
            this.mEventsDirty = true;
            this.mEvents.clear();
            AtomicFile readFrom = this.mInjector.getFile(EVENTS_FILE);
            if (readFrom != null && readFrom.exists()) {
                FileInputStream input = null;
                try {
                    input = readFrom.openRead();
                    readEventsLocked(input);
                } catch (IOException e) {
                    readFrom.delete();
                    Slog.e(TAG, "Failed to read change mEvents.", e);
                }
                IoUtils.closeQuietly(input);
            }
        }
    }

    private void readAmbientBrightnessStats() {
        this.mAmbientBrightnessStatsTracker = new AmbientBrightnessStatsTracker(this.mUserManager, null);
        AtomicFile readFrom = this.mInjector.getFile(AMBIENT_BRIGHTNESS_STATS_FILE);
        if (readFrom != null && readFrom.exists()) {
            FileInputStream input = null;
            try {
                try {
                    input = readFrom.openRead();
                    this.mAmbientBrightnessStatsTracker.readStats(input);
                } catch (IOException e) {
                    readFrom.delete();
                    Slog.e(TAG, "Failed to read ambient brightness stats.", e);
                }
            } finally {
                IoUtils.closeQuietly(input);
            }
        }
    }

    void writeEventsLocked(OutputStream stream) throws IOException {
        TypedXmlSerializer out = Xml.resolveSerializer(stream);
        out.startDocument((String) null, true);
        out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        out.startTag((String) null, TAG_EVENTS);
        BrightnessChangeEvent[] toWrite = (BrightnessChangeEvent[]) this.mEvents.toArray();
        this.mEvents.clear();
        if (DEBUG) {
            Slog.d(TAG, "Writing events " + toWrite.length);
        }
        long timeCutOff = this.mInjector.currentTimeMillis() - MAX_EVENT_AGE;
        for (int i = 0; i < toWrite.length; i++) {
            int userSerialNo = this.mInjector.getUserSerialNumber(this.mUserManager, toWrite[i].userId);
            if (userSerialNo != -1 && toWrite[i].timeStamp > timeCutOff) {
                this.mEvents.append(toWrite[i]);
                out.startTag((String) null, TAG_EVENT);
                out.attributeFloat((String) null, ATTR_NITS, toWrite[i].brightness);
                out.attributeLong((String) null, "timestamp", toWrite[i].timeStamp);
                out.attribute((String) null, "packageName", toWrite[i].packageName);
                out.attributeInt((String) null, ATTR_USER, userSerialNo);
                String uniqueDisplayId = toWrite[i].uniqueDisplayId;
                if (uniqueDisplayId == null) {
                    uniqueDisplayId = "";
                }
                out.attribute((String) null, ATTR_UNIQUE_DISPLAY_ID, uniqueDisplayId);
                out.attributeFloat((String) null, ATTR_BATTERY_LEVEL, toWrite[i].batteryLevel);
                out.attributeBoolean((String) null, ATTR_NIGHT_MODE, toWrite[i].nightMode);
                out.attributeInt((String) null, ATTR_COLOR_TEMPERATURE, toWrite[i].colorTemperature);
                out.attributeBoolean((String) null, ATTR_REDUCE_BRIGHT_COLORS, toWrite[i].reduceBrightColors);
                out.attributeInt((String) null, ATTR_REDUCE_BRIGHT_COLORS_STRENGTH, toWrite[i].reduceBrightColorsStrength);
                out.attributeFloat((String) null, ATTR_REDUCE_BRIGHT_COLORS_OFFSET, toWrite[i].reduceBrightColorsOffset);
                out.attributeFloat((String) null, ATTR_LAST_NITS, toWrite[i].lastBrightness);
                out.attributeBoolean((String) null, ATTR_DEFAULT_CONFIG, toWrite[i].isDefaultBrightnessConfig);
                out.attributeFloat((String) null, ATTR_POWER_SAVE, toWrite[i].powerBrightnessFactor);
                out.attributeBoolean((String) null, ATTR_USER_POINT, toWrite[i].isUserSetBrightness);
                StringBuilder luxValues = new StringBuilder();
                StringBuilder luxTimestamps = new StringBuilder();
                for (int j = 0; j < toWrite[i].luxValues.length; j++) {
                    if (j > 0) {
                        luxValues.append(',');
                        luxTimestamps.append(',');
                    }
                    luxValues.append(Float.toString(toWrite[i].luxValues[j]));
                    luxTimestamps.append(Long.toString(toWrite[i].luxTimestamps[j]));
                }
                out.attribute((String) null, ATTR_LUX, luxValues.toString());
                out.attribute((String) null, ATTR_LUX_TIMESTAMPS, luxTimestamps.toString());
                if (toWrite[i].colorValueBuckets != null && toWrite[i].colorValueBuckets.length > 0) {
                    out.attributeLong((String) null, ATTR_COLOR_SAMPLE_DURATION, toWrite[i].colorSampleDuration);
                    StringBuilder buckets = new StringBuilder();
                    for (int j2 = 0; j2 < toWrite[i].colorValueBuckets.length; j2++) {
                        if (j2 > 0) {
                            buckets.append(',');
                        }
                        buckets.append(Long.toString(toWrite[i].colorValueBuckets[j2]));
                    }
                    out.attribute((String) null, ATTR_COLOR_VALUE_BUCKETS, buckets.toString());
                }
                out.endTag((String) null, TAG_EVENT);
            }
        }
        out.endTag((String) null, TAG_EVENTS);
        out.endDocument();
        stream.flush();
    }

    void readEventsLocked(InputStream stream) throws IOException {
        int i;
        String str;
        TypedXmlPullParser parser;
        int type;
        int outerDepth;
        String str2;
        TypedXmlPullParser parser2;
        String tag;
        int type2;
        int outerDepth2;
        String str3 = ",";
        try {
            TypedXmlPullParser parser3 = Xml.resolvePullParser(stream);
            while (true) {
                int type3 = parser3.next();
                i = 1;
                if (type3 == 1 || type3 == 2) {
                    break;
                }
            }
            String tag2 = parser3.getName();
            if (!TAG_EVENTS.equals(tag2)) {
                throw new XmlPullParserException("Events not found in brightness tracker file " + tag2);
            }
            long timeCutOff = this.mInjector.currentTimeMillis() - MAX_EVENT_AGE;
            int outerDepth3 = parser3.getDepth();
            while (true) {
                int type4 = parser3.next();
                if (type4 != i) {
                    if (type4 != 3 || parser3.getDepth() > outerDepth3) {
                        if (type4 == 3) {
                            str = str3;
                            parser = parser3;
                            type = type4;
                            outerDepth = outerDepth3;
                        } else if (type4 == 4) {
                            str = str3;
                            parser = parser3;
                            type = type4;
                            outerDepth = outerDepth3;
                        } else {
                            String tag3 = parser3.getName();
                            if (!TAG_EVENT.equals(tag3)) {
                                str2 = str3;
                                parser2 = parser3;
                                tag = tag3;
                                type2 = type4;
                                outerDepth2 = outerDepth3;
                            } else {
                                BrightnessChangeEvent.Builder builder = new BrightnessChangeEvent.Builder();
                                builder.setBrightness(parser3.getAttributeFloat((String) null, ATTR_NITS));
                                builder.setTimeStamp(parser3.getAttributeLong((String) null, "timestamp"));
                                builder.setPackageName(parser3.getAttributeValue((String) null, "packageName"));
                                builder.setUserId(this.mInjector.getUserId(this.mUserManager, parser3.getAttributeInt((String) null, ATTR_USER)));
                                String uniqueDisplayId = parser3.getAttributeValue((String) null, ATTR_UNIQUE_DISPLAY_ID);
                                if (uniqueDisplayId == null) {
                                    uniqueDisplayId = "";
                                }
                                builder.setUniqueDisplayId(uniqueDisplayId);
                                builder.setBatteryLevel(parser3.getAttributeFloat((String) null, ATTR_BATTERY_LEVEL));
                                builder.setNightMode(parser3.getAttributeBoolean((String) null, ATTR_NIGHT_MODE));
                                builder.setColorTemperature(parser3.getAttributeInt((String) null, ATTR_COLOR_TEMPERATURE));
                                builder.setReduceBrightColors(parser3.getAttributeBoolean((String) null, ATTR_REDUCE_BRIGHT_COLORS));
                                builder.setReduceBrightColorsStrength(parser3.getAttributeInt((String) null, ATTR_REDUCE_BRIGHT_COLORS_STRENGTH));
                                builder.setReduceBrightColorsOffset(parser3.getAttributeFloat((String) null, ATTR_REDUCE_BRIGHT_COLORS_OFFSET));
                                builder.setLastBrightness(parser3.getAttributeFloat((String) null, ATTR_LAST_NITS));
                                String luxValue = parser3.getAttributeValue((String) null, ATTR_LUX);
                                String luxTimestamp = parser3.getAttributeValue((String) null, ATTR_LUX_TIMESTAMPS);
                                String[] luxValuesStrings = luxValue.split(str3);
                                String[] luxTimestampsStrings = luxTimestamp.split(str3);
                                tag = tag3;
                                type2 = type4;
                                if (luxValuesStrings.length != luxTimestampsStrings.length) {
                                    str2 = str3;
                                    parser2 = parser3;
                                    outerDepth2 = outerDepth3;
                                } else {
                                    float[] luxValues = new float[luxValuesStrings.length];
                                    long[] luxTimestamps = new long[luxValuesStrings.length];
                                    outerDepth2 = outerDepth3;
                                    int outerDepth4 = 0;
                                    while (true) {
                                        String uniqueDisplayId2 = uniqueDisplayId;
                                        if (outerDepth4 >= luxValues.length) {
                                            break;
                                        }
                                        luxValues[outerDepth4] = Float.parseFloat(luxValuesStrings[outerDepth4]);
                                        luxTimestamps[outerDepth4] = Long.parseLong(luxTimestampsStrings[outerDepth4]);
                                        outerDepth4++;
                                        uniqueDisplayId = uniqueDisplayId2;
                                    }
                                    builder.setLuxValues(luxValues);
                                    builder.setLuxTimestamps(luxTimestamps);
                                    builder.setIsDefaultBrightnessConfig(parser3.getAttributeBoolean((String) null, ATTR_DEFAULT_CONFIG, false));
                                    builder.setPowerBrightnessFactor(parser3.getAttributeFloat((String) null, ATTR_POWER_SAVE, 1.0f));
                                    builder.setUserBrightnessPoint(parser3.getAttributeBoolean((String) null, ATTR_USER_POINT, false));
                                    long colorSampleDuration = parser3.getAttributeLong((String) null, ATTR_COLOR_SAMPLE_DURATION, -1L);
                                    String colorValueBucketsString = parser3.getAttributeValue((String) null, ATTR_COLOR_VALUE_BUCKETS);
                                    if (colorSampleDuration == -1 || colorValueBucketsString == null) {
                                        str2 = str3;
                                        parser2 = parser3;
                                    } else {
                                        String[] buckets = colorValueBucketsString.split(str3);
                                        long[] bucketValues = new long[buckets.length];
                                        str2 = str3;
                                        int i2 = 0;
                                        while (true) {
                                            parser2 = parser3;
                                            if (i2 >= bucketValues.length) {
                                                break;
                                            }
                                            bucketValues[i2] = Long.parseLong(buckets[i2]);
                                            i2++;
                                            parser3 = parser2;
                                        }
                                        builder.setColorValues(bucketValues, colorSampleDuration);
                                    }
                                    BrightnessChangeEvent event = builder.build();
                                    if (DEBUG) {
                                        Slog.i(TAG, "Read event " + event.brightness + " " + event.packageName);
                                    }
                                    if (event.userId != -1 && event.timeStamp > timeCutOff && event.luxValues.length > 0) {
                                        this.mEvents.append(event);
                                    }
                                }
                            }
                            outerDepth3 = outerDepth2;
                            parser3 = parser2;
                            str3 = str2;
                            i = 1;
                        }
                        outerDepth3 = outerDepth;
                        parser3 = parser;
                        str3 = str;
                        i = 1;
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        } catch (IOException | NullPointerException | NumberFormatException | XmlPullParserException e) {
            this.mEvents = new RingBuffer<>(BrightnessChangeEvent.class, 100);
            Slog.e(TAG, "Failed to parse brightness event", e);
            throw new IOException("failed to parse file", e);
        }
    }

    public void dump(final PrintWriter pw) {
        pw.println("BrightnessTracker state:");
        synchronized (this.mDataCollectionLock) {
            pw.println("  mStarted=" + this.mStarted);
            pw.println("  mLightSensor=" + this.mLightSensor);
            pw.println("  mLastBatteryLevel=" + this.mLastBatteryLevel);
            pw.println("  mLastBrightness=" + this.mLastBrightness);
            pw.println("  mLastSensorReadings.size=" + this.mLastSensorReadings.size());
            if (!this.mLastSensorReadings.isEmpty()) {
                pw.println("  mLastSensorReadings time span " + this.mLastSensorReadings.peekFirst().timestamp + "->" + this.mLastSensorReadings.peekLast().timestamp);
            }
        }
        synchronized (this.mEventsLock) {
            pw.println("  mEventsDirty=" + this.mEventsDirty);
            pw.println("  mEvents.size=" + this.mEvents.size());
            BrightnessChangeEvent[] events = (BrightnessChangeEvent[]) this.mEvents.toArray();
            for (int i = 0; i < events.length; i++) {
                pw.print("    " + FORMAT.format(new Date(events[i].timeStamp)));
                pw.print(", userId=" + events[i].userId);
                pw.print(", " + events[i].lastBrightness + "->" + events[i].brightness);
                pw.print(", isUserSetBrightness=" + events[i].isUserSetBrightness);
                pw.print(", powerBrightnessFactor=" + events[i].powerBrightnessFactor);
                pw.print(", isDefaultBrightnessConfig=" + events[i].isDefaultBrightnessConfig);
                pw.print(" {");
                for (int j = 0; j < events[i].luxValues.length; j++) {
                    if (j != 0) {
                        pw.print(", ");
                    }
                    pw.print("(" + events[i].luxValues[j] + "," + events[i].luxTimestamps[j] + ")");
                }
                pw.println("}");
            }
        }
        pw.println("  mWriteBrightnessTrackerStateScheduled=" + this.mWriteBrightnessTrackerStateScheduled);
        this.mBgHandler.runWithScissors(new Runnable() { // from class: com.android.server.display.BrightnessTracker$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BrightnessTracker.this.m3224lambda$dump$1$comandroidserverdisplayBrightnessTracker(pw);
            }
        }, 1000L);
        if (this.mAmbientBrightnessStatsTracker != null) {
            pw.println();
            this.mAmbientBrightnessStatsTracker.dump(pw);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: dumpLocal */
    public void m3224lambda$dump$1$comandroidserverdisplayBrightnessTracker(PrintWriter pw) {
        pw.println("  mSensorRegistered=" + this.mSensorRegistered);
        pw.println("  mColorSamplingEnabled=" + this.mColorSamplingEnabled);
        pw.println("  mNoFramesToSample=" + this.mNoFramesToSample);
        pw.println("  mFrameRate=" + this.mFrameRate);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableColorSampling() {
        BrightnessConfiguration brightnessConfiguration;
        if (!this.mInjector.isBrightnessModeAutomatic(this.mContentResolver) || !this.mInjector.isInteractive(this.mContext) || this.mColorSamplingEnabled || (brightnessConfiguration = this.mBrightnessConfiguration) == null || !brightnessConfiguration.shouldCollectColorSamples()) {
            return;
        }
        float frameRate = this.mInjector.getFrameRate(this.mContext);
        this.mFrameRate = frameRate;
        if (frameRate <= 0.0f) {
            Slog.wtf(TAG, "Default display has a zero or negative framerate.");
            return;
        }
        this.mNoFramesToSample = (int) (frameRate * ((float) COLOR_SAMPLE_DURATION));
        DisplayedContentSamplingAttributes attributes = this.mInjector.getSamplingAttributes();
        boolean z = DEBUG;
        if (z && attributes != null) {
            Slog.d(TAG, "Color sampling mask=0x" + Integer.toHexString(attributes.getComponentMask()) + " dataSpace=0x" + Integer.toHexString(attributes.getDataspace()) + " pixelFormat=0x" + Integer.toHexString(attributes.getPixelFormat()));
        }
        if (attributes != null && attributes.getPixelFormat() == 55 && (attributes.getComponentMask() & 4) != 0) {
            this.mColorSamplingEnabled = this.mInjector.enableColorSampling(true, this.mNoFramesToSample);
            if (z) {
                Slog.i(TAG, "turning on color sampling for " + this.mNoFramesToSample + " frames, success=" + this.mColorSamplingEnabled);
            }
        }
        if (this.mColorSamplingEnabled && this.mDisplayListener == null) {
            DisplayListener displayListener = new DisplayListener();
            this.mDisplayListener = displayListener;
            this.mInjector.registerDisplayListener(this.mContext, displayListener, this.mBgHandler);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void disableColorSampling() {
        if (!this.mColorSamplingEnabled) {
            return;
        }
        this.mInjector.enableColorSampling(false, 0);
        this.mColorSamplingEnabled = false;
        DisplayListener displayListener = this.mDisplayListener;
        if (displayListener != null) {
            this.mInjector.unRegisterDisplayListener(this.mContext, displayListener);
            this.mDisplayListener = null;
        }
        if (DEBUG) {
            Slog.i(TAG, "turning off color sampling");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateColorSampling() {
        if (!this.mColorSamplingEnabled) {
            return;
        }
        float frameRate = this.mInjector.getFrameRate(this.mContext);
        if (frameRate != this.mFrameRate) {
            disableColorSampling();
            enableColorSampling();
        }
    }

    public ParceledListSlice<AmbientBrightnessDayStats> getAmbientBrightnessStats(int userId) {
        ArrayList<AmbientBrightnessDayStats> stats;
        AmbientBrightnessStatsTracker ambientBrightnessStatsTracker = this.mAmbientBrightnessStatsTracker;
        if (ambientBrightnessStatsTracker != null && (stats = ambientBrightnessStatsTracker.getUserStats(userId)) != null) {
            return new ParceledListSlice<>(stats);
        }
        return ParceledListSlice.emptyList();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class LightData {
        public float lux;
        public long timestamp;

        private LightData() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recordSensorEvent(SensorEvent event) {
        long horizon = this.mInjector.elapsedRealtimeNanos() - LUX_EVENT_HORIZON;
        synchronized (this.mDataCollectionLock) {
            if (DEBUG) {
                Slog.v(TAG, "Sensor event " + event);
            }
            if (this.mLastSensorReadings.isEmpty() || event.timestamp >= this.mLastSensorReadings.getLast().timestamp) {
                LightData data = null;
                while (!this.mLastSensorReadings.isEmpty() && this.mLastSensorReadings.getFirst().timestamp < horizon) {
                    data = this.mLastSensorReadings.removeFirst();
                }
                if (data != null) {
                    this.mLastSensorReadings.addFirst(data);
                }
                LightData data2 = new LightData();
                data2.timestamp = event.timestamp;
                data2.lux = event.values[0];
                this.mLastSensorReadings.addLast(data2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recordAmbientBrightnessStats(SensorEvent event) {
        this.mAmbientBrightnessStatsTracker.add(this.mCurrentUserId, event.values[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void batteryLevelChanged(int level, int scale) {
        synchronized (this.mDataCollectionLock) {
            this.mLastBatteryLevel = level / scale;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SensorListener implements SensorEventListener {
        private SensorListener() {
        }

        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            BrightnessTracker.this.recordSensorEvent(event);
            BrightnessTracker.this.recordAmbientBrightnessStats(event);
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DisplayListener implements DisplayManager.DisplayListener {
        private DisplayListener() {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            if (displayId == 0) {
                BrightnessTracker.this.updateColorSampling();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (BrightnessTracker.DEBUG) {
                Slog.v(BrightnessTracker.TAG, "settings change " + uri);
            }
            if (BrightnessTracker.this.mInjector.isBrightnessModeAutomatic(BrightnessTracker.this.mContentResolver)) {
                BrightnessTracker.this.mBgHandler.obtainMessage(3).sendToTarget();
            } else {
                BrightnessTracker.this.mBgHandler.obtainMessage(2).sendToTarget();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (BrightnessTracker.DEBUG) {
                Slog.d(BrightnessTracker.TAG, "Received " + intent.getAction());
            }
            String action = intent.getAction();
            if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                BrightnessTracker.this.stop();
                BrightnessTracker.this.scheduleWriteBrightnessTrackerState();
            } else if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                int level = intent.getIntExtra("level", -1);
                int scale = intent.getIntExtra("scale", 0);
                if (level != -1 && scale != 0) {
                    BrightnessTracker.this.batteryLevelChanged(level, scale);
                }
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                BrightnessTracker.this.mBgHandler.obtainMessage(2).sendToTarget();
            } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                BrightnessTracker.this.mBgHandler.obtainMessage(3).sendToTarget();
            }
        }
    }

    /* loaded from: classes.dex */
    private final class TrackerHandler extends Handler {
        public TrackerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            boolean z = false;
            switch (msg.what) {
                case 0:
                    BrightnessTracker.this.backgroundStart(((Float) msg.obj).floatValue());
                    return;
                case 1:
                    BrightnessChangeValues values = (BrightnessChangeValues) msg.obj;
                    boolean userInitiatedChange = msg.arg1 == 1;
                    BrightnessTracker.this.handleBrightnessChanged(values.brightness, userInitiatedChange, values.powerBrightnessFactor, values.isUserSetBrightness, values.isDefaultBrightnessConfig, values.timestamp, values.uniqueDisplayId);
                    return;
                case 2:
                    BrightnessTracker.this.stopSensorListener();
                    BrightnessTracker.this.disableColorSampling();
                    return;
                case 3:
                    BrightnessTracker.this.startSensorListener();
                    BrightnessTracker.this.enableColorSampling();
                    return;
                case 4:
                    BrightnessTracker.this.mBrightnessConfiguration = (BrightnessConfiguration) msg.obj;
                    if (BrightnessTracker.this.mBrightnessConfiguration != null && BrightnessTracker.this.mBrightnessConfiguration.shouldCollectColorSamples()) {
                        z = true;
                    }
                    boolean shouldCollectColorSamples = z;
                    if (shouldCollectColorSamples && !BrightnessTracker.this.mColorSamplingEnabled) {
                        BrightnessTracker.this.enableColorSampling();
                        return;
                    } else if (!shouldCollectColorSamples && BrightnessTracker.this.mColorSamplingEnabled) {
                        BrightnessTracker.this.disableColorSampling();
                        return;
                    } else {
                        return;
                    }
                case 5:
                    BrightnessTracker.this.handleSensorChanged((Sensor) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    private static class BrightnessChangeValues {
        public final float brightness;
        public final boolean isDefaultBrightnessConfig;
        public final boolean isUserSetBrightness;
        public final float powerBrightnessFactor;
        public final long timestamp;
        public final String uniqueDisplayId;

        BrightnessChangeValues(float brightness, float powerBrightnessFactor, boolean isUserSetBrightness, boolean isDefaultBrightnessConfig, long timestamp, String uniqueDisplayId) {
            this.brightness = brightness;
            this.powerBrightnessFactor = powerBrightnessFactor;
            this.isUserSetBrightness = isUserSetBrightness;
            this.isDefaultBrightnessConfig = isDefaultBrightnessConfig;
            this.timestamp = timestamp;
            this.uniqueDisplayId = uniqueDisplayId;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        Injector() {
        }

        public void registerSensorListener(Context context, SensorEventListener sensorListener, Sensor lightSensor, Handler handler) {
            SensorManager sensorManager = (SensorManager) context.getSystemService(SensorManager.class);
            sensorManager.registerListener(sensorListener, lightSensor, 3, handler);
        }

        public void unregisterSensorListener(Context context, SensorEventListener sensorListener) {
            SensorManager sensorManager = (SensorManager) context.getSystemService(SensorManager.class);
            sensorManager.unregisterListener(sensorListener);
        }

        public void registerBrightnessModeObserver(ContentResolver resolver, ContentObserver settingsObserver) {
            resolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, settingsObserver, -1);
        }

        public void unregisterBrightnessModeObserver(Context context, ContentObserver settingsObserver) {
            context.getContentResolver().unregisterContentObserver(settingsObserver);
        }

        public void registerReceiver(Context context, BroadcastReceiver receiver, IntentFilter filter) {
            context.registerReceiver(receiver, filter);
        }

        public void unregisterReceiver(Context context, BroadcastReceiver receiver) {
            context.unregisterReceiver(receiver);
        }

        public Handler getBackgroundHandler() {
            return BackgroundThread.getHandler();
        }

        public boolean isBrightnessModeAutomatic(ContentResolver resolver) {
            return Settings.System.getIntForUser(resolver, "screen_brightness_mode", 0, -2) == 1;
        }

        public int getSecureIntForUser(ContentResolver resolver, String setting, int defaultValue, int userId) {
            return Settings.Secure.getIntForUser(resolver, setting, defaultValue, userId);
        }

        public AtomicFile getFile(String filename) {
            return new AtomicFile(new File(Environment.getDataSystemDeDirectory(), filename));
        }

        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        public long elapsedRealtimeNanos() {
            return SystemClock.elapsedRealtimeNanos();
        }

        public int getUserSerialNumber(UserManager userManager, int userId) {
            return userManager.getUserSerialNumber(userId);
        }

        public int getUserId(UserManager userManager, int userSerialNumber) {
            return userManager.getUserHandle(userSerialNumber);
        }

        public int[] getProfileIds(UserManager userManager, int userId) {
            return userManager != null ? userManager.getProfileIds(userId, false) : new int[]{userId};
        }

        public ActivityTaskManager.RootTaskInfo getFocusedStack() throws RemoteException {
            return ActivityTaskManager.getService().getFocusedRootTaskInfo();
        }

        public void scheduleIdleJob(Context context) {
            BrightnessIdleJob.scheduleJob(context);
        }

        public void cancelIdleJob(Context context) {
            BrightnessIdleJob.cancelJob(context);
        }

        public boolean isInteractive(Context context) {
            return ((PowerManager) context.getSystemService(PowerManager.class)).isInteractive();
        }

        public int getNightDisplayColorTemperature(Context context) {
            return ((ColorDisplayManager) context.getSystemService(ColorDisplayManager.class)).getNightDisplayColorTemperature();
        }

        public boolean isNightDisplayActivated(Context context) {
            return ((ColorDisplayManager) context.getSystemService(ColorDisplayManager.class)).isNightDisplayActivated();
        }

        public int getReduceBrightColorsStrength(Context context) {
            return ((ColorDisplayManager) context.getSystemService(ColorDisplayManager.class)).getReduceBrightColorsStrength();
        }

        public float getReduceBrightColorsOffsetFactor(Context context) {
            return ((ColorDisplayManager) context.getSystemService(ColorDisplayManager.class)).getReduceBrightColorsOffsetFactor();
        }

        public boolean isReduceBrightColorsActivated(Context context) {
            return ((ColorDisplayManager) context.getSystemService(ColorDisplayManager.class)).isReduceBrightColorsActivated();
        }

        public DisplayedContentSample sampleColor(int noFramesToSample) {
            DisplayManagerInternal displayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
            return displayManagerInternal.getDisplayedContentSample(0, noFramesToSample, 0L);
        }

        public float getFrameRate(Context context) {
            DisplayManager displayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
            Display display = displayManager.getDisplay(0);
            return display.getRefreshRate();
        }

        public DisplayedContentSamplingAttributes getSamplingAttributes() {
            DisplayManagerInternal displayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
            return displayManagerInternal.getDisplayedContentSamplingAttributes(0);
        }

        public boolean enableColorSampling(boolean enable, int noFrames) {
            DisplayManagerInternal displayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
            return displayManagerInternal.setDisplayedContentSamplingEnabled(0, enable, 4, noFrames);
        }

        public void registerDisplayListener(Context context, DisplayManager.DisplayListener listener, Handler handler) {
            DisplayManager displayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
            displayManager.registerDisplayListener(listener, handler);
        }

        public void unRegisterDisplayListener(Context context, DisplayManager.DisplayListener listener) {
            DisplayManager displayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
            displayManager.unregisterDisplayListener(listener);
        }
    }

    public void adjustSensorListenerWithProximity(boolean disableLightSensor) {
        Handler handler = this.mBgHandler;
        if (handler != null) {
            if (disableLightSensor) {
                handler.obtainMessage(2).sendToTarget();
            } else {
                handler.sendEmptyMessageDelayed(3, 0L);
            }
        }
    }
}
