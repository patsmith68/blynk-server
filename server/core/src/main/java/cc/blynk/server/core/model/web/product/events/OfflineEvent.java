package cc.blynk.server.core.model.web.product.events;

import cc.blynk.server.core.model.web.product.Event;
import cc.blynk.server.core.model.web.product.EventType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 11.05.17.
 */
public class OfflineEvent extends Event {

    public int ignorePeriod;

    @Override
    public boolean isSame(String eventCode) {
        return false;
    }

    @Override
    public EventType getType() {
        return EventType.OFFLINE;
    }
}
