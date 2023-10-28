package com.android.server.pm.verify.domain;

import android.content.pm.IntentFilterVerificationInfo;
import android.util.ArrayMap;
import android.util.SparseIntArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.server.pm.SettingsXml;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class DomainVerificationLegacySettings {
    public static final String ATTR_PACKAGE_NAME = "packageName";
    public static final String ATTR_STATE = "state";
    public static final String ATTR_USER_ID = "userId";
    public static final String TAG_DOMAIN_VERIFICATIONS_LEGACY = "domain-verifications-legacy";
    public static final String TAG_USER_STATE = "user-state";
    public static final String TAG_USER_STATES = "user-states";
    private final Object mLock = new Object();
    private final ArrayMap<String, LegacyState> mStates = new ArrayMap<>();

    public void add(String packageName, IntentFilterVerificationInfo info) {
        synchronized (this.mLock) {
            getOrCreateStateLocked(packageName).setInfo(info);
        }
    }

    public void add(String packageName, int userId, int state) {
        synchronized (this.mLock) {
            getOrCreateStateLocked(packageName).addUserState(userId, state);
        }
    }

    public int getUserState(String packageName, int userId) {
        synchronized (this.mLock) {
            LegacyState state = this.mStates.get(packageName);
            if (state != null) {
                return state.getUserState(userId);
            }
            return 0;
        }
    }

    public SparseIntArray getUserStates(String packageName) {
        synchronized (this.mLock) {
            LegacyState state = this.mStates.get(packageName);
            if (state != null) {
                return state.getUserStates();
            }
            return null;
        }
    }

    public IntentFilterVerificationInfo remove(String packageName) {
        synchronized (this.mLock) {
            LegacyState state = this.mStates.get(packageName);
            if (state != null && !state.isAttached()) {
                state.markAttached();
                return state.getInfo();
            }
            return null;
        }
    }

    private LegacyState getOrCreateStateLocked(String packageName) {
        LegacyState state = this.mStates.get(packageName);
        if (state == null) {
            LegacyState state2 = new LegacyState();
            this.mStates.put(packageName, state2);
            return state2;
        }
        return state;
    }

    public void writeSettings(TypedXmlSerializer xmlSerializer) throws IOException {
        SettingsXml.Serializer serializer = SettingsXml.serializer(xmlSerializer);
        try {
            SettingsXml.WriteSection ignored = serializer.startSection(TAG_DOMAIN_VERIFICATIONS_LEGACY);
            synchronized (this.mLock) {
                int statesSize = this.mStates.size();
                for (int stateIndex = 0; stateIndex < statesSize; stateIndex++) {
                    LegacyState state = this.mStates.valueAt(stateIndex);
                    SparseIntArray userStates = state.getUserStates();
                    if (userStates != null) {
                        String packageName = this.mStates.keyAt(stateIndex);
                        SettingsXml.WriteSection userStatesSection = serializer.startSection(TAG_USER_STATES).attribute(ATTR_PACKAGE_NAME, packageName);
                        int userStatesSize = userStates.size();
                        for (int userStateIndex = 0; userStateIndex < userStatesSize; userStateIndex++) {
                            int userId = userStates.keyAt(userStateIndex);
                            int userState = userStates.valueAt(userStateIndex);
                            userStatesSection.startSection("user-state").attribute("userId", userId).attribute("state", userState).finish();
                        }
                        if (userStatesSection != null) {
                            userStatesSection.close();
                        }
                    }
                }
            }
            if (ignored != null) {
                ignored.close();
            }
            if (serializer != null) {
                serializer.close();
            }
        } catch (Throwable th) {
            if (serializer != null) {
                try {
                    serializer.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public void readSettings(TypedXmlPullParser xmlParser) throws IOException, XmlPullParserException {
        SettingsXml.ChildSection child = SettingsXml.parser(xmlParser).children();
        while (child.moveToNext()) {
            if (TAG_USER_STATES.equals(child.getName())) {
                readUserStates(child);
            }
        }
    }

    private void readUserStates(SettingsXml.ReadSection section) {
        String packageName = section.getString(ATTR_PACKAGE_NAME);
        synchronized (this.mLock) {
            LegacyState legacyState = getOrCreateStateLocked(packageName);
            SettingsXml.ChildSection child = section.children();
            while (child.moveToNext()) {
                if ("user-state".equals(child.getName())) {
                    readUserState(child, legacyState);
                }
            }
        }
    }

    private void readUserState(SettingsXml.ReadSection section, LegacyState legacyState) {
        int userId = section.getInt("userId");
        int state = section.getInt("state");
        legacyState.addUserState(userId, state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class LegacyState {
        private boolean attached;
        private IntentFilterVerificationInfo mInfo;
        private SparseIntArray mUserStates;

        LegacyState() {
        }

        public IntentFilterVerificationInfo getInfo() {
            return this.mInfo;
        }

        public int getUserState(int userId) {
            return this.mUserStates.get(userId, 0);
        }

        public SparseIntArray getUserStates() {
            return this.mUserStates;
        }

        public void setInfo(IntentFilterVerificationInfo info) {
            this.mInfo = info;
        }

        public void addUserState(int userId, int state) {
            if (this.mUserStates == null) {
                this.mUserStates = new SparseIntArray(1);
            }
            this.mUserStates.put(userId, state);
        }

        public boolean isAttached() {
            return this.attached;
        }

        public void markAttached() {
            this.attached = true;
        }
    }
}
