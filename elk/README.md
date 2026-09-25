# ELK export configuration

This directory contains a sample Logstash pipeline for exporting MongoDB support events into Elasticsearch.

## Purpose

The application stores each support query in MongoDB via the `support_query_events` collection. This Logstash configuration moves those records into Elasticsearch so they can be visualized in Kibana.

## Remote configuration

This setup supports a remote Elasticsearch endpoint. Replace the example values in `logstash.env.example` with your real cluster details.

## Run Logstash

```bash
export $(grep -v '^#' elk/logstash.env.example | xargs)
logstash -f elk/logstash-mongo-to-elastic.conf
```

## Expected data flow

MongoDB `support_query_events` -> Logstash -> Elasticsearch `support-query-events-*`

Fields exported include:
- customerId
- orderId
- question
- answer
- status
- confidenceScore
- promptTokens
- completionTokens
- totalTokens
- createdAt
