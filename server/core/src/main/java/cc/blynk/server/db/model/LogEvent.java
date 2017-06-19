package cc.blynk.server.db.model;

import cc.blynk.server.core.model.web.product.Event;
import cc.blynk.server.core.model.web.product.EventType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.06.16.
 */
public class LogEvent {

    public final int id;

    public final int deviceId;

    public final EventType eventType;

    public final long ts;

    public final int eventHashcode;

    public final boolean isResolved;

    private final String resolvedBy;

    public String name;
    public String description;

    //for tests mostly
    public LogEvent(int deviceId, EventType eventType, long ts, int eventHashCode, String description) {
        this(-1, deviceId, eventType, ts, eventHashCode, description, false, null);
    }

    @JsonCreator
    public LogEvent(@JsonProperty("id") int id,
                    @JsonProperty("deviceId") int deviceId,
                    @JsonProperty("eventType") EventType eventType,
                    @JsonProperty("ts") long ts,
                    @JsonProperty("eventHashcode") int eventHashcode,
                    @JsonProperty("description") String description,
                    @JsonProperty("isResolved") boolean isResolved,
                    @JsonProperty("resolvedBy") String resolvedBy) {
        this.id = id;
        this.deviceId = deviceId;
        this.eventType = eventType;
        this.ts = ts;
        this.eventHashcode = eventHashcode;
        this.description = description;
        this.isResolved = isResolved;
        this.resolvedBy = resolvedBy;
    }

    public void update(Event event) {
        if (description == null || description.isEmpty()) {
            this.description = event.description;
        }
        this.name = event.name;
    }

    @Override
    public String toString() {
        return "LogEvent{" +
                "id=" + id +
                ", deviceId=" + deviceId +
                ", eventType=" + eventType +
                ", ts=" + ts +
                ", eventHashcode=" + eventHashcode +
                ", isResolved=" + isResolved +
                ", resolvedBy='" + resolvedBy + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
