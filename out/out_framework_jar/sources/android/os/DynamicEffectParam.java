package android.os;

import android.os.Parcelable;
import java.util.ArrayList;
/* loaded from: classes2.dex */
public class DynamicEffectParam implements Parcelable {
    public static final Parcelable.Creator<DynamicEffectParam> CREATOR = new Parcelable.Creator<DynamicEffectParam>() { // from class: android.os.DynamicEffectParam.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DynamicEffectParam createFromParcel(Parcel source) {
            return new DynamicEffectParam(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DynamicEffectParam[] newArray(int size) {
            return new DynamicEffectParam[size];
        }
    };
    public int mAmplitude;
    public int mInterval;
    public int mLoop;
    private Metadata mMetadata;
    private Pattern mPattern;
    public int mSFrequency;

    public int getLoop() {
        return this.mLoop;
    }

    public void setLoop(int var) {
        this.mLoop = var;
    }

    public int getInterval() {
        return this.mInterval;
    }

    public void setInterval(int var) {
        this.mInterval = var;
    }

    public int getAmplitude() {
        return this.mAmplitude;
    }

    public void setAmplitude(int var) {
        this.mAmplitude = var;
    }

    public int getFrequency() {
        return this.mSFrequency;
    }

    public void setFrequency(int var) {
        this.mSFrequency = var;
    }

    public Metadata getMetadata() {
        return this.mMetadata;
    }

    public void setMetadata(Metadata var) {
        this.mMetadata = var;
    }

    public Pattern getPattern() {
        return this.mPattern;
    }

    public void setPattern(Pattern var) {
        this.mPattern = var;
    }

    /* loaded from: classes2.dex */
    public static class Metadata implements Parcelable {
        public static final Parcelable.Creator<Metadata> CREATOR = new Parcelable.Creator<Metadata>() { // from class: android.os.DynamicEffectParam.Metadata.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Metadata createFromParcel(Parcel source) {
                return new Metadata(source);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Metadata[] newArray(int size) {
                return new Metadata[size];
            }
        };
        private String mCreatedData;
        private String mDescription;
        private int mVersion;

        public int getVersion() {
            return this.mVersion;
        }

        public void setVersion(int var) {
            this.mVersion = var;
        }

        public String getCreatedData() {
            return this.mCreatedData;
        }

        public void setCreatedData(String var) {
            this.mCreatedData = var;
        }

        public String getDescription() {
            return this.mDescription;
        }

        public void setDescription(String var) {
            this.mDescription = var;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mVersion);
            dest.writeString(this.mCreatedData);
            dest.writeString(this.mDescription);
        }

        public void readFromParcel(Parcel source) {
            this.mVersion = source.readInt();
            this.mCreatedData = source.readString();
            this.mDescription = source.readString();
        }

        public Metadata() {
        }

        protected Metadata(Parcel in) {
            this.mVersion = in.readInt();
            this.mCreatedData = in.readString();
            this.mDescription = in.readString();
        }
    }

    /* loaded from: classes2.dex */
    public static class Pattern implements Parcelable {
        public static final Parcelable.Creator<Pattern> CREATOR = new Parcelable.Creator<Pattern>() { // from class: android.os.DynamicEffectParam.Pattern.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Pattern createFromParcel(Parcel source) {
                return new Pattern(source);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Pattern[] newArray(int size) {
                return new Pattern[size];
            }
        };
        private ArrayList<Event> mEvent;

        public ArrayList<Event> getEvent() {
            return this.mEvent;
        }

        public void setEvent(ArrayList<Event> evt) {
            this.mEvent = evt;
        }

        /* loaded from: classes2.dex */
        public static class Event implements Parcelable {
            public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() { // from class: android.os.DynamicEffectParam.Pattern.Event.1
                /* JADX DEBUG: Method merged with bridge method */
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // android.os.Parcelable.Creator
                public Event createFromParcel(Parcel source) {
                    return new Event(source);
                }

                /* JADX DEBUG: Method merged with bridge method */
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // android.os.Parcelable.Creator
                public Event[] newArray(int size) {
                    return new Event[size];
                }
            };
            private long mBaseFrequence;
            private ArrayList<Curve> mCurve;
            private long mDuration;
            private long mGlobalIntensity;
            private long mRelativeTime;
            private String mType;

            public void setType(String var) {
                this.mType = var;
            }

            public String getType() {
                return this.mType;
            }

            public void setRelativeTime(long var) {
                this.mRelativeTime = var;
            }

            public long getRelativeTime() {
                return this.mRelativeTime;
            }

            public void setDuration(long var) {
                this.mDuration = var;
            }

            public long getDuration() {
                return this.mDuration;
            }

            public void setGlobalIntensity(long var) {
                this.mGlobalIntensity = var;
            }

            public long getGlobalIntensity() {
                return this.mGlobalIntensity;
            }

            public void setBaseFrequence(long var) {
                this.mBaseFrequence = var;
            }

            public long getBaseFrequence() {
                return this.mBaseFrequence;
            }

            public void setCurve(ArrayList<Curve> var) {
                this.mCurve = var;
            }

