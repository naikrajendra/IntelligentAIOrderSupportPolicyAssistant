# Architecture

This document captures the system architecture of the Intelligent AI Order Support Policy Assistant using Mermaid C4 diagrams.

## 1) System Context Diagram

```mermaid
C4Context
title System Context - Intelligent AI Order Support Policy Assistant

Person(supportAgent, "Support Agent", "Customer support user asking about order status, refunds, or cancellations.")
Person(admin, "Operations/Admin", "Loads policy content and seeds sample order data.")
System(orderSupportSystem, "Intelligent AI Order Support Policy Assistant", "Spring Boot app that answers support questions using order data, policies, and an LLM.")
System_Ext(mongo, "MongoDB", "Stores order snapshots and policy chunks.")
System_Ext(policyDocs, "Policy Documents", "Markdown, PDF, and TXT policy content in the policies folder.")
System_Ext(openAi, "OpenAI API", "LLM used to generate policy-aware responses.")

Rel(supportAgent, orderSupportSystem, "Asks order and policy questions", "HTTPS")
Rel(admin, orderSupportSystem, "Uploads policy files and seeds orders", "HTTPS")
Rel(orderSupportSystem, mongo, "Reads and writes order and policy data", "MongoDB")
Rel(orderSupportSystem, policyDocs, "Ingests local policy files", "File system")
Rel(orderSupportSystem, openAi, "Requests business response generation", "HTTPS")
```

## 2) Container Diagram

```mermaid
C4Container
title Container Diagram - Intelligent AI Order Support Policy Assistant

Person(supportAgent, "Support Agent", "Uses the assistant to answer order support questions.")
Person(admin, "Operations/Admin", "Manages policy ingestion and sample data.")

System_Boundary(c1, "Order Support Platform") {
    Container(webUi, "Web UI", "HTML, JavaScript", "Browser UI for support interactions.")
    Container(api, "Order Support API", "Java, Spring Boot", "Exposes support, ingestion, and seeding endpoints.")
    Container(mcpServer, "Internal MCP Server", "Java, Spring Boot, JSON-RPC", "Exposes transactional order tools.")
    Container(aiLayer, "AI Reasoning Layer", "Spring AI, OpenAI", "Combines order context, policy chunks, and LLM reasoning.")
    ContainerDb(mongo, "MongoDB", "MongoDB", "Stores order snapshots and policy chunk documents.")
}

System_Ext(policyDocs, "Policy Documents", "Markdown, PDF, and TXT files.")
System_Ext(openAi, "OpenAI API", "Generative model used for policy-aware answers.")

Rel(supportAgent, webUi, "Uses", "HTTPS")
Rel(webUi, api, "POST /api/support/query", "HTTPS")
Rel(admin, api, "POST /api/support/policies/ingest and /api/support/orders/seed", "HTTPS")
Rel(api, aiLayer, "Builds prompt with order + policy context", "Java")
Rel(aiLayer, openAi, "Calls chat completion", "HTTPS")
Rel(api, mongo, "Reads/writes orders and policy chunks", "MongoDB")
Rel(api, mcpServer, "Invokes order tools over internal MCP", "JSON-RPC")
Rel(mcpServer, mongo, "Reads and updates order records", "MongoDB")
Rel(api, policyDocs, "Loads files for policy ingestion", "Filesystem")
Rel(policyDocs, mongo, "Stored as policy chunks", "Bulk ingest")
```

## 3) Component Diagram

```mermaid
C4Component
title Component Diagram - Order Support API and MCP Server

Container_Boundary(apiBoundary, "Order Support API") {
    Component(controller, "OrderSupportController", "REST Controller", "Handles /api/support/query, /policies/ingest, and /orders/seed.")
    Component(orderService, "OrderSupportService", "Spring Service", "Fetches order status, evaluates cancellation intent, retrieves policy context, and asks the LLM.")
    Component(policyIngestion, "PolicyIngestionService", "Spring Service", "Reads policy files, extracts text, splits into chunks, and stores them.")
    Component(policySearch, "PolicyChunkSearchService", "Spring Service", "Retrieves the most relevant policy chunks for the customer question.")
    Component(seedService, "OrderSeedService", "Spring Service", "Seeds default order records for demo/testing.")
    Component(mcpClient, "InternalMcpToolClient", "Client", "Calls internal MCP tools to retrieve order status and cancel orders.")
}

Container_Boundary(mcpBoundary, "Internal MCP Server") {
    Component(mcpController, "InternalMcpController", "REST Controller", "Exposes MCP endpoint for tool discovery and execution.")
    Component(dispatcher, "McpToolDispatcher", "Dispatcher", "Routes JSON-RPC tool calls to service implementations.")
    Component(orderToolService, "OrderMcpToolService", "Spring Service", "Implements getOrderStatus and cancelOrder.")
    Component(orderRepo, "OrderRepository", "MongoRepository", "Persists and queries order snapshots.")
}

Component(aiAdapter, "ChatClient / Spring AI", "External API Adapter", "Builds chat prompts and invokes OpenAI.")
Component(policyRepo, "PolicyChunkRepository", "MongoRepository", "Stores the ingested policy chunks.")
ComponentDb(mongo, "MongoDB", "Database", "Order and policy document storage.")

Rel(controller, orderService, "Calls generatePolicyAwareAnswer()")
Rel(controller, policyIngestion, "Calls ingestFromDirectory()")
Rel(controller, seedService, "Calls seedDefaultOrders()")
Rel(orderService, mcpClient, "Gets order status / cancellation actions")
Rel(orderService, policySearch, "Fetches policy context")
Rel(orderService, aiAdapter, "Sends prompt + context")
Rel(policyIngestion, policyRepo, "Saves chunked policy documents")
Rel(policySearch, policyRepo, "Queries relevant policy chunks")
Rel(mcpClient, mcpController, "POST /api/mcp requests")
Rel(mcpController, dispatcher, "Dispatches JSON-RPC tool call")
Rel(dispatcher, orderToolService, "Routes getOrderStatus / cancelOrder")
Rel(orderToolService, orderRepo, "Reads/writes order data")
Rel(aiAdapter, mongo, "Reads policy/order context via app services")
Rel(orderRepo, mongo, "Persists order snapshots")
Rel(policyRepo, mongo, "Persists policy chunks")
```

## Summary

This application follows a familiar pattern for an AI-enabled support assistant:

- a user-facing API layer
- a retrieval layer for policy documents
- a transactional layer for order operations via MCP
- an LLM reasoning layer for natural-language support
a MongoDB persistence layer for both operational and knowledge data
