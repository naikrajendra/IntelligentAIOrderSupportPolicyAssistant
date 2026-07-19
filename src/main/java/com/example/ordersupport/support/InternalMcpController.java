package com.example.ordersupport.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mcp")
public class InternalMcpController {

    private final McpToolDispatcher mcpToolDispatcher;
    private final String mcpApiKey;

    public InternalMcpController(
            McpToolDispatcher mcpToolDispatcher,
            @Value("${app.mcp.api-key:}") String mcpApiKey
    ) {
        this.mcpToolDispatcher = mcpToolDispatcher;
        this.mcpApiKey = mcpApiKey;
    }

    @PostMapping
    public ResponseEntity<McpRpcResponse> handle(
            @RequestBody McpRpcRequest request,
            @RequestHeader(value = "X-MCP-API-KEY", required = false) String providedApiKey
    ) {
        String requestId = request == null ? null : request.id();

        if (!StringUtils.hasText(mcpApiKey)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(McpRpcResponse.failure(requestId, -32000, "MCP auth is not configured"));
        }

        if (!mcpApiKey.equals(providedApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(McpRpcResponse.failure(requestId, -32001, "Unauthorized MCP request"));
        }

        return ResponseEntity.ok(mcpToolDispatcher.dispatch(request));
    }
}
