package android.app.cloudsearch;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
@SystemApi
/* loaded from: classes.dex */
public final class SearchRequest implements Parcelable {
    public static final String CONSTRAINT_IS_PRESUBMIT_SUGGESTION = "android.app.cloudsearch.IS_PRESUBMIT_SUGGESTION";
    public static final String CONSTRAINT_SEARCH_PROVIDER_FILTER = "android.app.cloudsearch.SEARCH_PROVIDER_FILTER";
    public static final Parcelable.Creator<SearchRequest> CREATOR = new Parcelable.Creator<SearchRequest>() { // from class: android.app.cloudsearch.SearchRequest.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SearchRequest createFromParcel(Parcel p) {
            return new SearchRequest(p);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SearchRequest[] newArray(int size) {
            return new SearchRequest[size];
        }
    };
    private String mCallerPackageName;
    private String mId;
    private final float mMaxLatencyMillis;
    private final String mQuery;
    private final int mResultNumber;
    private final int mResultOffset;
    private Bundle mSearchConstraints;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface SearchConstraintKey {
    }

    private SearchRequest(Parcel in) {
        this.mId = null;
        this.mQuery = in.readString();
        this.mResultOffset = in.readInt();
        this.mResultNumber = in.readInt();
        this.mMaxLatencyMillis = in.readFloat();
        this.mSearchConstraints = in.readBundle();
        this.mId = in.readString();
        this.mCallerPackageName = in.readString();
    }

    private SearchRequest(String query, int resultOffset, int resultNumber, float maxLatencyMillis, Bundle searchConstraints, String callerPackageName) {
        this.mId = null;
        this.mQuery = query;
        this.mResultOffset = resultOffset;
        this.mResultNumber = resultNumber;
        this.mMaxLatencyMillis = maxLatencyMillis;
        this.mSearchConstraints = searchConstraints;
        this.mCallerPackageName = callerPackageName;
    }

    public String getQuery() {
        return this.mQuery;
    }

    public int getResultOffset() {
        return this.mResultOffset;
    }

    public int getResultNumber() {
        return this.mResultNumber;
    }

    public float getMaxLatencyMillis() {
        return this.mMaxLatencyMillis;
    }

    public Bundle getSearchConstraints() {
        return this.mSearchConstraints;
    }

    public String getCallerPackageName() {
        return this.mCallerPackageName;
    }

    public String getRequestId() {
        String str = this.mId;
        if (str == null || str.length() == 0) {
            this.mId = String.valueOf(toString().hashCode());
        }
        return this.mId;
    }

    public void setCallerPackageName(String callerPackageName) {
        this.mCallerPackageName = callerPackageName;
    }

    private SearchRequest(Builder b) {
        this.mId = null;
        this.mQuery = (String) Objects.requireNonNull(b.mQuery);
        this.mResultOffset = b.mResultOffset;
        this.mResultNumber = b.mResultNumber;
        this.mMaxLatencyMillis = b.mMaxLatencyMillis;
        this.mSearchConstraints = (Bundle) Objects.requireNonNull(b.mSearchConstraints);
        this.mCallerPackageName = (String) Objects.requireNonNull(b.mCallerPackageName);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mQuery);
        dest.writeInt(this.mResultOffset);
        dest.writeInt(this.mResultNumber);
        dest.writeFloat(this.mMaxLatencyMillis);
        dest.writeBundle(this.mSearchConstraints);
        dest.writeString(getRequestId());
        dest.writeString(this.mCallerPackageName);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SearchRequest that = (SearchRequest) obj;
        if (Objects.equals(this.mQuery, that.mQuery) && this.mResultOffset == that.mResultOffset && this.mResultNumber == that.mResultNumber && this.mMaxLatencyMillis == that.mMaxLatencyMillis && Objects.equals(this.mSearchConstraints, that.mSearchConstraints) && Objects.equals(this.mCallerPackageName, that.mCallerPackageName)) {
            return true;
        }
        return false;
    }

    public String toString() {
        boolean isPresubmit = this.mSearchConstraints.containsKey(CONSTRAINT_IS_PRESUBMIT_SUGGESTION) && this.mSearchConstraints.getBoolean(CONSTRAINT_IS_PRESUBMIT_SUGGESTION);
        String searchProvider = "EMPTY";
        if (this.mSearchConstraints.containsKey(CONSTRAINT_SEARCH_PROVIDER_FILTER)) {
            searchProvider = this.mSearchConstraints.getString(CONSTRAINT_SEARCH_PROVIDER_FILTER);
        }
        return String.format("SearchRequest: {query:%s,offset:%d;number:%d;max_latency:%f;is_presubmit:%b;search_provider:%s;callerPackageName:%s}", this.mQuery, Integer.valueOf(this.mResultOffset), Integer.valueOf(this.mResultNumber), Float.valueOf(this.mMaxLatencyMillis), Boolean.valueOf(isPresubmit), searchProvider, this.mCallerPackageName);
    }

    public int hashCode() {
        return Objects.hash(this.mQuery, Integer.valueOf(this.mResultOffset), Integer.valueOf(this.mResultNumber), Float.valueOf(this.mMaxLatencyMillis), this.mSearchConstraints, this.mCallerPackageName);
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class Builder {
        private String mQuery;
        private int mResultOffset = 0;
        private int mResultNumber = 10;
        private float mMaxLatencyMillis = 200.0f;
        private Bundle mSearchConstraints = Bundle.EMPTY;
        private String mCallerPackageName = "DEFAULT_CALLER";

        @SystemApi
        public Builder(String query) {
            this.mQuery = query;
        }

        public Builder setQuery(String query) {
            this.mQuery = query;
            return this;
        }

        public Builder setResultOffset(int resultOffset) {
            this.mResultOffset = resultOffset;
            return this;
        }

        public Builder setResultNumber(int resultNumber) {
            this.mResultNumber = resultNumber;
            return this;
        }

        public Builder setMaxLatencyMillis(float maxLatencyMillis) {
            this.mMaxLatencyMillis = maxLatencyMillis;
            return this;
        }

        public Builder setSearchConstraints(Bundle searchConstraints) {
            this.mSearchConstraints = searchConstraints;
            return this;
        }

        public Builder setCallerPackageName(String callerPackageName) {
            this.mCallerPackageName = callerPackageName;
            return this;
        }

        public SearchRequest build() {
            if (this.mQuery == null || this.mResultOffset < 0 || this.mResultNumber < 1 || this.mMaxLatencyMillis < 0.0f || this.mSearchConstraints == null) {
                throw new IllegalStateException("Please make sure all required args are valid.");
            }
            return new SearchRequest(this.mQuery, this.mResultOffset, this.mResultNumber, this.mMaxLatencyMillis, this.mSearchConstraints, this.mCallerPackageName);
        }
    }
}
