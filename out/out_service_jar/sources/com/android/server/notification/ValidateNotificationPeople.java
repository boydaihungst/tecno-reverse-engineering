package com.android.server.notification;

import android.app.Person;
import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.LruCache;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import libcore.util.EmptyArray;
/* loaded from: classes2.dex */
public class ValidateNotificationPeople implements NotificationSignalExtractor {
    private static final boolean ENABLE_PEOPLE_VALIDATOR = true;
    private static final int MAX_PEOPLE = 10;
    static final float NONE = 0.0f;
    private static final int PEOPLE_CACHE_SIZE = 200;
    private static final String SETTING_ENABLE_PEOPLE_VALIDATOR = "validate_notification_people_enabled";
    static final float STARRED_CONTACT = 1.0f;
    static final float VALID_CONTACT = 0.5f;
    private Context mBaseContext;
    protected boolean mEnabled;
    private int mEvictionCount;
    private Handler mHandler;
    private ContentObserver mObserver;
    private LruCache<String, LookupResult> mPeopleCache;
    private NotificationUsageStats mUsageStats;
    private Map<Integer, Context> mUserToContextMap;
    private static final String TAG = "ValidateNoPeople";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String[] LOOKUP_PROJECTION = {"_id", "lookup", "starred", "has_phone_number"};
    static final String[] PHONE_LOOKUP_PROJECTION = {"data4", "data1"};

