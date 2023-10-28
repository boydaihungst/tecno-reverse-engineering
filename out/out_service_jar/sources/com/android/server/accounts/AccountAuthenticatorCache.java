package com.android.server.accounts;

import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.pm.RegisteredServicesCache;
import android.content.pm.XmlSerializerAndParser;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.internal.R;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
class AccountAuthenticatorCache extends RegisteredServicesCache<AuthenticatorDescription> implements IAccountAuthenticatorCache {
    private static final String TAG = "Account";
    private static final MySerializer sSerializer = new MySerializer();

    @Override // com.android.server.accounts.IAccountAuthenticatorCache
    public /* bridge */ /* synthetic */ RegisteredServicesCache.ServiceInfo getServiceInfo(AuthenticatorDescription authenticatorDescription, int i) {
        return super.getServiceInfo(authenticatorDescription, i);
    }

    public AccountAuthenticatorCache(Context context) {
        super(context, "android.accounts.AccountAuthenticator", "android.accounts.AccountAuthenticator", "account-authenticator", sSerializer);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* renamed from: parseServiceAttributes */
    public AuthenticatorDescription m768parseServiceAttributes(Resources res, String packageName, AttributeSet attrs) {
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AccountAuthenticator);
        try {
            String accountType = sa.getString(2);
            int labelId = sa.getResourceId(0, 0);
            int iconId = sa.getResourceId(1, 0);
            int smallIconId = sa.getResourceId(3, 0);
            int prefId = sa.getResourceId(4, 0);
            boolean customTokens = sa.getBoolean(5, false);
            if (!TextUtils.isEmpty(accountType)) {
                return new AuthenticatorDescription(accountType, packageName, labelId, iconId, smallIconId, prefId, customTokens);
            }
            return null;
        } finally {
            sa.recycle();
        }
    }

    /* loaded from: classes.dex */
    private static class MySerializer implements XmlSerializerAndParser<AuthenticatorDescription> {
        private MySerializer() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        public void writeAsXml(AuthenticatorDescription item, TypedXmlSerializer out) throws IOException {
            out.attribute((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE, item.type);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* renamed from: createFromXml */
        public AuthenticatorDescription m769createFromXml(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
            return AuthenticatorDescription.newKey(parser.getAttributeValue((String) null, DatabaseHelper.SoundModelContract.KEY_TYPE));
        }
    }
}
