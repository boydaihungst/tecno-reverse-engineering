package com.android.server.app;

import android.content.ComponentName;
import android.os.UserHandle;
import android.text.TextUtils;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class GameServiceConfiguration {
    private final GameServiceComponentConfiguration mGameServiceComponentConfiguration;
    private final String mPackageName;

    /* JADX INFO: Access modifiers changed from: package-private */
    public GameServiceConfiguration(String packageName, GameServiceComponentConfiguration gameServiceComponentConfiguration) {
        Objects.requireNonNull(packageName);
        this.mPackageName = packageName;
        this.mGameServiceComponentConfiguration = gameServiceComponentConfiguration;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public GameServiceComponentConfiguration getGameServiceComponentConfiguration() {
        return this.mGameServiceComponentConfiguration;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof GameServiceConfiguration) {
            GameServiceConfiguration that = (GameServiceConfiguration) o;
            return TextUtils.equals(this.mPackageName, that.mPackageName) && Objects.equals(this.mGameServiceComponentConfiguration, that.mGameServiceComponentConfiguration);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mPackageName, this.mGameServiceComponentConfiguration);
    }

    public String toString() {
        return "GameServiceConfiguration{packageName=" + this.mPackageName + ", gameServiceComponentConfiguration=" + this.mGameServiceComponentConfiguration + '}';
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class GameServiceComponentConfiguration {
        private final ComponentName mGameServiceComponentName;
        private final ComponentName mGameSessionServiceComponentName;
        private final UserHandle mUserHandle;

        /* JADX INFO: Access modifiers changed from: package-private */
        public GameServiceComponentConfiguration(UserHandle userHandle, ComponentName gameServiceComponentName, ComponentName gameSessionServiceComponentName) {
            Objects.requireNonNull(userHandle);
            Objects.requireNonNull(gameServiceComponentName);
            Objects.requireNonNull(gameSessionServiceComponentName);
            this.mUserHandle = userHandle;
            this.mGameServiceComponentName = gameServiceComponentName;
            this.mGameSessionServiceComponentName = gameSessionServiceComponentName;
        }

        public UserHandle getUserHandle() {
            return this.mUserHandle;
        }

        public ComponentName getGameServiceComponentName() {
            return this.mGameServiceComponentName;
        }

        public ComponentName getGameSessionServiceComponentName() {
            return this.mGameSessionServiceComponentName;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof GameServiceComponentConfiguration) {
                GameServiceComponentConfiguration that = (GameServiceComponentConfiguration) o;
                return this.mUserHandle.equals(that.mUserHandle) && this.mGameServiceComponentName.equals(that.mGameServiceComponentName) && this.mGameSessionServiceComponentName.equals(that.mGameSessionServiceComponentName);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.mUserHandle, this.mGameServiceComponentName, this.mGameSessionServiceComponentName);
        }

        public String toString() {
            return "GameServiceComponentConfiguration{userHandle=" + this.mUserHandle + ", gameServiceComponentName=" + this.mGameServiceComponentName + ", gameSessionServiceComponentName=" + this.mGameSessionServiceComponentName + "}";
        }
    }
}
