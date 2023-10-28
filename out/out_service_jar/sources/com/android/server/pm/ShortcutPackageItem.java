package com.android.server.pm;

import android.content.pm.PackageInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class ShortcutPackageItem {
    private static final String KEY_NAME = "name";
    private static final String TAG = "ShortcutService";
    private final ShortcutPackageInfo mPackageInfo;
    private final String mPackageName;
    private final int mPackageUserId;
    protected ShortcutBitmapSaver mShortcutBitmapSaver;
    protected ShortcutUser mShortcutUser;
    protected final Object mLock = new Object();
    private final Runnable mSaveShortcutPackageRunner = new Runnable() { // from class: com.android.server.pm.ShortcutPackageItem$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            ShortcutPackageItem.this.saveShortcutPackageItem();
        }
    };

    protected abstract boolean canRestoreAnyVersion();

    public abstract int getOwnerUserId();

    protected abstract File getShortcutPackageItemFile();

    protected abstract void onRestored(int i);

    public abstract void saveToXml(TypedXmlSerializer typedXmlSerializer, boolean z) throws IOException, XmlPullParserException;

    /* JADX INFO: Access modifiers changed from: protected */
    public ShortcutPackageItem(ShortcutUser shortcutUser, int packageUserId, String packageName, ShortcutPackageInfo packageInfo) {
        this.mShortcutUser = shortcutUser;
        this.mPackageUserId = packageUserId;
        this.mPackageName = (String) Preconditions.checkStringNotEmpty(packageName);
        this.mPackageInfo = (ShortcutPackageInfo) Objects.requireNonNull(packageInfo);
        this.mShortcutBitmapSaver = new ShortcutBitmapSaver(shortcutUser.mService);
    }

    public void replaceUser(ShortcutUser user) {
        this.mShortcutUser = user;
    }

    public ShortcutUser getUser() {
        return this.mShortcutUser;
    }

    public int getPackageUserId() {
        return this.mPackageUserId;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public ShortcutPackageInfo getPackageInfo() {
        return this.mPackageInfo;
    }

    public void refreshPackageSignatureAndSave() {
        if (this.mPackageInfo.isShadow()) {
            return;
        }
        ShortcutService s = this.mShortcutUser.mService;
        this.mPackageInfo.refreshSignature(s, this);
        scheduleSave();
    }

    public void attemptToRestoreIfNeededAndSave() {
        int restoreBlockReason;
        if (!this.mPackageInfo.isShadow()) {
            return;
        }
        ShortcutService s = this.mShortcutUser.mService;
        if (!s.isPackageInstalled(this.mPackageName, this.mPackageUserId)) {
            return;
        }
        if (!this.mPackageInfo.hasSignatures()) {
            s.wtf("Attempted to restore package " + this.mPackageName + "/u" + this.mPackageUserId + " but signatures not found in the restore data.");
            restoreBlockReason = 102;
        } else {
            PackageInfo pi = s.getPackageInfoWithSignatures(this.mPackageName, this.mPackageUserId);
            pi.getLongVersionCode();
            restoreBlockReason = this.mPackageInfo.canRestoreTo(s, pi, canRestoreAnyVersion());
        }
        onRestored(restoreBlockReason);
        this.mPackageInfo.setShadow(false);
        scheduleSave();
    }

    public void saveToFileLocked(File path, boolean forBackup) {
        TypedXmlSerializer itemOut;
        AtomicFile file = new AtomicFile(path);
        FileOutputStream os = null;
        try {
            os = file.startWrite();
            if (forBackup) {
                itemOut = Xml.newFastSerializer();
                itemOut.setOutput(os, StandardCharsets.UTF_8.name());
            } else {
                itemOut = Xml.resolveSerializer(os);
            }
            itemOut.startDocument((String) null, true);
            saveToXml(itemOut, forBackup);
            itemOut.endDocument();
            os.flush();
            file.finishWrite(os);
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Failed to write to file " + file.getBaseFile(), e);
            file.failWrite(os);
        }
    }

    void scheduleSaveToAppSearchLocked() {
    }

    public JSONObject dumpCheckin(boolean clear) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("name", this.mPackageName);
        return result;
    }

    public void verifyStates() {
    }

    public void scheduleSave() {
        ShortcutService shortcutService = this.mShortcutUser.mService;
        Runnable runnable = this.mSaveShortcutPackageRunner;
        shortcutService.injectPostToHandlerDebounced(runnable, runnable);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveShortcutPackageItem() {
        boolean success = waitForBitmapSaves();
        if (!success) {
            this.mShortcutUser.mService.wtf("Timed out waiting on saving bitmaps.");
        }
        File path = getShortcutPackageItemFile();
        synchronized (this.mLock) {
            path.getParentFile().mkdirs();
            saveToFileLocked(path, false);
            scheduleSaveToAppSearchLocked();
        }
    }

    public boolean waitForBitmapSaves() {
        boolean waitForAllSavesLocked;
        synchronized (this.mLock) {
            waitForAllSavesLocked = this.mShortcutBitmapSaver.waitForAllSavesLocked();
        }
        return waitForAllSavesLocked;
    }

    public void saveBitmap(ShortcutInfo shortcut, int maxDimension, Bitmap.CompressFormat format, int quality) {
        synchronized (this.mLock) {
            this.mShortcutBitmapSaver.saveBitmapLocked(shortcut, maxDimension, format, quality);
        }
    }

    public String getBitmapPathMayWait(ShortcutInfo shortcut) {
        String bitmapPathMayWaitLocked;
        synchronized (this.mLock) {
            bitmapPathMayWaitLocked = this.mShortcutBitmapSaver.getBitmapPathMayWaitLocked(shortcut);
        }
        return bitmapPathMayWaitLocked;
    }

    public void removeIcon(ShortcutInfo shortcut) {
        synchronized (this.mLock) {
            this.mShortcutBitmapSaver.removeIcon(shortcut);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeShortcutPackageItem() {
        synchronized (this.mLock) {
            getShortcutPackageItemFile().delete();
        }
    }
}
