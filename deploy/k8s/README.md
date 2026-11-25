# Kubernetes deployment

Declarative manifests for the six services and their six PostgreSQL databases, rendered with
Kustomize. Everything is namespaced, resource-limited, health-checked, non-root and segmented with
NetworkPolicies.

```
deploy/k8s/
├── namespace.yaml       # sovmestim, Pod Security Admission "restricted"
├── secrets.yaml         # dev-only credentials (replace in production)
├── configmaps.yaml      # non-secret env per service, including gRPC service discovery
├── postgres.yaml        # 6 headless Services + StatefulSets with 1Gi PVCs
├── apps/                # ServiceAccount + Service + Deployment per service
├── networkpolicies.yaml # default deny + explicit call paths
├── ingress.yaml         # optional, requires ingress-nginx
├── kustomization.yaml
└── build-images.sh
```

## Prerequisites

- A cluster (kind, minikube, k3s or a cloud cluster) and `kubectl` 1.29+.
- The six images available to the cluster. The default tag is `0.2.0` (see `kustomization.yaml`).

## 1. Build and load images

```bash
./deploy/k8s/build-images.sh 0.2.0

# kind
kind create cluster --name sovmestim
for s in identity patient catalog intake advice gateway; do
  kind load docker-image "sovmestim-${s}:0.2.0" --name sovmestim
done

# minikube
minikube start
for s in identity patient catalog intake advice gateway; do
  minikube image load "sovmestim-${s}:0.2.0"
done
```

If you use a registry, push the images and set `newName` in `kustomization.yaml` (or
`kustomize edit set image`).

## 2. Deploy

```bash
kubectl apply -k deploy/k8s

kubectl -n sovmestim rollout status statefulset/db-identity
kubectl -n sovmestim rollout status deployment/identity
# ... repeat, or watch everything:
kubectl -n sovmestim get pods -w
```

Flyway migrates each database on service startup. The catalog demo seed fills the catalog; the
advice service seeds the danger levels.

## 3. Access the API

```bash
kubectl -n sovmestim port-forward svc/gateway 8080:8080
# then, in another shell:
curl -s localhost:8080/actuator/health
curl -s -X POST localhost:8080/v1/auth/request-code \
  -H 'Content-Type: application/json' -d '{"email":"demo@example.com"}'
```

The `Ingress` manifest routes `api.sovmestim.local` to the gateway when an ingress-nginx
controller is installed. Otherwise use the port-forward above.

## Configuration and secrets

`secrets.yaml` contains development values. In a real cluster replace it with an `ExternalSecret`,
a `SealedSecret` or a CSI secret store. Two rules matter:

1. `SOVMESTIM_JWT_SECRET` must be identical in `identity` (signs) and `gateway` (verifies).
2. `POSTGRES_USER` / `POSTGRES_PASSWORD` are consumed by every database StatefulSet and by every
   service, so rotate them together.

`SOVMESTIM_RLS_API_KEY` is only used when `SOVMESTIM_RLS_MODE=http`; the default is `demo`.

Non-secret settings live in `configmaps.yaml`. Service discovery is plain Kubernetes DNS
(`catalog:9093`, `patient:9092`, `intake:9094`, `advice:9095`, `identity:9091`).

## Storage

Each database has a `volumeClaimTemplates` request of `1Gi` and uses the cluster's default
StorageClass. Change the size or class in `postgres.yaml` for real workloads. For production
databases prefer a managed PostgreSQL or an operator (CloudNativePG, Zalando) instead of these
single-replica StatefulSets.

## Security posture

- Pod Security Admission `restricted` on the namespace.
- All pods: non-root, `seccompProfile: RuntimeDefault`, no privilege escalation, all capabilities
  dropped. Application containers also use a read-only root filesystem with an `emptyDir` for
  `/tmp`.
- Dedicated ServiceAccounts with token automounting disabled; the applications call no Kubernetes
  API, so no Role/RoleBinding is granted.
- NetworkPolicies deny all ingress and egress, then allow DNS, each service's own database, and
  only the gRPC calls the architecture requires. PostgreSQL pods have no egress.
- No `latest` image tags.

## Scaling and availability

Each Deployment is stateless and can be scaled with `kubectl scale`. Keep `gateway` at **one**
replica until the sync idempotency path is made race-free across replicas (the current insert of
`sync_request` relies on a unique constraint, so two simultaneous pushes with the same key can end
in one 500 instead of the stored response). The domain services can scale horizontally because all
state is in their databases.

Rollback:

```bash
kubectl -n sovmestim rollout undo deployment/advice
kubectl -n sovmestim rollout status deployment/advice
```

## Validation

The manifests are validated in CI with:

```bash
kubectl kustomize deploy/k8s > /tmp/rendered.yaml
docker run --rm -v /tmp/rendered.yaml:/rendered.yaml \
  ghcr.io/yannh/kubeconform:latest -strict -summary /rendered.yaml
```

## Teardown

```bash
kubectl delete -k deploy/k8s
# the namespace also removes the PVCs created by the StatefulSets
```
