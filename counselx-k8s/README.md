# CounselX Kubernetes Base

Includes Namespace, Auth Service Deployment/Service/ConfigMap, Secret template,
Ingress, and monitoring notes.

## Before apply
1. Build/push the Auth Service image and replace `image:` in deployment.yaml.
2. Fill the Secret template privately. Never commit real secrets.
3. Ensure Kubernetes can reach AWS RDS and AWS Valkey.
4. Install NGINX Ingress before applying ingress.yaml.

## Apply
kubectl apply -f namespace.yaml
kubectl apply -f secrets/secret-template.yaml
kubectl apply -f auth-service/configmap.yaml
kubectl apply -f auth-service/service.yaml
kubectl apply -f auth-service/deployment.yaml
kubectl apply -f ingress/ingress.yaml

## Verify
kubectl get pods -n counselx
kubectl get svc -n counselx
kubectl get ingress -n counselx
kubectl rollout status deployment/auth-service -n counselx
kubectl logs -f deployment/auth-service -n counselx
