# Intelligent AI Order Support Policy Assistant

A Spring Boot assistant that:
- Looks up order data from MongoDB (`orderdb`)
- Ingests policy files into a MongoDB-backed policy chunk store
- Answers support questions using order context + policy context with an OpenAI chat model

## What This Project Provides

- Order support query API: `POST /api/support/query`
- Policy ingestion API: `POST /api/support/policies/ingest`
- Internal MCP server API: `POST /api/mcp`
- Local policy examples in the `policies/` folder
- Seed order records inserted automatically on first startup

## Tech Stack

- Java 21
- Spring Boot 3.3.x
- Spring Data MongoDB
- Spring AI OpenAI Chat

## Prerequisites

- Java 21 installed
- Maven installed
- MongoDB running locally on port 27017
- OpenAI API key

## Configuration

The app reads these environment variables:

- `MONGODB_URI` (default: `mongodb://localhost:27017/orderdb`)
- `OPENAI_API_KEY` (required for chat responses)
- `OPENAI_MODEL` (default: `gpt-4o-mini`)
- `SERVER_PORT` (default: `8080`)
- `POLICY_PDF_DIRECTORY` (default: `./policies`)
- `MCP_API_KEY` (required for calling `/api/mcp`)

## Run the Application

PowerShell:

```powershell
$env:MONGODB_URI="mongodb://localhost:27017/orderdb"
$env:OPENAI_API_KEY="your-openai-api-key"
$env:MCP_API_KEY="your-internal-mcp-key"
mvn spring-boot:run
```

The API will be available at:

- `http://localhost:8080`

Web UI:

- `http://localhost:8080/`

## Load Policy Files (RAG Ingestion)

This endpoint scans a directory and ingests supported policy files:
- `.pdf`
- `.txt`
- `.md`

### Option 1: Use default folder (`./policies`)

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/support/policies/ingest"
```

### Option 2: Pass a custom directory

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/support/policies/ingest?directory=./policies"
```

Example response:

```json
{
  "filesIngested": 3,
  "chunksIngested": 18
}
```

## Ask an Order Support Question

Endpoint:

- `POST /api/support/query`

Request payload:

```json
{
  "customerId": "C-991",
  "orderId": "O-1001",
  "question": "My order is delayed. Can I get shipping fee refund or compensation?",
  "confirmCancellation": false
}
```

PowerShell call:

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

Example response shape:

```json
{
  "answer": "...assistant response..."
}
```

Cancellation safety behavior:

- The assistant does not execute cancellation automatically.
- It checks order status and policy context first.
- It only executes `cancelOrder` when explicit confirmation is provided.

To explicitly confirm cancellation, send:

```json
{
  "customerId": "C-991",
  "orderId": "O-1001",
  "question": "Please cancel this order. I confirm cancellation.",
  "confirmCancellation": true
}
```

## Internal MCP Tools For Transactional Operations

The app includes an internal MCP server that exposes real-time order tools for chatbot use:

- `getOrderStatus(id)`
- `cancelOrder(id)`

Endpoint:

- `POST /api/mcp`
- Required header: `X-MCP-API-KEY: <your MCP_API_KEY>`

PowerShell example with header:

```powershell
$mcpHeaders = @{ "X-MCP-API-KEY" = $env:MCP_API_KEY }
```

### List tools

```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "tools/list",
  "params": {}
}
```

PowerShell call:

```powershell
$listBody = @{
  jsonrpc = "2.0"
  id      = "1"
  method  = "tools/list"
  params  = @{}
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/mcp" `
  -Headers $mcpHeaders `
  -ContentType "application/json" `
  -Body $listBody
```

### Call getOrderStatus

```json
{
  "jsonrpc": "2.0",
  "id": "2",
  "method": "tools/call",
  "params": {
    "name": "getOrderStatus",
    "arguments": {
      "id": "O-1001"
    }
  }
}
```

PowerShell call:

```powershell
$statusBody = @{
  jsonrpc = "2.0"
  id      = "2"
  method  = "tools/call"
  params  = @{
    name      = "getOrderStatus"
    arguments = @{ id = "O-1001" }
  }
} | ConvertTo-Json -Depth 8

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/mcp" `
  -Headers $mcpHeaders `
  -ContentType "application/json" `
  -Body $statusBody
```

### Call cancelOrder

```json
{
  "jsonrpc": "2.0",
  "id": "3",
  "method": "tools/call",
  "params": {
    "name": "cancelOrder",
    "arguments": {
      "id": "O-1001"
    }
  }
}
```

PowerShell call:

```powershell
$cancelBody = @{
  jsonrpc = "2.0"
  id      = "3"
  method  = "tools/call"
  params  = @{
    name      = "cancelOrder"
    arguments = @{ id = "O-1001" }
  }
} | ConvertTo-Json -Depth 8

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/mcp" `
  -Headers $mcpHeaders `
  -ContentType "application/json" `
  -Body $cancelBody
```

Notes:

- The chatbot automatically invokes `getOrderStatus` for each support question.
- If the question includes words like "cancel" or "cancellation", the chatbot also invokes `cancelOrder`.

## Seed Data Included

On startup, the app upserts two default records. You can also trigger the same upsert on demand:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/support/orders/seed"
```

Example response:

```json
{
  "ordersSeeded": 2
}
```

Default records:

- `customerId: C-991`, `orderId: O-1001`, status `IN_TRANSIT`
- `customerId: C-992`, `orderId: O-1002`, status `DELIVERED`

## Local Policies Included

The project includes these starter policy documents:

- `policies/returns-and-refunds-policy.md`
- `policies/shipping-and-delivery-policy.md`
- `policies/cancellations-and-modifications-policy.md`

## Troubleshooting

- If query answers mention missing policy context, run ingestion first.
- If order is not found, verify `customerId` + `orderId` values or check MongoDB data in `orderdb.orders`.
- If OpenAI calls fail, verify `OPENAI_API_KEY` and outbound internet access.
