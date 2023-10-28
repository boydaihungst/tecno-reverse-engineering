package com.android.server.notification;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.CalendarContract;
import android.service.notification.ZenModeConfig;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Objects;
/* loaded from: classes2.dex */
public class CalendarTracker {
    private static final String ATTENDEE_SELECTION = "event_id = ? AND attendeeEmail = ?";
    private static final boolean DEBUG_ATTENDEES = false;
    private static final int EVENT_CHECK_LOOKAHEAD = 86400000;
    private static final String INSTANCE_ORDER_BY = "begin ASC";
    private static final String TAG = "ConditionProviders.CT";
    private Callback mCallback;
    private final ContentObserver mObserver = new ContentObserver(null) { // from class: com.android.server.notification.CalendarTracker.1
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri u) {
            if (CalendarTracker.DEBUG) {
                Log.d(CalendarTracker.TAG, "onChange selfChange=" + selfChange + " uri=" + u + " u=" + CalendarTracker.this.mUserContext.getUserId());
            }
            CalendarTracker.this.mCallback.onChanged();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            if (CalendarTracker.DEBUG) {
                Log.d(CalendarTracker.TAG, "onChange selfChange=" + selfChange);
            }
        }
    };
    private boolean mRegistered;
    private final Context mSystemContext;
    private final Context mUserContext;
    private static final boolean DEBUG = Log.isLoggable("ConditionProviders", 3);
    private static final String[] INSTANCE_PROJECTION = {"begin", "end", "title", "visible", "event_id", "calendar_displayName", "ownerAccount", "calendar_id", "availability"};
    private static final String[] ATTENDEE_PROJECTION = {"event_id", "attendeeEmail", "attendeeStatus"};

    /* loaded from: classes2.dex */
    public interface Callback {
        void onChanged();
    }

    /* loaded from: classes2.dex */
    public static class CheckEventResult {
        public boolean inEvent;
        public long recheckAt;
    }

    public CalendarTracker(Context systemContext, Context userContext) {
        this.mSystemContext = systemContext;
        this.mUserContext = userContext;
    }

    public void setCallback(Callback callback) {
        if (this.mCallback == callback) {
            return;
        }
        this.mCallback = callback;
        setRegistered(callback != null);
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mCallback=");
        pw.println(this.mCallback);
        pw.print(prefix);
        pw.print("mRegistered=");
        pw.println(this.mRegistered);
        pw.print(prefix);
        pw.print("u=");
        pw.println(this.mUserContext.getUserId());
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE] complete} */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x0049, code lost:
        if (r11 == null) goto L11;
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x004e, code lost:
        if (com.android.server.notification.CalendarTracker.DEBUG == false) goto L14;
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0050, code lost:
        android.util.Log.d(com.android.server.notification.CalendarTracker.TAG, "getCalendarsWithAccess took " + (java.lang.System.currentTimeMillis() - r1));
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x006b, code lost:
        return r3;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private ArraySet<Long> getCalendarsWithAccess() {
        long start = System.currentTimeMillis();
        ArraySet<Long> rt = new ArraySet<>();
        String[] projection = {"_id"};
        Cursor cursor = null;
        try {
            try {
                cursor = this.mUserContext.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, projection, "calendar_access_level >= 500 AND sync_events = 1", null, null);
                while (cursor != null) {
                    if (!cursor.moveToNext()) {
                        break;
                    }
                    rt.add(Long.valueOf(cursor.getLong(0)));
                }
            } catch (SQLiteException e) {
                Slog.w(TAG, "error querying calendar content provider", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [172=6, 175=8] */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x0146, code lost:
        if (java.util.Objects.equals(r36.calName, r9) == false) goto L83;
     */
    /* JADX WARN: Removed duplicated region for block: B:112:0x0209  */
    /* JADX WARN: Removed duplicated region for block: B:60:0x015f  */
    /* JADX WARN: Removed duplicated region for block: B:63:0x0165 A[ADDED_TO_REGION] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public CheckEventResult checkEvent(ZenModeConfig.EventInfo filter, long time) {
        CheckEventResult result;
        ArraySet<Long> calendars;
        Uri.Builder uriBuilder;
        Uri uri;
        Cursor cursor;
        String name;
        String owner;
        boolean z;
        boolean meetsCalendar;
        CalendarTracker calendarTracker = this;
        Uri.Builder uriBuilder2 = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uriBuilder2, time);
        ContentUris.appendId(uriBuilder2, time + 86400000);
        Uri uri2 = uriBuilder2.build();
        Cursor cursor2 = null;
        CheckEventResult result2 = new CheckEventResult();
        result2.recheckAt = 86400000 + time;
        try {
            result = result2;
            try {
                cursor2 = calendarTracker.mUserContext.getContentResolver().query(uri2, INSTANCE_PROJECTION, null, null, INSTANCE_ORDER_BY);
                try {
                    ArraySet<Long> calendars2 = getCalendarsWithAccess();
                    while (cursor2 != null) {
                        if (!cursor2.moveToNext()) {
                            break;
                        }
                        long begin = cursor2.getLong(0);
                        long end = cursor2.getLong(1);
                        String title = cursor2.getString(2);
                        boolean calendarVisible = cursor2.getInt(3) == 1;
                        int eventId = cursor2.getInt(4);
                        String name2 = cursor2.getString(5);
                        String owner2 = cursor2.getString(6);
                        long calendarId = cursor2.getLong(7);
                        int availability = cursor2.getInt(8);
                        boolean canAccessCal = calendars2.contains(Long.valueOf(calendarId));
                        boolean z2 = DEBUG;
                        if (z2) {
                            calendars = calendars2;
                            uriBuilder = uriBuilder2;
                            try {
                                Object[] objArr = new Object[10];
                                z = false;
                                objArr[0] = title;
                                uri = uri2;
                                try {
                                    objArr[1] = new Date(begin);
                                    objArr[2] = new Date(end);
                                    objArr[3] = Boolean.valueOf(calendarVisible);
                                    objArr[4] = availabilityToString(availability);
                                    objArr[5] = Integer.valueOf(eventId);
                                    name = name2;
                                    objArr[6] = name;
                                    cursor = cursor2;
                                    owner = owner2;
                                } catch (Exception e) {
                                    e = e;
                                } catch (Throwable th) {
                                    th = th;
                                }
                                try {
                                    objArr[7] = owner;
                                    objArr[8] = Long.valueOf(calendarId);
                                    objArr[9] = Boolean.valueOf(canAccessCal);
                                    Log.d(TAG, String.format("title=%s time=%s-%s vis=%s availability=%s eventId=%s name=%s owner=%s calId=%s canAccessCal=%s", objArr));
                                } catch (Exception e2) {
                                    e = e2;
                                    cursor2 = cursor;
                                    try {
                                        Slog.w(TAG, "error reading calendar", e);
                                        if (cursor2 != null) {
                                            cursor2.close();
                                        }
                                        return result;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        if (cursor2 != null) {
                                            cursor2.close();
                                        }
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    cursor2 = cursor;
                                    if (cursor2 != null) {
                                    }
                                    throw th;
                                }
                            } catch (Exception e3) {
                                e = e3;
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        } else {
                            calendars = calendars2;
                            uriBuilder = uriBuilder2;
                            uri = uri2;
                            cursor = cursor2;
                            name = name2;
                            owner = owner2;
                            z = false;
                        }
                        boolean meetsTime = (time < begin || time >= end) ? z : true;
                        if (calendarVisible && canAccessCal) {
                            if (filter.calName == null && filter.calendarId == null) {
                                meetsCalendar = true;
                                if (availability != 1) {
                                    z = true;
                                }
                                boolean meetsAvailability = z;
                                if (!meetsCalendar && meetsAvailability) {
                                    if (z2) {
                                        Log.d(TAG, "  MEETS CALENDAR & AVAILABILITY");
                                    }
                                    boolean meetsAttendee = calendarTracker.meetsAttendee(filter, eventId, owner);
                                    if (meetsAttendee) {
                                        if (z2) {
                                            Log.d(TAG, "    MEETS ATTENDEE");
                                        }
                                        if (meetsTime) {
                                            if (z2) {
                                                Log.d(TAG, "      MEETS TIME");
                                            }
                                            result.inEvent = true;
                                        }
                                        if (begin > time && begin < result.recheckAt) {
                                            result.recheckAt = begin;
                                        }
                                        if (end > time && end < result.recheckAt) {
                                            result.recheckAt = end;
                                        }
                                    }
                                }
                                calendarTracker = this;
                                cursor2 = cursor;
                                calendars2 = calendars;
                                uriBuilder2 = uriBuilder;
                                uri2 = uri;
                            }
                            if (!Objects.equals(filter.calendarId, Long.valueOf(calendarId))) {
                            }
                            meetsCalendar = true;
                            if (availability != 1) {
                            }
                            boolean meetsAvailability2 = z;
                            if (!meetsCalendar) {
                            }
                            calendarTracker = this;
                            cursor2 = cursor;
                            calendars2 = calendars;
                            uriBuilder2 = uriBuilder;
                            uri2 = uri;
                        }
                        meetsCalendar = z;
                        if (availability != 1) {
                        }
                        boolean meetsAvailability22 = z;
                        if (!meetsCalendar) {
                        }
                        calendarTracker = this;
                        cursor2 = cursor;
                        calendars2 = calendars;
                        uriBuilder2 = uriBuilder;
                        uri2 = uri;
                    }
                    Cursor cursor3 = cursor2;
                    if (cursor3 != null) {
                        cursor3.close();
                    }
                } catch (Exception e4) {
                    e = e4;
                } catch (Throwable th5) {
                    th = th5;
                }
            } catch (Exception e5) {
                e = e5;
            } catch (Throwable th6) {
                th = th6;
            }
        } catch (Exception e6) {
            e = e6;
            result = result2;
        } catch (Throwable th7) {
            th = th7;
        }
        return result;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [213=4, 217=7, 218=4, 220=4] */
    private boolean meetsAttendee(ZenModeConfig.EventInfo filter, int eventId, String email) {
        String[] selectionArgs;
        String selection;
        long start = System.currentTimeMillis();
        String selection2 = ATTENDEE_SELECTION;
        int i = 2;
        int i2 = 0;
        int i3 = 1;
        String[] selectionArgs2 = {Integer.toString(eventId), email};
        Cursor cursor = null;
        try {
            try {
                cursor = this.mUserContext.getContentResolver().query(CalendarContract.Attendees.CONTENT_URI, ATTENDEE_PROJECTION, ATTENDEE_SELECTION, selectionArgs2, null);
                try {
                    if (cursor != null && cursor.getCount() != 0) {
                        boolean rt = false;
                        while (cursor != null) {
                            if (!cursor.moveToNext()) {
                                break;
                            }
                            long rowEventId = cursor.getLong(i2);
                            String rowEmail = cursor.getString(i3);
                            int status = cursor.getInt(i);
                            boolean meetsReply = meetsReply(filter.reply, status);
                            if (DEBUG) {
                                selectionArgs = selectionArgs2;
                                try {
                                    selection = selection2;
                                } catch (SQLiteException e) {
                                    e = e;
                                } catch (Throwable th) {
                                    e = th;
                                }
                                try {
                                    Log.d(TAG, "" + String.format("status=%s, meetsReply=%s", attendeeStatusToString(status), Boolean.valueOf(meetsReply)));
                                } catch (SQLiteException e2) {
                                    e = e2;
                                    Slog.w(TAG, "error querying attendees content provider", e);
                                    if (cursor != null) {
                                        cursor.close();
                                    }
                                    if (DEBUG) {
                                        Log.d(TAG, "meetsAttendee took " + (System.currentTimeMillis() - start));
                                        return false;
                                    }
                                    return false;
                                } catch (Throwable th2) {
                                    e = th2;
                                    if (cursor != null) {
                                        cursor.close();
                                    }
                                    if (DEBUG) {
                                        Log.d(TAG, "meetsAttendee took " + (System.currentTimeMillis() - start));
                                    }
                                    throw e;
                                }
                            } else {
                                selectionArgs = selectionArgs2;
                                selection = selection2;
                            }
                            boolean eventMeets = rowEventId == ((long) eventId) && Objects.equals(rowEmail, email) && meetsReply;
                            rt |= eventMeets;
                            selectionArgs2 = selectionArgs;
                            selection2 = selection;
                            i = 2;
                            i2 = 0;
                            i3 = 1;
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                        if (DEBUG) {
                            Log.d(TAG, "meetsAttendee took " + (System.currentTimeMillis() - start));
                        }
                        return rt;
                    }
                    boolean z = DEBUG;
                    if (z) {
                        Log.d(TAG, "No attendees found");
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (z) {
                        Log.d(TAG, "meetsAttendee took " + (System.currentTimeMillis() - start));
                        return true;
                    }
                    return true;
                } catch (SQLiteException e3) {
                    e = e3;
                }
            } catch (Throwable th3) {
                e = th3;
            }
        } catch (SQLiteException e4) {
            e = e4;
        } catch (Throwable th4) {
            e = th4;
        }
    }

    private void setRegistered(boolean registered) {
        if (this.mRegistered == registered) {
            return;
        }
        ContentResolver cr = this.mSystemContext.getContentResolver();
        int userId = this.mUserContext.getUserId();
        if (this.mRegistered) {
            if (DEBUG) {
                Log.d(TAG, "unregister content observer u=" + userId);
            }
            cr.unregisterContentObserver(this.mObserver);
        }
        this.mRegistered = registered;
        boolean z = DEBUG;
        if (z) {
            Log.d(TAG, "mRegistered = " + registered + " u=" + userId);
        }
        if (this.mRegistered) {
            if (z) {
                Log.d(TAG, "register content observer u=" + userId);
            }
            cr.registerContentObserver(CalendarContract.Instances.CONTENT_URI, true, this.mObserver, userId);
            cr.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, this.mObserver, userId);
            cr.registerContentObserver(CalendarContract.Calendars.CONTENT_URI, true, this.mObserver, userId);
        }
    }

    private static String attendeeStatusToString(int status) {
        switch (status) {
            case 0:
                return "ATTENDEE_STATUS_NONE";
            case 1:
                return "ATTENDEE_STATUS_ACCEPTED";
            case 2:
                return "ATTENDEE_STATUS_DECLINED";
            case 3:
                return "ATTENDEE_STATUS_INVITED";
            case 4:
                return "ATTENDEE_STATUS_TENTATIVE";
            default:
                return "ATTENDEE_STATUS_UNKNOWN_" + status;
        }
    }

    private static String availabilityToString(int availability) {
        switch (availability) {
            case 0:
                return "AVAILABILITY_BUSY";
            case 1:
                return "AVAILABILITY_FREE";
            case 2:
                return "AVAILABILITY_TENTATIVE";
            default:
                return "AVAILABILITY_UNKNOWN_" + availability;
        }
    }

    private static boolean meetsReply(int reply, int attendeeStatus) {
        switch (reply) {
            case 0:
                return attendeeStatus != 2;
            case 1:
                return attendeeStatus == 1 || attendeeStatus == 4;
            case 2:
                return attendeeStatus == 1;
            default:
                return false;
        }
    }
}
