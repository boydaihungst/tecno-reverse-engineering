package com.android.server.companion;

import android.companion.AssociationInfo;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.List;
/* loaded from: classes.dex */
public interface AssociationStore {
    public static final int CHANGE_TYPE_ADDED = 0;
    public static final int CHANGE_TYPE_REMOVED = 1;
    public static final int CHANGE_TYPE_UPDATED_ADDRESS_CHANGED = 2;
    public static final int CHANGE_TYPE_UPDATED_ADDRESS_UNCHANGED = 3;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface ChangeType {
    }

    AssociationInfo getAssociationById(int i);

    Collection<AssociationInfo> getAssociations();

    List<AssociationInfo> getAssociationsByAddress(String str);

    List<AssociationInfo> getAssociationsForPackage(int i, String str);

    AssociationInfo getAssociationsForPackageWithAddress(int i, String str, String str2);

    List<AssociationInfo> getAssociationsForUser(int i);

    void registerListener(OnChangeListener onChangeListener);

    void unregisterListener(OnChangeListener onChangeListener);

    /* loaded from: classes.dex */
    public interface OnChangeListener {
        default void onAssociationChanged(int changeType, AssociationInfo association) {
            switch (changeType) {
                case 0:
                    onAssociationAdded(association);
                    return;
                case 1:
                    onAssociationRemoved(association);
                    return;
                case 2:
                    onAssociationUpdated(association, true);
                    return;
                case 3:
                    onAssociationUpdated(association, false);
                    return;
                default:
                    return;
            }
        }

        default void onAssociationAdded(AssociationInfo association) {
        }

        default void onAssociationRemoved(AssociationInfo association) {
        }

        default void onAssociationUpdated(AssociationInfo association, boolean addressChanged) {
        }
    }

    static String changeTypeToString(int changeType) {
        switch (changeType) {
            case 0:
                return "ASSOCIATION_ADDED";
            case 1:
                return "ASSOCIATION_REMOVED";
            case 2:
                return "ASSOCIATION_UPDATED";
            case 3:
                return "ASSOCIATION_UPDATED_ADDRESS_UNCHANGED";
            default:
                return "Unknown (" + changeType + ")";
        }
    }
}
