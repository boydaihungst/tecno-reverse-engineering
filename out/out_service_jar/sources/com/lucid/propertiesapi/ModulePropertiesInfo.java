package com.lucid.propertiesapi;
/* loaded from: classes2.dex */
public class ModulePropertiesInfo {
    private Mode mMode = Mode.MANUAL_MODE;
    private State mState = State.NO_SAVE;
    private int mEnabled = -1;
    private int mParam = 0;

    public void setMode(Mode mode) {
        this.mMode = mode;
    }

    public void setState(State state) {
        this.mState = state;
    }

    public Mode getMode() {
        return this.mMode;
    }

    public State getState() {
        return this.mState;
    }

    public int getModeValue() {
        return this.mMode.getValue();
    }

    public int getStateValue() {
        return this.mState.getValue();
    }

    /* loaded from: classes2.dex */
    public enum Mode {
        MANUAL_MODE(0),
        AUTOMATIC_MODE(1),
        OVERRIDE_MODE(2);
        
        private int value;

        Mode(int i) {
            this.value = i;
        }

        public int getValue() {
            return this.value;
        }
    }

    /* loaded from: classes2.dex */
    public enum State {
        NO_SAVE(0),
        NORMAL_SAVE(2),
        HIGH_SAVE(3),
        ULTRA_SAVE(4);
        
        private int value;

        State(int i) {
            this.value = i;
        }

        public int getValue() {
            return this.value;
        }
    }

    public void setEnabled(int enabled) {
        this.mEnabled = enabled;
    }

    public int getEnabled() {
        return this.mEnabled;
    }

    public void setParam(int param) {
        this.mParam = param;
    }

    public int getParam() {
        return this.mParam;
    }
}