    @Override // com.android.server.notification.NotificationSignalExtractor
    public void initialize(Context context, NotificationUsageStats usageStats) {
        if (DEBUG) {
            Slog.d(TAG, "Initializing  " + getClass().getSimpleName() + ".");
        }
        this.mUserToContextMap = new ArrayMap();
        this.mBaseContext = context;
        this.mUsageStats = usageStats;
        this.mPeopleCache = new LruCache<>(200);
        boolean z = 1 == Settings.Global.getInt(this.mBaseContext.getContentResolver(), SETTING_ENABLE_PEOPLE_VALIDATOR, 1);
        this.mEnabled = z;
        if (z) {
            this.mHandler = new Handler();
            this.mObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.notification.ValidateNotificationPeople.1
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange, Uri uri, int userId) {
                    super.onChange(selfChange, uri, userId);
                    if ((ValidateNotificationPeople.DEBUG || ValidateNotificationPeople.this.mEvictionCount % 100 == 0) && ValidateNotificationPeople.VERBOSE) {
                        Slog.i(ValidateNotificationPeople.TAG, "mEvictionCount: " + ValidateNotificationPeople.this.mEvictionCount);
                    }
                    ValidateNotificationPeople.this.mPeopleCache.evictAll();
                    ValidateNotificationPeople.this.mEvictionCount++;
                }
            };
            this.mBaseContext.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, this.mObserver, -1);
        }
    }

    @Override // com.android.server.notification.NotificationSignalExtractor
    public RankingReconsideration process(NotificationRecord record) {
        if (!this.mEnabled) {
            if (VERBOSE) {
                Slog.i(TAG, ServiceConfigAccessor.PROVIDER_MODE_DISABLED);
            }
            return null;
        } else if (record == null || record.getNotification() == null) {
            if (VERBOSE) {
                Slog.i(TAG, "skipping empty notification");
            }
            return null;
        } else if (record.getUserId() == -1) {
            if (VERBOSE) {
                Slog.i(TAG, "skipping global notification");
            }
            return null;
        } else {
            Context context = getContextAsUser(record.getUser());
            if (context == null) {
                if (VERBOSE) {
                    Slog.i(TAG, "skipping notification that lacks a context");
                }
                return null;
            }
            return validatePeople(context, record);
        }
    }

    @Override // com.android.server.notification.NotificationSignalExtractor
    public void setConfig(RankingConfig config) {
    }

    @Override // com.android.server.notification.NotificationSignalExtractor
    public void setZenHelper(ZenModeHelper helper) {
    }

    public float getContactAffinity(UserHandle userHandle, Bundle extras, int timeoutMs, float timeoutAffinity) {
        if (DEBUG) {
            Slog.d(TAG, "checking affinity for " + userHandle);
        }
        if (extras == null) {
            return NONE;
        }
        String key = Long.toString(System.nanoTime());
        float[] affinityOut = new float[1];
        Context context = getContextAsUser(userHandle);
        if (context == null) {
            return NONE;
        }
        final PeopleRankingReconsideration prr = validatePeople(context, key, extras, null, affinityOut);
        float affinity = affinityOut[0];
        if (prr != null) {
            final Semaphore s = new Semaphore(0);
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() { // from class: com.android.server.notification.ValidateNotificationPeople.2
                @Override // java.lang.Runnable
                public void run() {
                    prr.work();
                    s.release();
                }
            });
            try {
                if (!s.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                    Slog.w(TAG, "Timeout while waiting for affinity: " + key + ". Returning timeoutAffinity=" + timeoutAffinity);
                    return timeoutAffinity;
                }
                return Math.max(prr.getContactAffinity(), affinity);
            } catch (InterruptedException e) {
                Slog.w(TAG, "InterruptedException while waiting for affinity: " + key + ". Returning affinity=" + affinity, e);
                return affinity;
            }
        }
        return affinity;
    }

    private Context getContextAsUser(UserHandle userHandle) {
        Context context = this.mUserToContextMap.get(Integer.valueOf(userHandle.getIdentifier()));
        if (context == null) {
            try {
                context = this.mBaseContext.createPackageContextAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, userHandle);
                this.mUserToContextMap.put(Integer.valueOf(userHandle.getIdentifier()), context);
                return context;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "failed to create package context for lookups", e);
                return context;
            }
        }
        return context;
    }

    private RankingReconsideration validatePeople(Context context, NotificationRecord record) {
        boolean z;
        String key = record.getKey();
        Bundle extras = record.getNotification().extras;
        float[] affinityOut = new float[1];
        PeopleRankingReconsideration rr = validatePeople(context, key, extras, record.getPeopleOverride(), affinityOut);
        boolean z2 = false;
        float affinity = affinityOut[0];
        record.setContactAffinity(affinity);
        if (rr == null) {
            NotificationUsageStats notificationUsageStats = this.mUsageStats;
            if (affinity > NONE) {
                z = true;
            } else {
                z = false;
            }
            if (affinity == 1.0f) {
                z2 = true;
            }
            notificationUsageStats.registerPeopleAffinity(record, z, z2, true);
        } else {
            rr.setRecord(record);
        }
        return rr;
    }

    /* JADX WARN: Removed duplicated region for block: B:29:0x0097 A[Catch: all -> 0x00ab, TryCatch #0 {, blocks: (B:19:0x006c, B:21:0x007e, B:24:0x0085, B:26:0x0089, B:29:0x0097, B:30:0x00a0, B:31:0x00a1, B:27:0x0092), top: B:50:0x006c }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private PeopleRankingReconsideration validatePeople(Context context, String key, Bundle extras, List<String> peopleOverride, float[] affinityOut) {
        float affinity;
        if (extras == null) {
            return null;
        }
        Set<String> people = new ArraySet<>(peopleOverride);
        String[] notificationPeople = getExtraPeople(extras);
        if (notificationPeople != null) {
            people.addAll(Arrays.asList(notificationPeople));
        }
        if (VERBOSE) {
            Slog.i(TAG, "Validating: " + key + " for " + context.getUserId());
        }
        LinkedList<String> pendingLookups = new LinkedList<>();
        Iterator<String> it = people.iterator();
        int personIdx = 0;
        float affinity2 = 0.0f;
        while (true) {
            if (!it.hasNext()) {
                affinity = affinity2;
                break;
            }
            String handle = it.next();
            if (!TextUtils.isEmpty(handle)) {
                synchronized (this.mPeopleCache) {
                    String cacheKey = getCacheKey(context.getUserId(), handle);
                    LookupResult lookupResult = this.mPeopleCache.get(cacheKey);
                    if (lookupResult != null && !lookupResult.isExpired()) {
                        if (DEBUG) {
                            Slog.d(TAG, "using cached lookupResult");
                        }
                        if (lookupResult != null) {
                            affinity2 = Math.max(affinity2, lookupResult.getAffinity());
                        }
                    }
                    pendingLookups.add(handle);
                    if (lookupResult != null) {
                    }
                }
                personIdx++;
                if (personIdx == 10) {
                    affinity = affinity2;
                    break;
                }
            }
        }
        affinityOut[0] = affinity;
        if (pendingLookups.isEmpty()) {
            if (VERBOSE) {
                Slog.i(TAG, "final affinity: " + affinity);
            }
            return null;
        }
        if (DEBUG) {
            Slog.d(TAG, "Pending: future work scheduled for: " + key);
        }
        return new PeopleRankingReconsideration(context, key, pendingLookups);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getCacheKey(int userId, String handle) {
        return Integer.toString(userId) + ":" + handle;
    }

    public static String[] getExtraPeople(Bundle extras) {
        String[] peopleList = getExtraPeopleForKey(extras, "android.people.list");
        String[] legacyPeople = getExtraPeopleForKey(extras, "android.people");
        return combineLists(legacyPeople, peopleList);
    }

    private static String[] combineLists(String[] first, String[] second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        ArraySet<String> people = new ArraySet<>(first.length + second.length);
        for (String person : first) {
            people.add(person);
        }
        for (String person2 : second) {
            people.add(person2);
        }
        return (String[]) people.toArray(EmptyArray.STRING);
    }

    private static String[] getExtraPeopleForKey(Bundle extras, String key) {
        Object people = extras.get(key);
        if (people instanceof String[]) {
            return (String[]) people;
        }
        if (people instanceof ArrayList) {
            ArrayList arrayList = (ArrayList) people;
            if (arrayList.isEmpty()) {
                return null;
            }
            if (arrayList.get(0) instanceof String) {
                return (String[]) arrayList.toArray(new String[arrayList.size()]);
            }
            if (arrayList.get(0) instanceof CharSequence) {
                int N = arrayList.size();
                String[] array = new String[N];
                for (int i = 0; i < N; i++) {
                    array[i] = ((CharSequence) arrayList.get(i)).toString();
                }
                return array;
            } else if (arrayList.get(0) instanceof Person) {
                int N2 = arrayList.size();
                String[] array2 = new String[N2];
                for (int i2 = 0; i2 < N2; i2++) {
                    array2[i2] = ((Person) arrayList.get(i2)).resolveToLegacyUri();
                }
                return array2;
            } else {
                return null;
            }
        } else if (people instanceof String) {
            return new String[]{(String) people};
        } else {
            if (people instanceof char[]) {
                return new String[]{new String((char[]) people)};
            }
            if (people instanceof CharSequence) {
                return new String[]{((CharSequence) people).toString()};
            }
            if (people instanceof CharSequence[]) {
                CharSequence[] charSeqArray = (CharSequence[]) people;
                int N3 = charSeqArray.length;
                String[] array3 = new String[N3];
                for (int i3 = 0; i3 < N3; i3++) {
                    array3[i3] = charSeqArray[i3].toString();
                }
                return array3;
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LookupResult resolvePhoneContact(Context context, String number) {
        Uri phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        return searchContacts(context, phoneUri);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LookupResult resolveEmailContact(Context context, String email) {
        Uri numberUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI, Uri.encode(email));
        return searchContacts(context, numberUri);
    }

    LookupResult searchContacts(Context context, Uri lookupUri) {
        LookupResult lookupResult = new LookupResult();
        Uri corpLookupUri = ContactsContract.Contacts.createCorpLookupUriFromEnterpriseLookupUri(lookupUri);
        if (corpLookupUri == null) {
            addContacts(lookupResult, context, lookupUri);
        } else {
            addWorkContacts(lookupResult, context, corpLookupUri);
        }
        return lookupResult;
    }

    LookupResult searchContactsAndLookupNumbers(Context context, Uri lookupUri) {
        LookupResult lookupResult = searchContacts(context, lookupUri);
        String phoneLookupKey = lookupResult.getPhoneLookupKey();
        if (phoneLookupKey != null) {
            String[] selectionArgs = {phoneLookupKey};
            try {
                Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PHONE_LOOKUP_PROJECTION, "lookup = ?", selectionArgs, null);
                if (cursor == null) {
                    Slog.w(TAG, "Cursor is null when querying contact phone number.");
                    if (cursor != null) {
                        cursor.close();
                    }
                    return lookupResult;
                }
                while (cursor.moveToNext()) {
                    lookupResult.mergePhoneNumber(cursor);
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable t) {
                Slog.w(TAG, "Problem getting content resolver or querying phone numbers.", t);
            }
        }
        return lookupResult;
    }

    private void addWorkContacts(LookupResult lookupResult, Context context, Uri corpLookupUri) {
        int workUserId = findWorkUserId(context);
        if (workUserId == -1) {
            Slog.w(TAG, "Work profile user ID not found for work contact: " + corpLookupUri);
            return;
        }
        Uri corpLookupUriWithUserId = ContentProvider.maybeAddUserId(corpLookupUri, workUserId);
        addContacts(lookupResult, context, corpLookupUriWithUserId);
    }

    private int findWorkUserId(Context context) {
        UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
        int[] profileIds = userManager.getProfileIds(context.getUserId(), true);
        for (int profileId : profileIds) {
            if (userManager.isManagedProfile(profileId)) {
                return profileId;
            }
        }
        return -1;
    }

    private void addContacts(LookupResult lookupResult, Context context, Uri uri) {
        try {
            Cursor c = context.getContentResolver().query(uri, LOOKUP_PROJECTION, null, null, null);
            if (c == null) {
                Slog.w(TAG, "Null cursor from contacts query.");
                if (c != null) {
                    c.close();
                    return;
                }
                return;
            }
            while (c.moveToNext()) {
                lookupResult.mergeContact(c);
            }
            if (c != null) {
                c.close();
            }
        } catch (Throwable t) {
            Slog.w(TAG, "Problem getting content resolver or performing contacts query.", t);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class LookupResult {
        private static final long CONTACT_REFRESH_MILLIS = 3600000;
        private float mAffinity = ValidateNotificationPeople.NONE;
        private boolean mHasPhone = false;
        private String mPhoneLookupKey = null;
        private ArraySet<String> mPhoneNumbers = new ArraySet<>();
        private final long mExpireMillis = System.currentTimeMillis() + 3600000;

        public void mergeContact(Cursor cursor) {
            this.mAffinity = Math.max(this.mAffinity, 0.5f);
            int idIdx = cursor.getColumnIndex("_id");
            if (idIdx < 0) {
                Slog.i(ValidateNotificationPeople.TAG, "invalid cursor: no _ID");
            } else {
                int id = cursor.getInt(idIdx);
                if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "contact _ID is: " + id);
                }
            }
            int lookupKeyIdx = cursor.getColumnIndex("lookup");
            if (lookupKeyIdx >= 0) {
                this.mPhoneLookupKey = cursor.getString(lookupKeyIdx);
                if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "contact LOOKUP_KEY is: " + this.mPhoneLookupKey);
                }
            } else if (ValidateNotificationPeople.DEBUG) {
                Slog.d(ValidateNotificationPeople.TAG, "invalid cursor: no LOOKUP_KEY");
            }
            int starIdx = cursor.getColumnIndex("starred");
            if (starIdx >= 0) {
                boolean isStarred = cursor.getInt(starIdx) != 0;
                if (isStarred) {
                    this.mAffinity = Math.max(this.mAffinity, 1.0f);
                }
                if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "contact STARRED is: " + isStarred);
                }
            } else if (ValidateNotificationPeople.DEBUG) {
                Slog.d(ValidateNotificationPeople.TAG, "invalid cursor: no STARRED");
            }
            int hasPhoneIdx = cursor.getColumnIndex("has_phone_number");
            if (hasPhoneIdx >= 0) {
                this.mHasPhone = cursor.getInt(hasPhoneIdx) != 0;
                if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "contact HAS_PHONE_NUMBER is: " + this.mHasPhone);
                }
            } else if (ValidateNotificationPeople.DEBUG) {
                Slog.d(ValidateNotificationPeople.TAG, "invalid cursor: no HAS_PHONE_NUMBER");
            }
        }

        public String getPhoneLookupKey() {
            if (!this.mHasPhone) {
                return null;
            }
            return this.mPhoneLookupKey;
        }

        public void mergePhoneNumber(Cursor cursor) {
            int normalizedNumIdx = cursor.getColumnIndex("data4");
            if (normalizedNumIdx >= 0) {
                this.mPhoneNumbers.add(cursor.getString(normalizedNumIdx));
            } else if (ValidateNotificationPeople.DEBUG) {
                Slog.d(ValidateNotificationPeople.TAG, "cursor data not found: no NORMALIZED_NUMBER");
            }
            int numIdx = cursor.getColumnIndex("data1");
            if (numIdx >= 0) {
                this.mPhoneNumbers.add(cursor.getString(numIdx));
            } else if (ValidateNotificationPeople.DEBUG) {
                Slog.d(ValidateNotificationPeople.TAG, "cursor data not found: no NUMBER");
            }
        }

        public ArraySet<String> getPhoneNumbers() {
            return this.mPhoneNumbers;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isExpired() {
            return this.mExpireMillis < System.currentTimeMillis();
        }

        private boolean isInvalid() {
            return this.mAffinity == ValidateNotificationPeople.NONE || isExpired();
        }

        public float getAffinity() {
            if (isInvalid()) {
                return ValidateNotificationPeople.NONE;
            }
            return this.mAffinity;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class PeopleRankingReconsideration extends RankingReconsideration {
        private static final long LOOKUP_TIME = 1000;
        private float mContactAffinity;
        private final Context mContext;
        private final LinkedList<String> mPendingLookups;
        private ArraySet<String> mPhoneNumbers;
        private NotificationRecord mRecord;

        private PeopleRankingReconsideration(Context context, String key, LinkedList<String> pendingLookups) {
            super(key, 1000L);
            this.mContactAffinity = ValidateNotificationPeople.NONE;
            this.mPhoneNumbers = null;
            this.mContext = context;
            this.mPendingLookups = pendingLookups;
        }

        @Override // com.android.server.notification.RankingReconsideration
        public void work() {
            LookupResult lookupResult;
            if (ValidateNotificationPeople.VERBOSE) {
                Slog.i(ValidateNotificationPeople.TAG, "Executing: validation for: " + this.mKey);
            }
            long timeStartMs = System.currentTimeMillis();
            Iterator<String> it = this.mPendingLookups.iterator();
            while (it.hasNext()) {
                String handle = it.next();
                String cacheKey = ValidateNotificationPeople.this.getCacheKey(this.mContext.getUserId(), handle);
                boolean cacheHit = false;
                synchronized (ValidateNotificationPeople.this.mPeopleCache) {
                    lookupResult = (LookupResult) ValidateNotificationPeople.this.mPeopleCache.get(cacheKey);
                    if (lookupResult != null && !lookupResult.isExpired()) {
                        cacheHit = true;
                    }
                }
                if (!cacheHit) {
                    Uri uri = Uri.parse(handle);
                    if ("tel".equals(uri.getScheme())) {
                        if (ValidateNotificationPeople.DEBUG) {
                            Slog.d(ValidateNotificationPeople.TAG, "checking telephone URI: " + handle);
                        }
                        lookupResult = ValidateNotificationPeople.this.resolvePhoneContact(this.mContext, uri.getSchemeSpecificPart());
                    } else if ("mailto".equals(uri.getScheme())) {
                        if (ValidateNotificationPeople.DEBUG) {
                            Slog.d(ValidateNotificationPeople.TAG, "checking mailto URI: " + handle);
                        }
                        lookupResult = ValidateNotificationPeople.this.resolveEmailContact(this.mContext, uri.getSchemeSpecificPart());
                    } else if (handle.startsWith(ContactsContract.Contacts.CONTENT_LOOKUP_URI.toString())) {
                        if (ValidateNotificationPeople.DEBUG) {
                            Slog.d(ValidateNotificationPeople.TAG, "checking lookup URI: " + handle);
                        }
                        lookupResult = ValidateNotificationPeople.this.searchContactsAndLookupNumbers(this.mContext, uri);
                    } else {
                        lookupResult = new LookupResult();
                        if (!"name".equals(uri.getScheme())) {
                            Slog.w(ValidateNotificationPeople.TAG, "unsupported URI " + handle);
                        }
                    }
                }
                if (lookupResult != null) {
                    if (!cacheHit) {
                        synchronized (ValidateNotificationPeople.this.mPeopleCache) {
                            ValidateNotificationPeople.this.mPeopleCache.put(cacheKey, lookupResult);
                        }
                    }
                    if (ValidateNotificationPeople.DEBUG) {
                        Slog.d(ValidateNotificationPeople.TAG, "lookup contactAffinity is " + lookupResult.getAffinity());
                    }
                    this.mContactAffinity = Math.max(this.mContactAffinity, lookupResult.getAffinity());
                    if (lookupResult.getPhoneNumbers() != null) {
                        if (this.mPhoneNumbers == null) {
                            this.mPhoneNumbers = new ArraySet<>();
                        }
                        this.mPhoneNumbers.addAll((ArraySet<? extends String>) lookupResult.getPhoneNumbers());
                    }
                } else if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "lookupResult is null");
                }
            }
            if (ValidateNotificationPeople.DEBUG) {
                Slog.d(ValidateNotificationPeople.TAG, "Validation finished in " + (System.currentTimeMillis() - timeStartMs) + "ms");
            }
            if (this.mRecord != null) {
                NotificationUsageStats notificationUsageStats = ValidateNotificationPeople.this.mUsageStats;
                NotificationRecord notificationRecord = this.mRecord;
                float f = this.mContactAffinity;
                notificationUsageStats.registerPeopleAffinity(notificationRecord, f > ValidateNotificationPeople.NONE, f == 1.0f, false);
            }
        }

        @Override // com.android.server.notification.RankingReconsideration
        public void applyChangesLocked(NotificationRecord operand) {
            float affinityBound = operand.getContactAffinity();
            operand.setContactAffinity(Math.max(this.mContactAffinity, affinityBound));
            if (ValidateNotificationPeople.VERBOSE) {
                Slog.i(ValidateNotificationPeople.TAG, "final affinity: " + operand.getContactAffinity());
            }
            operand.mergePhoneNumbers(this.mPhoneNumbers);
        }

        public float getContactAffinity() {
            return this.mContactAffinity;
        }

        public void setRecord(NotificationRecord record) {
            this.mRecord = record;
        }
    }
}
