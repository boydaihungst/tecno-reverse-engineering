package android.media.audiopolicy;

import android.annotation.SystemApi;
import android.media.AudioAttributes;
import android.os.Parcel;
import android.util.Log;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
@SystemApi
/* loaded from: classes2.dex */
public class AudioMixingRule {
    public static final int MIX_ROLE_INJECTOR = 1;
    public static final int MIX_ROLE_PLAYERS = 0;
    public static final int RULE_EXCLUDE_ATTRIBUTE_CAPTURE_PRESET = 32770;
    public static final int RULE_EXCLUDE_ATTRIBUTE_USAGE = 32769;
    public static final int RULE_EXCLUDE_UID = 32772;
    public static final int RULE_EXCLUDE_USERID = 32776;
    private static final int RULE_EXCLUSION_MASK = 32768;
    public static final int RULE_MATCH_ATTRIBUTE_CAPTURE_PRESET = 2;
    public static final int RULE_MATCH_ATTRIBUTE_USAGE = 1;
    public static final int RULE_MATCH_UID = 4;
    public static final int RULE_MATCH_USERID = 8;
    private boolean mAllowPrivilegedPlaybackCapture;
    private final ArrayList<AudioMixMatchCriterion> mCriteria;
    private final int mTargetMixType;
    private boolean mVoiceCommunicationCaptureAllowed;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface MixRole {
    }

    private AudioMixingRule(int mixType, ArrayList<AudioMixMatchCriterion> criteria, boolean allowPrivilegedMediaPlaybackCapture, boolean voiceCommunicationCaptureAllowed) {
        this.mAllowPrivilegedPlaybackCapture = false;
        this.mVoiceCommunicationCaptureAllowed = false;
        this.mCriteria = criteria;
        this.mTargetMixType = mixType;
        this.mAllowPrivilegedPlaybackCapture = allowPrivilegedMediaPlaybackCapture;
        this.mVoiceCommunicationCaptureAllowed = voiceCommunicationCaptureAllowed;
    }

    /* loaded from: classes2.dex */
    public static final class AudioMixMatchCriterion {
        final AudioAttributes mAttr;
        final int mIntProp;
        final int mRule;

        AudioMixMatchCriterion(AudioAttributes attributes, int rule) {
            this.mAttr = attributes;
            this.mIntProp = Integer.MIN_VALUE;
            this.mRule = rule;
        }

        AudioMixMatchCriterion(Integer intProp, int rule) {
            this.mAttr = null;
            this.mIntProp = intProp.intValue();
            this.mRule = rule;
        }

