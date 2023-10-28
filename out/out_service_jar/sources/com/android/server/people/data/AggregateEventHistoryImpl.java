package com.android.server.people.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
/* loaded from: classes2.dex */
class AggregateEventHistoryImpl implements EventHistory {
    private final List<EventHistory> mEventHistoryList = new ArrayList();

    @Override // com.android.server.people.data.EventHistory
    public EventIndex getEventIndex(int eventType) {
        for (EventHistory eventHistory : this.mEventHistoryList) {
            EventIndex eventIndex = eventHistory.getEventIndex(eventType);
            if (!eventIndex.isEmpty()) {
                return eventIndex;
            }
        }
        return EventIndex.EMPTY;
    }

    @Override // com.android.server.people.data.EventHistory
    public EventIndex getEventIndex(Set<Integer> eventTypes) {
        EventIndex merged = null;
        for (EventHistory eventHistory : this.mEventHistoryList) {
            EventIndex eventIndex = eventHistory.getEventIndex(eventTypes);
            if (merged == null) {
                merged = eventIndex;
            } else if (!eventIndex.isEmpty()) {
                merged = EventIndex.combine(merged, eventIndex);
            }
        }
        return merged != null ? merged : EventIndex.EMPTY;
    }

    @Override // com.android.server.people.data.EventHistory
    public List<Event> queryEvents(Set<Integer> eventTypes, long startTime, long endTime) {
        List<Event> results = new ArrayList<>();
        for (EventHistory eventHistory : this.mEventHistoryList) {
            EventIndex eventIndex = eventHistory.getEventIndex(eventTypes);
            if (!eventIndex.isEmpty()) {
                List<Event> queryResults = eventHistory.queryEvents(eventTypes, startTime, endTime);
                results = combineEventLists(results, queryResults);
            }
        }
        return results;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addEventHistory(EventHistory eventHistory) {
        this.mEventHistoryList.add(eventHistory);
    }

    private List<Event> combineEventLists(List<Event> lhs, List<Event> rhs) {
        List<Event> results = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < lhs.size() && j < rhs.size()) {
            if (lhs.get(i).getTimestamp() < rhs.get(j).getTimestamp()) {
                results.add(lhs.get(i));
                i++;
            } else {
                int i2 = j + 1;
                results.add(rhs.get(j));
                j = i2;
            }
        }
        int j2 = lhs.size();
        if (i < j2) {
            results.addAll(lhs.subList(i, lhs.size()));
        } else if (j < rhs.size()) {
            results.addAll(rhs.subList(j, rhs.size()));
        }
        return results;
    }
}
