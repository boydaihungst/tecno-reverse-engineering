package com.android.server.apphibernation;

import android.util.Slog;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import defpackage.CompanionAppsPermissions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
final class GlobalLevelHibernationProto implements ProtoReadWriter<List<GlobalLevelState>> {
    private static final String TAG = "GlobalLevelHibernationProtoReadWriter";

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.apphibernation.ProtoReadWriter
    public void writeToProto(ProtoOutputStream stream, List<GlobalLevelState> data) {
        int size = data.size();
        for (int i = 0; i < size; i++) {
            long token = stream.start(CompanionAppsPermissions.APP_PERMISSIONS);
            GlobalLevelState state = data.get(i);
            stream.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, state.packageName);
            stream.write(1133871366146L, state.hibernated);
            stream.write(1112396529667L, state.savedByte);
            stream.end(token);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.apphibernation.ProtoReadWriter
    public List<GlobalLevelState> readFromProto(ProtoInputStream stream) throws IOException {
        List<GlobalLevelState> list = new ArrayList<>();
        while (stream.nextField() != -1) {
            if (stream.getFieldNumber() == 1) {
                GlobalLevelState state = new GlobalLevelState();
                long token = stream.start((long) CompanionAppsPermissions.APP_PERMISSIONS);
                while (stream.nextField() != -1) {
                    switch (stream.getFieldNumber()) {
                        case 1:
                            state.packageName = stream.readString((long) CompanionAppsPermissions.AppPermissions.PACKAGE_NAME);
                            break;
                        case 2:
                            state.hibernated = stream.readBoolean(1133871366146L);
                            break;
                        case 3:
                            state.savedByte = stream.readLong(1112396529667L);
                            break;
                        default:
                            Slog.w(TAG, "Undefined field in proto: " + stream.getFieldNumber());
                            break;
                    }
                }
                stream.end(token);
                list.add(state);
            }
        }
        return list;
    }
}
