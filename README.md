# Intelligent AI Order Support Policy Assistant

A Spring Boot assistant that:
- Looks up order data from MongoDB (`orderdb`)
- Ingests policy files into a MongoDB-backed policy chunk store
- Answers support questions using order context + policy context with an OpenAI chat model

## What This Project Provides

- Order support query API: `POST /api/support/query`
- Policy ingestion API: `POST /api/support/policies/ingest`
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

## Run the Application

PowerShell:

```powershell
$env:MONGODB_URI="mongodb://localhost:27017/orderdb"
$env:OPENAI_API_KEY="your-openai-api-key"
mvn spring-boot:run
```

The API will be available at:

- `http://localhost:8080`

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
  "question": "My order is delayed. Can I get shipping fee refund or compensation?"
}
```

PowerShell call:

```powershell
$body = @{
  customerId = "C-991"
  orderId    = "O-1001"
  question   = "My order is delayed. Can I get shipping fee refund or compensation?"
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
