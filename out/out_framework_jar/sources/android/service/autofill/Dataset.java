package android.service.autofill;

import android.annotation.SystemApi;
import android.content.ClipData;
import android.content.IntentSender;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.autofill.Presentations;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.autofill.Helper;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;
/* loaded from: classes3.dex */
public final class Dataset implements Parcelable {
    public static final Parcelable.Creator<Dataset> CREATOR = new Parcelable.Creator<Dataset>() { // from class: android.service.autofill.Dataset.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Dataset createFromParcel(Parcel parcel) {
            Presentations.Builder presentationsBuilder;
            Builder builder;
            RemoteViews presentation = (RemoteViews) parcel.readParcelable(null, RemoteViews.class);
            RemoteViews dialogPresentation = (RemoteViews) parcel.readParcelable(null, RemoteViews.class);
            InlinePresentation inlinePresentation = (InlinePresentation) parcel.readParcelable(null, InlinePresentation.class);
            InlinePresentation inlineTooltipPresentation = (InlinePresentation) parcel.readParcelable(null, InlinePresentation.class);
            ArrayList<AutofillId> ids = parcel.createTypedArrayList(AutofillId.CREATOR);
            ArrayList<AutofillValue> values = parcel.createTypedArrayList(AutofillValue.CREATOR);
            ArrayList<RemoteViews> presentations = parcel.createTypedArrayList(RemoteViews.CREATOR);
            ArrayList<RemoteViews> dialogPresentations = parcel.createTypedArrayList(RemoteViews.CREATOR);
            ArrayList<InlinePresentation> inlinePresentations = parcel.createTypedArrayList(InlinePresentation.CREATOR);
            ArrayList<InlinePresentation> inlineTooltipPresentations = parcel.createTypedArrayList(InlinePresentation.CREATOR);
            ArrayList<DatasetFieldFilter> filters = parcel.createTypedArrayList(DatasetFieldFilter.CREATOR);
            ClipData fieldContent = (ClipData) parcel.readParcelable(null, ClipData.class);
            IntentSender authentication = (IntentSender) parcel.readParcelable(null, IntentSender.class);
            String datasetId = parcel.readString();
            if (presentation != null || inlinePresentation != null || dialogPresentation != null) {
                Presentations.Builder presentationsBuilder2 = new Presentations.Builder();
                if (presentation == null) {
                    presentationsBuilder = presentationsBuilder2;
                } else {
                    presentationsBuilder = presentationsBuilder2;
                    presentationsBuilder.setMenuPresentation(presentation);
                }
                if (inlinePresentation != null) {
                    presentationsBuilder.setInlinePresentation(inlinePresentation);
                }
                if (inlineTooltipPresentation != null) {
                    presentationsBuilder.setInlineTooltipPresentation(inlineTooltipPresentation);
                }
                if (dialogPresentation != null) {
                    presentationsBuilder.setDialogPresentation(dialogPresentation);
                }
                builder = new Builder(presentationsBuilder.build());
            } else {
                builder = new Builder();
            }
            if (fieldContent != null) {
                builder.setContent(ids.get(0), fieldContent);
            }
            int inlinePresentationsSize = inlinePresentations.size();
            int i = 0;
            while (true) {
                RemoteViews dialogPresentation2 = dialogPresentation;
                if (i < ids.size()) {
                    AutofillId id = ids.get(i);
                    AutofillValue value = values.get(i);
                    RemoteViews fieldPresentation = presentations.get(i);
                    RemoteViews fieldDialogPresentation = dialogPresentations.get(i);
                    InlinePresentation fieldInlinePresentation = i < inlinePresentationsSize ? inlinePresentations.get(i) : null;
                    InlinePresentation fieldInlineTooltipPresentation = i < inlinePresentationsSize ? inlineTooltipPresentations.get(i) : null;
                    DatasetFieldFilter filter = filters.get(i);
                    builder.setLifeTheUniverseAndEverything(id, value, fieldPresentation, fieldInlinePresentation, fieldInlineTooltipPresentation, filter, fieldDialogPresentation);
                    i++;
                    dialogPresentation = dialogPresentation2;
                } else {
                    builder.setAuthentication(authentication);
                    builder.setId(datasetId);
                    return builder.build();
                }
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Dataset[] newArray(int size) {
            return new Dataset[size];
        }
    };
    private final IntentSender mAuthentication;
    private final RemoteViews mDialogPresentation;
    private final ClipData mFieldContent;
    private final ArrayList<RemoteViews> mFieldDialogPresentations;
    private final ArrayList<DatasetFieldFilter> mFieldFilters;
    private final ArrayList<AutofillId> mFieldIds;
    private final ArrayList<InlinePresentation> mFieldInlinePresentations;
    private final ArrayList<InlinePresentation> mFieldInlineTooltipPresentations;
    private final ArrayList<RemoteViews> mFieldPresentations;
    private final ArrayList<AutofillValue> mFieldValues;
    String mId;
    private final InlinePresentation mInlinePresentation;
    private final InlinePresentation mInlineTooltipPresentation;
    private final RemoteViews mPresentation;

    private Dataset(Builder builder) {
        this.mFieldIds = builder.mFieldIds;
        this.mFieldValues = builder.mFieldValues;
        this.mFieldPresentations = builder.mFieldPresentations;
        this.mFieldDialogPresentations = builder.mFieldDialogPresentations;
        this.mFieldInlinePresentations = builder.mFieldInlinePresentations;
        this.mFieldInlineTooltipPresentations = builder.mFieldInlineTooltipPresentations;
        this.mFieldFilters = builder.mFieldFilters;
        this.mFieldContent = builder.mFieldContent;
        this.mPresentation = builder.mPresentation;
        this.mDialogPresentation = builder.mDialogPresentation;
        this.mInlinePresentation = builder.mInlinePresentation;
        this.mInlineTooltipPresentation = builder.mInlineTooltipPresentation;
        this.mAuthentication = builder.mAuthentication;
        this.mId = builder.mId;
    }

    public ArrayList<AutofillId> getFieldIds() {
        return this.mFieldIds;
    }

    public ArrayList<AutofillValue> getFieldValues() {
        return this.mFieldValues;
    }

    public RemoteViews getFieldPresentation(int index) {
        RemoteViews customPresentation = this.mFieldPresentations.get(index);
        return customPresentation != null ? customPresentation : this.mPresentation;
    }

    public RemoteViews getFieldDialogPresentation(int index) {
        RemoteViews customPresentation = this.mFieldDialogPresentations.get(index);
        return customPresentation != null ? customPresentation : this.mDialogPresentation;
    }

    public InlinePresentation getFieldInlinePresentation(int index) {
        InlinePresentation inlinePresentation = this.mFieldInlinePresentations.get(index);
        return inlinePresentation != null ? inlinePresentation : this.mInlinePresentation;
    }

    public InlinePresentation getFieldInlineTooltipPresentation(int index) {
        InlinePresentation inlineTooltipPresentation = this.mFieldInlineTooltipPresentations.get(index);
        return inlineTooltipPresentation != null ? inlineTooltipPresentation : this.mInlineTooltipPresentation;
    }

    public DatasetFieldFilter getFilter(int index) {
        return this.mFieldFilters.get(index);
    }

    public ClipData getFieldContent() {
        return this.mFieldContent;
    }

    public IntentSender getAuthentication() {
        return this.mAuthentication;
    }

    public boolean isEmpty() {
        ArrayList<AutofillId> arrayList = this.mFieldIds;
        return arrayList == null || arrayList.isEmpty();
    }

    public String toString() {
        if (Helper.sDebug) {
            StringBuilder builder = new StringBuilder("Dataset[");
            if (this.mId == null) {
                builder.append("noId");
            } else {
                builder.append("id=").append(this.mId.length()).append("_chars");
            }
            if (this.mFieldIds != null) {
                builder.append(", fieldIds=").append(this.mFieldIds);
            }
            if (this.mFieldValues != null) {
                builder.append(", fieldValues=").append(this.mFieldValues);
            }
            if (this.mFieldContent != null) {
                builder.append(", fieldContent=").append(this.mFieldContent);
            }
            if (this.mFieldPresentations != null) {
                builder.append(", fieldPresentations=").append(this.mFieldPresentations.size());
            }
            if (this.mFieldDialogPresentations != null) {
                builder.append(", fieldDialogPresentations=").append(this.mFieldDialogPresentations.size());
            }
            if (this.mFieldInlinePresentations != null) {
                builder.append(", fieldInlinePresentations=").append(this.mFieldInlinePresentations.size());
            }
            if (this.mFieldInlineTooltipPresentations != null) {
                builder.append(", fieldInlineTooltipInlinePresentations=").append(this.mFieldInlineTooltipPresentations.size());
            }
            if (this.mFieldFilters != null) {
                builder.append(", fieldFilters=").append(this.mFieldFilters.size());
            }
            if (this.mPresentation != null) {
                builder.append(", hasPresentation");
            }
            if (this.mDialogPresentation != null) {
                builder.append(", hasDialogPresentation");
            }
            if (this.mInlinePresentation != null) {
                builder.append(", hasInlinePresentation");
            }
            if (this.mInlineTooltipPresentation != null) {
                builder.append(", hasInlineTooltipPresentation");
            }
            if (this.mAuthentication != null) {
                builder.append(", hasAuthentication");
            }
            return builder.append(']').toString();
        }
        return super.toString();
    }

    public String getId() {
        return this.mId;
    }

    /* loaded from: classes3.dex */
    public static final class Builder {
        private IntentSender mAuthentication;
        private boolean mDestroyed;
        private RemoteViews mDialogPresentation;
        private ClipData mFieldContent;
        private ArrayList<RemoteViews> mFieldDialogPresentations;
        private ArrayList<DatasetFieldFilter> mFieldFilters;
        private ArrayList<AutofillId> mFieldIds;
        private ArrayList<InlinePresentation> mFieldInlinePresentations;
        private ArrayList<InlinePresentation> mFieldInlineTooltipPresentations;
        private ArrayList<RemoteViews> mFieldPresentations;
        private ArrayList<AutofillValue> mFieldValues;
        private String mId;
        private InlinePresentation mInlinePresentation;
        private InlinePresentation mInlineTooltipPresentation;
        private RemoteViews mPresentation;

        @Deprecated
        public Builder(RemoteViews presentation) {
            Objects.requireNonNull(presentation, "presentation must be non-null");
            this.mPresentation = presentation;
        }

        @SystemApi
        @Deprecated
        public Builder(InlinePresentation inlinePresentation) {
            Objects.requireNonNull(inlinePresentation, "inlinePresentation must be non-null");
            this.mInlinePresentation = inlinePresentation;
        }

        public Builder(Presentations presentations) {
            Objects.requireNonNull(presentations, "presentations must be non-null");
            this.mPresentation = presentations.getMenuPresentation();
            this.mInlinePresentation = presentations.getInlinePresentation();
            this.mInlineTooltipPresentation = presentations.getInlineTooltipPresentation();
            this.mDialogPresentation = presentations.getDialogPresentation();
        }

        public Builder() {
        }

        @Deprecated
        public Builder setInlinePresentation(InlinePresentation inlinePresentation) {
            throwIfDestroyed();
            Objects.requireNonNull(inlinePresentation, "inlinePresentation must be non-null");
            this.mInlinePresentation = inlinePresentation;
            return this;
        }

        @Deprecated
        public Builder setInlinePresentation(InlinePresentation inlinePresentation, InlinePresentation inlineTooltipPresentation) {
            throwIfDestroyed();
            Objects.requireNonNull(inlinePresentation, "inlinePresentation must be non-null");
            Objects.requireNonNull(inlineTooltipPresentation, "inlineTooltipPresentation must be non-null");
            this.mInlinePresentation = inlinePresentation;
            this.mInlineTooltipPresentation = inlineTooltipPresentation;
            return this;
        }

        public Builder setAuthentication(IntentSender authentication) {
            throwIfDestroyed();
            this.mAuthentication = authentication;
            return this;
        }

        public Builder setId(String id) {
            throwIfDestroyed();
            this.mId = id;
            return this;
        }

        @SystemApi
        public Builder setContent(AutofillId id, ClipData content) {
            throwIfDestroyed();
            if (content != null) {
                for (int i = 0; i < content.getItemCount(); i++) {
                    Preconditions.checkArgument(content.getItemAt(i).getIntent() == null, "Content items cannot contain an Intent: content=" + content);
                }
            }
            setLifeTheUniverseAndEverything(id, null, null, null, null, null, null);
            this.mFieldContent = content;
            return this;
        }

        @Deprecated
        public Builder setValue(AutofillId id, AutofillValue value) {
            throwIfDestroyed();
            setLifeTheUniverseAndEverything(id, value, null, null, null, null, null);
            return this;
        }

        @Deprecated
        public Builder setValue(AutofillId id, AutofillValue value, RemoteViews presentation) {
            throwIfDestroyed();
            Objects.requireNonNull(presentation, "presentation cannot be null");
            setLifeTheUniverseAndEverything(id, value, presentation, null, null, null, null);
            return this;
        }

        @Deprecated
        public Builder setValue(AutofillId id, AutofillValue value, Pattern filter) {
            throwIfDestroyed();
            Preconditions.checkState(this.mPresentation != null, "Dataset presentation not set on constructor");
            setLifeTheUniverseAndEverything(id, value, null, null, null, new DatasetFieldFilter(filter), null);
            return this;
        }

        @Deprecated
        public Builder setValue(AutofillId id, AutofillValue value, Pattern filter, RemoteViews presentation) {
            throwIfDestroyed();
            Objects.requireNonNull(presentation, "presentation cannot be null");
            setLifeTheUniverseAndEverything(id, value, presentation, null, null, new DatasetFieldFilter(filter), null);
            return this;
        }

        @Deprecated
        public Builder setValue(AutofillId id, AutofillValue value, RemoteViews presentation, InlinePresentation inlinePresentation) {
            throwIfDestroyed();
            Objects.requireNonNull(presentation, "presentation cannot be null");
            Objects.requireNonNull(inlinePresentation, "inlinePresentation cannot be null");
            setLifeTheUniverseAndEverything(id, value, presentation, inlinePresentation, null, null, null);
            return this;
        }

        @Deprecated
        public Builder setValue(AutofillId id, AutofillValue value, RemoteViews presentation, InlinePresentation inlinePresentation, InlinePresentation inlineTooltipPresentation) {
            throwIfDestroyed();
            Objects.requireNonNull(presentation, "presentation cannot be null");
            Objects.requireNonNull(inlinePresentation, "inlinePresentation cannot be null");
            Objects.requireNonNull(inlineTooltipPresentation, "inlineTooltipPresentation cannot be null");
            setLifeTheUniverseAndEverything(id, value, presentation, inlinePresentation, inlineTooltipPresentation, null, null);
            return this;
        }

        @Deprecated
        public Builder setValue(AutofillId id, AutofillValue value, Pattern filter, RemoteViews presentation, InlinePresentation inlinePresentation) {
            throwIfDestroyed();
            Objects.requireNonNull(presentation, "presentation cannot be null");
            Objects.requireNonNull(inlinePresentation, "inlinePresentation cannot be null");
            setLifeTheUniverseAndEverything(id, value, presentation, inlinePresentation, null, new DatasetFieldFilter(filter), null);
            return this;
        }

        @Deprecated
        public Builder setValue(AutofillId id, AutofillValue value, Pattern filter, RemoteViews presentation, InlinePresentation inlinePresentation, InlinePresentation inlineTooltipPresentation) {
            throwIfDestroyed();
            Objects.requireNonNull(presentation, "presentation cannot be null");
            Objects.requireNonNull(inlinePresentation, "inlinePresentation cannot be null");
            Objects.requireNonNull(inlineTooltipPresentation, "inlineTooltipPresentation cannot be null");
            setLifeTheUniverseAndEverything(id, value, presentation, inlinePresentation, inlineTooltipPresentation, new DatasetFieldFilter(filter), null);
            return this;
        }

        public Builder setField(AutofillId id, Field field) {
            throwIfDestroyed();
            if (field == null) {
                setLifeTheUniverseAndEverything(id, null, null, null, null, null, null);
            } else {
                DatasetFieldFilter filter = field.getDatasetFieldFilter();
                Presentations presentations = field.getPresentations();
                if (presentations == null) {
                    setLifeTheUniverseAndEverything(id, field.getValue(), null, null, null, filter, null);
                } else {
                    setLifeTheUniverseAndEverything(id, field.getValue(), presentations.getMenuPresentation(), presentations.getInlinePresentation(), presentations.getInlineTooltipPresentation(), filter, presentations.getDialogPresentation());
                }
            }
            return this;
        }

        @SystemApi
        @Deprecated
        public Builder setFieldInlinePresentation(AutofillId id, AutofillValue value, Pattern filter, InlinePresentation inlinePresentation) {
            throwIfDestroyed();
            Objects.requireNonNull(inlinePresentation, "inlinePresentation cannot be null");
            setLifeTheUniverseAndEverything(id, value, null, inlinePresentation, null, new DatasetFieldFilter(filter), null);
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setLifeTheUniverseAndEverything(AutofillId id, AutofillValue value, RemoteViews presentation, InlinePresentation inlinePresentation, InlinePresentation tooltip, DatasetFieldFilter filter, RemoteViews dialogPresentation) {
            Objects.requireNonNull(id, "id cannot be null");
            ArrayList<AutofillId> arrayList = this.mFieldIds;
            if (arrayList != null) {
                int existingIdx = arrayList.indexOf(id);
                if (existingIdx >= 0) {
                    this.mFieldValues.set(existingIdx, value);
                    this.mFieldPresentations.set(existingIdx, presentation);
                    this.mFieldDialogPresentations.set(existingIdx, dialogPresentation);
                    this.mFieldInlinePresentations.set(existingIdx, inlinePresentation);
                    this.mFieldInlineTooltipPresentations.set(existingIdx, tooltip);
                    this.mFieldFilters.set(existingIdx, filter);
                    return;
                }
            } else {
                this.mFieldIds = new ArrayList<>();
                this.mFieldValues = new ArrayList<>();
                this.mFieldPresentations = new ArrayList<>();
                this.mFieldDialogPresentations = new ArrayList<>();
                this.mFieldInlinePresentations = new ArrayList<>();
                this.mFieldInlineTooltipPresentations = new ArrayList<>();
                this.mFieldFilters = new ArrayList<>();
            }
            this.mFieldIds.add(id);
            this.mFieldValues.add(value);
            this.mFieldPresentations.add(presentation);
            this.mFieldDialogPresentations.add(dialogPresentation);
            this.mFieldInlinePresentations.add(inlinePresentation);
            this.mFieldInlineTooltipPresentations.add(tooltip);
            this.mFieldFilters.add(filter);
        }

        public Dataset build() {
            throwIfDestroyed();
            this.mDestroyed = true;
            ArrayList<AutofillId> arrayList = this.mFieldIds;
            if (arrayList == null) {
                throw new IllegalStateException("at least one value must be set");
            }
            if (this.mFieldContent != null) {
                if (arrayList.size() > 1) {
                    throw new IllegalStateException("when filling content, only one field can be filled");
                }
                if (this.mFieldValues.get(0) != null) {
                    throw new IllegalStateException("cannot fill both content and values");
                }
            }
            return new Dataset(this);
        }

        private void throwIfDestroyed() {
            if (this.mDestroyed) {
                throw new IllegalStateException("Already called #build()");
            }
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(this.mPresentation, flags);
        parcel.writeParcelable(this.mDialogPresentation, flags);
        parcel.writeParcelable(this.mInlinePresentation, flags);
        parcel.writeParcelable(this.mInlineTooltipPresentation, flags);
        parcel.writeTypedList(this.mFieldIds, flags);
        parcel.writeTypedList(this.mFieldValues, flags);
        parcel.writeTypedList(this.mFieldPresentations, flags);
        parcel.writeTypedList(this.mFieldDialogPresentations, flags);
        parcel.writeTypedList(this.mFieldInlinePresentations, flags);
        parcel.writeTypedList(this.mFieldInlineTooltipPresentations, flags);
        parcel.writeTypedList(this.mFieldFilters, flags);
        parcel.writeParcelable(this.mFieldContent, flags);
        parcel.writeParcelable(this.mAuthentication, flags);
        parcel.writeString(this.mId);
    }

    /* loaded from: classes3.dex */
    public static final class DatasetFieldFilter implements Parcelable {
        public static final Parcelable.Creator<DatasetFieldFilter> CREATOR = new Parcelable.Creator<DatasetFieldFilter>() { // from class: android.service.autofill.Dataset.DatasetFieldFilter.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public DatasetFieldFilter createFromParcel(Parcel parcel) {
                return new DatasetFieldFilter((Pattern) parcel.readSerializable(Pattern.class.getClassLoader(), Pattern.class));
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public DatasetFieldFilter[] newArray(int size) {
                return new DatasetFieldFilter[size];
            }
        };
        public final Pattern pattern;

        /* JADX INFO: Access modifiers changed from: package-private */
        public DatasetFieldFilter(Pattern pattern) {
            this.pattern = pattern;
        }

        public String toString() {
            return !Helper.sDebug ? super.toString() : this.pattern == null ? "null" : this.pattern.pattern().length() + "_chars";
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeSerializable(this.pattern);
        }
    }
}
