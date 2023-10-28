package android.media;

import android.annotation.SystemApi;
import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.StaleDataException;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.media.VolumeShaper;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.database.SortCursor;
import com.google.android.mms.ContentType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/* loaded from: classes2.dex */
public class RingtoneManager {
    public static final String ACTION_RINGTONE_PICKER = "android.intent.action.RINGTONE_PICKER";
    public static final String EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS = "android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS";
    public static final String EXTRA_RINGTONE_DEFAULT_URI = "android.intent.extra.ringtone.DEFAULT_URI";
    public static final String EXTRA_RINGTONE_EXISTING_URI = "android.intent.extra.ringtone.EXISTING_URI";
    @Deprecated
    public static final String EXTRA_RINGTONE_INCLUDE_DRM = "android.intent.extra.ringtone.INCLUDE_DRM";
    public static final String EXTRA_RINGTONE_PICKED_URI = "android.intent.extra.ringtone.PICKED_URI";
    public static final String EXTRA_RINGTONE_SHOW_DEFAULT = "android.intent.extra.ringtone.SHOW_DEFAULT";
    public static final String EXTRA_RINGTONE_SHOW_SILENT = "android.intent.extra.ringtone.SHOW_SILENT";
    public static final String EXTRA_RINGTONE_TITLE = "android.intent.extra.ringtone.TITLE";
    public static final String EXTRA_RINGTONE_TYPE = "android.intent.extra.ringtone.TYPE";
    private static final String GET_COUNTRY_NULL = "get_country_null";
    public static final int ID_COLUMN_INDEX = 0;
    private static final String[] INTERNAL_COLUMNS;
    private static final String[] MEDIA_COLUMNS;
    private static final String NO_INIT = "no_init";
    private static final boolean RingtoneManagerDebugTAG;
    private static final String TAG = "RingtoneManager";
    public static final int TITLE_COLUMN_INDEX = 1;
    public static final int TYPE_ALARM = 4;
    public static final int TYPE_ALL = 7;
    public static final int TYPE_NOTIFICATION = 2;
    public static final int TYPE_NOTIFICATION2 = 16;
    public static final int TYPE_RINGTONE = 1;
    public static final int TYPE_RINGTONE2 = 32;
    public static final int URI_COLUMN_INDEX = 2;
    private static final HashMap<String, String> mCountryDefaultNotificationMap;
    private static final HashMap<String, String> mCountryDefaultRingtoneMap;
    private final List<String> DZ_KEPT_RINGTONES_LIST;
    private final List<String> DZ_KEPT_RINGTONES_LIST_S_OTA;
    private final List<String> NOTIFICATION2;
    private final Activity mActivity;
    private final Context mContext;
    private Cursor mCursor;
    private final List<String> mFilterColumns;
    private boolean mIncludeParentRingtones;
    private Ringtone mPreviousRingtone;
    private boolean mStopPreviousRingtone;
    private int mType;

