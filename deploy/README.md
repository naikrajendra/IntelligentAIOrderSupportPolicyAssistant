# Deployment notes

This project is containerized for deployment on Kubernetes-based platforms including Google Cloud Run / GKE and OpenShift.

## Build the container image

```bash
docker build -t intelligent-ai-order-support:latest .
```

## Run locally

```bash
docker run -p 8080:8080 \
  -e MONGODB_URI=mongodb://host.docker.internal:27017/orderdb \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=6379 \
  -e OPENAI_API_KEY=your-key \
  -e MCP_API_KEY=your-mcp-key \
  -e COPILOT_POLICY_AGENT_API_KEY=your-copilot-key \
  intelligent-ai-order-support:latest
```

## GCP deployment

Use the Kubernetes manifest in `deploy/kubernetes/deployment.yaml` and replace `gcr.io/PROJECT_ID/...` with your registry path.

Example:

```bash
docker tag intelligent-ai-order-support:latest gcr.io/my-project/intelligent-ai-order-support:latest
docker push gcr.io/my-project/intelligent-ai-order-support:latest
kubectl apply -f deploy/kubernetes/deployment.yaml
```

## OpenShift deployment

Use the manifest in `deploy/openshift/deploymentconfig.yaml` and replace `IMAGE_NAME` with the image available in your registry.

Example:

```bash
oc new-project order-support
oc create secret generic app-secrets \
  --from-literal=mongodb-uri='mongodb://mongo:27017/orderdb' \
  --from-literal=openai-api-key='your-key' \
  --from-literal=mcp-api-key='your-mcp-key' \
  --from-literal=copilot-policy-agent-api-key='your-copilot-key'
oc apply -f deploy/openshift/deploymentconfig.yaml
```

## Notes

- This app is designed to run behind a cluster service or route.
- For production, place secrets in the platform secret store rather than in plain YAML.
- If you are using a remote MongoDB, Redis, or MCP endpoint, set the corresponding environment variables to the external hostnames.
