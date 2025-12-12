#!/bin/bash
set -e
echo "🚀 Installing Istio Service Mesh on Kind Cluster"
# Check if istioctl is installed
if ! command -v istioctl &> /dev/null; then
    echo "❌ istioctl is not installed."
    echo "Install it from: https://istio.io/latest/docs/setup/getting-started/#download"
    echo ""
    echo "Quick install:"
    echo "  curl -L https://istio.io/downloadIstio | sh -"
    echo "  cd istio-*"
    echo "  export PATH=\$PWD/bin:\$PATH"
    exit 1
fi
# Check Kubernetes connection
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Cannot connect to Kubernetes cluster"
    echo "Run: ./k8s/scripts/setup-kind.sh first"
    exit 1
fi
echo "📦 Installing Istio with demo profile..."
istioctl install --set profile=demo -y
echo "⏳ Waiting for Istio components..."
kubectl wait --for=condition=available --timeout=300s \
  deployment/istiod -n istio-system
kubectl wait --for=condition=available --timeout=300s \
  deployment/istio-ingressgateway -n istio-system
echo "🏷️  Enabling automatic sidecar injection for namespaces..."
kubectl label namespace platform-core istio-injection=enabled --overwrite
kubectl label namespace platform-system istio-injection=enabled --overwrite
kubectl label namespace platform-infra istio-injection=enabled --overwrite
echo "📊 Installing Istio addons (Prometheus, Grafana, Kiali, Jaeger)..."
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/prometheus.yaml
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/grafana.yaml
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/kiali.yaml
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/jaeger.yaml
echo "⏳ Waiting for addons..."
sleep 10
kubectl rollout status deployment/kiali -n istio-system --timeout=300s
kubectl rollout status deployment/prometheus -n istio-system --timeout=300s
echo ""
echo "✅ Istio installation complete!"
echo ""
echo "Istio components:"
kubectl get pods -n istio-system
echo ""
echo "Labeled namespaces:"
kubectl get namespace -L istio-injection | grep platform
echo ""
echo "Next steps:"
echo "  1. Apply resilience policies: kubectl apply -f k8s/service-mesh/resilience-policies/"
echo "  2. Restart pods to inject sidecars: kubectl rollout restart deployment -n platform-core"
echo "  3. Access Kiali dashboard: istioctl dashboard kiali"
echo "  4. Access Grafana: istioctl dashboard grafana"
echo "  5. Access Jaeger: istioctl dashboard jaeger"