    static {
        RingtoneManagerDebugTAG = "1".equals(SystemProperties.get("persist.user.root.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS)) || Build.TYPE.equals("userdebug") || "1".equals(SystemProperties.get("persist.sys.fans.support", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS));
        INTERNAL_COLUMNS = new String[]{"_id", "title", "title", "title_key"};
        MEDIA_COLUMNS = new String[]{"_id", "title", "title", "title_key"};
        mCountryDefaultRingtoneMap = new HashMap<>();
        mCountryDefaultNotificationMap = new HashMap<>();
    }

    public RingtoneManager(Activity activity) {
        this(activity, false);
    }

    public RingtoneManager(Activity activity, boolean includeParentRingtones) {
        this.mType = 1;
        this.mFilterColumns = new ArrayList();
        this.mStopPreviousRingtone = true;
        this.DZ_KEPT_RINGTONES_LIST = new ArrayList<String>() { // from class: android.media.RingtoneManager.1
            {
                add("'Airship.ogg'");
                add("'Crystal_Clear.ogg'");
                add("'Lating.ogg'");
                add("'Peripatetic.ogg'");
                add("'Pluto.ogg'");
                add("'Quantum.ogg'");
                add("'Reggae.ogg'");
                add("'Your_Eyes.ogg'");
                add("'Suling_Flute.ogg'");
                add("'We_Can.ogg'");
                add("'Cartoon.ogg'");
                add("'Dripping.ogg'");
                add("'Drop.ogg'");
                add("'Gentle.ogg'");
                add("'Zero.ogg'");
                add("'Alarm.ogg'");
                add("'Beep.ogg'");
                add("'Brahms.ogg'");
                add("'Butterfly.ogg'");
                add("'Confident.ogg'");
                add("'Dragon.ogg'");
                add("'Golden_Sea.ogg'");
                add("'Good_Times.ogg'");
                add("'Grow.ogg'");
                add("'Hasty.ogg'");
                add("'Love_of_The_Sea.ogg'");
                add("'Magic_Sky.ogg'");
                add("'Meditation.ogg'");
                add("'Meet_Love_at_The_Corner.ogg'");
                add("'A_Mellow_Pulse.ogg'");
                add("'Miss_You.ogg'");
                add("'Moring.ogg'");
                add("'Peripatetic.ogg'");
                add("'Piano.ogg'");
                add("'Radar.ogg'");
                add("'String_Of_Pearls.ogg'");
                add("'Sunrise.ogg'");
                add("'Warm_Spring.ogg'");
                add("'Yesterday.ogg'");
            }
        };
        this.DZ_KEPT_RINGTONES_LIST_S_OTA = new ArrayList<String>() { // from class: android.media.RingtoneManager.2
            {
                add("'Airship.ogg'");
                add("'Crystal_Clear.ogg'");
                add("'Lating.ogg'");
                add("'Peripatetic.ogg'");
                add("'Pluto.ogg'");
                add("'Quantum.ogg'");
                add("'Reggae.ogg'");
                add("'Your_Eyes.ogg'");
                add("'We_Can.ogg'");
                add("'ShiningMoment.ogg'");
                add("'Cartoon.ogg'");
                add("'Dripping.ogg'");
                add("'Drop.ogg'");
                add("'Gentle.ogg'");
                add("'Woodblock.ogg'");
                add("'A_Mellow_Pulse.ogg'");
                add("'Alarm.ogg'");
                add("'Beep.ogg'");
                add("'Butterfly.ogg'");
                add("'Brahms.ogg'");
                add("'Confident.ogg'");
                add("'Dragon.ogg'");
                add("'Golden_Sea.ogg'");
                add("'Good_Times.ogg'");
                add("'Grow.ogg'");
                add("'Hasty.ogg'");
                add("'Love_of_The_Sea.ogg'");
                add("'Magic_Sky.ogg'");
                add("'Meditation.ogg'");
                add("'Meet_Love_at_The_Corner.ogg'");
                add("'Miss_You.ogg'");
                add("'Moring.ogg'");
                add("'Peripatetic.ogg'");
                add("'Piano.ogg'");
                add("'Radar.ogg'");
                add("'String_Of_Pearls.ogg'");
                add("'Sunrise.ogg'");
                add("'Warm_Spring.ogg'");
                add("'Yesterday.ogg'");
                add("'Dawn.ogg'");
            }
        };
        this.NOTIFICATION2 = new ArrayList<String>() { // from class: android.media.RingtoneManager.3
            {
                add("'Simple.ogg'");
                add("'Technology.ogg'");
                add("'Clear.ogg'");
                add("'Elegant.ogg'");
            }
        };
        this.mActivity = activity;
        this.mContext = activity;
        setType(this.mType);
        this.mIncludeParentRingtones = includeParentRingtones;
    }

    public RingtoneManager(Context context) {
        this(context, false);
    }

    public RingtoneManager(Context context, boolean includeParentRingtones) {
        this.mType = 1;
        this.mFilterColumns = new ArrayList();
        this.mStopPreviousRingtone = true;
        this.DZ_KEPT_RINGTONES_LIST = new ArrayList<String>() { // from class: android.media.RingtoneManager.1
            {
                add("'Airship.ogg'");
                add("'Crystal_Clear.ogg'");
                add("'Lating.ogg'");
                add("'Peripatetic.ogg'");
                add("'Pluto.ogg'");
                add("'Quantum.ogg'");
                add("'Reggae.ogg'");
                add("'Your_Eyes.ogg'");
                add("'Suling_Flute.ogg'");
                add("'We_Can.ogg'");
                add("'Cartoon.ogg'");
                add("'Dripping.ogg'");
                add("'Drop.ogg'");
                add("'Gentle.ogg'");
                add("'Zero.ogg'");
                add("'Alarm.ogg'");
                add("'Beep.ogg'");
                add("'Brahms.ogg'");
                add("'Butterfly.ogg'");
                add("'Confident.ogg'");
                add("'Dragon.ogg'");
                add("'Golden_Sea.ogg'");
                add("'Good_Times.ogg'");
                add("'Grow.ogg'");
                add("'Hasty.ogg'");
                add("'Love_of_The_Sea.ogg'");
                add("'Magic_Sky.ogg'");
                add("'Meditation.ogg'");
                add("'Meet_Love_at_The_Corner.ogg'");
                add("'A_Mellow_Pulse.ogg'");
                add("'Miss_You.ogg'");
                add("'Moring.ogg'");
                add("'Peripatetic.ogg'");
                add("'Piano.ogg'");
                add("'Radar.ogg'");
                add("'String_Of_Pearls.ogg'");
                add("'Sunrise.ogg'");
                add("'Warm_Spring.ogg'");
                add("'Yesterday.ogg'");
            }
        };
        this.DZ_KEPT_RINGTONES_LIST_S_OTA = new ArrayList<String>() { // from class: android.media.RingtoneManager.2
            {
                add("'Airship.ogg'");
                add("'Crystal_Clear.ogg'");
                add("'Lating.ogg'");
                add("'Peripatetic.ogg'");
                add("'Pluto.ogg'");
                add("'Quantum.ogg'");
                add("'Reggae.ogg'");
                add("'Your_Eyes.ogg'");
                add("'We_Can.ogg'");
                add("'ShiningMoment.ogg'");
                add("'Cartoon.ogg'");
                add("'Dripping.ogg'");
                add("'Drop.ogg'");
                add("'Gentle.ogg'");
                add("'Woodblock.ogg'");
                add("'A_Mellow_Pulse.ogg'");
                add("'Alarm.ogg'");
                add("'Beep.ogg'");
                add("'Butterfly.ogg'");
                add("'Brahms.ogg'");
                add("'Confident.ogg'");
                add("'Dragon.ogg'");
                add("'Golden_Sea.ogg'");
                add("'Good_Times.ogg'");
                add("'Grow.ogg'");
                add("'Hasty.ogg'");
                add("'Love_of_The_Sea.ogg'");
                add("'Magic_Sky.ogg'");
                add("'Meditation.ogg'");
                add("'Meet_Love_at_The_Corner.ogg'");
                add("'Miss_You.ogg'");
                add("'Moring.ogg'");
                add("'Peripatetic.ogg'");
                add("'Piano.ogg'");
                add("'Radar.ogg'");
                add("'String_Of_Pearls.ogg'");
                add("'Sunrise.ogg'");
                add("'Warm_Spring.ogg'");
                add("'Yesterday.ogg'");
                add("'Dawn.ogg'");
            }
        };
        this.NOTIFICATION2 = new ArrayList<String>() { // from class: android.media.RingtoneManager.3
            {
                add("'Simple.ogg'");
                add("'Technology.ogg'");
                add("'Clear.ogg'");
                add("'Elegant.ogg'");
            }
        };
        this.mActivity = null;
        this.mContext = context;
        setType(this.mType);
        this.mIncludeParentRingtones = includeParentRingtones;
    }

    public void setType(int type) {
        if (this.mCursor != null) {
            throw new IllegalStateException("Setting filter columns should be done before querying for ringtones.");
        }
        this.mType = type;
        setFilterColumnsList(type);
    }

    public int inferStreamType() {
        switch (this.mType) {
            case 2:
                return 5;
            case 3:
            default:
                return 2;
            case 4:
                return 4;
        }
    }

    public void setStopPreviousRingtone(boolean stopPreviousRingtone) {
        this.mStopPreviousRingtone = stopPreviousRingtone;
    }

    public boolean getStopPreviousRingtone() {
        return this.mStopPreviousRingtone;
    }

    public void stopPreviousRingtone() {
        Ringtone ringtone = this.mPreviousRingtone;
        if (ringtone != null) {
            ringtone.stop();
        }
    }

    @Deprecated
    public boolean getIncludeDrm() {
        return false;
    }

    @Deprecated
    public void setIncludeDrm(boolean includeDrm) {
        if (includeDrm) {
            Log.w(TAG, "setIncludeDrm no longer supported");
        }
    }

    public Cursor getCursor() {
        Cursor parentRingtonesCursor;
        Cursor cursor = this.mCursor;
        if (cursor != null && cursor.requery()) {
            return this.mCursor;
        }
        ArrayList<Cursor> ringtoneCursors = new ArrayList<>();
        ringtoneCursors.add(getInternalRingtones());
        if (this.mType != 16) {
            ringtoneCursors.add(getMediaRingtones());
        }
        if (this.mIncludeParentRingtones && (parentRingtonesCursor = getParentProfileRingtones()) != null) {
            ringtoneCursors.add(parentRingtonesCursor);
        }
        SortCursor sortCursor = new SortCursor((Cursor[]) ringtoneCursors.toArray(new Cursor[ringtoneCursors.size()]), "title_key");
        this.mCursor = sortCursor;
        return sortCursor;
    }

    private Cursor getParentProfileRingtones() {
        Context parentContext;
        Cursor res;
        UserManager um = UserManager.get(this.mContext);
        UserInfo parentInfo = um.getProfileParent(this.mContext.getUserId());
        if (parentInfo == null || parentInfo.id == this.mContext.getUserId() || (parentContext = createPackageContextAsUser(this.mContext, parentInfo.id)) == null || (res = getMediaRingtones(parentContext)) == null) {
            return null;
        }
        return new ExternalRingtonesCursorWrapper(res, ContentProvider.maybeAddUserId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, parentInfo.id));
    }

    public Ringtone getRingtone(int position) {
        Ringtone ringtone;
        if (this.mStopPreviousRingtone && (ringtone = this.mPreviousRingtone) != null) {
            ringtone.stop();
        }
        Ringtone ringtone2 = getRingtone(this.mContext, getRingtoneUri(position), inferStreamType());
        this.mPreviousRingtone = ringtone2;
        return ringtone2;
    }

    public Uri getRingtoneUri(int position) {
        try {
            Cursor cursor = this.mCursor;
            if (cursor != null) {
                if (cursor.moveToPosition(position)) {
                    return getUriFromCursor(this.mContext, this.mCursor);
                }
            }
            return null;
        } catch (StaleDataException | IllegalStateException e) {
            Log.e(TAG, "Unexpected Exception has been catched.", e);
            return null;
        }
    }

    private static Uri getUriFromCursor(Context context, Cursor cursor) {
        Uri uri = ContentUris.withAppendedId(Uri.parse(cursor.getString(2)), cursor.getLong(0));
        return context.getContentResolver().canonicalizeOrElse(uri);
    }

    public int getRingtonePosition(Uri ringtoneUri) {
        if (ringtoneUri == null) {
            return -1;
        }
        try {
            Cursor cursor = getCursor();
            cursor.moveToPosition(-1);
            if (RingtoneManagerDebugTAG) {
                Log.d(TAG, "dumpCursorToString = " + DatabaseUtils.dumpCursorToString(cursor) + " ringtoneUri = " + ringtoneUri);
            }
            while (cursor.moveToNext()) {
                Uri uriFromCursor = getUriFromCursor(this.mContext, cursor);
                if (ringtoneUri.equals(uriFromCursor)) {
                    return cursor.getPosition();
                }
            }
            if (isExternalPrimaryRingtoneUri(ringtoneUri)) {
                String uri = ringtoneUri.toString();
                Uri ringtoneUri2 = this.mContext.getContentResolver().canonicalizeOrElse(Uri.parse(uri.replaceFirst("external_primary", "external")));
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    Uri uriFromCursor2 = getUriFromCursor(this.mContext, cursor);
                    if (ringtoneUri2.equals(uriFromCursor2)) {
                        return cursor.getPosition();
                    }
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "NumberFormatException while getting ringtone position, returning -1", e);
        }
        Log.e(TAG, "getRingtonePosition end, pos: -1 ");
        return -1;
    }

    public static Uri getValidRingtoneUri(Context context) {
        RingtoneManager rm = new RingtoneManager(context);
        Uri uri = getValidRingtoneUriFromCursorAndClose(context, rm.getInternalRingtones());
        if (uri == null) {
            return getValidRingtoneUriFromCursorAndClose(context, rm.getMediaRingtones());
        }
        return uri;
    }

    private static Uri getValidRingtoneUriFromCursorAndClose(Context context, Cursor cursor) {
        if (cursor != null) {
            Uri uri = null;
            if (cursor.moveToFirst()) {
                uri = getUriFromCursor(context, cursor);
            }
            cursor.close();
            return uri;
        }
        return null;
    }

    private Cursor getInternalRingtones() {
        Cursor res;
        if ("TECNO".equals(SystemProperties.get("ro.product.brand")) && "DZ".equals(SystemProperties.get("persist.sys.oobe_country")) && this.mType != 16) {
            String listString = this.DZ_KEPT_RINGTONES_LIST.toString().replace(NavigationBarInflaterView.SIZE_MOD_START, "").replace(NavigationBarInflaterView.SIZE_MOD_END, "");
            if (SystemProperties.get("ro.tran_sounds_ota_support").equals("1")) {
                listString = this.DZ_KEPT_RINGTONES_LIST_S_OTA.toString().replace(NavigationBarInflaterView.SIZE_MOD_START, "").replace(NavigationBarInflaterView.SIZE_MOD_END, "");
            }
            String whereForList = " and _display_name IN (" + listString + NavigationBarInflaterView.KEY_CODE_END;
            res = query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, INTERNAL_COLUMNS, constructBooleanTrueWhereClause(this.mFilterColumns) + whereForList, null, "title_key");
        } else if (!AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS.equals(SystemProperties.get("ro.config.notification_cust", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS))) {
            String listString2 = this.NOTIFICATION2.toString().replace(NavigationBarInflaterView.SIZE_MOD_START, "").replace(NavigationBarInflaterView.SIZE_MOD_END, "");
            String whereForList2 = " and _display_name NOT IN (" + listString2 + NavigationBarInflaterView.KEY_CODE_END;
            if (this.mType == 16) {
                whereForList2 = " and _display_name IN (" + listString2 + NavigationBarInflaterView.KEY_CODE_END;
            }
            res = query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, INTERNAL_COLUMNS, constructBooleanTrueWhereClause(this.mFilterColumns) + whereForList2, null, "title_key");
        } else if (isNoFFCountryForFFProject()) {
            String whereForList3 = "";
            int i = this.mType;
            if (i == 1 || i == 32) {
                String[] ffRingtoneListStringArray = this.mContext.getResources().getStringArray(R.array.tran_ff_hot_ringtone);
                String ffRingtoneListString = getSelectionsSting(ffRingtoneListStringArray);
                whereForList3 = " and _display_name NOT IN (" + ffRingtoneListString + NavigationBarInflaterView.KEY_CODE_END;
            } else if (i == 2 || i == 16) {
                String[] ffNotificationListStringArray = this.mContext.getResources().getStringArray(R.array.tran_ff_hot_notification);
                String ffNotificationListString = getSelectionsSting(ffNotificationListStringArray);
                whereForList3 = " and _display_name NOT IN (" + ffNotificationListString + NavigationBarInflaterView.KEY_CODE_END;
            }
            res = query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, INTERNAL_COLUMNS, constructBooleanTrueWhereClause(this.mFilterColumns) + whereForList3, null, "title_key");
        } else {
            res = query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, INTERNAL_COLUMNS, constructBooleanTrueWhereClause(this.mFilterColumns), null, "title_key");
        }
        return new ExternalRingtonesCursorWrapper(res, MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
    }

    private String getSelectionsSting(String[] stringArray) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stringArray.length; i++) {
            String filename = stringArray[i];
            sb.append("'").append(filename).append("'");
            if (i < stringArray.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private boolean isNoFFCountryForFFProject() {
        boolean isNoFFCountry = false;
        if (isCountryDefaultSupport()) {
            String countryCode = SystemProperties.get("persist.sys.oobe_country");
            initCountryDefaultMaps(this.mContext);
            if (!mCountryDefaultRingtoneMap.containsKey(countryCode) && !mCountryDefaultNotificationMap.containsKey(countryCode)) {
                isNoFFCountry = true;
            }
            clearCountryDefaultMaps();
            return isNoFFCountry;
        }
        return false;
    }

    private Cursor getMediaRingtones() {
        Cursor res = getMediaRingtones(this.mContext);
        if (res == null) {
            return null;
        }
        return new ExternalRingtonesCursorWrapper(res, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
    }

    private Cursor getMediaRingtones(Context context) {
        String status = Environment.getExternalStorageState();
        Log.w(TAG, "getMediaRingtones,ext storage status:" + status);
        if (status.equals(Environment.MEDIA_MOUNTED) || status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            return query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MEDIA_COLUMNS, constructBooleanTrueWhereClause(this.mFilterColumns), null, "title_key", context);
        }
        return null;
    }

    private void setFilterColumnsList(int type) {
        List<String> columns = this.mFilterColumns;
        columns.clear();
        if ((type & 1) != 0 || (type & 32) != 0) {
            columns.add("is_ringtone");
        }
        if ((type & 2) != 0 || (type & 16) != 0) {
            columns.add("is_notification");
        }
        if ((type & 4) != 0) {
            columns.add("is_alarm");
        }
    }

    private static String constructBooleanTrueWhereClause(List<String> columns) {
        if (columns == null || columns.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(NavigationBarInflaterView.KEY_CODE_START);
        for (int i = columns.size() - 1; i >= 0; i--) {
            sb.append(columns.get(i)).append("=1 or ");
        }
        int i2 = columns.size();
        if (i2 > 0) {
            sb.setLength(sb.length() - 4);
        }
        sb.append(NavigationBarInflaterView.KEY_CODE_END);
        return sb.toString();
    }

    private Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return query(uri, projection, selection, selectionArgs, sortOrder, this.mContext);
    }

    private Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, Context context) {
        Activity activity = this.mActivity;
        if (activity != null) {
            return activity.managedQuery(uri, projection, selection, selectionArgs, sortOrder);
        }
        return context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
    }

    public static Ringtone getRingtone(Context context, Uri ringtoneUri) {
        return getRingtone(context, ringtoneUri, -1);
    }

    public static Ringtone getRingtone(Context context, Uri ringtoneUri, VolumeShaper.Configuration volumeShaperConfig) {
        return getRingtone(context, ringtoneUri, -1, volumeShaperConfig);
    }

    private static Ringtone getRingtone(Context context, Uri ringtoneUri, int streamType) {
        return getRingtone(context, ringtoneUri, streamType, null);
    }

    private static Ringtone getRingtone(Context context, Uri ringtoneUri, int streamType, VolumeShaper.Configuration volumeShaperConfig) {
        try {
            Ringtone r = new Ringtone(context, true);
            if (streamType >= 0) {
                r.setStreamType(streamType);
            }
            r.setUri(ringtoneUri, volumeShaperConfig);
            return r;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to open ringtone " + ringtoneUri + ": " + ex);
            return null;
        }
    }

    public static Uri getActualDefaultRingtoneUri(Context context, int type) {
        if (Settings.System.getInt(context.getContentResolver(), GET_COUNTRY_NULL, 0) == 1) {
            resetCountryDefaultRingtone(context);
            Settings.System.putInt(context.getContentResolver(), GET_COUNTRY_NULL, 0);
        }
        String setting = getSettingForType(type);
        if (setting == null) {
            return null;
        }
        String uriString = Settings.System.getStringForUser(context.getContentResolver(), setting, context.getUserId());
        Uri ringtoneUri = uriString != null ? Uri.parse(uriString) : null;
        if (RingtoneManagerDebugTAG) {
            Log.w(TAG, "getActualDefaultRingtoneUri ringtoneUri: " + ringtoneUri + ",type " + type);
        }
        if (ringtoneUri != null && ContentProvider.getUserIdFromUri(ringtoneUri) == context.getUserId()) {
            ringtoneUri = ContentProvider.getUriWithoutUserId(ringtoneUri);
        }
        if (ringtoneUri != null && ((isExternalRingtoneUri(ringtoneUri) || isExternalPrimaryRingtoneUri(ringtoneUri) || isInternalRingtoneUri(ringtoneUri)) && context.getUserId() == 0 && !isRingtoneExist(context, ringtoneUri))) {
            ringtoneUri = getDefaultRingtoneUri(context, type);
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                setActualDefaultRingtoneUri(context, type, ringtoneUri);
            }
        }
        return ringtoneUri;
    }

    public static void setActualDefaultRingtoneUri(Context context, int type, Uri ringtoneUri) {
        String setting = getSettingForType(type);
        if (setting == null) {
            return;
        }
        if (RingtoneManagerDebugTAG) {
            Log.w(TAG, "setActualDefaultRingtoneUri uri: " + ringtoneUri + "," + type);
        }
        ContentResolver resolver = context.getContentResolver();
        if (!isInternalRingtoneUri(ringtoneUri)) {
            ringtoneUri = ContentProvider.maybeAddUserId(ringtoneUri, context.getUserId());
        }
        Settings.System.putStringForUser(resolver, setting, ringtoneUri != null ? ringtoneUri.toString() : null, context.getUserId());
        if (ringtoneUri != null) {
            Uri cacheUri = getCacheForType(type, context.getUserId());
            try {
                InputStream in = openRingtone(context, ringtoneUri);
                OutputStream out = resolver.openOutputStream(cacheUri);
                try {
                    FileUtils.copy(in, out);
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (Throwable th) {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to cache ringtone: " + e);
            }
        }
    }

    private static boolean isInternalRingtoneUri(Uri uri) {
        return isRingtoneUriInStorage(uri, MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
    }

    private static boolean isExternalRingtoneUri(Uri uri) {
        return isRingtoneUriInStorage(uri, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
    }

    private static boolean isRingtoneUriInStorage(Uri ringtone, Uri storage) {
        Uri uriWithoutUserId = ContentProvider.getUriWithoutUserId(ringtone);
        if (uriWithoutUserId == null) {
            return false;
        }
        return uriWithoutUserId.toString().startsWith(storage.toString());
    }

    public Uri addCustomExternalRingtone(Uri fileUri, int type) throws FileNotFoundException, IllegalArgumentException, IOException {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new IOException("External storage is not mounted. Unable to install ringtones.");
        }
        String mimeType = this.mContext.getContentResolver().getType(fileUri);
        if (mimeType == null || (!mimeType.startsWith("audio/") && !mimeType.equals(ContentType.AUDIO_OGG))) {
            throw new IllegalArgumentException("Ringtone file must have MIME type \"audio/*\". Given file has MIME type \"" + mimeType + "\"");
        }
        if ("audio/x-ms-wma".equals(mimeType) || "audio/x-aiff".equals(mimeType) || "audio/basic".equals(mimeType)) {
            Log.w(TAG, "UnSupport audio format. mimeType:" + mimeType + " fileUri = " + fileUri);
            return null;
        }
        String subdirectory = getExternalDirectoryForType(type);
        Context context = this.mContext;
        File outFile = Utils.getUniqueExternalFile(context, subdirectory, FileUtils.buildValidFatFilename(Utils.getFileDisplayNameFromUri(context, fileUri)), mimeType);
        if (outFile == null) {
            Log.w(TAG, "The number of repeated files is limited in FileUtils::buildUniqueFileWithExtension() and can not exceed 32!");
            return null;
        }
        InputStream input = this.mContext.getContentResolver().openInputStream(fileUri);
        try {
            OutputStream output = new FileOutputStream(outFile);
            FileUtils.copy(input, output);
            output.close();
            if (input != null) {
                input.close();
            }
            return MediaStore.scanFile(this.mContext.getContentResolver(), outFile);
        } catch (Throwable th) {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static final String getExternalDirectoryForType(int type) {
        switch (type) {
            case 1:
            case 32:
                return Environment.DIRECTORY_RINGTONES;
            case 2:
                return Environment.DIRECTORY_NOTIFICATIONS;
            case 4:
                return Environment.DIRECTORY_ALARMS;
            default:
                throw new IllegalArgumentException("Unsupported ringtone type: " + type);
        }
    }

    private static InputStream openRingtone(Context context, Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        try {
            return resolver.openInputStream(uri);
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Failed to open directly; attempting failover: " + e);
            IRingtonePlayer player = ((AudioManager) context.getSystemService(AudioManager.class)).getRingtonePlayer();
            try {
                return new ParcelFileDescriptor.AutoCloseInputStream(player.openRingtone(uri));
            } catch (Exception e2) {
                throw new IOException(e2);
            }
        }
    }

    private static String getSettingForType(int type) {
        if ((type & 1) != 0) {
            return Settings.System.RINGTONE;
        }
        if ((type & 32) != 0) {
            return Settings.System.RINGTONE2;
        }
        if ((type & 2) != 0) {
            return Settings.System.NOTIFICATION_SOUND;
        }
        if ((type & 16) != 0) {
            return Settings.System.NOTIFICATION2;
        }
        if ((type & 4) != 0) {
            return Settings.System.ALARM_ALERT;
        }
        return null;
    }

    public static Uri getCacheForType(int type) {
        return getCacheForType(type, UserHandle.getCallingUserId());
    }

    public static Uri getCacheForType(int type, int userId) {
        if ((type & 1) != 0) {
            return ContentProvider.maybeAddUserId(Settings.System.RINGTONE_CACHE_URI, userId);
        }
        if ((type & 32) != 0) {
            return ContentProvider.maybeAddUserId(Settings.System.RINGTONE2_CACHE_URI, userId);
        }
        if ((type & 2) != 0) {
            return ContentProvider.maybeAddUserId(Settings.System.NOTIFICATION_SOUND_CACHE_URI, userId);
        }
        if ((type & 16) != 0) {
            return ContentProvider.maybeAddUserId(Settings.System.NOTIFICATION2_CACHE_URI, userId);
        }
        if ((type & 4) != 0) {
            return ContentProvider.maybeAddUserId(Settings.System.ALARM_ALERT_CACHE_URI, userId);
        }
        return null;
    }

    public static boolean isDefault(Uri ringtoneUri) {
        return getDefaultType(ringtoneUri) != -1;
    }

    public static int getDefaultType(Uri defaultRingtoneUri) {
        Uri defaultRingtoneUri2 = ContentProvider.getUriWithoutUserId(defaultRingtoneUri);
        if (defaultRingtoneUri2 == null) {
            return -1;
        }
        if (defaultRingtoneUri2.equals(Settings.System.DEFAULT_RINGTONE_URI)) {
            return 1;
        }
        if (defaultRingtoneUri2.equals(Settings.System.DEFAULT_RINGTONE2_URI)) {
            return 32;
        }
        if (defaultRingtoneUri2.equals(Settings.System.DEFAULT_NOTIFICATION_URI)) {
            return 2;
        }
        if (defaultRingtoneUri2.equals(Settings.System.DEFAULT_NOTIFICATION2_URI)) {
            return 16;
        }
        if (!defaultRingtoneUri2.equals(Settings.System.DEFAULT_ALARM_ALERT_URI)) {
            return -1;
        }
        return 4;
    }

    public static Uri getDefaultUri(int type) {
        if ((type & 1) != 0) {
            return Settings.System.DEFAULT_RINGTONE_URI;
        }
        if ((type & 32) != 0) {
            return Settings.System.DEFAULT_RINGTONE2_URI;
        }
        if ((type & 2) != 0) {
            return Settings.System.DEFAULT_NOTIFICATION_URI;
        }
        if ((type & 16) != 0) {
            return Settings.System.DEFAULT_NOTIFICATION2_URI;
        }
        if ((type & 4) != 0) {
            return Settings.System.DEFAULT_ALARM_ALERT_URI;
        }
        return null;
    }

    public static AssetFileDescriptor openDefaultRingtoneUri(Context context, Uri uri) throws FileNotFoundException {
        int type = getDefaultType(uri);
        Uri cacheUri = getCacheForType(type, context.getUserId());
        Uri actualUri = getActualDefaultRingtoneUri(context, type);
        ContentResolver resolver = context.getContentResolver();
        AssetFileDescriptor afd = null;
        if (cacheUri != null && (afd = resolver.openAssetFileDescriptor(cacheUri, "r")) != null) {
            return afd;
        }
        if (actualUri != null) {
            AssetFileDescriptor afd2 = resolver.openAssetFileDescriptor(actualUri, "r");
            return afd2;
        }
        return afd;
    }

    public boolean hasHapticChannels(int position) {
        return AudioManager.hasHapticChannels(this.mContext, getRingtoneUri(position));
    }

    public static boolean hasHapticChannels(Uri ringtoneUri) {
        return AudioManager.hasHapticChannels(null, ringtoneUri);
    }

    public static boolean hasHapticChannels(Context context, Uri ringtoneUri) {
        return AudioManager.hasHapticChannels(context, ringtoneUri);
    }

    private static Context createPackageContextAsUser(Context context, int userId) {
        try {
            return context.createPackageContextAsUser(context.getPackageName(), 0, UserHandle.of(userId));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to create package context", e);
            return null;
        }
    }

    @SystemApi
    public static void ensureDefaultRingtones(Context context) {
        int[] iArr = {1, 32, 2, 16, 4};
        for (int i = 0; i < 5; i++) {
            int type = iArr[i];
            String setting = getDefaultRingtoneSetting(type);
            if (Settings.System.getInt(context.getContentResolver(), setting, 0) == 0) {
                if (isCountryDefaultSupport()) {
                    initCountryDefaultMaps(context);
                    if (Settings.System.getInt(context.getContentResolver(), GET_COUNTRY_NULL, 0) == 0 && NO_INIT.equals(getCountry())) {
                        Log.d(TAG, "oobe_country null ");
                        Settings.System.putInt(context.getContentResolver(), GET_COUNTRY_NULL, 1);
                    }
                }
                String filename = getDefaultRingtoneFilename(type);
                Log.d(TAG, "ensureDefaultRingtones getDefaultRingtoneFilename filename = " + filename + " type = " + type);
                String whichAudio = getQueryStringForType(type);
                String where = "_display_name=? AND " + whichAudio + "=?";
                Uri baseUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
                Cursor cursor = context.getContentResolver().query(baseUri, new String[]{"_id"}, where, new String[]{filename, "1"}, null);
                try {
                    if (cursor.moveToFirst()) {
                        Uri ringtoneUri = context.getContentResolver().canonicalizeOrElse(ContentUris.withAppendedId(baseUri, cursor.getLong(0)));
                        setDefaultRingtoneUri(context, type, ringtoneUri);
                        setActualDefaultRingtoneUri(context, type, ringtoneUri);
                        Settings.System.putInt(context.getContentResolver(), setting, 1);
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Throwable th) {
                    if (cursor != null) {
                        try {
                            cursor.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            }
        }
        if (isCountryDefaultSupport()) {
            clearCountryDefaultMaps();
        }
    }

    private static String getDefaultRingtoneSetting(int type) {
        switch (type) {
            case 1:
                return "ringtone_set";
            case 2:
                return "notification_sound_set";
            case 4:
                return "alarm_alert_set";
            case 16:
                return "notification2_set";
            case 32:
                return "ringtone2_set";
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String getDefaultRingtoneFilename(int type) {
        switch (type) {
            case 1:
            case 32:
                String countryRingName = null;
                HashMap<String, String> hashMap = mCountryDefaultRingtoneMap;
                if (hashMap.size() > 0) {
                    String countryRingName2 = hashMap.get(getCountry());
                    countryRingName = countryRingName2;
                }
                Log.d(TAG, "mCountryDefaultRingtoneMap size = " + hashMap.size() + " countryRingName = " + countryRingName + " country = " + getCountry());
                if (!isCountryDefaultSupport() || countryRingName == null) {
                    return getSplitRingtoneFilename(type);
                }
                return countryRingName;
            case 2:
                String countryNotificationName = null;
                HashMap<String, String> hashMap2 = mCountryDefaultNotificationMap;
                if (hashMap2.size() > 0) {
                    String countryNotificationName2 = hashMap2.get(getCountry());
                    countryNotificationName = countryNotificationName2;
                }
                Log.d(TAG, "mCountryDefaultNotificationMap size = " + hashMap2.size() + " countryNotificationName = " + countryNotificationName + " country = " + getCountry());
                if (!isCountryDefaultSupport() || countryNotificationName == null) {
                    return SystemProperties.get("ro.config.notification_sound");
                }
                return countryNotificationName;
            case 4:
                return SystemProperties.get("ro.config.alarm_alert");
            case 16:
                return SystemProperties.get("ro.config.notification_cust");
            default:
                throw new IllegalArgumentException();
        }
    }

    private static void initCountryDefaultMaps(Context context) {
        HashMap<String, String> hashMap = mCountryDefaultRingtoneMap;
        if (hashMap != null && hashMap.size() == 0) {
            Log.d(TAG, "set mCountryDefaultRingtoneMap ");
            String[] countryDefaultRingtoneMaps = context.getResources().getStringArray(R.array.tran_country_default_ringtone);
            if (countryDefaultRingtoneMaps != null && countryDefaultRingtoneMaps.length > 0) {
                for (String str_ring : countryDefaultRingtoneMaps) {
                    String[] countryRingtoneItem = str_ring.split(":");
                    mCountryDefaultRingtoneMap.put(countryRingtoneItem[0], countryRingtoneItem[1]);
                }
            }
        }
        HashMap<String, String> hashMap2 = mCountryDefaultNotificationMap;
        if (hashMap2 != null && hashMap2.size() == 0) {
            Log.d(TAG, "set mCountryDefaultNotificationMap ");
            String[] countryDefaultNotificationMaps = context.getResources().getStringArray(R.array.tran_country_default_notification);
            if (countryDefaultNotificationMaps != null && countryDefaultNotificationMaps.length > 0) {
                for (String str_notification : countryDefaultNotificationMaps) {
                    String[] countryNotoficationItem = str_notification.split(":");
                    mCountryDefaultNotificationMap.put(countryNotoficationItem[0], countryNotoficationItem[1]);
                }
            }
        }
    }

    private static void clearCountryDefaultMaps() {
        Log.d(TAG, "clearCountryDefaultMaps");
        HashMap<String, String> hashMap = mCountryDefaultRingtoneMap;
        if (hashMap != null && hashMap.size() > 0) {
            hashMap.clear();
        }
        HashMap<String, String> hashMap2 = mCountryDefaultNotificationMap;
        if (hashMap2 != null && hashMap2.size() > 0) {
            hashMap.clear();
        }
    }

    private static String getCountry() {
        String country = SystemProperties.get("persist.sys.oobe_country", NO_INIT);
        return country;
    }

    private static boolean isCountryDefaultSupport() {
        boolean isSupport = SystemProperties.getBoolean("ro.country.ringtone.support", false);
        return isSupport;
    }

    private static void resetCountryDefaultRingtone(Context context) {
        Log.d(TAG, "resetCountryDefaultRingtone");
        int[] iArr = {1, 32, 2, 4};
        for (int i = 0; i < 4; i++) {
            int type = iArr[i];
            String setting = getDefaultRingtoneSetting(type);
            if (Settings.System.getInt(context.getContentResolver(), setting, 0) == 1) {
                Settings.System.putInt(context.getContentResolver(), setting, 0);
            }
        }
        ensureDefaultRingtones(context);
    }

    private static String getQueryStringForType(int type) {
        switch (type) {
            case 1:
                return "is_ringtone";
            case 2:
                return "is_notification";
            case 4:
                return "is_alarm";
            case 16:
                return "is_notification";
            case 32:
                return "is_ringtone";
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String getSplitRingtoneFilename(int type) {
        String mDefaultRingtoneFilename;
        String mDefaultRingtoneFilename2;
        String mRingtonesFilename = SystemProperties.get("ro.config.ringtone");
        String[] mRingtonesFilenameArr = !TextUtils.isEmpty(mRingtonesFilename) ? mRingtonesFilename.split("&") : null;
        if (mRingtonesFilenameArr != null && mRingtonesFilenameArr.length > 1) {
            mDefaultRingtoneFilename = mRingtonesFilenameArr[0];
            mDefaultRingtoneFilename2 = mRingtonesFilenameArr[1];
        } else {
            mDefaultRingtoneFilename = mRingtonesFilename;
            mDefaultRingtoneFilename2 = mRingtonesFilename;
        }
        switch (type) {
            case 1:
                return mDefaultRingtoneFilename;
            case 32:
                return mDefaultRingtoneFilename2;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static String getRingtoneDefaultName(int type) {
        return "default_" + getSettingForType(type);
    }

    public static String getRingtoneDataName(int type) {
        return getSettingForType(type) + "_data";
    }

    public static void setDefaultRingtoneUri(Context context, int type, Uri ringtoneUri) {
        if (ringtoneUri != null) {
            String settingName = getRingtoneDefaultName(type);
            String value = ringtoneUri.toString();
            Settings.System.putString(context.getContentResolver(), settingName, value);
            Log.d(TAG, "setDefaultRingtone: name=" + settingName + " value=" + value);
        }
    }

    public static Uri getDefaultRingtoneUri(Context context, int type) {
        try {
            String ringtone = Settings.System.getString(context.getContentResolver(), getRingtoneDefaultName(type));
            if (ringtone != null) {
                return Uri.parse(ringtone);
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "getDefaultRingtoneUri error: " + e);
            return null;
        }
    }

    public static boolean isRingtoneExist(Context context, Uri ringtoneUri) {
        boolean exist = getExternalUriData(context.getContentResolver(), ringtoneUri) != null;
        Log.d(TAG, ringtoneUri + " is exist " + exist);
        return exist;
    }

    /* JADX WARN: Code restructure failed: missing block: B:22:0x005b, code lost:
        if (r2 == null) goto L18;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x005e, code lost:
        android.util.Log.d(android.media.RingtoneManager.TAG, "getExternalUriData for " + r11 + " with data: " + r1);
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x007e, code lost:
        return r1;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static String getExternalUriData(ContentResolver resolver, Uri ringtoneUri) {
        if (ringtoneUri == null || ringtoneUri.toString().isEmpty() || isDefault(ringtoneUri)) {
            return null;
        }
        String data = null;
        Cursor cursor = null;
        try {
            try {
                cursor = resolver.query(ringtoneUri, new String[]{"_data"}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    data = cursor.getString(0);
                }
            } catch (Exception e) {
                Log.e(TAG, "getExternalUriData: e = " + e.toString());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static boolean isExternalPrimaryRingtoneUri(Uri uri) {
        return isRingtoneUriInStorage(uri, MediaStore.Audio.Media.getContentUri("external_primary"));
    }
}
