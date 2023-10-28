package android.telephony.emergency;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.PhoneNumberUtils;
import com.android.telephony.Rlog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes3.dex */
public final class EmergencyNumber implements Parcelable, Comparable<EmergencyNumber> {
    public static final Parcelable.Creator<EmergencyNumber> CREATOR;
    public static final int EMERGENCY_CALL_ROUTING_EMERGENCY = 1;
    public static final int EMERGENCY_CALL_ROUTING_NORMAL = 2;
    public static final int EMERGENCY_CALL_ROUTING_UNKNOWN = 0;
    public static final int EMERGENCY_NUMBER_CUSTOM_CONFIG = 32768;
    public static final int EMERGENCY_NUMBER_SOURCE_DATABASE = 16;
    public static final int EMERGENCY_NUMBER_SOURCE_DEFAULT = 8;
    public static final int EMERGENCY_NUMBER_SOURCE_MODEM_CONFIG = 4;
    public static final int EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING = 1;
    private static final Set<Integer> EMERGENCY_NUMBER_SOURCE_SET;
    public static final int EMERGENCY_NUMBER_SOURCE_SIM = 2;
    public static final int EMERGENCY_NUMBER_SOURCE_TEST = 32;
    public static final int EMERGENCY_SERVICE_CATEGORY_AIEC = 64;
    public static final int EMERGENCY_SERVICE_CATEGORY_AMBULANCE = 2;
    public static final int EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE = 4;
    public static final int EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD = 8;
    public static final int EMERGENCY_SERVICE_CATEGORY_MIEC = 32;
    public static final int EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE = 16;
    public static final int EMERGENCY_SERVICE_CATEGORY_POLICE = 1;
    private static final Set<Integer> EMERGENCY_SERVICE_CATEGORY_SET;
    public static final int EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED = 0;
    private static final String LOG_TAG = "EmergencyNumber";
    private final String mCountryIso;
    private final int mEmergencyCallRouting;
    private final int mEmergencyNumberSourceBitmask;
    private final int mEmergencyServiceCategoryBitmask;
    private final List<String> mEmergencyUrns;
    private final String mMnc;
    private final String mNumber;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface EmergencyCallRouting {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface EmergencyNumberSources {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface EmergencyServiceCategories {
    }

    static {
        HashSet hashSet = new HashSet();
        EMERGENCY_SERVICE_CATEGORY_SET = hashSet;
        hashSet.add(1);
        hashSet.add(2);
        hashSet.add(4);
        hashSet.add(8);
        hashSet.add(16);
        hashSet.add(32);
        hashSet.add(64);
        HashSet hashSet2 = new HashSet();
        EMERGENCY_NUMBER_SOURCE_SET = hashSet2;
        hashSet2.add(1);
        hashSet2.add(2);
        hashSet2.add(16);
        hashSet2.add(4);
        hashSet2.add(8);
        CREATOR = new Parcelable.Creator<EmergencyNumber>() { // from class: android.telephony.emergency.EmergencyNumber.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public EmergencyNumber createFromParcel(Parcel in) {
                return new EmergencyNumber(in);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public EmergencyNumber[] newArray(int size) {
                return new EmergencyNumber[size];
            }
        };
    }

    public EmergencyNumber(String number, String countryIso, String mnc, int emergencyServiceCategories, List<String> emergencyUrns, int emergencyNumberSources, int emergencyCallRouting) {
        this.mNumber = number;
        this.mCountryIso = countryIso;
        this.mMnc = mnc;
        this.mEmergencyServiceCategoryBitmask = emergencyServiceCategories;
        this.mEmergencyUrns = emergencyUrns;
        this.mEmergencyNumberSourceBitmask = emergencyNumberSources;
        this.mEmergencyCallRouting = emergencyCallRouting;
    }

    public EmergencyNumber(Parcel source) {
        this.mNumber = source.readString();
        this.mCountryIso = source.readString();
        this.mMnc = source.readString();
        this.mEmergencyServiceCategoryBitmask = source.readInt();
        this.mEmergencyUrns = source.createStringArrayList();
        this.mEmergencyNumberSourceBitmask = source.readInt();
        this.mEmergencyCallRouting = source.readInt();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mNumber);
        dest.writeString(this.mCountryIso);
        dest.writeString(this.mMnc);
        dest.writeInt(this.mEmergencyServiceCategoryBitmask);
        dest.writeStringList(this.mEmergencyUrns);
        dest.writeInt(this.mEmergencyNumberSourceBitmask);
        dest.writeInt(this.mEmergencyCallRouting);
    }

    public String getNumber() {
        return this.mNumber;
    }

    public String getCountryIso() {
        return this.mCountryIso;
    }

    public String getMnc() {
        return this.mMnc;
    }

    public int getEmergencyServiceCategoryBitmask() {
        return this.mEmergencyServiceCategoryBitmask;
    }

    public int getEmergencyServiceCategoryBitmaskInternalDial() {
        if (this.mEmergencyNumberSourceBitmask == 16) {
            return 0;
        }
        return this.mEmergencyServiceCategoryBitmask;
    }

    public List<Integer> getEmergencyServiceCategories() {
        List<Integer> categories = new ArrayList<>();
        if (serviceUnspecified()) {
            categories.add(0);
            return categories;
        }
        for (Integer category : EMERGENCY_SERVICE_CATEGORY_SET) {
            if (isInEmergencyServiceCategories(category.intValue())) {
                categories.add(category);
            }
        }
        return categories;
    }

    public List<String> getEmergencyUrns() {
        return Collections.unmodifiableList(this.mEmergencyUrns);
    }

    private boolean serviceUnspecified() {
        return this.mEmergencyServiceCategoryBitmask == 0;
    }

    public boolean isInEmergencyServiceCategories(int categories) {
        if (categories == 0) {
            return serviceUnspecified();
        }
        return serviceUnspecified() || (this.mEmergencyServiceCategoryBitmask & categories) == categories;
    }

    public int getEmergencyNumberSourceBitmask() {
        return this.mEmergencyNumberSourceBitmask;
    }

    public List<Integer> getEmergencyNumberSources() {
        List<Integer> sources = new ArrayList<>();
        for (Integer source : EMERGENCY_NUMBER_SOURCE_SET) {
            if ((this.mEmergencyNumberSourceBitmask & source.intValue()) == source.intValue()) {
                sources.add(source);
            }
        }
        return sources;
    }

    public boolean isFromSources(int sources) {
        return (this.mEmergencyNumberSourceBitmask & sources) == sources;
    }

    public int getEmergencyCallRouting() {
        return this.mEmergencyCallRouting;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "EmergencyNumber:Number-" + this.mNumber + "|CountryIso-" + this.mCountryIso + "|Mnc-" + this.mMnc + "|ServiceCategories-" + Integer.toBinaryString(this.mEmergencyServiceCategoryBitmask) + "|Urns-" + this.mEmergencyUrns + "|Sources-" + Integer.toBinaryString(this.mEmergencyNumberSourceBitmask) + "|Routing-" + Integer.toBinaryString(this.mEmergencyCallRouting);
    }

    public boolean equals(Object o) {
        if (EmergencyNumber.class.isInstance(o)) {
            EmergencyNumber other = (EmergencyNumber) o;
            return this.mNumber.equals(other.mNumber) && this.mCountryIso.equals(other.mCountryIso) && this.mMnc.equals(other.mMnc) && this.mEmergencyServiceCategoryBitmask == other.mEmergencyServiceCategoryBitmask && this.mEmergencyUrns.equals(other.mEmergencyUrns) && this.mEmergencyNumberSourceBitmask == other.mEmergencyNumberSourceBitmask && this.mEmergencyCallRouting == other.mEmergencyCallRouting;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mNumber, this.mCountryIso, this.mMnc, Integer.valueOf(this.mEmergencyServiceCategoryBitmask), this.mEmergencyUrns, Integer.valueOf(this.mEmergencyNumberSourceBitmask), Integer.valueOf(this.mEmergencyCallRouting));
    }

    private int getDisplayPriorityScore() {
        int score = 0;
        if (isFromSources(1)) {
            score = 0 + 16;
        }
        if (isFromSources(2)) {
            score += 8;
        }
        if (isFromSources(16)) {
            score += 4;
        }
        if (isFromSources(8)) {
            score += 2;
        }
        if (isFromSources(4)) {
            return score + 1;
        }
        return score;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.lang.Comparable
    public int compareTo(EmergencyNumber emergencyNumber) {
        if (getDisplayPriorityScore() > emergencyNumber.getDisplayPriorityScore()) {
            return -1;
        }
        if (getDisplayPriorityScore() < emergencyNumber.getDisplayPriorityScore()) {
            return 1;
        }
        if (getNumber().compareTo(emergencyNumber.getNumber()) != 0) {
            return getNumber().compareTo(emergencyNumber.getNumber());
        }
        if (getCountryIso().compareTo(emergencyNumber.getCountryIso()) != 0) {
            return getCountryIso().compareTo(emergencyNumber.getCountryIso());
        }
        if (getMnc().compareTo(emergencyNumber.getMnc()) != 0) {
            return getMnc().compareTo(emergencyNumber.getMnc());
        }
        if (getEmergencyServiceCategoryBitmask() != emergencyNumber.getEmergencyServiceCategoryBitmask()) {
            return getEmergencyServiceCategoryBitmask() > emergencyNumber.getEmergencyServiceCategoryBitmask() ? -1 : 1;
        } else if (getEmergencyUrns().toString().compareTo(emergencyNumber.getEmergencyUrns().toString()) != 0) {
            return getEmergencyUrns().toString().compareTo(emergencyNumber.getEmergencyUrns().toString());
        } else {
            if (getEmergencyCallRouting() != emergencyNumber.getEmergencyCallRouting()) {
                return getEmergencyCallRouting() > emergencyNumber.getEmergencyCallRouting() ? -1 : 1;
            }
            return 0;
        }
    }

    public static void mergeSameNumbersInEmergencyNumberList(List<EmergencyNumber> emergencyNumberList) {
        if (emergencyNumberList == null) {
            return;
        }
        Set<Integer> duplicatedEmergencyNumberPosition = new HashSet<>();
        for (int i = 0; i < emergencyNumberList.size(); i++) {
            for (int j = 0; j < i; j++) {
                if (areSameEmergencyNumbers(emergencyNumberList.get(i), emergencyNumberList.get(j))) {
                    Rlog.e(LOG_TAG, "Found unexpected duplicate numbers: " + emergencyNumberList.get(i) + " vs " + emergencyNumberList.get(j));
                    emergencyNumberList.set(i, mergeSameEmergencyNumbers(emergencyNumberList.get(i), emergencyNumberList.get(j)));
                    duplicatedEmergencyNumberPosition.add(Integer.valueOf(j));
                }
            }
        }
        int i2 = emergencyNumberList.size();
        for (int i3 = i2 - 1; i3 >= 0; i3--) {
            if (duplicatedEmergencyNumberPosition.contains(Integer.valueOf(i3))) {
                emergencyNumberList.remove(i3);
            }
        }
        Collections.sort(emergencyNumberList);
    }

    public static boolean areSameEmergencyNumbers(EmergencyNumber first, EmergencyNumber second) {
        if (first.getNumber().equals(second.getNumber()) && first.getCountryIso().equals(second.getCountryIso()) && first.getMnc().equals(second.getMnc()) && first.getEmergencyServiceCategoryBitmask() == second.getEmergencyServiceCategoryBitmask() && first.getEmergencyUrns().equals(second.getEmergencyUrns()) && first.getEmergencyCallRouting() == second.getEmergencyCallRouting()) {
            return !(second.isFromSources(32) ^ first.isFromSources(32));
        }
        return false;
    }

    public static EmergencyNumber mergeSameEmergencyNumbers(EmergencyNumber first, EmergencyNumber second) {
        if (areSameEmergencyNumbers(first, second)) {
            return new EmergencyNumber(first.getNumber(), first.getCountryIso(), first.getMnc(), first.getEmergencyServiceCategoryBitmask(), first.getEmergencyUrns(), second.getEmergencyNumberSourceBitmask() | first.getEmergencyNumberSourceBitmask(), first.getEmergencyCallRouting());
        }
        return null;
    }

    public static boolean validateEmergencyNumberAddress(String address) {
        char[] charArray;
        if (address == null) {
            return false;
        }
        for (char c : address.toCharArray()) {
            if (!PhoneNumberUtils.isDialable(c)) {
                return false;
            }
        }
        return true;
    }
}
