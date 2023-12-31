package com.android.server.tv;

import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContentRating;
import android.os.Environment;
import android.os.Handler;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class PersistentDataStore {
    private static final String ATTR_ENABLED = "enabled";
    private static final String ATTR_STRING = "string";
    private static final String TAG = "TvInputManagerService";
    private static final String TAG_BLOCKED_RATINGS = "blocked-ratings";
    private static final String TAG_PARENTAL_CONTROLS = "parental-controls";
    private static final String TAG_RATING = "rating";
    private static final String TAG_TV_INPUT_MANAGER_STATE = "tv-input-manager-state";
    private final AtomicFile mAtomicFile;
    private boolean mBlockedRatingsChanged;
    private final Context mContext;
    private boolean mLoaded;
    private boolean mParentalControlsEnabled;
    private boolean mParentalControlsEnabledChanged;
    private final Handler mHandler = new Handler();
    private final List<TvContentRating> mBlockedRatings = Collections.synchronizedList(new ArrayList());
    private final Runnable mSaveRunnable = new Runnable() { // from class: com.android.server.tv.PersistentDataStore.1
        @Override // java.lang.Runnable
        public void run() {
            PersistentDataStore.this.save();
        }
    };

    public PersistentDataStore(Context context, int userId) {
        this.mContext = context;
        File userDir = Environment.getUserSystemDirectory(userId);
        if (!userDir.exists() && !userDir.mkdirs()) {
            throw new IllegalStateException("User dir cannot be created: " + userDir);
        }
        this.mAtomicFile = new AtomicFile(new File(userDir, "tv-input-manager-state.xml"), "tv-input-state");
    }

    public boolean isParentalControlsEnabled() {
        loadIfNeeded();
        return this.mParentalControlsEnabled;
    }

    public void setParentalControlsEnabled(boolean enabled) {
        loadIfNeeded();
        if (this.mParentalControlsEnabled != enabled) {
            this.mParentalControlsEnabled = enabled;
            this.mParentalControlsEnabledChanged = true;
            postSave();
        }
    }

    public boolean isRatingBlocked(TvContentRating rating) {
        loadIfNeeded();
        synchronized (this.mBlockedRatings) {
            for (TvContentRating blockedRating : this.mBlockedRatings) {
                if (rating.contains(blockedRating)) {
                    return true;
                }
            }
            return false;
        }
    }

    public TvContentRating[] getBlockedRatings() {
        loadIfNeeded();
        List<TvContentRating> list = this.mBlockedRatings;
        return (TvContentRating[]) list.toArray(new TvContentRating[list.size()]);
    }

    public void addBlockedRating(TvContentRating rating) {
        loadIfNeeded();
        if (rating != null && !this.mBlockedRatings.contains(rating)) {
            this.mBlockedRatings.add(rating);
            this.mBlockedRatingsChanged = true;
            postSave();
        }
    }

    public void removeBlockedRating(TvContentRating rating) {
        loadIfNeeded();
        if (rating != null && this.mBlockedRatings.contains(rating)) {
            this.mBlockedRatings.remove(rating);
            this.mBlockedRatingsChanged = true;
            postSave();
        }
    }

    private void loadIfNeeded() {
        if (!this.mLoaded) {
            load();
            this.mLoaded = true;
        }
    }

    private void clearState() {
        this.mBlockedRatings.clear();
        this.mParentalControlsEnabled = false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [172=4] */
    private void load() {
        clearState();
        try {
            InputStream is = this.mAtomicFile.openRead();
            try {
                TypedXmlPullParser parser = Xml.resolvePullParser(is);
                loadFromXml(parser);
            } catch (IOException | XmlPullParserException ex) {
                Slog.w(TAG, "Failed to load tv input manager persistent store data.", ex);
                clearState();
            } finally {
                IoUtils.closeQuietly(is);
            }
        } catch (FileNotFoundException e) {
        }
    }

    private void postSave() {
        this.mHandler.removeCallbacks(this.mSaveRunnable);
        this.mHandler.post(this.mSaveRunnable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void save() {
        try {
            FileOutputStream os = this.mAtomicFile.startWrite();
            TypedXmlSerializer serializer = Xml.resolveSerializer(os);
            saveToXml(serializer);
            serializer.flush();
            if (1 != 0) {
                this.mAtomicFile.finishWrite(os);
                broadcastChangesIfNeeded();
            } else {
                this.mAtomicFile.failWrite(os);
            }
        } catch (IOException ex) {
            Slog.w(TAG, "Failed to save tv input manager persistent store data.", ex);
        }
    }

    private void broadcastChangesIfNeeded() {
        if (this.mParentalControlsEnabledChanged) {
            this.mParentalControlsEnabledChanged = false;
            this.mContext.sendBroadcastAsUser(new Intent("android.media.tv.action.PARENTAL_CONTROLS_ENABLED_CHANGED"), UserHandle.ALL);
        }
        if (this.mBlockedRatingsChanged) {
            this.mBlockedRatingsChanged = false;
            this.mContext.sendBroadcastAsUser(new Intent("android.media.tv.action.BLOCKED_RATINGS_CHANGED"), UserHandle.ALL);
        }
    }

    private void loadFromXml(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        XmlUtils.beginDocument(parser, TAG_TV_INPUT_MANAGER_STATE);
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals(TAG_BLOCKED_RATINGS)) {
                loadBlockedRatingsFromXml(parser);
            } else if (parser.getName().equals(TAG_PARENTAL_CONTROLS)) {
                this.mParentalControlsEnabled = parser.getAttributeBoolean((String) null, "enabled");
            }
        }
    }

    private void loadBlockedRatingsFromXml(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals(TAG_RATING)) {
                String ratingString = parser.getAttributeValue((String) null, ATTR_STRING);
                if (TextUtils.isEmpty(ratingString)) {
                    throw new XmlPullParserException("Missing string attribute on rating");
                }
                this.mBlockedRatings.add(TvContentRating.unflattenFromString(ratingString));
            }
        }
    }

    private void saveToXml(TypedXmlSerializer serializer) throws IOException {
        serializer.startDocument((String) null, true);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startTag((String) null, TAG_TV_INPUT_MANAGER_STATE);
        serializer.startTag((String) null, TAG_BLOCKED_RATINGS);
        synchronized (this.mBlockedRatings) {
            for (TvContentRating rating : this.mBlockedRatings) {
                serializer.startTag((String) null, TAG_RATING);
                serializer.attribute((String) null, ATTR_STRING, rating.flattenToString());
                serializer.endTag((String) null, TAG_RATING);
            }
        }
        serializer.endTag((String) null, TAG_BLOCKED_RATINGS);
        serializer.startTag((String) null, TAG_PARENTAL_CONTROLS);
        serializer.attributeBoolean((String) null, "enabled", this.mParentalControlsEnabled);
        serializer.endTag((String) null, TAG_PARENTAL_CONTROLS);
        serializer.endTag((String) null, TAG_TV_INPUT_MANAGER_STATE);
        serializer.endDocument();
    }
}
