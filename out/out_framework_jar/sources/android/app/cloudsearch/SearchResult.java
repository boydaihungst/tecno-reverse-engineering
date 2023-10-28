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
public final class SearchResult implements Parcelable {
    public static final Parcelable.Creator<SearchResult> CREATOR = new Parcelable.Creator<SearchResult>() { // from class: android.app.cloudsearch.SearchResult.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SearchResult createFromParcel(Parcel p) {
            return new SearchResult(p);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };
    public static final String EXTRAINFO_ACTION_APP_CARD = "android.app.cloudsearch.ACTION_APP_CARD";
    public static final String EXTRAINFO_ACTION_BUTTON_IMAGE_PREREGISTERING = "android.app.cloudsearch.ACTION_BUTTON_IMAGE";
    public static final String EXTRAINFO_ACTION_BUTTON_TEXT_PREREGISTERING = "android.app.cloudsearch.ACTION_BUTTON_TEXT";
    public static final String EXTRAINFO_ACTION_INSTALL_BUTTON = "android.app.cloudsearch.ACTION_INSTALL_BUTTON";
    public static final String EXTRAINFO_APP_BADGES = "android.app.cloudsearch.APP_BADGES";
    public static final String EXTRAINFO_APP_CONTAINS_ADS_DISCLAIMER = "android.app.cloudsearch.APP_CONTAINS_ADS_DISCLAIMER";
    public static final String EXTRAINFO_APP_CONTAINS_IAP_DISCLAIMER = "android.app.cloudsearch.APP_CONTAINS_IAP_DISCLAIMER";
    public static final String EXTRAINFO_APP_DEVELOPER_NAME = "android.app.cloudsearch.APP_DEVELOPER_NAME";
    public static final String EXTRAINFO_APP_DOMAIN_URL = "android.app.cloudsearch.APP_DOMAIN_URL";
    public static final String EXTRAINFO_APP_IARC = "android.app.cloudsearch.APP_IARC";
    public static final String EXTRAINFO_APP_ICON = "android.app.cloudsearch.APP_ICON";
    public static final String EXTRAINFO_APP_INSTALL_COUNT = "android.app.cloudsearch.APP_INSTALL_COUNT";
    public static final String EXTRAINFO_APP_PACKAGE_NAME = "android.app.cloudsearch.APP_PACKAGE_NAME";
    public static final String EXTRAINFO_APP_REVIEW_COUNT = "android.app.cloudsearch.APP_REVIEW_COUNT";
    public static final String EXTRAINFO_APP_SIZE_BYTES = "android.app.cloudsearch.APP_SIZE_BYTES";
    public static final String EXTRAINFO_APP_STAR_RATING = "android.app.cloudsearch.APP_STAR_RATING";
    public static final String EXTRAINFO_LONG_DESCRIPTION = "android.app.cloudsearch.LONG_DESCRIPTION";
    public static final String EXTRAINFO_SCREENSHOTS = "android.app.cloudsearch.SCREENSHOTS";
    public static final String EXTRAINFO_SHORT_DESCRIPTION = "android.app.cloudsearch.SHORT_DESCRIPTION";
    public static final String EXTRAINFO_WEB_ICON = "android.app.cloudsearch.WEB_ICON";
    public static final String EXTRAINFO_WEB_URL = "android.app.cloudsearch.WEB_URL";
    private Bundle mExtraInfos;
    private final float mScore;
    private final String mSnippet;
    private final String mTitle;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface SearchResultExtraInfoKey {
    }

    private SearchResult(Parcel in) {
        this.mTitle = in.readString();
        this.mSnippet = in.readString();
        this.mScore = in.readFloat();
        this.mExtraInfos = in.readBundle();
    }

    private SearchResult(String title, String snippet, float score, Bundle extraInfos) {
        this.mTitle = title;
        this.mSnippet = snippet;
        this.mScore = score;
        this.mExtraInfos = extraInfos;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public String getSnippet() {
        return this.mSnippet;
    }

    public float getScore() {
        return this.mScore;
    }

    public Bundle getExtraInfos() {
        return this.mExtraInfos;
    }

    private SearchResult(Builder b) {
        this.mTitle = (String) Objects.requireNonNull(b.mTitle);
        this.mSnippet = (String) Objects.requireNonNull(b.mSnippet);
        this.mScore = b.mScore;
        this.mExtraInfos = (Bundle) Objects.requireNonNull(b.mExtraInfos);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mTitle);
        dest.writeString(this.mSnippet);
        dest.writeFloat(this.mScore);
        dest.writeBundle(this.mExtraInfos);
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
        SearchResult that = (SearchResult) obj;
        if (Objects.equals(this.mTitle, that.mTitle) && Objects.equals(this.mSnippet, that.mSnippet) && this.mScore == that.mScore && Objects.equals(this.mExtraInfos, that.mExtraInfos)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mTitle, this.mSnippet, Float.valueOf(this.mScore), this.mExtraInfos);
    }

    @SystemApi
    /* loaded from: classes.dex */
    public static final class Builder {
        private Bundle mExtraInfos;
        private String mTitle;
        private String mSnippet = "";
        private float mScore = 0.0f;

        @SystemApi
        public Builder(String title, Bundle extraInfos) {
            this.mTitle = title;
            this.mExtraInfos = extraInfos;
        }

        public Builder setTitle(String title) {
            this.mTitle = title;
            return this;
        }

        public Builder setSnippet(String snippet) {
            this.mSnippet = snippet;
            return this;
        }

        public Builder setScore(float score) {
            this.mScore = score;
            return this;
        }

        public Builder setExtraInfos(Bundle extraInfos) {
            this.mExtraInfos = extraInfos;
            return this;
        }

        public SearchResult build() {
            if (this.mTitle == null || this.mExtraInfos == null || this.mSnippet == null) {
                throw new IllegalStateException("Please make sure all required args are assigned.");
            }
            return new SearchResult(this.mTitle, this.mSnippet, this.mScore, this.mExtraInfos);
        }
    }
}
