package com.android.server.app;

import android.content.ComponentName;
import android.service.games.IGameSession;
import android.view.SurfaceControlViewHost;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class GameSessionRecord {
    private final IGameSession mIGameSession;
    private final ComponentName mRootComponentName;
    private final State mState;
    private final SurfaceControlViewHost.SurfacePackage mSurfacePackage;
    private final int mTaskId;

    /* loaded from: classes.dex */
    private enum State {
        NO_GAME_SESSION_REQUESTED,
        GAME_SESSION_REQUESTED,
        GAME_SESSION_ATTACHED,
        GAME_SESSION_ENDED_PROCESS_DEATH
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static GameSessionRecord awaitingGameSessionRequest(int taskId, ComponentName rootComponentName) {
        return new GameSessionRecord(taskId, State.NO_GAME_SESSION_REQUESTED, rootComponentName, null, null);
    }

    private GameSessionRecord(int taskId, State state, ComponentName rootComponentName, IGameSession gameSession, SurfaceControlViewHost.SurfacePackage surfacePackage) {
        this.mTaskId = taskId;
        this.mState = state;
        this.mRootComponentName = rootComponentName;
        this.mIGameSession = gameSession;
        this.mSurfacePackage = surfacePackage;
    }

    public boolean isAwaitingGameSessionRequest() {
        return this.mState == State.NO_GAME_SESSION_REQUESTED;
    }

    public GameSessionRecord withGameSessionRequested() {
        return new GameSessionRecord(this.mTaskId, State.GAME_SESSION_REQUESTED, this.mRootComponentName, null, null);
    }

    public boolean isGameSessionRequested() {
        return this.mState == State.GAME_SESSION_REQUESTED;
    }

    public GameSessionRecord withGameSession(IGameSession gameSession, SurfaceControlViewHost.SurfacePackage surfacePackage) {
        Objects.requireNonNull(gameSession);
        return new GameSessionRecord(this.mTaskId, State.GAME_SESSION_ATTACHED, this.mRootComponentName, gameSession, surfacePackage);
    }

    public GameSessionRecord withGameSessionEndedOnProcessDeath() {
        return new GameSessionRecord(this.mTaskId, State.GAME_SESSION_ENDED_PROCESS_DEATH, this.mRootComponentName, null, null);
    }

    public boolean isGameSessionEndedForProcessDeath() {
        return this.mState == State.GAME_SESSION_ENDED_PROCESS_DEATH;
    }

    public int getTaskId() {
        return this.mTaskId;
    }

    public ComponentName getComponentName() {
        return this.mRootComponentName;
    }

    public IGameSession getGameSession() {
        return this.mIGameSession;
    }

    public SurfaceControlViewHost.SurfacePackage getSurfacePackage() {
        return this.mSurfacePackage;
    }

    public String toString() {
        return "GameSessionRecord{mTaskId=" + this.mTaskId + ", mState=" + this.mState + ", mRootComponentName=" + this.mRootComponentName + ", mIGameSession=" + this.mIGameSession + ", mSurfacePackage=" + this.mSurfacePackage + '}';
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof GameSessionRecord) {
            GameSessionRecord that = (GameSessionRecord) o;
            return this.mTaskId == that.mTaskId && this.mState == that.mState && this.mRootComponentName.equals(that.mRootComponentName) && Objects.equals(this.mIGameSession, that.mIGameSession) && Objects.equals(this.mSurfacePackage, that.mSurfacePackage);
        }
        return false;
    }

    public int hashCode() {
        State state = this.mState;
        return Objects.hash(Integer.valueOf(this.mTaskId), state, this.mRootComponentName, this.mIGameSession, state, this.mSurfacePackage);
    }
}
