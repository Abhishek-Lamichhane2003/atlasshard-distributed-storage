package com.atlasshard.storage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NodeRingServiceTest {
    @Test
    void returnsThreeDistinctHealthyReplicas() {
        NodeRingService service = new NodeRingService();
        List<NodeRingService.Node> replicas = service.replicas("abc");
        assertEquals(3, replicas.size());
        assertEquals(3, replicas.stream().map(NodeRingService.Node::id).distinct().count());
        assertTrue(replicas.stream().allMatch(node -> node.status().equals("healthy")));
    }

    @Test
    void placementIsDeterministicWhileRingIsStable() {
        NodeRingService service = new NodeRingService();
        List<String> first = service.replicas("object-hash").stream().map(NodeRingService.Node::id).toList();
        List<String> second = service.replicas("object-hash").stream().map(NodeRingService.Node::id).toList();
        assertEquals(first, second);
    }

    @Test
    void failedNodeIsRemovedFromPlacement() {
        NodeRingService service = new NodeRingService();
        String failed = service.replicas("abc").getFirst().id();
        service.toggle(failed);
        assertFalse(service.replicas("abc").stream().anyMatch(node -> node.id().equals(failed)));
    }
}
