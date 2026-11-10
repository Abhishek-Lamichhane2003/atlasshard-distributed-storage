package com.atlasshard.storage.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NodeRingService {
    public record Node(String id, String region, String status, int capacityPct) {}
    private static final int VIRTUAL_NODES = 64;
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();

    public NodeRingService() {
        nodes.put("node-a", new Node("node-a", "us-west", "healthy", 41));
        nodes.put("node-b", new Node("node-b", "us-west", "healthy", 56));
        nodes.put("node-c", new Node("node-c", "us-central", "healthy", 48));
        nodes.put("node-d", new Node("node-d", "us-east", "healthy", 61));
        nodes.put("node-e", new Node("node-e", "us-east", "healthy", 35));
        nodes.put("node-f", new Node("node-f", "eu-west", "healthy", 52));
    }

    public List<Node> all() {
        return nodes.values().stream().sorted(Comparator.comparing(Node::id)).toList();
    }

    public List<Node> replicas(String objectHash) {
        NavigableMap<Long, String> ring = buildRing();
        if (ring.isEmpty()) return List.of();
        long point = hash64(objectHash);
        LinkedHashSet<String> chosen = new LinkedHashSet<>();
        walk(ring.tailMap(point, true), chosen);
        if (chosen.size() < 3) walk(ring, chosen);
        return chosen.stream().limit(3).map(nodes::get).filter(Objects::nonNull).toList();
    }

    public Node toggle(String id) {
        Node n = Optional.ofNullable(nodes.get(id)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "node not found"));
        Node updated = new Node(n.id(), n.region(), n.status().equals("healthy") ? "offline" : "healthy", n.capacityPct());
        nodes.put(id, updated);
        return updated;
    }

    private NavigableMap<Long, String> buildRing() {
        NavigableMap<Long, String> ring = new TreeMap<>();
        all().stream().filter(n -> n.status().equals("healthy")).forEach(n -> {
            for (int i = 0; i < VIRTUAL_NODES; i++) ring.put(hash64(n.id() + "#" + i), n.id());
        });
        return ring;
    }

    private void walk(Map<Long, String> section, Set<String> chosen) {
        for (String nodeId : section.values()) {
            chosen.add(nodeId);
            if (chosen.size() >= 3) return;
        }
    }

    private long hash64(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest).getLong() & Long.MAX_VALUE;
        } catch (Exception e) {
            return Integer.toUnsignedLong(value.hashCode());
        }
    }
}
