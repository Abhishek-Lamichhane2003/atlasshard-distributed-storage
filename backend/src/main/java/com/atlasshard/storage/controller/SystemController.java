package com.atlasshard.storage.controller;

import com.atlasshard.storage.repo.AuditEventRepository;
import com.atlasshard.storage.service.FileService;
import com.atlasshard.storage.service.NodeRingService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final NodeRingService nodes;
    private final AuditEventRepository audit;
    private final FileService files;

    public SystemController(NodeRingService nodes, AuditEventRepository audit, FileService files) {
        this.nodes = nodes;
        this.audit = audit;
        this.files = files;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        long healthy = nodes.all().stream().filter(node -> node.status().equals("healthy")).count();
        long logical = files.logicalBytes();
        long physical = files.uniqueBytes();
        double savings = logical == 0 ? 0.0 : Math.max(0.0, (1.0 - ((double) physical / logical)) * 100.0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("product", "AtlasShard");
        result.put("nodes", nodes.all().size());
        result.put("healthyNodes", healthy);
        result.put("replicationFactor", 3);
        result.put("hashing", "SHA-256 + consistent hash ring");
        result.put("fileVersions", files.fileCount());
        result.put("uniqueObjects", files.uniqueObjectCount());
        result.put("logicalBytes", logical);
        result.put("physicalBytes", physical);
        result.put("dedupSavingsPct", Math.round(savings * 10.0) / 10.0);
        return result;
    }

    @GetMapping("/activity")
    public Object activity() {
        return audit.findTop50ByOrderByCreatedAtDesc();
    }
}