        public int hashCode() {
            return Objects.hash(this.mAttr, Integer.valueOf(this.mIntProp), Integer.valueOf(this.mRule));
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void writeToParcel(Parcel dest) {
            dest.writeInt(this.mRule);
            int match_rule = this.mRule & (-32769);
            switch (match_rule) {
                case 1:
                case 2:
                    this.mAttr.writeToParcel(dest, 1);
                    return;
                case 4:
                case 8:
                    dest.writeInt(this.mIntProp);
                    return;
                default:
                    Log.e("AudioMixMatchCriterion", "Unknown match rule" + match_rule + " when writing to Parcel");
                    dest.writeInt(-1);
                    return;
            }
        }

        public AudioAttributes getAudioAttributes() {
            return this.mAttr;
        }

        public int getIntProp() {
            return this.mIntProp;
        }

        public int getRule() {
            return this.mRule;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAffectingUsage(int usage) {
        Iterator<AudioMixMatchCriterion> it = this.mCriteria.iterator();
        while (it.hasNext()) {
            AudioMixMatchCriterion criterion = it.next();
            if ((criterion.mRule & 1) != 0 && criterion.mAttr != null && criterion.mAttr.getSystemUsage() == usage) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean containsMatchAttributeRuleForUsage(int usage) {
        Iterator<AudioMixMatchCriterion> it = this.mCriteria.iterator();
        while (it.hasNext()) {
            AudioMixMatchCriterion criterion = it.next();
            if (criterion.mRule == 1 && criterion.mAttr != null && criterion.mAttr.getSystemUsage() == usage) {
                return true;
            }
        }
        return false;
    }

    private static boolean areCriteriaEquivalent(ArrayList<AudioMixMatchCriterion> cr1, ArrayList<AudioMixMatchCriterion> cr2) {
        if (cr1 == null || cr2 == null) {
            return false;
        }
        if (cr1 == cr2) {
            return true;
        }
        if (cr1.size() != cr2.size() || cr1.hashCode() != cr2.hashCode()) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getTargetMixType() {
        return this.mTargetMixType;
    }

    public int getTargetMixRole() {
        return this.mTargetMixType == 1 ? 1 : 0;
    }

    public ArrayList<AudioMixMatchCriterion> getCriteria() {
        return this.mCriteria;
    }

    public boolean allowPrivilegedMediaPlaybackCapture() {
        return this.mAllowPrivilegedPlaybackCapture;
    }

    public boolean voiceCommunicationCaptureAllowed() {
        return this.mVoiceCommunicationCaptureAllowed;
    }

    public void setVoiceCommunicationCaptureAllowed(boolean allowed) {
        this.mVoiceCommunicationCaptureAllowed = allowed;
    }

    public boolean isForCallRedirection() {
        Iterator<AudioMixMatchCriterion> it = this.mCriteria.iterator();
        while (it.hasNext()) {
            AudioMixMatchCriterion criterion = it.next();
            if (criterion.mAttr != null && criterion.mAttr.isForCallRedirection() && ((criterion.mRule == 1 && (criterion.mAttr.getUsage() == 2 || criterion.mAttr.getUsage() == 3)) || (criterion.mRule == 2 && criterion.mAttr.getCapturePreset() == 7))) {
                return true;
            }
        }
        return false;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudioMixingRule that = (AudioMixingRule) o;
        if (this.mTargetMixType == that.mTargetMixType && areCriteriaEquivalent(this.mCriteria, that.mCriteria) && this.mAllowPrivilegedPlaybackCapture == that.mAllowPrivilegedPlaybackCapture && this.mVoiceCommunicationCaptureAllowed == that.mVoiceCommunicationCaptureAllowed) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mTargetMixType), this.mCriteria, Boolean.valueOf(this.mAllowPrivilegedPlaybackCapture), Boolean.valueOf(this.mVoiceCommunicationCaptureAllowed));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidSystemApiRule(int rule) {
        switch (rule) {
            case 1:
            case 2:
            case 4:
            case 8:
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidAttributesSystemApiRule(int rule) {
        switch (rule) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isValidRule(int rule) {
        int match_rule = (-32769) & rule;
        switch (match_rule) {
            case 1:
            case 2:
            case 4:
            case 8:
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isPlayerRule(int rule) {
        int match_rule = (-32769) & rule;
        switch (match_rule) {
            case 1:
            case 8:
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isRecorderRule(int rule) {
        int match_rule = (-32769) & rule;
        switch (match_rule) {
            case 2:
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isAudioAttributeRule(int match_rule) {
        switch (match_rule) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    /* loaded from: classes2.dex */
    public static class Builder {
        private int mTargetMixType = -1;
        private boolean mAllowPrivilegedMediaPlaybackCapture = false;
        private boolean mVoiceCommunicationCaptureAllowed = false;
        private ArrayList<AudioMixMatchCriterion> mCriteria = new ArrayList<>();

        public Builder addRule(AudioAttributes attrToMatch, int rule) throws IllegalArgumentException {
            if (!AudioMixingRule.isValidAttributesSystemApiRule(rule)) {
                throw new IllegalArgumentException("Illegal rule value " + rule);
            }
            return checkAddRuleObjInternal(rule, attrToMatch);
        }

        public Builder excludeRule(AudioAttributes attrToMatch, int rule) throws IllegalArgumentException {
            if (!AudioMixingRule.isValidAttributesSystemApiRule(rule)) {
                throw new IllegalArgumentException("Illegal rule value " + rule);
            }
            return checkAddRuleObjInternal(32768 | rule, attrToMatch);
        }

        public Builder addMixRule(int rule, Object property) throws IllegalArgumentException {
            if (!AudioMixingRule.isValidSystemApiRule(rule)) {
                throw new IllegalArgumentException("Illegal rule value " + rule);
            }
            return checkAddRuleObjInternal(rule, property);
        }

        public Builder excludeMixRule(int rule, Object property) throws IllegalArgumentException {
            if (!AudioMixingRule.isValidSystemApiRule(rule)) {
                throw new IllegalArgumentException("Illegal rule value " + rule);
            }
            return checkAddRuleObjInternal(32768 | rule, property);
        }

        public Builder allowPrivilegedPlaybackCapture(boolean allow) {
            this.mAllowPrivilegedMediaPlaybackCapture = allow;
            return this;
        }

        public Builder voiceCommunicationCaptureAllowed(boolean allowed) {
            this.mVoiceCommunicationCaptureAllowed = allowed;
            return this;
        }

        public Builder setTargetMixRole(int mixRole) {
            if (mixRole != 0 && mixRole != 1) {
                throw new IllegalArgumentException("Illegal argument for mix role");
            }
            Log.i("AudioMixingRule", "Builder setTargetMixRole " + mixRole);
            this.mTargetMixType = mixRole != 1 ? 0 : 1;
            return this;
        }

        private Builder checkAddRuleObjInternal(int rule, Object property) throws IllegalArgumentException {
            if (property == null) {
                throw new IllegalArgumentException("Illegal null argument for mixing rule");
            }
            if (!AudioMixingRule.isValidRule(rule)) {
                throw new IllegalArgumentException("Illegal rule value " + rule);
            }
            int match_rule = (-32769) & rule;
            if (AudioMixingRule.isAudioAttributeRule(match_rule)) {
                if (!(property instanceof AudioAttributes)) {
                    throw new IllegalArgumentException("Invalid AudioAttributes argument");
                }
                return addRuleInternal((AudioAttributes) property, null, rule);
            } else if (!(property instanceof Integer)) {
                throw new IllegalArgumentException("Invalid Integer argument");
            } else {
                return addRuleInternal(null, (Integer) property, rule);
            }
        }

        private Builder addRuleInternal(AudioAttributes attrToMatch, Integer intProp, int rule) throws IllegalArgumentException {
            if (this.mTargetMixType == -1) {
                if (AudioMixingRule.isPlayerRule(rule)) {
                    this.mTargetMixType = 0;
                } else if (AudioMixingRule.isRecorderRule(rule)) {
                    this.mTargetMixType = 1;
                } else {
                    this.mTargetMixType = 0;
                }
            } else if ((AudioMixingRule.isPlayerRule(rule) && this.mTargetMixType != 0) || (AudioMixingRule.isRecorderRule(rule) && this.mTargetMixType != 1)) {
                throw new IllegalArgumentException("Incompatible rule for mix");
            }
            synchronized (this.mCriteria) {
                Iterator<AudioMixMatchCriterion> crIterator = this.mCriteria.iterator();
                int match_rule = rule & (-32769);
                while (crIterator.hasNext()) {
                    AudioMixMatchCriterion criterion = crIterator.next();
                    if ((criterion.mRule & (-32769)) == match_rule) {
                        switch (match_rule) {
                            case 1:
                                if (criterion.mAttr.getSystemUsage() == attrToMatch.getSystemUsage()) {
                                    if (criterion.mRule == rule) {
                                        return this;
                                    }
                                    throw new IllegalArgumentException("Contradictory rule exists for " + attrToMatch);
                                }
                                break;
                            case 2:
                                if (criterion.mAttr.getCapturePreset() == attrToMatch.getCapturePreset()) {
                                    if (criterion.mRule == rule) {
                                        return this;
                                    }
                                    throw new IllegalArgumentException("Contradictory rule exists for " + attrToMatch);
                                }
                                break;
                            case 4:
                                if (criterion.mIntProp == intProp.intValue()) {
                                    if (criterion.mRule == rule) {
                                        return this;
                                    }
                                    throw new IllegalArgumentException("Contradictory rule exists for UID " + intProp);
                                }
                                break;
                            case 8:
                                if (criterion.mIntProp == intProp.intValue()) {
                                    if (criterion.mRule == rule) {
                                        return this;
                                    }
                                    throw new IllegalArgumentException("Contradictory rule exists for userId " + intProp);
                                }
                                break;
                        }
                    }
                }
                switch (match_rule) {
                    case 1:
                    case 2:
                        this.mCriteria.add(new AudioMixMatchCriterion(attrToMatch, rule));
                        break;
                    case 4:
                    case 8:
                        this.mCriteria.add(new AudioMixMatchCriterion(intProp, rule));
                        break;
                    default:
                        throw new IllegalStateException("Unreachable code in addRuleInternal()");
                }
                return this;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder addRuleFromParcel(Parcel in) throws IllegalArgumentException {
            int rule = in.readInt();
            int match_rule = (-32769) & rule;
            AudioAttributes attr = null;
            Integer intProp = null;
            switch (match_rule) {
                case 1:
                case 2:
                    AudioAttributes attr2 = AudioAttributes.CREATOR.createFromParcel(in);
                    attr = attr2;
                    break;
                case 4:
                case 8:
                    intProp = new Integer(in.readInt());
                    break;
                default:
                    in.readInt();
                    throw new IllegalArgumentException("Illegal rule value " + rule + " in parcel");
            }
            return addRuleInternal(attr, intProp, rule);
        }

        public AudioMixingRule build() {
            return new AudioMixingRule(this.mTargetMixType, this.mCriteria, this.mAllowPrivilegedMediaPlaybackCapture, this.mVoiceCommunicationCaptureAllowed);
        }
    }
}
