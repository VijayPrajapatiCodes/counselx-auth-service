# Monitoring
Auth Service exposes health/liveness/readiness through Actuator.
Deployment already has liveness and readiness probes.
Do not add a ServiceMonitor until Prometheus Operator is installed.
