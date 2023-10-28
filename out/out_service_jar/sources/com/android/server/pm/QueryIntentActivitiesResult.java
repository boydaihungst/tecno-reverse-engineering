package com.android.server.pm;

import android.content.pm.ResolveInfo;
import java.util.List;
/* loaded from: classes2.dex */
public final class QueryIntentActivitiesResult {
    public boolean addInstant;
    public List<ResolveInfo> answer;
    public List<ResolveInfo> result;
    public boolean sortResult;

    /* JADX INFO: Access modifiers changed from: package-private */
    public QueryIntentActivitiesResult(List<ResolveInfo> l) {
        this.sortResult = false;
        this.addInstant = false;
        this.result = null;
        this.answer = null;
        this.answer = l;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public QueryIntentActivitiesResult(boolean s, boolean a, List<ResolveInfo> l) {
        this.sortResult = false;
        this.addInstant = false;
        this.result = null;
        this.answer = null;
        this.sortResult = s;
        this.addInstant = a;
        this.result = l;
    }
}
