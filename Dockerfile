# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV TZ=UTC \
    SERVER_PORT=8080 \
    MONGODB_URI=mongodb://mongo:27017/orderdb \
    REDIS_HOST=redis \
    REDIS_PORT=6379 \
    OPENAI_API_KEY= \
    OPENAI_MODEL=gpt-4o-mini \
    POLICY_PDF_DIRECTORY=./policies \
    MCP_API_KEY= \
    MCP_INQUIRY_BASE_URL=http://mcp-inquiry:8081 \
    MCP_PROCESSING_BASE_URL=http://mcp-processing:8082 \
    MCP_TIMEOUT_MS=15000 \
    COPILOT_POLICY_AGENT_BASE_URL=https://copilot.example.com \
    COPILOT_POLICY_AGENT_API_KEY= \
    COPILOT_POLICY_AGENT_MODEL=gpt-4o-mini \
    COPILOT_POLICY_AGENT_TIMEOUT_MS=15000 \
    ELK_EXPORT_ENABLED=false \
    ELASTICSEARCH_HOSTS=https://localhost:9200 \
    ELASTICSEARCH_USERNAME= \
    ELASTICSEARCH_PASSWORD= \
    ELK_INDEX_PREFIX=support-query-events

COPY --from=build /workspace/target/*.jar app.jar
COPY policies ./policies

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
