package com.android.internal.widget.commonphrase;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes4.dex */
public class CommonPhraseRepository {
    public static final Uri CONTENT_URI = Uri.parse("content://com.hoffnung.commonphrase.PhraseProvider/words");
    public static final String COUNT = "count";
    public static final String PROVIDER_NAME = "com.hoffnung.commonphrase.PhraseProvider";
    public static final String WORD = "word";

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE, CONST] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [45=4] */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x003e, code lost:
        if (r8 == null) goto L11;
     */
    /* JADX WARN: Code restructure failed: missing block: B:16:0x0040, code lost:
        r8.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x0044, code lost:
        return r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x0035, code lost:
        if (r8 != null) goto L13;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static List<String> queryWords(Context context) {
        List<String> tempList = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = null;
        try {
            try {
                cursor = resolver.query(CONTENT_URI, null, "word GLOB ?", new String[]{"*"}, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int columnIndex = cursor.getColumnIndex("word");
                        String word = cursor.getString(columnIndex);
                        tempList.add(word);
                    }
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }
}
