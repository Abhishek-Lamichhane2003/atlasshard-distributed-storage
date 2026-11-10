package com.atlasshard.storage.controller;

import com.atlasshard.storage.service.EventService;
import com.atlasshard.storage.service.NodeRingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {
    private final NodeRingService nodes;
    private final EventService events;

    public NodeController(NodeRingService nodes, EventService events) {
        this.nodes = nodes;
        this.events = events;
    }

    @GetMapping
    public List<NodeRingService.Node> all() {
        return nodes.all();
    }

    @GetMapping("/placement/{hash}")
    public List<NodeRingService.Node> placement(@PathVariable String hash) {
        return nodes.replicas(hash);
    }

    @PostMapping("/{id}/toggle")
    public NodeRingService.Node toggle(@PathVariable String id) {
        NodeRingService.Node node = nodes.toggle(id);
        events.publish(node.status().equals("healthy") ? "NODE_RECOVERED" : "NODE_OFFLINE", node.id());
        return node;
    }
}
