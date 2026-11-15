#!/usr/bin/env bash
# Builds all six service images with the tags and actuator ports expected by deploy/k8s.
# Usage: ./deploy/k8s/build-images.sh [tag]   (default tag: 0.2.0)
set -euo pipefail

tag="${1:-0.2.0}"

services=(
  "identity:8081"
  "patient:8082"
  "catalog:8083"
  "intake:8084"
  "advice:8085"
  "gateway:8080"
)

for entry in "${services[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  echo "Building sovmestim-${name}:${tag} (MODULE=services/${name}, HEALTH_PORT=${port})"
  docker build \
    --build-arg MODULE="services/${name}" \
    --build-arg HEALTH_PORT="${port}" \
    -t "sovmestim-${name}:${tag}" \
    .
done

echo "Done. Load the images into your cluster (kind load / minikube image load) before applying."
