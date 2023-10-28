package com.android.server.timezonedetector.location;

import com.android.server.timezonedetector.GeolocationTimeZoneSuggestion;
import com.android.server.timezonedetector.location.LocationTimeZoneProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class LocationTimeZoneManagerServiceState {
    private final String mControllerState;
    private final List<String> mControllerStates;
    private final GeolocationTimeZoneSuggestion mLastSuggestion;
    private final List<LocationTimeZoneProvider.ProviderState> mPrimaryProviderStates;
    private final List<LocationTimeZoneProvider.ProviderState> mSecondaryProviderStates;

    LocationTimeZoneManagerServiceState(Builder builder) {
        this.mControllerState = builder.mControllerState;
        this.mLastSuggestion = builder.mLastSuggestion;
        this.mControllerStates = (List) Objects.requireNonNull(builder.mControllerStates);
        this.mPrimaryProviderStates = (List) Objects.requireNonNull(builder.mPrimaryProviderStates);
        this.mSecondaryProviderStates = (List) Objects.requireNonNull(builder.mSecondaryProviderStates);
    }

    public String getControllerState() {
        return this.mControllerState;
    }

    public GeolocationTimeZoneSuggestion getLastSuggestion() {
        return this.mLastSuggestion;
    }

    public List<String> getControllerStates() {
        return this.mControllerStates;
    }

    public List<LocationTimeZoneProvider.ProviderState> getPrimaryProviderStates() {
        return Collections.unmodifiableList(this.mPrimaryProviderStates);
    }

    public List<LocationTimeZoneProvider.ProviderState> getSecondaryProviderStates() {
        return Collections.unmodifiableList(this.mSecondaryProviderStates);
    }

    public String toString() {
        return "LocationTimeZoneManagerServiceState{mControllerState=" + this.mControllerState + ", mLastSuggestion=" + this.mLastSuggestion + ", mControllerStates=" + this.mControllerStates + ", mPrimaryProviderStates=" + this.mPrimaryProviderStates + ", mSecondaryProviderStates=" + this.mSecondaryProviderStates + '}';
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class Builder {
        private String mControllerState;
        private List<String> mControllerStates;
        private GeolocationTimeZoneSuggestion mLastSuggestion;
        private List<LocationTimeZoneProvider.ProviderState> mPrimaryProviderStates;
        private List<LocationTimeZoneProvider.ProviderState> mSecondaryProviderStates;

        public Builder setControllerState(String stateEnum) {
            this.mControllerState = stateEnum;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setLastSuggestion(GeolocationTimeZoneSuggestion lastSuggestion) {
            this.mLastSuggestion = (GeolocationTimeZoneSuggestion) Objects.requireNonNull(lastSuggestion);
            return this;
        }

        public Builder setStateChanges(List<String> states) {
            this.mControllerStates = new ArrayList(states);
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setPrimaryProviderStateChanges(List<LocationTimeZoneProvider.ProviderState> primaryProviderStates) {
            this.mPrimaryProviderStates = new ArrayList(primaryProviderStates);
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setSecondaryProviderStateChanges(List<LocationTimeZoneProvider.ProviderState> secondaryProviderStates) {
            this.mSecondaryProviderStates = new ArrayList(secondaryProviderStates);
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public LocationTimeZoneManagerServiceState build() {
            return new LocationTimeZoneManagerServiceState(this);
        }
    }
}
