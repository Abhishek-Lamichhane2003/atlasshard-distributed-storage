package com.atlasshard.storage.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventService {
    private final KafkaTemplate<String, String> kafka;

    public EventService(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    public void publish(String type, String detail) {
        kafka.send("storage-events", type + "|" + detail);
    }
}
