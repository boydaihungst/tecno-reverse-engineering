package android.os;

import android.os.DynamicEffectParam;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;
/* loaded from: classes2.dex */
public final class DynamicEffect extends VibrationEffect implements Parcelable {
    public static final Parcelable.Creator<DynamicEffect> CREATOR = new Parcelable.Creator<DynamicEffect>() { // from class: android.os.DynamicEffect.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DynamicEffect createFromParcel(Parcel in) {
            in.readInt();
            return new DynamicEffect(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DynamicEffect[] newArray(int size) {
            return new DynamicEffect[size];
        }
    };
    private static final int PARCEL_TOKEN_DYNAMICEFFECT = -1;
    private static final String TAG = "DynamicEffect";
    private static String mEffectJson;
    private static DynamicEffectParam mEffectParam;

    public DynamicEffect(Parcel in) {
        mEffectParam = (DynamicEffectParam) in.readParcelable(DynamicEffectParam.class.getClassLoader());
    }

    public DynamicEffect() {
    }

    public static DynamicEffect create(String json) {
        Log.d(TAG, "create json effect, effect=" + json);
        DynamicEffect ret = new DynamicEffect();
        ret.setEffectJson(json);
        return ret;
    }

    @Override // android.os.VibrationEffect
    public long getDuration() {
        DynamicEffectParam dynamicEffectParam = mEffectParam;
        if (dynamicEffectParam == null) {
            return -1L;
        }
        int loop = dynamicEffectParam.getLoop();
        if (loop < 0) {
            return Long.MAX_VALUE;
        }
        if (loop == 0) {
            return 0L;
        }
        int interval = mEffectParam.getInterval();
        ArrayList<DynamicEffectParam.Pattern.Event> events = mEffectParam.getPattern().getEvent();
        int duration = 0;
        Iterator<DynamicEffectParam.Pattern.Event> it = events.iterator();
        while (it.hasNext()) {
            DynamicEffectParam.Pattern.Event event = it.next();
            duration = (int) (duration + event.getDuration());
        }
        return ((loop - 1) * interval) + duration;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.os.VibrationEffect
    public DynamicEffect scale(float scaleFactor) {
        DynamicEffect effect = copy();
        return effect;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.os.VibrationEffect
    public DynamicEffect resolve(int defaultAmplitude) {
        DynamicEffect effect = copy();
        return effect;
    }

    @Override // android.os.VibrationEffect
    public void validate() {
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(-1);
        out.writeParcelable(mEffectParam, flags);
    }

    public DynamicEffect copy() {
        DynamicEffect effect = new DynamicEffect();
        try {
            effect.setDynamicEffectParam(mEffectParam);
            effect.setEffectJson(mEffectJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return effect;
    }

    public String getEffectJson() {
        return mEffectJson;
    }

    public void setEffectJson(String json) {
        mEffectJson = json;
    }

    public DynamicEffectParam getDynamicEffectParam() {
        return mEffectParam;
    }

    public void setDynamicEffectParam(DynamicEffectParam var) {
        mEffectParam = var;
    }

    public static DynamicEffectParam ParseJson(String var) {
        DynamicEffectParam mEft;
        DynamicEffectParam.Metadata md;
        DynamicEffectParam mEft2;
        DynamicEffectParam.Metadata md2;
        JSONObject jmeta;
        JSONArray jptn;
        DynamicEffectParam mEft3 = new DynamicEffectParam();
        DynamicEffectParam.Metadata md3 = new DynamicEffectParam.Metadata();
        DynamicEffectParam.Pattern ptn = new DynamicEffectParam.Pattern();
        if (TextUtils.isEmpty(var)) {
            mEft = mEft3;
            md = md3;
        } else {
            try {
                JSONObject jsonObject = new JSONObject(var);
                JSONObject jmeta2 = jsonObject.getJSONObject("Metadata");
                md3.setVersion(jmeta2.getInt("Version"));
                md3.setCreatedData(jmeta2.getString("Created"));
                md3.setDescription(jmeta2.getString("Description"));
                ArrayList<DynamicEffectParam.Pattern.Event> evts = new ArrayList<>();
                JSONArray jptn2 = jsonObject.getJSONArray("Pattern");
                int i = 0;
                while (i < jptn2.length()) {
                    JSONObject jevt = jptn2.getJSONObject(i).getJSONObject("Event");
                    JSONObject jparam = jevt.getJSONObject("Parameters");
                    DynamicEffectParam.Pattern.Event evt = new DynamicEffectParam.Pattern.Event();
                    evt.setType(jevt.getString("Type"));
                    JSONObject jsonObject2 = jsonObject;
                    evt.setRelativeTime(jevt.getLong("RelativeTime"));
                    evt.setGlobalIntensity(jparam.getInt("Intensity"));
                    evt.setBaseFrequence(jparam.getInt("Frequency"));
                    String string = jevt.getString("Type");
                    char c = 65535;
                    switch (string.hashCode()) {
                        case 379114255:
                            if (string.equals("continuous")) {
                                c = 1;
                                break;
                            }
                            break;
                        case 1052746378:
                            try {
                                if (string.equals("transient")) {
                                    c = 0;
                                    break;
                                }
                            } catch (Exception e) {
                                e = e;
                                mEft = mEft3;
                                md = md3;
                                e.printStackTrace();
                                DynamicEffectParam mEft4 = mEft;
                                mEft4.setMetadata(md);
                                mEft4.setPattern(ptn);
                                return mEft4;
                            }
                            break;
                    }
                    switch (c) {
                        case 0:
                            mEft2 = mEft3;
                            md2 = md3;
                            jmeta = jmeta2;
                            jptn = jptn2;
                            break;
                        case 1:
                            evt.setDuration(jevt.getLong("Duration"));
                            JSONArray jcrv = jparam.getJSONArray("Curve");
                            ArrayList<DynamicEffectParam.Pattern.Event.Curve> crvs = new ArrayList<>();
                            int j = 0;
                            while (true) {
                                jmeta = jmeta2;
                                if (j < jcrv.length()) {
                                    DynamicEffectParam.Pattern.Event.Curve crv = new DynamicEffectParam.Pattern.Event.Curve();
                                    JSONObject jcrvparam = jcrv.getJSONObject(j);
                                    JSONArray jcrv2 = jcrv;
                                    md = md3;
                                    JSONArray jptn3 = jptn2;
                                    mEft = mEft3;
                                    try {
                                        crv.setTime(jcrvparam.getLong("Time"));
                                        crv.setIntensity(jcrvparam.getDouble("Intensity"));
                                        crv.setFrequency(jcrvparam.getLong("Frequency"));
                                        crvs.add(crv);
                                        j++;
                                        jmeta2 = jmeta;
                                        jcrv = jcrv2;
                                        mEft3 = mEft;
                                        jptn2 = jptn3;
                                        md3 = md;
                                    } catch (Exception e2) {
                                        e = e2;
                                        e.printStackTrace();
                                        DynamicEffectParam mEft42 = mEft;
                                        mEft42.setMetadata(md);
                                        mEft42.setPattern(ptn);
                                        return mEft42;
                                    }
                                } else {
                                    mEft2 = mEft3;
                                    md2 = md3;
                                    jptn = jptn2;
                                    evt.setCurve(crvs);
                                    break;
                                }
                            }
                        default:
                            Log.e(TAG, "ParseJson: No such Type: " + jevt.getString("Type"));
                            throw new Exception("type" + jevt.getString("Type") + "unsupport");
                    }
                    evts.add(evt);
                    i++;
                    jsonObject = jsonObject2;
                    jmeta2 = jmeta;
                    mEft3 = mEft2;
                    jptn2 = jptn;
                    md3 = md2;
                }
                mEft = mEft3;
                md = md3;
                ptn.setEvent(evts);
            } catch (Exception e3) {
                e = e3;
                mEft = mEft3;
                md = md3;
            }
        }
        DynamicEffectParam mEft422 = mEft;
        mEft422.setMetadata(md);
        mEft422.setPattern(ptn);
        return mEft422;
    }
}
