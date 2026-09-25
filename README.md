# Intelligent AI Order Support Policy Assistant

A Spring Boot application for order-support workflows that combines order data, policy retrieval, remote MCP tools, and AI-powered guidance for support agents.

## Overview

The assistant supports a real business workflow:

- looks up customer order data from MongoDB
- ingests policy documents from the local policies directory
- stores policy content as searchable chunks in MongoDB
- calls remote MCP servers for order inquiry and processing tools
- uses a Copilot-backed policy agent for policy grounding and guidance
- keeps the active chat session in Redis
- archives completed conversations to MongoDB
- enforces a per-user rate limit of 50,000 tokens per rolling hour
- compacts older chat history before sending the prompt to the LLM so the model still receives relevant context without exceeding token limits

## Technology Stack

- Java 21
- Spring Boot 3.3.x
- Spring AI OpenAI Chat
- Spring Data MongoDB
- Spring Data Redis
- Redis for live session state and rate limiting
- MongoDB for orders, policy chunks, and archived chats
- Remote MCP servers for inquiry and processing tools
- Mermaid C4 architecture diagrams

## Main Features

- POST /api/support/query for business support responses
- POST /api/support/policies/ingest to load policy documents
- POST /api/support/orders/seed to seed sample order records
- POST /api/support/chat/end to persist the active conversation from Redis to MongoDB
- remote MCP integration for order status and processing actions
- Copilot policy agent integration for policy-aware answers
- Redis-backed token enforcement with a rolling 1-hour quota
- compact prompt construction that preserves recent context and a short earlier summary

## Architecture

See [docs/architecture.md](docs/architecture.md) for the detailed system, container, and component diagrams.

## Configuration

The app reads the following configuration values:

- MONGODB_URI, default: mongodb://localhost:27017/orderdb
- REDIS_HOST, default: localhost
- REDIS_PORT, default: 6379
- OPENAI_API_KEY, required for model access
- OPENAI_MODEL, default: gpt-4o-mini
- SERVER_PORT, default: 8080
- POLICY_PDF_DIRECTORY, default: ./policies
- MCP_API_KEY, used for MCP authentication
- MCP_INQUIRY_BASE_URL, default: http://localhost:8081
- MCP_PROCESSING_BASE_URL, default: http://localhost:8082
- MCP_INQUIRY_API_KEY, optional
- MCP_PROCESSING_API_KEY, optional
- MCP_TIMEOUT_MS, default: 15000
- COPILOT_POLICY_AGENT_BASE_URL, default: https://copilot.example.com
- COPILOT_POLICY_AGENT_API_KEY, required for the policy assistant
- COPILOT_POLICY_AGENT_MODEL, default: gpt-4o-mini
- COPILOT_POLICY_AGENT_TIMEOUT_MS, default: 15000

## Run the Application

PowerShell example:

```powershell
$env:MONGODB_URI="mongodb://localhost:27017/orderdb"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:OPENAI_API_KEY="your-openai-api-key"
$env:MCP_API_KEY="your-internal-mcp-key"
$env:MCP_INQUIRY_BASE_URL="http://localhost:8081"
$env:MCP_PROCESSING_BASE_URL="http://localhost:8082"
$env:COPILOT_POLICY_AGENT_BASE_URL="https://copilot.example.com"
$env:COPILOT_POLICY_AGENT_API_KEY="your-copilot-key"
mvn spring-boot:run
```

The app runs at:

- http://localhost:8080
- Web UI: http://localhost:8080/

## UI and API flow

The browser UI calls the support API when a user enters a question.

### Question submission endpoint

```http
POST /api/support/query
Content-Type: application/json
```

Request body example:

```json
{
  "customerId": "C-991",
  "orderId": "O-1001",
  "question": "My order is delayed. Can I get a shipping refund?",
  "confirmCancellation": false
}
```

Response example:

```json
{
  "answer": "Based on the order status and applicable shipping policy, this shipment is eligible for a partial review..."
}
```

## Use the APIs

### Load policy files

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/support/policies/ingest"
```

### Seed sample orders

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/support/orders/seed"
```

### Ask a support question

```powershell
$body = @{
  customerId = "C-991"
  orderId    = "O-1001"
  question   = "My order is delayed. Can I get shipping fee refund or compensation?"
  confirmCancellation = $false
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/support/query" `
  -ContentType "application/json" `
  -Body $body
```

### Finish a chat session and persist it to MongoDB

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/support/chat/end?userId=C-991"
```

## Remote MCP and Copilot policy architecture

The application is designed around remote services rather than local in-process tool calls:

- inquiry MCP server: order lookup, metadata and status tools
- processing MCP server: update or cancellation workflows
- Copilot orderPolicyAssistant: policy retrieval and policy-grounded guidance
- OpenAI model: final business answer generation

Live interaction state is kept in Redis while the session is active. When the user ends the chat, the completed conversation is moved to MongoDB for durable archival and later review.

## Rate limiting and conversation memory

The platform enforces a Redis-backed token budget per user:

- maximum: 50,000 tokens
- window: 1 hour
- scope: per user/customer

Before the model call, the service compacts the flow into a smaller, optimized history:

- keeps the most recent 3-6 turns verbatim
- summarizes the older conversation into a brief continuity record
- includes the most relevant business facts such as order ID, policy constraints, and pending confirmations

This reduces token waste while preserving the decision context needed by the model.

## Seed data and local policies

The project includes starter policy documents:

- policies/returns-and-refunds-policy.md
- policies/shipping-and-delivery-policy.md
- policies/cancellations-and-modifications-policy.md

Default seeded records:

- customerId: C-991, orderId: O-1001, status: IN_TRANSIT
- customerId: C-992, orderId: O-1002, status: DELIVERED

## Troubleshooting

- If policy answers seem thin or incomplete, run ingestion first.
- If an order is missing, verify the customerId and orderId values or inspect the MongoDB order collection.
- If OpenAI calls fail, confirm OPENAI_API_KEY and outbound access.
- If Redis quota errors appear, the user has exhausted the rolling 1-hour token limit.
- If chat history is not persisting, call the chat-end endpoint to move the active session to MongoDB.


