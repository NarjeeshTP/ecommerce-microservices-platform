#!/bin/bash
set -e
echo "🚀 Setting up Minikube cluster for E-Commerce Platform"
# Check if minikube is installed
if ! command -v minikube &> /dev/null; then
    echo "❌ Minikube is not installed. Install it from: https://minikube.sigs.k8s.io/docs/start/"
    exit 1
fi
# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl is not installed. Install it from: https://kubernetes.io/docs/tasks/tools/"
    exit 1
fi
# Delete existing cluster if it exists
if minikube status &> /dev/null; then
    echo "🗑️  Deleting existing cluster..."
    minikube delete
fi
# Create cluster with sufficient resources
echo "📦 Creating Minikube cluster..."
minikube start \
  --cpus=4 \
  --memory=8192 \
  --disk-size=40gb \
  --driver=docker \
  --kubernetes-version=v1.28.0 \
  --addons=ingress,metrics-server,dashboard
# Verify cluster
echo "✅ Verifying cluster..."
kubectl cluster-info
# Create namespaces
echo "📁 Creating namespaces..."
kubectl apply -f k8s/namespaces/namespaces.yaml
# Enable addons
echo "🔧 Enabling Minikube addons..."
minikube addons enable ingress
minikube addons enable metrics-server
minikube addons enable dashboard
echo ""
echo "✅ Minikube cluster setup complete!"
echo ""
echo "Cluster info:"
kubectl get nodes
echo ""
echo "Namespaces:"
kubectl get namespaces | grep platform
echo ""
echo "Minikube IP: $(minikube ip)"
echo ""
echo "Next steps:"
echo "  1. Deploy infrastructure: ./k8s/scripts/deploy-infra.sh"
echo "  2. Deploy services: ./k8s/scripts/deploy-services.sh"
echo "  3. Access dashboard: minikube dashboard"
echo "  4. Tunnel for LoadBalancer: minikube tunnel (in separate terminal)"
