package com.android.server.people.data;

import java.util.function.Predicate;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class UserData$$ExternalSyntheticLambda1 implements Predicate {
    public final /* synthetic */ UserData f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.server.people.data.UserData.createPackageData(java.lang.String):com.android.server.people.data.PackageData, com.android.server.people.data.UserData.loadUserData():void] */
    public /* synthetic */ UserData$$ExternalSyntheticLambda1(UserData userData) {
        this.f$0 = userData;
    }

    @Override // java.util.function.Predicate
    public final boolean test(Object obj) {
        return UserData.$r8$lambda$6PdLrJGHlGwu_DoqTJnYEj3j0A0(this.f$0, (String) obj);
    }
}
