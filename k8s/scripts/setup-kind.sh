#!/bin/bash
set -e
echo "🚀 Setting up Kind cluster for E-Commerce Platform"
# Check if kind is installed
if ! command -v kind &> /dev/null; then
    echo "❌ Kind is not installed. Install it from: https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
    exit 1
fi
# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl is not installed. Install it from: https://kubernetes.io/docs/tasks/tools/"
    exit 1
fi
# Delete existing cluster if it exists
if kind get clusters | grep -q "ecommerce-platform"; then
    echo "🗑️  Deleting existing cluster..."
    kind delete cluster --name ecommerce-platform
fi
# Create cluster
echo "📦 Creating Kind cluster..."
kind create cluster --config k8s/scripts/kind-cluster-config.yaml --wait 5m
# Verify cluster
echo "✅ Verifying cluster..."
kubectl cluster-info --context kind-ecommerce-platform
# Create namespaces
echo "📁 Creating namespaces..."
kubectl apply -f k8s/namespaces/namespaces.yaml
# Install NGINX Ingress Controller
echo "🌐 Installing NGINX Ingress Controller..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
# Wait for ingress controller
echo "⏳ Waiting for ingress controller..."
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s
# Install Metrics Server (for HPA)
echo "📊 Installing Metrics Server..."
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
# Patch Metrics Server for Kind (disable TLS verification)
kubectl patch deployment metrics-server -n kube-system --type='json' \
  -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--kubelet-insecure-tls"}]'
echo ""
echo "✅ Kind cluster setup complete!"
echo ""
echo "Cluster info:"
kubectl get nodes
echo ""
echo "Namespaces:"
kubectl get namespaces | grep platform
echo ""
echo "Next steps:"
echo "  1. Deploy infrastructure: ./k8s/scripts/deploy-infra.sh"
echo "  2. Deploy services: ./k8s/scripts/deploy-services.sh"
echo "  3. Check status: kubectl get all -n platform-core"
