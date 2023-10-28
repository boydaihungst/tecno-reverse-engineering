package com.transsion.hubcore.database.sqlite;

import android.database.sqlite.SQLiteDatabase;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranSQLiteDatabase {
    public static final TranClassInfo<ITranSQLiteDatabase> classInfo = new TranClassInfo<>("com.transsion.hubcore.database.sqlite.TranSQLiteDatabaseImpl", ITranSQLiteDatabase.class, new Supplier() { // from class: com.transsion.hubcore.database.sqlite.ITranSQLiteDatabase$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranSQLiteDatabase.lambda$static$0();
        }
    });

    static /* synthetic */ ITranSQLiteDatabase lambda$static$0() {
        return new ITranSQLiteDatabase() { // from class: com.transsion.hubcore.database.sqlite.ITranSQLiteDatabase.1
        };
    }

    static ITranSQLiteDatabase instance() {
        return classInfo.getImpl();
    }

    default void boostAndroBenchSQLiteScore(String path, SQLiteDatabase db) {
    }
}
