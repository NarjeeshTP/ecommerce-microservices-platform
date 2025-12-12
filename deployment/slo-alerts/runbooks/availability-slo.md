# Availability SLO Breach Runbook
**Alert:** SLOAvailabilityBudgetBurnRateCritical  
**Severity:** Critical  
**SLO Target:** 99.9% availability
---
## Symptoms
- Error rate > 5% for 2+ minutes
- SLO dashboard showing red availability metric
- User reports of "500 Internal Server Error"
- Slack alert in #incidents channel
## Impact
**Business:**
- Users unable to complete transactions
- Revenue loss
- Reputation damage
**Technical:**
- Error budget burning rapidly
- May exhaust monthly budget within hours
---
## Investigation
### 1. Identify Failing Service(s)
```bash
# Check error rate by service
kubectl logs -n platform-core --selector=app.kubernetes.io/name=platform \
  --tail=100 --timestamps | grep "ERROR"
# Prometheus query
sum by (job) (rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
```
### 2. Check Recent Changes
```bash
# List recent deployments
helm list -n platform-core --date | head -5
# Check rollout status
kubectl rollout history deployment -n platform-core
# Git commits in last hour
git log --since="1 hour ago" --oneline
```
### 3. Check Dependencies
```bash
# Database
kubectl get pods -n platform-infra -l app=postgres
# Kafka
kubectl get pods -n platform-infra -l app=kafka
# Redis
kubectl get pods -n platform-infra -l app=redis
```
---
## Mitigation Steps
### Option 1: Rollback Recent Deployment
```bash
# Identify last deployment
helm list -n platform-core
# Rollback
helm rollback <service-name> -n platform-core
# Verify
kubectl rollout status deployment/<service-name> -n platform-core
curl http://<service-name>.platform-core:8080/actuator/health
```
### Option 2: Scale Up (if Resource Constrained)
```bash
# Check resource usage
kubectl top pods -n platform-core
# Scale up
kubectl scale deployment/<service-name> --replicas=5 -n platform-core
# Monitor
watch kubectl get pods -n platform-core
```
### Option 3: Enable Circuit Breaker
```bash
# Apply circuit breaker policy
kubectl apply -f deployment/k8s/service-mesh/resilience-policies/circuit-breaker.yaml
# Verify in Kiali
istioctl dashboard kiali
# Navigate to Graph → Check for circuit breaker icon
```
---
## Verification
```bash
# Check error rate dropped
# Prometheus query:
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) < 0.01
# Check SLO dashboard
# Grafana → SLO Overview → Error Rate should be < 1%
# Check user-facing health
curl https://api.yourcompany.com/health
```
---
## Communication
1. **Update Status Page**
   ```bash
   # If using StatusPage.io or similar
   curl -X POST https://api.statuspage.io/v1/pages/PAGE_ID/incidents \
     -H "Authorization: OAuth YOUR_TOKEN" \
     -d "name=Service Degradation" \
     -d "status=investigating"
   ```
2. **Notify Team**
   - Post in #incidents Slack channel
   - Update incident ticket
   - Notify on-call manager if > 15 min
3. **Customer Communication**
   - If > 5 min, tweet status update
   - If > 15 min, send email to affected users
---
## Root Cause Analysis
After mitigation, schedule blameless post-mortem:
1. Timeline of events
2. Root cause identification
3. Detection time analysis
4. Mitigation effectiveness
5. Action items (prevent recurrence)
**Post-mortem template:** `docs/incidents/YYYY-MM-DD-availability-breach.md`
---
## Related Alerts
- SLOErrorBudgetBurnFast
- CheckoutJourneyFailureRateHigh
- DatabaseSlowQueries
---
**Last Updated:** Dec 12, 2025  
**Owner:** Platform SRE Team
