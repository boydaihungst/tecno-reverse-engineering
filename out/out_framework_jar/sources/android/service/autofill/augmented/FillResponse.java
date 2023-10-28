package android.service.autofill.augmented;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.service.autofill.Dataset;
import java.util.ArrayList;
import java.util.List;
@SystemApi
/* loaded from: classes3.dex */
public final class FillResponse {
    private Bundle mClientState;
    private FillWindow mFillWindow;
    private List<Dataset> mInlineSuggestions;

    /* renamed from: -$$Nest$smdefaultClientState  reason: not valid java name */
    static /* bridge */ /* synthetic */ Bundle m3450$$Nest$smdefaultClientState() {
        return defaultClientState();
    }

    /* renamed from: -$$Nest$smdefaultFillWindow  reason: not valid java name */
    static /* bridge */ /* synthetic */ FillWindow m3451$$Nest$smdefaultFillWindow() {
        return defaultFillWindow();
    }

    /* renamed from: -$$Nest$smdefaultInlineSuggestions  reason: not valid java name */
    static /* bridge */ /* synthetic */ List m3452$$Nest$smdefaultInlineSuggestions() {
        return defaultInlineSuggestions();
    }

    private static FillWindow defaultFillWindow() {
        return null;
    }

    private static List<Dataset> defaultInlineSuggestions() {
        return null;
    }

    private static Bundle defaultClientState() {
        return null;
    }

    /* loaded from: classes3.dex */
    static abstract class BaseBuilder {
        abstract Builder addInlineSuggestion(Dataset dataset);

        BaseBuilder() {
        }
    }

    FillResponse(FillWindow fillWindow, List<Dataset> inlineSuggestions, Bundle clientState) {
        this.mFillWindow = fillWindow;
        this.mInlineSuggestions = inlineSuggestions;
        this.mClientState = clientState;
    }

    public FillWindow getFillWindow() {
        return this.mFillWindow;
    }

    public List<Dataset> getInlineSuggestions() {
        return this.mInlineSuggestions;
    }

    public Bundle getClientState() {
        return this.mClientState;
    }

    /* loaded from: classes3.dex */
    public static final class Builder extends BaseBuilder {
        private long mBuilderFieldsSet = 0;
        private Bundle mClientState;
        private FillWindow mFillWindow;
        private List<Dataset> mInlineSuggestions;

        public Builder setFillWindow(FillWindow value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 1;
            this.mFillWindow = value;
            return this;
        }

        public Builder setInlineSuggestions(List<Dataset> value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 2;
            this.mInlineSuggestions = value;
            return this;
        }

        @Override // android.service.autofill.augmented.FillResponse.BaseBuilder
        Builder addInlineSuggestion(Dataset value) {
            if (this.mInlineSuggestions == null) {
                setInlineSuggestions(new ArrayList());
            }
            this.mInlineSuggestions.add(value);
            return this;
        }

        public Builder setClientState(Bundle value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 4;
            this.mClientState = value;
            return this;
        }

        public FillResponse build() {
            checkNotUsed();
            long j = this.mBuilderFieldsSet | 8;
            this.mBuilderFieldsSet = j;
            if ((j & 1) == 0) {
                this.mFillWindow = FillResponse.m3451$$Nest$smdefaultFillWindow();
            }
            if ((this.mBuilderFieldsSet & 2) == 0) {
                this.mInlineSuggestions = FillResponse.m3452$$Nest$smdefaultInlineSuggestions();
            }
            if ((this.mBuilderFieldsSet & 4) == 0) {
                this.mClientState = FillResponse.m3450$$Nest$smdefaultClientState();
            }
            FillResponse o = new FillResponse(this.mFillWindow, this.mInlineSuggestions, this.mClientState);
            return o;
        }

        private void checkNotUsed() {
            if ((this.mBuilderFieldsSet & 8) != 0) {
                throw new IllegalStateException("This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @Deprecated
    private void __metadata() {
    }
}
