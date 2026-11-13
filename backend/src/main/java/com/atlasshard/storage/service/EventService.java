package com.atlasshard.storage.service;

import com.atlasshard.storage.model.AuditEvent;
import com.atlasshard.storage.repo.AuditEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventService {
    private final KafkaTemplate<String, String> kafka;
    private final AuditEventRepository auditEvents;
    private final boolean direct;

    public EventService(
            KafkaTemplate<String, String> kafka,
            AuditEventRepository auditEvents,
            @Value("${app.events.mode:kafka}") String mode) {
        this.kafka = kafka;
        this.auditEvents = auditEvents;
        this.direct = "direct".equalsIgnoreCase(mode);
    }

    public void publish(String type, String detail) {
        if (!direct) {
            kafka.send("storage-events", type + "|" + detail);
            return;
        }
        AuditEvent event = new AuditEvent();
        event.setType(type);
        event.setDetail(detail);
        auditEvents.save(event);
    }
}
