package android.app.cloudsearch;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
@SystemApi
/* loaded from: classes.dex */
public final class SearchResponse implements Parcelable {
    public static final Parcelable.Creator<SearchResponse> CREATOR = new Parcelable.Creator<SearchResponse>() { // from class: android.app.cloudsearch.SearchResponse.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SearchResponse createFromParcel(Parcel p) {
            return new SearchResponse(p);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SearchResponse[] newArray(int size) {
            return new SearchResponse[size];
        }
    };
    public static final int SEARCH_STATUS_NO_INTERNET = 2;
    public static final int SEARCH_STATUS_OK = 0;
    public static final int SEARCH_STATUS_TIME_OUT = 1;
    public static final int SEARCH_STATUS_UNKNOWN = -1;
    private final List<SearchResult> mSearchResults;
    private String mSource;
    private final int mStatusCode;

    /* loaded from: classes.dex */
    public @interface SearchStatusCode {
    }

    private SearchResponse(Parcel in) {
        this.mStatusCode = in.readInt();
        this.mSource = in.readString();
        this.mSearchResults = in.createTypedArrayList(SearchResult.CREATOR);
    }

    private SearchResponse(int statusCode, String source, List<SearchResult> searchResults) {
        this.mStatusCode = statusCode;
        this.mSource = source;
        this.mSearchResults = searchResults;
    }

    public int getStatusCode() {
        return this.mStatusCode;
    }

    public String getSource() {
        return this.mSource;
    }

    public List<SearchResult> getSearchResults() {
        return this.mSearchResults;
    }

    public void setSource(String source) {
        this.mSource = source;
    }

    private SearchResponse(Builder b) {
        this.mStatusCode = b.mStatusCode;
        this.mSource = (String) Objects.requireNonNull(b.mSource);
        this.mSearchResults = (List) Objects.requireNonNull(b.mSearchResults);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mStatusCode);
        dest.writeString(this.mSource);
        dest.writeTypedList(this.mSearchResults);
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
        SearchResponse that = (SearchResponse) obj;
        if (this.mStatusCode == that.mStatusCode && Objects.equals(this.mSource, that.mSource) && Objects.equals(this.mSearchResults, that.mSearchResults)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mStatusCode), this.mSource, this.mSearchResults);
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class Builder {
        private int mStatusCode;
        private String mSource = "DEFAULT";
        private List<SearchResult> mSearchResults = new ArrayList();

        @SystemApi
        public Builder(int statusCode) {
            this.mStatusCode = statusCode;
        }

        public Builder setStatusCode(int statusCode) {
            this.mStatusCode = statusCode;
            return this;
        }

        public Builder setSource(String source) {
            this.mSource = source;
            return this;
        }

        public Builder setSearchResults(List<SearchResult> searchResults) {
            this.mSearchResults = searchResults;
            return this;
        }

        public SearchResponse build() {
            int i = this.mStatusCode;
            if (i < -1 || i > 2 || this.mSearchResults == null) {
                throw new IllegalStateException("Please make sure all @NonNull args are assigned.");
            }
            return new SearchResponse(this.mStatusCode, this.mSource, this.mSearchResults);
        }
    }
}
