package com.android.server.biometrics.sensors;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricAuthenticator.Identifier;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public abstract class BiometricUserState<T extends BiometricAuthenticator.Identifier> {
    private static final String ATTR_INVALIDATION = "authenticatorIdInvalidation_attr";
    private static final String TAG = "UserState";
    private static final String TAG_INVALIDATION = "authenticatorIdInvalidation_tag";
    protected final Context mContext;
    protected final File mFile;
    protected boolean mInvalidationInProgress;
    protected final ArrayList<T> mBiometrics = new ArrayList<>();
    private final Runnable mWriteStateRunnable = new Runnable() { // from class: com.android.server.biometrics.sensors.BiometricUserState$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            BiometricUserState.this.doWriteStateInternal();
        }
    };
    private T mBiometric = null;

    protected abstract void doWriteState(TypedXmlSerializer typedXmlSerializer) throws Exception;

    protected abstract String getBiometricsTag();

    protected abstract ArrayList<T> getCopy(ArrayList<T> arrayList);

    protected abstract int getNameTemplateResource();

    protected abstract void parseBiometricsLocked(TypedXmlPullParser typedXmlPullParser) throws IOException, XmlPullParserException;

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX INFO: Access modifiers changed from: private */
    public void doWriteStateInternal() {
        AtomicFile destination = new AtomicFile(this.mFile);
        FileOutputStream out = null;
        try {
            out = destination.startWrite();
            TypedXmlSerializer serializer = Xml.resolveSerializer(out);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startDocument((String) null, true);
            serializer.startTag((String) null, TAG_INVALIDATION);
            serializer.attributeBoolean((String) null, ATTR_INVALIDATION, this.mInvalidationInProgress);
            serializer.endTag((String) null, TAG_INVALIDATION);
            doWriteState(serializer);
            serializer.endDocument();
            destination.finishWrite(out);
        } finally {
        }
    }

    public BiometricUserState(Context context, int userId, String fileName) {
        this.mFile = getFileForUser(userId, fileName);
        this.mContext = context;
        synchronized (this) {
            readStateSyncLocked();
        }
    }

    public void setInvalidationInProgress(boolean invalidationInProgress) {
        synchronized (this) {
            this.mInvalidationInProgress = invalidationInProgress;
            scheduleWriteStateLocked();
        }
    }

    public boolean isInvalidationInProgress() {
        boolean z;
        synchronized (this) {
            z = this.mInvalidationInProgress;
        }
        return z;
    }

    public void addBiometric(T identifier) {
        synchronized (this) {
            this.mBiometrics.add(identifier);
            this.mBiometric = identifier;
            scheduleWriteStateLocked();
        }
    }

    public void removeBiometric(int biometricId) {
        synchronized (this) {
            int i = 0;
            while (true) {
                if (i >= this.mBiometrics.size()) {
                    break;
                } else if (this.mBiometrics.get(i).getBiometricId() != biometricId) {
                    i++;
                } else {
                    this.mBiometrics.remove(i);
                    scheduleWriteStateLocked();
                    break;
                }
            }
        }
    }

    public void renameBiometric(int biometricId, CharSequence name) {
        synchronized (this) {
            int i = 0;
            while (true) {
                if (i >= this.mBiometrics.size()) {
                    break;
                } else if (this.mBiometrics.get(i).getBiometricId() != biometricId) {
                    i++;
                } else {
                    BiometricAuthenticator.Identifier identifier = this.mBiometrics.get(i);
                    identifier.setName(name);
                    scheduleWriteStateLocked();
                    break;
                }
            }
        }
    }

    public List<T> getBiometrics() {
        ArrayList<T> copy;
        synchronized (this) {
            copy = getCopy(this.mBiometrics);
        }
        return copy;
    }

    public void setAppBiometrics(int biometricId, CharSequence packagename, int userId) {
        synchronized (this) {
            int i = 0;
            while (true) {
                if (i >= this.mBiometrics.size()) {
                    break;
                } else if (this.mBiometrics.get(i).getBiometricId() != biometricId) {
                    i++;
                } else {
                    BiometricAuthenticator.Identifier identifier = this.mBiometrics.get(i);
                    identifier.setAppPkgName(packagename);
                    identifier.setSubUserId(userId);
                    scheduleWriteStateLocked();
                    break;
                }
            }
        }
    }

    public String getAppPackagename(int biometricId) {
        synchronized (this) {
            for (int i = 0; i < this.mBiometrics.size(); i++) {
                if (this.mBiometrics.get(i).getBiometricId() == biometricId) {
                    return this.mBiometrics.get(i).getAppPkgName().toString();
                }
            }
            return null;
        }
    }

    public boolean hasAppPackagename() {
        synchronized (this) {
            for (int i = 0; i < this.mBiometrics.size(); i++) {
                if (!this.mBiometrics.get(i).getAppPkgName().toString().equals("")) {
                    return true;
                }
            }
            return false;
        }
    }

    public int getAppUserId(int biometricId) {
        synchronized (this) {
            for (int i = 0; i < this.mBiometrics.size(); i++) {
                if (this.mBiometrics.get(i).getBiometricId() == biometricId) {
                    return this.mBiometrics.get(i).getSubUserId();
                }
            }
            return 0;
        }
    }

    public String getUniqueName() {
        int guess = 1;
        while (true) {
            String name = this.mContext.getString(getNameTemplateResource(), Integer.valueOf(guess));
            if (isUnique(name)) {
                return name;
            }
            guess++;
        }
    }

    public boolean isUnique(String name) {
        Iterator<T> it = this.mBiometrics.iterator();
        while (it.hasNext()) {
            T identifier = it.next();
            if (identifier.getName().equals(name)) {
                return false;
            }
        }
        return true;
    }

    private File getFileForUser(int userId, String fileName) {
        return new File(Environment.getUserSystemDirectory(userId), fileName);
    }

    private void scheduleWriteStateLocked() {
        AsyncTask.execute(this.mWriteStateRunnable);
    }

    private void readStateSyncLocked() {
        if (!this.mFile.exists()) {
            return;
        }
        try {
            FileInputStream in = new FileInputStream(this.mFile);
            try {
                try {
                    TypedXmlPullParser parser = Xml.resolvePullParser(in);
                    parseStateLocked(parser);
                } catch (IOException | XmlPullParserException e) {
                    throw new IllegalStateException("Failed parsing settings file: " + this.mFile, e);
                }
            } finally {
                IoUtils.closeQuietly(in);
            }
        } catch (FileNotFoundException e2) {
            Slog.i(TAG, "No fingerprint state");
        }
    }

    private void parseStateLocked(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(getBiometricsTag())) {
                            parseBiometricsLocked(parser);
                        } else if (tagName.equals(TAG_INVALIDATION)) {
                            this.mInvalidationInProgress = parser.getAttributeBoolean((String) null, ATTR_INVALIDATION);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    public T getAddBiometric() {
        return this.mBiometric;
    }
}
