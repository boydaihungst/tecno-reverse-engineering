package com.android.server.people.prediction;

import com.android.server.people.data.ConversationInfo;
import com.android.server.people.data.EventHistory;
/* loaded from: classes2.dex */
class ConversationData {
    private final ConversationInfo mConversationInfo;
    private final EventHistory mEventHistory;
    private final String mPackageName;
    private final int mUserId;

    ConversationData(String packageName, int userId, ConversationInfo conversationInfo, EventHistory eventHistory) {
        this.mPackageName = packageName;
        this.mUserId = userId;
        this.mConversationInfo = conversationInfo;
        this.mEventHistory = eventHistory;
    }

    String getPackageName() {
        return this.mPackageName;
    }

    int getUserId() {
        return this.mUserId;
    }

    ConversationInfo getConversationInfo() {
        return this.mConversationInfo;
    }

    EventHistory getEventHistory() {
        return this.mEventHistory;
    }
}