            public ArrayList<Curve> getCurve() {
                return this.mCurve;
            }

            /* loaded from: classes2.dex */
            public static class Curve implements Parcelable {
                public static final Parcelable.Creator<Curve> CREATOR = new Parcelable.Creator<Curve>() { // from class: android.os.DynamicEffectParam.Pattern.Event.Curve.1
                    /* JADX DEBUG: Method merged with bridge method */
                    /* JADX WARN: Can't rename method to resolve collision */
                    @Override // android.os.Parcelable.Creator
                    public Curve createFromParcel(Parcel source) {
                        return new Curve(source);
                    }

                    /* JADX DEBUG: Method merged with bridge method */
                    /* JADX WARN: Can't rename method to resolve collision */
                    @Override // android.os.Parcelable.Creator
                    public Curve[] newArray(int size) {
                        return new Curve[size];
                    }
                };
                private long mFrequency;
                private double mIntensity;
                private long mTime;

                public void setTime(long var) {
                    this.mTime = var;
                }

                public long getTime() {
                    return this.mTime;
                }

                public void setIntensity(double var) {
                    this.mIntensity = var;
                }

                public double getIntensity() {
                    return this.mIntensity;
                }

                public void setFrequency(long var) {
                    this.mFrequency = var;
                }

                public long getFrequency() {
                    return this.mFrequency;
                }

                @Override // android.os.Parcelable
                public int describeContents() {
                    return 0;
                }

                @Override // android.os.Parcelable
                public void writeToParcel(Parcel dest, int flags) {
                    dest.writeLong(this.mTime);
                    dest.writeDouble(this.mIntensity);
                    dest.writeLong(this.mFrequency);
                }

                public void readFromParcel(Parcel source) {
                    this.mTime = source.readLong();
                    this.mIntensity = source.readDouble();
                    this.mFrequency = source.readLong();
                }

                public Curve() {
                }

                protected Curve(Parcel in) {
                    this.mTime = in.readLong();
                    this.mIntensity = in.readDouble();
                    this.mFrequency = in.readLong();
                }
            }

            @Override // android.os.Parcelable
            public int describeContents() {
                return 0;
            }

            @Override // android.os.Parcelable
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeString(this.mType);
                dest.writeLong(this.mRelativeTime);
                dest.writeLong(this.mDuration);
                dest.writeLong(this.mGlobalIntensity);
                dest.writeLong(this.mBaseFrequence);
                dest.writeList(this.mCurve);
            }

            public void readFromParcel(Parcel source) {
                this.mType = source.readString();
                this.mRelativeTime = source.readLong();
                this.mDuration = source.readLong();
                this.mGlobalIntensity = source.readLong();
                this.mBaseFrequence = source.readLong();
                ArrayList<Curve> arrayList = new ArrayList<>();
                this.mCurve = arrayList;
                source.readList(arrayList, Curve.class.getClassLoader());
            }

            public Event() {
            }

            protected Event(Parcel in) {
                this.mType = in.readString();
                this.mRelativeTime = in.readLong();
                this.mDuration = in.readLong();
                this.mGlobalIntensity = in.readLong();
                this.mBaseFrequence = in.readLong();
                ArrayList<Curve> arrayList = new ArrayList<>();
                this.mCurve = arrayList;
                in.readList(arrayList, Curve.class.getClassLoader());
            }
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeList(this.mEvent);
        }

        public void readFromParcel(Parcel source) {
            ArrayList<Event> arrayList = new ArrayList<>();
            this.mEvent = arrayList;
            source.readList(arrayList, Event.class.getClassLoader());
        }

        public Pattern() {
            this.mEvent = new ArrayList<>();
        }

        protected Pattern(Parcel in) {
            this.mEvent = new ArrayList<>();
            ArrayList<Event> arrayList = new ArrayList<>();
            this.mEvent = arrayList;
            in.readList(arrayList, Event.class.getClassLoader());
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mMetadata, flags);
        dest.writeParcelable(this.mPattern, flags);
        dest.writeInt(this.mLoop);
        dest.writeInt(this.mInterval);
        dest.writeInt(this.mAmplitude);
        dest.writeInt(this.mSFrequency);
    }

    public void readFromParcel(Parcel source) {
        this.mMetadata = (Metadata) source.readParcelable(Metadata.class.getClassLoader());
        this.mPattern = (Pattern) source.readParcelable(Pattern.class.getClassLoader());
        this.mLoop = source.readInt();
        this.mInterval = source.readInt();
        this.mAmplitude = source.readInt();
        this.mSFrequency = source.readInt();
    }

    public DynamicEffectParam() {
    }

    protected DynamicEffectParam(Parcel in) {
        this.mMetadata = (Metadata) in.readParcelable(Metadata.class.getClassLoader());
        this.mPattern = (Pattern) in.readParcelable(Pattern.class.getClassLoader());
        this.mLoop = in.readInt();
        this.mInterval = in.readInt();
        this.mAmplitude = in.readInt();
        this.mSFrequency = in.readInt();
    }
}
