package android.util;
/* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
/* JADX WARN: Unknown enum class pattern. Please report as an issue! */
/* loaded from: classes3.dex */
public class DataUnit {
    private static final /* synthetic */ DataUnit[] $VALUES;
    public static final DataUnit GIBIBYTES;
    public static final DataUnit GIGABYTES;
    public static final DataUnit KIBIBYTES;
    public static final DataUnit KILOBYTES;
    public static final DataUnit MEBIBYTES;
    public static final DataUnit MEGABYTES;

    /* renamed from: android.util.DataUnit$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    enum AnonymousClass1 extends DataUnit {
        private AnonymousClass1(String str, int i) {
            super(str, i);
        }

        @Override // android.util.DataUnit
        public long toBytes(long v) {
            return 1000 * v;
        }
    }

    private DataUnit(String str, int i) {
    }

    public static DataUnit valueOf(String name) {
        return (DataUnit) Enum.valueOf(DataUnit.class, name);
    }

    public static DataUnit[] values() {
        return (DataUnit[]) $VALUES.clone();
    }

    /* renamed from: android.util.DataUnit$2  reason: invalid class name */
    /* loaded from: classes3.dex */
    enum AnonymousClass2 extends DataUnit {
        private AnonymousClass2(String str, int i) {
            super(str, i);
        }

        @Override // android.util.DataUnit
        public long toBytes(long v) {
            return TimeUtils.NANOS_PER_MS * v;
        }
    }

    static {
        AnonymousClass1 anonymousClass1 = new AnonymousClass1("KILOBYTES", 0);
        KILOBYTES = anonymousClass1;
        AnonymousClass2 anonymousClass2 = new AnonymousClass2("MEGABYTES", 1);
        MEGABYTES = anonymousClass2;
        AnonymousClass3 anonymousClass3 = new AnonymousClass3("GIGABYTES", 2);
        GIGABYTES = anonymousClass3;
        AnonymousClass4 anonymousClass4 = new AnonymousClass4("KIBIBYTES", 3);
        KIBIBYTES = anonymousClass4;
        AnonymousClass5 anonymousClass5 = new AnonymousClass5("MEBIBYTES", 4);
        MEBIBYTES = anonymousClass5;
        AnonymousClass6 anonymousClass6 = new AnonymousClass6("GIBIBYTES", 5);
        GIBIBYTES = anonymousClass6;
        $VALUES = new DataUnit[]{anonymousClass1, anonymousClass2, anonymousClass3, anonymousClass4, anonymousClass5, anonymousClass6};
    }

    /* renamed from: android.util.DataUnit$3  reason: invalid class name */
    /* loaded from: classes3.dex */
    enum AnonymousClass3 extends DataUnit {
        private AnonymousClass3(String str, int i) {
            super(str, i);
        }

        @Override // android.util.DataUnit
        public long toBytes(long v) {
            return 1000000000 * v;
        }
    }

    /* renamed from: android.util.DataUnit$4  reason: invalid class name */
    /* loaded from: classes3.dex */
    enum AnonymousClass4 extends DataUnit {
        private AnonymousClass4(String str, int i) {
            super(str, i);
        }

        @Override // android.util.DataUnit
        public long toBytes(long v) {
            return 1024 * v;
        }
    }

    /* renamed from: android.util.DataUnit$5  reason: invalid class name */
    /* loaded from: classes3.dex */
    enum AnonymousClass5 extends DataUnit {
        private AnonymousClass5(String str, int i) {
            super(str, i);
        }

        @Override // android.util.DataUnit
        public long toBytes(long v) {
            return 1048576 * v;
        }
    }

    /* renamed from: android.util.DataUnit$6  reason: invalid class name */
    /* loaded from: classes3.dex */
    enum AnonymousClass6 extends DataUnit {
        private AnonymousClass6(String str, int i) {
            super(str, i);
        }

        @Override // android.util.DataUnit
        public long toBytes(long v) {
            return 1073741824 * v;
        }
    }

    public long toBytes(long v) {
        throw new AbstractMethodError();
    }
}
